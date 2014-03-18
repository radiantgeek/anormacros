package ru.radiant.anormacro

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import ru.radiant.anormacro.parser.Parsers

// ------------------------------------------------------------------------------------------------------------

class Table(val tablename: String, keys: Seq[String], cachedBy: String = "") extends StaticAnnotation {

  def macroTransform(annottees: Any*) = macro Macros.annotation_impl
}

class Column(val name: String) extends StaticAnnotation { }

// ------------------------------------------------------------------------------------------------------------

object Macros {

  // ------------------------------------------------------------------------------------------------------------
  //  implementation for: @Table case class T(...)

  def annotation_impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // ------------------------------------------------------------------------------------------------------------
    //  utils

    def findColumnAnnotation(name: TermName, typ: Ident, mods: Modifiers) = {
      var column = name.toString

      mods.annotations.map { z =>
        z match {
          case q"""new Column($text)""" => {
            val Literal(Constant(z)) = text
            column = z.toString
          }
          case _ => ;
        }
      }

      column
    }

    case class Column(name: TermName, column: String, _def: ValDef)

    def parseColumns(body: List[Tree]) = {
      val re = "Pk\\[.+\\]".r
      var pkColumn: Option[TermName] = None
      val columns = body.map { t =>
        t match {
          case ValDef(mods, name, typ, b) => {
            val column = findColumnAnnotation(name, typ.asInstanceOf[Ident], mods)

            if (re.pattern.matcher(typ.toString()).matches) {
              pkColumn = Some(name)
            }
            Column(name, column, ValDef(mods, name, typ, b))
          }
        }
      }
      (pkColumn, columns)
    }

    //
    def generateCompanion(tName: TermName, comp: List[Tree], parserDef: List[Tree], parents:List[Tree]=List()): ModuleDef = {
      val o = comp match {
        case Nil => c.abort(c.enclosingPosition, "Companion object for "+tName+" case class was not found")
        case moduleDef => moduleDef.head.asInstanceOf[ModuleDef]
      }

      ModuleDef(o.mods, o.name, Template(o.impl.parents ::: parents, o.impl.self, o.impl.body ::: parserDef))
    }

    // ------------------------------------------------------------------------------------------------------------

    def generateColumnParsers(columns: List[Column]): List[Tree] = {
      var res = List[Tree]()

      var helper = columns.map {
        case Column(nam, column, _def) => {
          val typ = _def.tpt
          q"""val $nam = parsers.get[$typ]($column); """
        }
      }

      res ::= q""" object & {  ..$helper }; """

      helper = columns.map {
        case Column(nam, column, _def) => {
          val typ = _def.tpt
          q"""val $nam = parsers.get[Option[$typ]]($column); """
        }
      }

      res ::= q""" object | {  ..$helper }; """

      res
    }


    def parserString(obj:Tree, columns: List[Column]): Tree = {
      val parsersList: List[Tree] = columns.map(_.name).map { nam => { q"$obj.$nam" } }

      parsersList.reduceLeft[Tree] {
        case (s, el) => Apply( Select(s, newTermName("$tilde")), List(el) )
      }
    }

    //
    def generateDefaultParser(columns: List[Column], tName: TermName): List[Tree] = {
      var res = List[Tree]()

      var parser = parserString(q"&", columns)

      res ::= q""" override def * = {
            val list = $parser
            list map(flatter) map(t => ($tName.apply _).tupled(t))
      } """

      parser = parserString(q"|", columns)

      val tupleList = 1.to(columns.size).map( n =>
        Select(Select(Ident(newTermName("t")), newTermName("_"+n)), newTermName("get"))
      )
      val tuple = Apply(Ident(newTermName("Tuple"+columns.size)), tupleList.toList)

      res ::= q""" override def *? = {
            val list = $parser
            list map(flatter) map {  t =>
              val nul = t._1.isEmpty
              if (nul) None
              else {
                val r = $tuple
                val z = ($tName.apply _).tupled( r )
                Option( z )
              }
            }
      } """

      res
    }

    def generateCreate(list: List[Column], names: List[String], typ: TypeName) = {
      val terms = list.map(_.name.toTermName)
      val values = terms.map { term => q"v.$term" }
      val params = List(Literal(Constant(names.mkString(", ")))) ::: values

      q""" override def create(v: $typ)(implicit cs: ConnSource): Option[$typ] = { Some(v.copy(id = createRecord(..$params))) } """
    }

    def generateUpdate(list: List[Column], names: List[String], typ: TypeName) = {
      val terms = list.map(_.name.toTermName)
      val values = terms.map { term => q"v.$term" }
      val params = List(Literal(Constant(names.mkString(", ")))) ::: values

      q""" override def update(v: $typ)(implicit cs: ConnSource): Option[$typ] = { Some(v.copy(id = updateRecord(..$params))) } """
    }

