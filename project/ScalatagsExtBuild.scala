
import sbt._
import sbt.Keys._
import com.inthenow.sbt.scalajs._
import com.inthenow.sbt.scalajs.SbtScalajs._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._


object ScalatagsExtBuild extends Build {

  val module = XModule(id = "scalatags-ext", defaultSettings = buildSettings)

  val logger = ConsoleLogger()

  lazy val buildSettings: Seq[Setting[_]] = Seq(
    organization := "uk.co.turingatemyhamster",
    scalaVersion := "2.11.4",
    crossScalaVersions := Seq("2.11.4", "2.11.2"),
    scalacOptions ++= Seq("-deprecation", "-unchecked"),
    version := "0.1.1"
  )

  lazy val scalatagsExt           = module.project(scalatagsExtJvm, scalatagsExtJs).settings(
    packageBin in Compile  := file(""))
  lazy val scalatagsExtJvm        = module.jvmProject(scalatagsExtSharedJvm)
  lazy val scalatagsExtJs         = module.jsProject(scalatagsExtSharedJs)
  lazy val scalatagsExtSharedJvm  = module.jvmShared().settings(
      Dependencies.scalarx ++ Dependencies.scalatags : _*)
  lazy val scalatagsExtSharedJs   = module.jsShared(scalatagsExtSharedJvm).settings(
        Dependencies.scalarx ++ Dependencies.scalatags ++ Dependencies.sclalajsDom : _*)

}
