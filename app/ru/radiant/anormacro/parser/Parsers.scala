package ru.radiant.anormacro.parser

import anorm._
import java.util.Date
import java.math.BigDecimal

//  column parsers for table
trait Parsers {

  implicit val tablePrefix: String
  
  def get[T](columnName:String)(implicit extractor : anorm.Column[T]): RowParser[T] = anorm.SqlParser.get(tablePrefix + columnName)

  def str(columnName:String): RowParser[String] = anorm.SqlParser.str(tablePrefix + columnName)

  def bool(columnName:String): RowParser[Boolean] = anorm.SqlParser.bool(tablePrefix + columnName)

  def int(columnName:String): RowParser[Int] = anorm.SqlParser.int(tablePrefix + columnName)

  def long(columnName:String): RowParser[Long] = anorm.SqlParser.long(tablePrefix + columnName)

  def date(columnName:String): RowParser[Date] = anorm.SqlParser.date(tablePrefix + columnName)


  def id(columnName:String="id")(implicit extractor : anorm.Column[Pk[Long]]) = get[Pk[Long]](columnName)(extractor)

  def decimal(columnName:String)(implicit extractor : anorm.Column[BigDecimal]) = get[BigDecimal](columnName)(extractor)

  def double(columnName:String)(implicit extractor : anorm.Column[Double]) = get[Double](columnName)(extractor)

}

//  column parsers for sql (add id and decimal to anorm.SqlParser)
object Parsers extends Parsers {
  val tablePrefix = ""

}
