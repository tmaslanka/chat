import sbt._
import sbtassembly.AssemblyPlugin.autoImport

enablePlugins(DockerPlugin)

name := "chat"
organization := "tmaslanka"
version := "1.0-SNAPSHOT"

scalaVersion := "2.12.7"
scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint", "-Ywarn-dead-code", "-Ywarn-unused-import", "-Xfatal-warnings")
scalacOptions in Test := Seq("-Yrangepos")
fork in Test := true

//skip tests during assembly
test in assembly := {}

assemblyJarName in assembly := "chat-assembly.jar"
mainClass in assembly := Some("tmaslanka.chat.Main")

resolvers := Seq(
  Resolver.defaultLocal,
  Resolver.mavenLocal,
  DefaultMavenRepository
)

publishMavenStyle := true
publishArtifact in Compile := true
publishArtifact in Test := false
exportJars := true

lazy val ItTest = config("it-test") extend Test
parallelExecution in ItTest := false
fork in ItTest := false


val akkaV = "2.5.21"
val akkaHttpV = "10.1.7"
val akkaCassandraV =  "0.93"
val cassandraLauncherV = "0.93"
val logbackV = "1.2.3"
val slf4jV = "1.7.25"
val scalaLoggingV = "3.9.0"
val typesafeConfigV = "1.3.2"
val phantomV = "2.9.2"
val catsV = "1.6.0"
val scalatestV = "3.0.5"
val restAssuredV = "3.3.0"


libraryDependencies ++= Seq(
  "com.typesafe.akka"           %% "akka-http" % akkaHttpV,
  "com.typesafe.akka"           %% "akka-http-spray-json" % akkaHttpV,
  "com.typesafe.akka"           %% "akka-actor" % akkaV,
  "com.typesafe.akka"           %% "akka-stream" % akkaV,
  "com.typesafe.akka"           %% "akka-cluster-sharding" % akkaV,
  "com.typesafe.akka"           %% "akka-persistence" % akkaV,
  "com.typesafe.akka"           %% "akka-persistence-query" % akkaV,
  "com.typesafe.akka"           %% "akka-persistence-cassandra"  % akkaCassandraV,
  "com.typesafe.akka"           %% "akka-slf4j" % akkaV,
  "com.typesafe.akka"           %% "akka-persistence-cassandra-launcher" % cassandraLauncherV,

  "com.outworkers"              %% "phantom-dsl" % phantomV,

  "org.typelevel"               %% "cats-core" % catsV,

  "ch.qos.logback"              %  "logback-classic" % logbackV,
  "org.slf4j"                   %  "slf4j-api" % slf4jV,
  "com.typesafe.scala-logging"  %% "scala-logging" % scalaLoggingV,

  "com.typesafe"                %  "config" % typesafeConfigV,

  "org.scalatest"               %% "scalatest" % scalatestV % "test,it-test",
  "io.rest-assured"             % "rest-assured" % restAssuredV % "test,it-test",
  "io.rest-assured"             % "scala-support" % restAssuredV % "test,it-test"
)

configs(ItTest)
inConfig(ItTest)(Defaults.testSettings)


assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
  case x => (assemblyMergeStrategy in assembly).value(x)
}

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.withClassifier(Some("assembly"))
}
addArtifact(artifact in (Compile, assembly), assembly)

dockerfile in docker := {
  // The assembly task generates a fat JAR file`
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8u191-jdk-alpine3.8")
    add(artifact, artifactTargetPath)
    expose(8080)
    entryPoint("java", "-jar", artifactTargetPath)

  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(version.value)
  )
)