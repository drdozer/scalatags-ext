
import sbt._
import sbt.Keys._
import com.inthenow.sbt.scalajs._
import com.inthenow.sbt.scalajs.SbtScalajs._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import bintray.Plugin._
import bintray.Keys._
import org.eclipse.jgit.lib._

object ScalatagsExtBuild extends Build {

  val module = XModule(id = "scalatags-ext", defaultSettings = buildSettings)

  val logger = ConsoleLogger()

  val baseVersion = "0.1.3"

  lazy val buildSettings: Seq[Setting[_]] = bintrayPublishSettings ++ Seq(
    organization := "uk.co.turingatemyhamster",
    scalaVersion := "2.11.4",
    crossScalaVersions := Seq("2.11.4", "2.10.4"),
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    version := makeVersion(baseVersion),
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

  lazy val scalatagsExt           = module.project(scalatagsExtJvm, scalatagsExtJs).settings(
    packageBin in Compile  := file(""))
  lazy val scalatagsExtJvm        = module.jvmProject(scalatagsExtSharedJvm).
    settings(scalatagsExtPlatformJvmSettings : _*)
  lazy val scalatagsExtJs         = module.jsProject(scalatagsExtSharedJs).
    settings(scalatagsExtPlatformJsSettings : _*)
  lazy val scalatagsExtSharedJvm  = module.jvmShared().
    settings(scalatagsExtSharedJvmSettings : _*)
  lazy val scalatagsExtSharedJs   = module.jsShared(scalatagsExtSharedJvm).
    settings(scalatagsExtSharedJsSettings  : _*)

  lazy val scalatagsExtPlatformJvmSettings = Seq(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.12.1" % "test",
      "uk.co.turingatemyhamster" %% "scalarx_shared" % "0.2.6"
    )
  )

  lazy val scalatagsExtPlatformJsSettings = Seq(
    libraryDependencies ++= Seq(
      "uk.co.turingatemyhamster" %%% "scalarx_shared" % "0.2.6"
    )
  )

  lazy val scalatagsExtSharedJvmSettings = utest.jsrunner.Plugin.utestJvmSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.scalatags" %% "scalatags" % "0.4.1",
      "uk.co.turingatemyhamster" %% "scalarx_shared" % "0.2.6",
      "com.lihaoyi" %% "utest" % "0.2.4" % "test"
    )
  )

  lazy val scalatagsExtSharedJsSettings = utest.jsrunner.Plugin.utestJsSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.scalatags" %%% "scalatags" % "0.4.1",
      "uk.co.turingatemyhamster" %%% "scalarx_shared" % "0.2.6",
      "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
      "com.lihaoyi" %% "utest" % "0.2.4" % "test"
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
