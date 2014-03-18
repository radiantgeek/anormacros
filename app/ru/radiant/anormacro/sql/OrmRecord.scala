package ru.radiant.anormacro.sql

import anorm._
import anorm.SqlParser._
import anorm.SimpleSql
import ru.radiant.anormacro.ConnSource

trait OrmRecord[T] {

  implicit def tableName: String

  // ------------------------------------------------------------------------------------------------------------
  //   work with objects

  def hasRecord(id: Long)(implicit cs: ConnSource): Boolean = cs.withConnection { implicit c =>
    SQL("SELECT COUNT(*) FROM "+tableName+" WHERE id={id}").onParams(id).as(scalar[Long].single) == 1
  }

  def createRecord(columns: String, params: anorm.ParameterValue[_]*)(implicit cs: ConnSource): Pk[Long] = {
    val sql = " INSERT INTO "+tableName+"("+columns+") VALUES ({"+columns.replaceAll(", ", "}, {")+"}) "
    Id(createSql(sql, params :_*))
  }

  def createSql(sql: String, params: anorm.ParameterValue[_]*)(implicit cs: ConnSource): Long = cs.withConnection { implicit c =>
    val a: SimpleSql[Row] = SQL(sql).onParams(params :_*)
    a.executeInsert[Option[Long]]() match {
      case Some(long) => long
      case None       => -1
    }
  }

  def make_sure(value: T, _get: (T)=>Option[T], _create: (T)=>Option[T])(implicit parser : RowParser[T], cs: ConnSource): T = _get(value) match {
    case Some(c) => c
    case None => _create(value).get
  }

  // ------------------------------------------------------------------------------------------------------------
  //  will be auto overridden by macros:

  def _where(v: T)(implicit cs: ConnSource): (String,) = None
  // will generate from @Table parameter 'keys':
  // * def getBy(v: T)(implicit cs: ConnSource): Option[T] = getBy("time={time} AND param={param} ", v.time, v.param)

  def getBy(v: T)(implicit cs: ConnSource): Option[T] = None
  // will generate from @Table parameter 'keys':
  // * def getBy(v: T)(implicit cs: ConnSource): Option[T] = getBy("time={time} AND param={param} ", v.time, v.param)

  def create(v: T)(implicit cs: ConnSource): Option[T] = None
  // will generate for parameters of case class:
  // * def create(v: T)(implicit cs: ConnSource): T = v.copy(id = createRecord("id, time, param, value", v.id, v.time, v.param, v.value))

  def sure(v: T)(implicit parser : RowParser[T], cs: ConnSource): T = make_sure(v, getBy, create)
  // will generate for parameters of case class:
  // * def sure(time: Long, param: String, value: Double)(implicit cs: ConnSource): T = sure(T(none, time, param, value))
  // * def sure(id: Pk[Long], time: Long, param: String, value: Double)(implicit cs: ConnSource): T = sure(T(id, time, param, value))

}
