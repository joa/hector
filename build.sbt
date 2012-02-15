name := "hector"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0-RC1"

libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided->default"
