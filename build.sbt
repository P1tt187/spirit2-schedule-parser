name := "spirit2-schedule-parser"

version := "1.0."+ sys.env.get("BUILD_NUMBER").getOrElse("00")

scalaVersion := "2.11.8"

enablePlugins(LinuxPlugin)


resolvers ++= Seq("snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "http://oss.sonatype.org/content/repositories/releases"
)

resolvers += Resolver.sonatypeRepo("public")


//Define dependencies. These ones are only required for Test and Integration Test scopes.
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.6.4" % "test",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.jsoup" % "jsoup" % "1.8.3" % "compile->default" withSources,
  "joda-time" % "joda-time" % "2.9.4" withSources()
)


// For Settings/Task reference, see http://www.scala-sbt.org/release/sxr/sbt/Keys.scala.html

// Compiler settings. Use scalac -X for other options and their description.
// See Here for more info http://www.scala-lang.org/files/archive/nightly/docs/manual/html/scalac.html
scalacOptions ++= List("-feature","-deprecation", "-unchecked", "-Xlint","-deprecation","-language:postfixOps", "-language:implicitConversions","-Yrangepos")

// ScalaTest settings.
// Ignore tests tagged as @Slow (they should be picked only by integration test)
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-l", "org.scalatest.tags.Slow", "-u","target/junit-xml-reports", "-oD", "-eS")