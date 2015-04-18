
import sbt._
import sbt.Keys._
import com.inthenow.sbt.scalajs._
import com.inthenow.sbt.scalajs.SbtScalajs._
import org.scalajs.sbtplugin.ScalaJSPlugin
import ScalaJSPlugin.autoImport._
import bintray.Plugin._
import bintray.Keys._
import org.eclipse.jgit.lib._

object ScalatagsExtBuild extends Build {

  implicit val logger = ConsoleLogger()

  /** the root project. */
  lazy val scalatagsM  = CrossModule(RootBuild,
    id              = "scalatags",
    defaultSettings = buildSettings)

  lazy val scalatags = scalatagsM.project(Module, scalatagsJvm, scalatagsJs)

  lazy val scalatagsJvm = scalatagsM.project(Jvm, scalatagsExtJvm)

  lazy val scalatagsJs = scalatagsM.project(Js, scalatagsExtJs)

  val module = CrossModule(SharedBuild,
    id = "ext",
    baseDir= "scalatags-ext",
    defaultSettings = buildSettings,
    modulePrefix    = "scalatags-")

  val baseVersion = "0.1.3"

  lazy val buildSettings: Seq[Setting[_]] = bintrayPublishSettings ++ Seq(
    organization := "uk.co.turingatemyhamster",
    scalaVersion := "2.11.5",
    crossScalaVersions := Seq("2.11.5", "2.10.4"),
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    version := makeVersion(baseVersion),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    resolvers += Resolver.url(
      "scalajs Rep",
      url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
      Resolver.ivyStylePatterns),
    resolvers += "drdozer Bintray Repo" at "http://dl.bintray.com/content/drdozer/maven",
    publishMavenStyle := true,
    repository in bintray := "maven",
    bintrayOrganization in bintray := None,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
  )

  lazy val scalatagsExt           = module.project(Module, scalatagsExtJvm, scalatagsExtJs).settings(
    packageBin in Compile  := file(""))
  lazy val scalatagsExtJvm        = module.project(Jvm, scalatagsExtSharedJvm).
    settings(scalatagsExtPlatformJvmSettings : _*)
  lazy val scalatagsExtJs         = module.project(Js, scalatagsExtSharedJs).
    settings(scalatagsExtPlatformJsSettings : _*)
  lazy val scalatagsExtSharedJvm  = module.project(Jvm, Shared).
    settings(scalatagsExtSharedJvmSettings : _*)
  lazy val scalatagsExtSharedJs   = module.project(Js, Shared).
    settings(scalatagsExtSharedJsSettings  : _*)

  lazy val scalatagsExtPlatformJvmSettings = Seq(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
      "com.lihaoyi" %% "scalarx" % "0.2.7"
    )
  )

  lazy val scalatagsExtPlatformJsSettings = Seq(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalarx" % "0.2.7"
    )
  )

  lazy val scalatagsExtSharedJvmSettings = Seq( //utest.jsrunner.Plugin.utestJvmSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "scalatags" % "0.4.5",
      "com.lihaoyi" %% "scalarx" % "0.2.7",
      "com.lihaoyi" %% "utest" % "0.3.0" % "test"
    )
  )

  lazy val scalatagsExtSharedJsSettings = Seq( //utest.jsrunner.Plugin.utestJsSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.4.5",
      "com.lihaoyi" %%% "scalarx" % "0.2.7",
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
    )
  )

  def fetchGitBranch(): String = {
    val builder = new RepositoryBuilder()
    builder.setGitDir(file(".git"))
    val repo = builder.readEnvironment().findGitDir().build()
    val gitBranch = repo.getBranch
    logger.info(s"Git branch reported as: $gitBranch")
    repo.close()
    val travisBranch = Option(System.getenv("TRAVIS_BRANCH"))
    logger.info(s"Travis branch reported as: $travisBranch")

    val branch = (travisBranch getOrElse gitBranch) replaceAll ("/", "_")
    logger.info(s"Computed branch is $branch")
    branch
  }

  def makeVersion(baseVersion: String): String = {
    val branch = fetchGitBranch()
    if(branch == "master") {
      baseVersion
    } else {
      val tjn = Option(System.getenv("TRAVIS_JOB_NUMBER"))
      s"$branch-$baseVersion${
        tjn.map("." + _) getOrElse ""
      }"
    }
  }
}
