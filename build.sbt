name := "anorm-macros"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

publishTo := Some(Resolver.file("file",  new File( "../github-repo" )) )

libraryDependencies ++= Seq(
  jdbc,
  anorm,
	"org.scala-lang" % "scala-reflect" % "2.10.2",
	"org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
)

play.Project.playScalaSettings

addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)
