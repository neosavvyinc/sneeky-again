import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt._
import Keys._
import sbtassembly.Plugin.{MergeStrategy, AssemblyKeys}
import sbtrelease.ReleasePlugin._
import spray.revolver.RevolverPlugin.Revolver
import scalariform.formatter.preferences._
import AssemblyKeys._


object Build extends sbt.Build {

  lazy val root =
    project(id = "phantom-data-services",
      base = file(".")) aggregate(core)

  lazy val core =
    project(id = "phantom-data-services-core",
      base = file("phantom-data-services-core"),
      settings = Seq(libraryDependencies ++=
        Shared.Spray ++
          Shared.Akka ++
          Shared.Logging ++
          Shared.Joda ++
          Shared.Other) ++ Revolver.settings ++ sbtassembly.Plugin.assemblySettings ++ Assembly.prefs
    )

  def project(id: String, base: File, settings: Seq[Def.Setting[_]] = Nil) =
    Project(id = id,
      base = base,
      settings =
        Project.defaultSettings ++
          Shared.settings ++
          releaseSettings ++
          settings ++
          Seq(
            resolvers += "spray" at "http://repo.spray.io/",
            compile <<= (compile in Compile) dependsOn (compile in Test, compile in IntegrationTest),
            libraryDependencies ++= Shared.testDeps
          )).settings(Defaults.itSettings: _*).configs(IntegrationTest)


}

object Shared {

  val AkkaVersion = "2.2.3"
  val SprayVersion = "1.2.0"
  val LogbackVersion = "1.0.13"
  val JodaVersion = "2.3"

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
    "org.specs2" %% "specs2" % "2.2.3" % "test",
    "junit" % "junit-dep" % "4.10"
  )

  val Other = Seq(
    "commons-codec" % "commons-codec" % "1.8"
  )

  val settings = Seq(
    organization := "com.phantom",
    scalaVersion := "2.10.3",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
    parallelExecution in Test := false,
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
    mergeStrategy in assembly <<= (mergeStrategy in assembly) {(old) =>
    {
      case "logback.properties" =>  MergeStrategy.discard
      case "application.conf" => MergeStrategy.discard
      case x => old(x)
    }})
}


