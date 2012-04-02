package hector {
  //
  // Analog to the Akka build definition:  
  //   https://github.com/akka/akka/blob/master/project/AkkaBuild.scala
  //

  import sbt._
  import sbt.Keys._
  import com.github.siasia.WebPlugin.webSettings

  object HectorBuild extends Build {
    lazy val buildSettings = Seq(
      organization := "hector",
      version      := "1.0-SNAPSHOT",
      scalaVersion := "2.9.1-1"
    )

    // Projects

    lazy val hector = Project(
      id = "hector",
      base = file("."),
      settings = parentSettings,
      aggregate = Seq(web)
    )

    lazy val web = Project(
      id = "hector-web",
      base = file("hector-web"),
      settings = defaultSettings ++ webSettings ++ Seq( // For now we use the webSettings here too.
        libraryDependencies ++= Dependencies.web
      )
    )

    override lazy val settings = super.settings ++ buildSettings

    lazy val baseSettings = Defaults.defaultSettings

    lazy val parentSettings = baseSettings ++ Seq(
      publishArtifact in Compile := false
    )

    lazy val defaultSettings = baseSettings ++ Seq(
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
      javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
    )
  }

  object Dependencies {
    import Dependency._

    val web = Seq(guava, akkaActor, Container.jettyWebapp, Provided.servletApi, Test.scalaTest)
  }

  //
  // Dependency Matrix
  //
  // Note that the Container dependencies will be gone with first GA. Jetty is 
  // listed only as a dependency during development of Hector since it is no
  // fun to switch repositories all the time.
  //
  // =====================================================================================================================
  //     Name          | Group                      | Artifact              | Version            | Scope       | License
  // =====================================================================================================================
  //

  object Dependency {
    val  guava         = "com.google.guava"     %     "guava"              %  "11.0.2"                           // Apache 2.0
    val  akkaActor     = "com.typesafe.akka"    %     "akka-actor"         %  "2.0"                              // Apache 2.0

    object Container {
      val  jettyWebapp = "org.eclipse.jetty"    %     "jetty-webapp"       %  "8.0.4.v20111024"  % "container"   // Eclipse License
    }

    object Provided {
      val  servletApi  = "javax.servlet"        %     "javax.servlet-api"  %  "3.0.1"            % "provided"    // CDDL 1.1
    }

    object Test {
      val  scalaTest   = "org.scalatest"        %%    "scalatest"          %  "1.7.1"            % "test"        // Apache 2.0
    }
  }
}
