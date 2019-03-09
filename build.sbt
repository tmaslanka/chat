import sbt._

enablePlugins(DockerPlugin)

name := "chat"
organization := "tmaslanka"
version := "1.0-SNAPSHOT"

scalaVersion := "2.12.7"
scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint", "-Ywarn-dead-code", "-Ywarn-unused-import")
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


val akkaV = "2.5.19"
val akkaHttpV = "10.1.7"
val logbackV = "1.2.3"
val slf4jV = "1.7.25"
val scalaLoggingV = "3.9.0"
val typesafeConfigV = "1.3.2"
val scalatestV = "3.0.5"
val restAssuredV = "3.3.0"


libraryDependencies ++= Seq(
  "com.typesafe.akka"           %% "akka-http" % akkaHttpV,
  "com.typesafe.akka"           %% "akka-http-spray-json" % akkaHttpV,
  "com.typesafe.akka"           %% "akka-actor" % akkaV,
  "com.typesafe.akka"           %% "akka-stream" % akkaV,

  "ch.qos.logback"              %  "logback-classic" % logbackV,
  "org.slf4j"                   % "slf4j-api" % slf4jV,
  "com.typesafe.scala-logging"  %% "scala-logging" % scalaLoggingV,

  "com.typesafe"                % "config" % typesafeConfigV,

  "org.scalatest"               %% "scalatest" % scalatestV % "test,it-test",
  "io.rest-assured"             % "rest-assured" % restAssuredV % "it-test",
  "io.rest-assured"             % "scala-support" % restAssuredV % "it-test"
)

configs(ItTest)
inConfig(ItTest)(Defaults.testSettings)



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