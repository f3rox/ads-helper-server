name := "server"

version := "1.0"

lazy val `server` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.7"
//libraryDependencies ++= Seq(jdbc, ehcache, ws, guice)
libraryDependencies ++= Seq(ws, guice)
libraryDependencies += "com.google.api-ads" % "google-ads" % "4.1.0"
libraryDependencies += "com.google.api-client" % "google-api-client" % "1.30.2"
libraryDependencies ++= List(
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "org.slf4j" % "slf4j-nop" % "1.7.26",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc42"
)
libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0"
scalacOptions += "-Ypartial-unification"