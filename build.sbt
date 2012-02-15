name := "hector"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

seq(webSettings: _*)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.4.v20111024" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
  "com.typesafe.akka" % "akka-actor" % "2.0-RC1"
)
