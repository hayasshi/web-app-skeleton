import java.nio.file.Files

name := "web-app-skeleton"

organization := "com.example"

version := "0.1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yinline-warnings",
  "-Ywarn-dead-code",
  "-Xfuture"
)

val akkaVersion = "2.4.10"
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalikejdbc" %% "scalikejdbc" % "2.4.2",
  "com.h2database" %  "h2" % "1.4.192",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
  "com.bionicspirit" %% "shade" % "1.7.4"
)

flywayUrl := "jdbc:h2:file:./target/myApp"
flywayUser := "sa"
flywayPassword := "sa"
//flywaySchemas := Seq("myApp")
flywayLocations := Seq(s"filesystem:${baseDirectory.value}/src/main/resources/db/")
parallelExecution in Test := false
testOptions in Test ++= Seq(
  Tests.Setup { () =>
    flywayMigrate.value
  },
  Tests.Cleanup { () =>
    Files.deleteIfExists(new File(s"${baseDirectory.value}/target/myApp.mv.db").toPath)
  }
)
