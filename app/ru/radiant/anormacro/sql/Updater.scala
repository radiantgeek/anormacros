package ru.radiant.anormacro.sql

import anorm._
import ru.radiant.anormacro.ConnSource

trait Updater {

  implicit def tableName: String

  def delete(id: Long)(implicit cs: ConnSource) = cs.withConnection { implicit c =>
    SQL("DELETE FROM "+tableName+" WHERE id = {id}").onParams(id).executeUpdate()
  }

  def clear(implicit cs: ConnSource) = cs.withConnection { implicit c =>
    SQL("DELETE FROM "+tableName).executeUpdate()
  }

  // ------------------------------------------------------------------------------------------------------------
  //  updaters

  def updateRecord(columns: String, where: String, params: anorm.ParameterValue[_]*)(implicit cs: ConnSource): Int = {
    val _columns = columns.split(", ").map(c => c+"={"+c+"}" )
    val sql = " UPDATE "+tableName+" SET "+_columns.mkString(", ")+" WHERE "+where
    updateSql(sql, params :_*)
  }

  def updateById(columns: String, params: anorm.ParameterValue[_]*)(implicit cs: ConnSource): Int = updateRecord(columns, "id={id}", params :_*)

  def updateSql(sql: String, params: anorm.ParameterValue[_]*)(implicit cs: ConnSource): Int = cs.withConnection { implicit c =>
    SQL(sql).onParams(params :_*).executeUpdate()
  }



}
