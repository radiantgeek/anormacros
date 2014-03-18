package ru.radiant.anormacro

import anorm._
import ru.radiant.anormacro.sql._
import ru.radiant.anormacro.parser.Parsers

// ------------------------------------------------------------------------------------------------------------

abstract class SimpleTable[T] extends Selector with Counters with Updater with OrmRecord[T] with Getters[T] {

  val none = anorm.NotAssigned

  implicit def tableName: String = ""

  val parsers = new Parsers {
    val tablePrefix = tableName+"."
  }

  // mock implementation, just to skip IDE's warning
  def `*`: RowParser[T] = mockParser              // default parser
  def `*?`: RowParser[Option[T]] = mockParser     // useful parser for for LEFT JOIN

  // ------------------------------------------------------------------------------------------------------------
  //   work with object lists

  def all(implicit cs: ConnSource): List[T] = all(*)

  def all(parser: RowParser[T], where: String="")(implicit cs: ConnSource): List[T] =
    select[T]("SELECT * FROM "+tableName+" "+where, parser)

  def all(where: String, params: anorm.ParameterValue[_]*)(implicit parser : RowParser[T], cs: ConnSource): List[T] =
    select[T]("SELECT * FROM "+tableName+" "+where, parser, params :_*)

  def allOn(where: String, params: (scala.Any, anorm.ParameterValue[_])*)(implicit parser : RowParser[T], cs: ConnSource): List[T] =
    selectOn[T]("SELECT * FROM "+tableName+" "+where, parser, params :_*)


  // ------------------------------------------------------------------------------------------------------------
  //  utils

  def mockParser[Z] =  new RowParser[Z] {
    def apply(row: Row): SqlResult[Z] = Error(UnexpectedNullableFound("SimpleTable's mock implementation - check for problems with macros."))
  }

  // to skip importing from real anorm
  def flatter[T1, T2, R](implicit f: anorm.TupleFlattener[(T1 ~ T2) => R]) = anorm.SqlParser.flatten

}