package hector {
  //
  // Analog to the Akka build definition:  
  //   https://github.com/akka/akka/blob/master/project/AkkaBuild.scala
  //

  import sbt._
  import sbt.Keys._
  import com.earldouglas.xsbtwebplugin.WebPlugin.webSettings

  object HectorBuild extends Build {
    lazy val buildSettings = Seq(
      organization := "hector",
      version      := "1.0-SNAPSHOT",
      scalaVersion := "2.10.2"
    )

    // Projects

    lazy val hector = Project(
      id = "hector",
      base = file("."),
      settings = parentSettings,
      aggregate = Seq(web, microbencmark, demo)
    )

    lazy val web = Project(
      id = "hector-web",
      base = file("hector-web"),
      settings = defaultSettings ++ Seq(libraryDependencies ++= Dependencies.web)
    )

    lazy val demo = Project(
      id = "hector-demo",
      base = file("hector-demo"),
      dependencies = Seq(web),
      settings = defaultSettings ++ webSettings ++ Seq( // For now we use the webSettings here too.
        libraryDependencies ++= Dependencies.web ++ Seq(Dependency.Container.jettyWebapp) // And jettyWebapp as well ...
      )
    )

    lazy val microbencmark = Project(
      id = "hector-microbenchmark",
      base = file("hector-microbenchmark"),
      dependencies = Seq(web),
      settings = benchmarkSettings
    )

    override lazy val settings = super.settings ++ buildSettings

    lazy val baseSettings = Defaults.defaultSettings

    lazy val parentSettings = baseSettings ++ Seq(
      publishArtifact in Compile := false
    )

    //
    // Sorry about this ...
    //
    // If anyone knows how to use SBT and is lucky enough to find the
    // documentation or anything in the source code that would 
    // point to correct usage ...
    //
    val AfterWastingThreeHoursWithSBTThisIsTheBestICouldComeUpWith = 
      Seq(
        "/home/joa/Development/hector/hector-microbenchmark/target/scala-2.10/classes",
        "/home/joa/Development/hector/hector-web/target/scala-2.10/classes",
        "/home/joa/.sbt/boot/scala-2.10.2/lib/scala-library.jar",
        "/home/joa/.ivy2/cache/com.google.guava/guava/bundles/guava-14.0.1.jar",
        "/home/joa/.ivy2/cache/com.google.code.findbugs/jsr305/jars/jsr305-1.3.9.jar",
        "/home/joa/.ivy2/cache/com.typesafe.akka/akka-actor_2.10/jars/akka-actor_2.10-2.2.0.jar",
        "/home/joa/.ivy2/cache/com.typesafe/config/bundles/config-1.0.2.jar",
        "/home/joa/.ivy2/cache/com.google.caliper/caliper/jars/caliper-0.5-rc1.jar",
        "/home/joa/.ivy2/cache/com.google.code.gson/gson/jars/gson-1.7.1.jar",
        "/home/joa/.ivy2/cache/com.google.code.java-allocation-instrumenter/java-allocation-instrumenter/jars/java-allocation-instrumenter-2.0.jar",
        "/home/joa/.ivy2/cache/asm/asm/jars/asm-3.3.1.jar",
        "/home/joa/.ivy2/cache/asm/asm-analysis/jars/asm-analysis-3.3.1.jar",
        "/home/joa/.ivy2/cache/asm/asm-tree/jars/asm-tree-3.3.1.jar",
        "/home/joa/.ivy2/cache/asm/asm-commons/jars/asm-commons-3.3.1.jar",
        "/home/joa/.ivy2/cache/asm/asm-util/jars/asm-util-3.3.1.jar",
        "/home/joa/.ivy2/cache/asm/asm-xml/jars/asm-xml-3.3.1.jar"
      )


    lazy val benchmarkSettings = baseSettings ++ Seq(
      libraryDependencies ++= Dependencies.benchmarking,

      fork in run := true,

      scalacOptions ++= Scalac.defaultOptions,

      javacOptions ++= Javac.defaultOptions,

      javaOptions in run ++= Seq("-cp", AfterWastingThreeHoursWithSBTThisIsTheBestICouldComeUpWith mkString ":")
    )

    lazy val defaultSettings = baseSettings ++ Seq(
      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",

      resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/repositories/releases/",

      scalacOptions ++= Scalac.defaultOptions,

      javacOptions  ++= Javac.defaultOptions
    )
  }

  object Scalac {
    val defaultOptions = Seq(
        "-encoding", "UTF-8",
        "-deprecation",
        "-unchecked", 
        "-feature",
        "-Yinline-warnings",
        "-language:implicitConversions")
  }

  object Javac {
    val defaultOptions = Seq("-Xlint:unchecked", "-Xlint:deprecation")
  }

  object Dependencies {
    import Dependency._

    val web = Seq(guava, akkaActor, jsr305, Provided.servletApi, Test.scalaTest)

    val benchmarking = web ++ Seq(Benchmark.caliper, Benchmark.allocInstr, Benchmark.gson)
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
    private[this] val google = "com.google."
    private[this] val JAI = "java-allocation-instrumenter"

    val guava          = (google+"guava")       %     "guava"              %  "14.0.1"                           // Apache 2.0

    val akkaActor      = "com.typesafe.akka"    %%    "akka-actor"         %  "2.2.0"                            // Apache 2.0
    val akkaCluster    = "com.typesafe.akka"    %%    "akka-cluster"       %  "2.2.0"                            // Apache 2.0


    val jsr305         = (google+"code.findbugs")%    "jsr305"             %  "1.3.7"                            // Apache 2.0

    object Container {
      val jettyWebapp  = "org.eclipse.jetty"    %     "jetty-webapp"       %  "8.0.4.v20111024"  % "container"   // Eclipse License
    }

    object Provided {
      val servletApi   = "javax.servlet"        %     "javax.servlet-api"  %  "3.0.1"            % "provided"    // CDDL 1.1
    }

    object Test {
      val scalaTest    = "org.scalatest"        %%    "scalatest"          %  "1.9.1"            % "test"        // Apache 2.0
    }

    object Benchmark {
      val caliper      = (google+"caliper")     %     "caliper"            %  "0.5-rc1"                          // Apache 2.0
      val gson         = (google+"code.gson")   %     "gson"               %  "1.7.1"                            // Apache 2.0
      val allocInstr   = (google+"code."+JAI)   %     JAI                  %  "2.0"                              // Apache 2.0
    }
  }
}
