package ru.radiant.anormacro.sql

import ru.radiant.anormacro.ConnSource
import anorm._

trait Getters[T] {

  implicit def tableName: String
  implicit def `*`: RowParser[T]

  def getById(id: Long)(implicit cs: ConnSource): Option[T] = getBy("id = {id}", id)(*, cs)
  def getById(id: Pk[Long])(implicit cs: ConnSource): Option[T] = getById(id.get)
  def getById(t: T=>Pk[Long])(obj: T)(implicit cs: ConnSource): Option[T] = getById(t(obj))

  def getBy(where: String, params: anorm.ParameterValue[_]*)(implicit parser : RowParser[T], cs: ConnSource): Option[T] =
    cs.withConnection { implicit c =>
      SQL("SELECT * FROM "+tableName+" WHERE "+where).onParams(params :_*).as(parser.singleOpt)
    }

}