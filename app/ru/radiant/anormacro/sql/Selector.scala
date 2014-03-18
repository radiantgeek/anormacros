package ru.radiant.anormacro.sql

import anorm._
import ru.radiant.anormacro.ConnSource

//   utils
trait Selector {

  //
  def selectOn[A](sql: String, parser: RowParser[A],
                  params: (scala.Any, anorm.ParameterValue[_])*) (implicit cs: ConnSource) : List[A]
  = cs.withConnection { implicit c =>
      SQL(sql).on(params :_*).as(parser *)
  }

  def select[A](sql: String, parser: RowParser[A],
                params: anorm.ParameterValue[_]*) (implicit cs: ConnSource) : List[A]
  = cs.withConnection { implicit c =>
      SQL(sql).onParams(params :_*).as(parser *)
  }

  def selectOne[A](sql: String, parser: RowParser[A],
                   params: anorm.ParameterValue[_]*) (implicit cs: ConnSource) : Option[A]
  = cs.withConnection { implicit c =>
      SQL(sql).onParams(params :_*).as(parser.singleOpt)
  }

  /*
    JDBC (so am anorm) can't use PredefinedStatement for list of values of the one parameter.
    For example, can't just write: WHERE blabla IN ({list})
    so we have to create 'formal'  parameter list, like: WHERE blabla IN (listValue0, listValue1, .., listValueN)
  */

  //  def valuesList(prefix: String, values: Any*): (String, Seq[(String, anorm.ParameterValue[_])]) = {
  //    val paramsList = for ( i <- 0 until values.size ) yield (prefix + i)
  //    val where = "{"+paramsList.mkString("},{")+"}"
  //    val anormValues  = values.map(toParameterValue(_))
  //    val valuesMap = paramsList.zip(anormValues)
  //
  //    (where, valuesMap)
  //  }

}
