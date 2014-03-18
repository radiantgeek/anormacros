package ru.radiant.anormacro.sql

import anorm.SqlParser._
import ru.radiant.anormacro.ConnSource

trait Counters extends Selector {

  implicit def tableName: String

  def count(sql: String, params: anorm.ParameterValue[_]*)(implicit cs: ConnSource): Long =
    selectOne(sql, scalar[Long], params :_*).get

  def count(implicit cs: ConnSource): Long = count("select count(*) from "+tableName)

  def countFor(where: String, params: anorm.ParameterValue[_]*)(implicit cs: ConnSource): Long =
    count("select count(*) from "+tableName+where, params :_*)

  def countOn(sql: String, params: (scala.Any, anorm.ParameterValue[_])*)(implicit cs: ConnSource): Long =
    selectOn(sql, scalar[Long], params :_*).head

}