    def generateWhere(list: List[Column], names: List[String], typ: TypeName) = {
      val terms = list.map(_.name.toTermName)
      val values = terms.map { term => q"v.$term" }
      val params = List(Literal(Constant(names.mkString(", ")))) ::: values

      q""" override def _where(v: $typ)(implicit cs: ConnSource): (String, ) = { Some(v.copy(id = updateRecord(..$params))) } """
    }

    //
    def generateRecordHelpers(list: List[Column], tName: TermName,
                              pkColumn: Option[TermName], keys: List[String]): List[Tree] = {
      var res = List[Tree]()
      val typ = tName.toTypeName
      val names = list.map(_.column)
      val map = list map {c => (c.name.toString, c)} toMap
      def colName(key: String) = map.get(key).get.column

      if (pkColumn.nonEmpty) {
        res ::= generateCreate(list, names, typ)
      }

      val sql = keys.map(n => colName(n)+"={"+n+"}").mkString(" AND ")
      val gets = keys.map { n => val k=newTermName(n); q"v.$k" }
      res ::= q"override def getBy(v: $typ)(implicit cs: ConnSource): Option[$typ] = getBy($sql, ..$gets)"

      val defs: List[ValDef] = list.map(_._def).map { f => ValDef(NoMods, f.name, f.tpt, EmptyTree) } // without default values
      var defValues = list.map(_.name).map { n => Ident(n) }

      res ::= q" def sure(..$defs)(implicit cs: ConnSource): $typ = { sure($tName(..$defValues)) } "

      if (pkColumn.nonEmpty) {
        val filtered = defs.filter(_.name != pkColumn.get)
        defValues = List(q"none") ::: defValues.tail
        res ::= q" def sure(..$filtered)(implicit cs: ConnSource): $typ = { sure($tName(..$defValues)) } "
      }

      res
    }

    //
    def generateCacheHelpers(tName: TermName, colName: String): List[Tree] = {
      var res = List[Tree]()
      val typ = tName.toTypeName
      val column = newTermName(colName)

      res ::= q" override def sure(v: $typ)(implicit parser: anorm.RowParser[$typ], cs: ConnSource): $typ = make_sure(v, test(getBy, _.$column), cache(create, _.$column)) "

      res ::= q" override def load(implicit cs: ConnSource) = loadByFunc(_.$column) "

      res
    }

    // ------------------------------------------------------------------------------------------------------------
    //  work

    val inputs = annottees.map(_.tree)

    def aKeys(keySeq: Tree): List[String] = (keySeq
    match {
        case AssignOrNamedArg(q"keys", q"Seq(..$mkeys)") => mkeys
        case q"Seq(..$mkeys)" => mkeys
        case _ => c.abort(c.enclosingPosition, "Please specify list of keys for record management. For example, just [keys=Seq('id')]")
      }).map(_.toString() filterNot (_ == '\"'))

    def aCacheKey(cacheKeys: Tree) = (cacheKeys match {
      case AssignOrNamedArg(q"cachedBy", q"$mkeys") => mkeys
      case q"$mkeys" => mkeys
    }).toString.replace("\"", "")

    val expandees = inputs match {
      case q"case class $name ( ..$body ) { ..$methods }" :: comp => {
        val tName = name.toTermName

        val caseClass :: comp = inputs

        val (tableName, keySeq, cachedBy: Option[String]) = c.prefix.tree match {
          case q"new Table($table, $seq, $cacheKeys)"  => (table, seq, Some(aCacheKey(cacheKeys)))
          case q"new Table($table, $seq)"              => (table, seq, None)
        }
        val tableDef = q"implicit override def tableName = $tableName"
        val keys = aKeys(keySeq)

        val (pkColumn, columns) = parseColumns(body)

        var tree = List[Tree](tableDef)
        tree :::= generateColumnParsers(columns)
        tree :::= generateDefaultParser(columns, tName)
        tree :::= generateRecordHelpers(columns, tName, pkColumn, keys)

        var newParents = List[Tree]()
        if (cachedBy.isDefined) {
          tree :::= generateCacheHelpers(tName, cachedBy.get)
          newParents ::= AppliedTypeTree(Ident(newTypeName("CachedTable")), List(Ident(newTypeName("String")), Ident(name)))
        }

        val newCompanion: ModuleDef = generateCompanion(tName, comp, tree.reverse, newParents)

        c.echo(c.enclosingPosition, "AnorMacro successfully updated @Table case class %s".format(name))

        List(caseClass, newCompanion)
      }
      case _ => c.abort(c.enclosingPosition, "This annotation is only supported on case class")
    }

    // ------------------------------------------------------------------------------------------------------------
    //  result

    c.info(null, expandees.toString(), false)

    c.Expr[Any](Block(expandees, Literal(Constant(()))))
  }


}
