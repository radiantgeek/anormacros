package ru.radiant.anormacro

import java.sql.Connection
import scala.util.control.{NonFatal, ControlThrowable}
import play.api.db.DB
import play.api.Play.current


trait ConnSource {
  def withConnection[A](block: Connection => A): A
  def withTransaction[A](block: Connection => A): A
}

/* get and release connection from connection pool for each time */
class PlayConnection(val name: String = "default") extends ConnSource {
  def withConnection[A](block: Connection => A): A = DB.withConnection(name)(block)
  def withTransaction[A](block: Connection => A): A = DB.withTransaction(name)(block)
}


/* use implicit connection from other part code */
class JavaConnection(implicit val connection: java.sql.Connection) extends ConnSource {

  def withConnection[A](block: Connection => A): A = {
    try {
      block(connection)
    } finally {
      //      connection.close()
    }
  }

  def withTransaction[A](block: Connection => A): A = withConnection { connection =>
    try {
      connection.setAutoCommit(false)
      val r = block(connection)
      connection.commit()
      r
    } catch {
      case e: ControlThrowable => connection.commit(); throw e
      case NonFatal(e) => connection.rollback(); throw e
    }
  }
}
