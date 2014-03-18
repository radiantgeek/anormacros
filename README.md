AnorMacro [Anorm macros magic]
=====================================

This library is utility to bring some syntax sugar to Play Anorm.

It was inspired by [old Anorm Magic](http://www.playframework.com/modules/scala-0.9.1/anorm) variant.
Unfortunately, since 2.x version [there is no](http://www.playframework.com/documentation/2.2.x/ScalaAnorm) such magic feature.
So current realization can cause [boilerplate code to use parsers](http://stackoverflow.com/questions/13034383/tool-to-automatically-generate-anorm-parser-combinators)


But since Scala 2.10 we have **Scala macros**!
With its help I tried to simplify working with parser and table.

Setup
-------------------------------------

Add library to your `build.sbt`:

```
    resolvers ++= Seq(
      "Local Play Repository" at "file://home/radiant/play/play-2.2.0/repository/local"
    )
    
    libraryDependencies ++= Seq(
      "anorm-macros" % "anorm-macros_2.10" % "1.0-SNAPSHOT",
    )     
```


Note
-------------------------------------

This library heavy use [quasiquotes](http://docs.scala-lang.org/overviews/macros/quasiquotes.html) and [macro annotations](http://docs.scala-lang.org/overviews/macros/annotations.html).
To use it you will need [macro paradise extension](http://docs.scala-lang.org/overviews/macros/paradise.html). 

Just add following lines to `build.sbt`:
```
    resolvers += Resolver.sonatypeRepo("snapshots")
    addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)
```

The library was tested for Scala 2.10.2, sbt 0.13.0.

Usage
-------------------------------------
At first, you must setup you DAO object:
```
    import ru.radiant.anormacro._

    @Table
    case class Person(id: Pk[Long], @Column("full_name") name: String, @Parser("int") age: Int)

    object Person extends SimpleTable[Person]("table_name")
```

So you have to:

1.  import package
2.  add `@Table` annotation to case class (`@Column` and `@Parser` annotations are optional)
3.  Extend object companion with `SimpleTable` abstract class and specify table name

After macros execution on compilation phase you can use:

* `&` - helper object:
```
    object & {
        val id   = id("table_name.id")
        val name = str("table_name.full_name")
        val age  = int("table_name.age")
    }
```

* `*` - default parser  for all columns:
```
    def * = &.id ~ &.name ~ &.age map(flatten) map(t => (Person.apply _).tupled(t))
```

* all helper methods from SimpleTable as usual :)

Helper methods
-------------------------------------

#### list
```
    def all: List[T]
    def all(where: String, params: anorm.ParameterValue[_]*): List[T]
    def allOn(where: String, params: (scala.Any, anorm.ParameterValue[_])*): List[T]
```

#### finders
```
    def getBy(where: String, params: anorm.ParameterValue[_]*): Option[T]
```
Feel free to create your own finders.
For example:
```
    def getById(id: Long): Option[T]
    def getById(id: Pk[Long]): Option[T]
```


Connection
-------------------------------------

To simplify Or you can use defaults ConnSource:
```
    import ru.radiant.anormacro.conf.Defaults._

    Person.all()
    Person.count()
```

It will cause running `DB.withConnection { .. }` for each db request.
So it will be get connection from default connection pool.

To use it more thrifty we can
Usage in program:
```
    DB.withConnection { s =>
        implicit cs: ConnSource = JavaConnection(s)

        Person.count()
        Person.all()
    }
```
It will use one connection for all requests.

Same way for transaction:
```
    DB.withTransaction { s =>
        implicit cs: ConnSource = JavaConnection(s)

        Person.count()
        Person.all()
    }
```
