import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt._
import sbt.Keys._
import sbtassembly.Plugin.{PathList, MergeStrategy, AssemblyKeys}
import sbtrelease.ReleasePlugin._
import spray.revolver.RevolverPlugin.Revolver
import scalariform.formatter.preferences._
import AssemblyKeys._
import com.typesafe.sbt.SbtAtmos.{ Atmos, atmosSettings }
import com.typesafe.sbt.SbtAtmos.AtmosKeys.{ traceable, sampling }


object Build extends sbt.Build {


 traceable in Atmos := Seq(
    "/user/twilio" -> true,  // trace this actor
    "/user/apple"  -> true,  // trace all actors in this subtree
    "/user/service"  -> true  // trace all actors in this subtree
  )


  lazy val root =
    project(id = "phantom-data-services",
      base = file(".")) aggregate core

  lazy val core =
    project(id = "phantom-data-services-core",
      base = file("phantom-data-services-core"),
      settings = Seq(libraryDependencies ++=
          Shared.Spray ++
          Shared.Akka ++
          Shared.Logging ++
          Shared.Joda ++
          Shared.Other ++
          Shared.Slick) ++
        Revolver.settings ++
        sbtassembly.Plugin.assemblySettings
        ++ Assembly.prefs
    )

  def project(id: String, base: File, settings: Seq[Def.Setting[_]] = Nil) =
    Project(id = id,
      base = base,
      settings =
        Project.defaultSettings ++
          Shared.settings ++
          releaseSettings ++
          settings ++
          Revolver.enableDebugging(port = 5050, suspend = false) ++
          Seq(
            resolvers += "spray" at "http://repo.spray.io/",
            resolvers += "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local",
            resolvers += "jets3t" at "http://www.jets3t.org/maven2",
            compile <<= (compile in Compile) dependsOn (compile in Test, compile in IntegrationTest),
            libraryDependencies ++= Shared.testDeps
          )).settings(Defaults.itSettings: _ *).settings(atmosSettings: _*).configs(IntegrationTest).configs(Atmos)



}

object Shared {

  val AkkaVersion = "2.2.3"
  val SprayVersion = "1.2.0"
  val LogbackVersion = "1.0.13"
  val JodaVersion = "2.3"
  val SlickVersion = "1.0.0"

  val Spray = Seq(
    "io.spray" % "spray-can" % SprayVersion,
    "io.spray" % "spray-routing" % SprayVersion,
    "io.spray" % "spray-testkit" % SprayVersion % "test",
    "io.spray" %% "spray-json" % "1.2.5"
  )

  val Akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test"
  )

  val Logging = Seq(
    "ch.qos.logback" % "logback-classic" % LogbackVersion,
    "ch.qos.logback" % "logback-core" % LogbackVersion,
    "org.slf4j" % "slf4j-api" % "1.7.5"
  )

  val Joda = Seq(
    "joda-time" % "joda-time" % JodaVersion,
    "org.joda" % "joda-convert" % "1.2"
  )

  val testDeps = Seq(
    "org.specs2" %% "specs2" % "2.2.3" % "test, it",
    "org.mockito" % "mockito-all" % "1.9.5" % "test, it",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "test, it"
  )

  val Other = Seq(
    "commons-codec"  % "commons-codec"   % "1.8",
    "com.twilio.sdk" % "twilio-java-sdk" % "3.3.9",
    "com.relayrides" % "pushy"           % "0.2",
    "commons-codec"  % "commons-codec"   % "1.6",
    "com.mandrillapp.wrapper.lutung" % "lutung" % "0.0.4",
    "nl.rhinofly" %% "play-s3" % "3.3.3",
    "net.java.dev.jets3t" % "jets3t" % "0.9.0"
  )

  val Slick = Seq(
    "com.typesafe.slick" %% "slick" % SlickVersion,
    "mysql" % "mysql-connector-java" % "5.1.22",
    "com.jolbox" % "bonecp" % "0.7.1.RELEASE",
    "com.github.tototoshi" %% "slick-joda-mapper" % "0.4.0"
  )

  val settings = Seq(
    organization := "com.phantom",
    scalaVersion := "2.10.3",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
    parallelExecution in Test := false,
    fork in Test := true,
    shellPrompt := ShellPrompt.buildShellPrompt
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++ SbtScalariform.scalariformSettings ++ Formatting.prefs

}

// Shell prompt which show the current project, git branch and build version
object ShellPrompt {

  object devnull extends ProcessLogger {
    def info(s: => String) {}

    def error(s: => String) {}

    def buffer[T](f: => T): T = f
  }

  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## "
    )

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract(state).currentProject.id
      "[%s](%s)$ ".format(
        currProject, currBranch
      )
    }
  }
}

object Formatting {
  /*list of options: https://github.com/mdr/scalariform */
  val prefs = ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(AlignParameters, true)
    .setPreference(SpaceBeforeColon, true)
}



object Assembly {
  val prefs = Set(jarName in assembly := "phantom.jar",
    mainClass in assembly := None,
    test in assembly := {},
    mergeStrategy in assembly <<= (mergeStrategy in assembly) {(old) =>
    {
      case PathList("org", "hamcrest", xs @ _*)         => MergeStrategy.first
      case "logback.properties" =>  MergeStrategy.discard
      case "application.conf" => MergeStrategy.concat
      case x => old(x)
    }})
}


