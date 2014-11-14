
import sbt._
import sbt.Keys._
import com.inthenow.sbt.scalajs._

import com.inthenow.sbt.scalajs.SbtScalajs._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._


object NotestsBuild extends Build {
  val prj = "scalajs-ext"
  
  val modules = file(".") 
  val logger = ConsoleLogger()

  lazy val buildSettings: Seq[Setting[_]] = Seq(
    organization := "uk.co.turingatemyhamster",
    scalaVersion := "2.11.4",
    crossScalaVersions := Seq("2.11.4", "2.11.2"),
    scalacOptions ++= Seq("-deprecation", "-unchecked")
  )

  // We want an empty root, i.e. no source in ",", nothing published

 lazy val prjModule = Project(
    id =  prj ,
    base =  modules,
    settings = buildSettings
  ).aggregate(sharedjvm, sharedjs, prjJs, prjJvm)
    .dependsOn(sharedjvm, prjJvm, sharedjs, prjJs)

  lazy val prjJvm: Project = Project(
    id = s"${prj}_jvm",
    base =  modules / "jvm",
    settings = buildSettings ++ scalajsJvmSettings 
  ).dependsOn(sharedjvm).aggregate(sharedjvm)

  lazy val prjJs: Project = Project(
    id = s"${prj}_js",
    base = modules / "js",
    settings = buildSettings ++ scalajsJsSettings
  ).enablePlugins(SbtScalajs)
    .dependsOn(sharedjs).aggregate(sharedjs)

  lazy val sharedjvm = Project(
    id = s"${prj}_shared_jvm",
    base =  modules / "shared",
    settings = buildSettings ++ scalajsJvmSettings ++
      Dependencies.scalarx ++ Dependencies.scalatags
  )

  lazy val sharedjs = Project(
    id = s"${prj}_shared_js",
    base =  modules / ".shared_js",
    settings =  buildSettings ++ scalajsJsSettings ++ linkedSources(sharedjvm) ++
      Dependencies.scalarx ++ Dependencies.scalatags ++ Dependencies.sclalajsDom
  ).enablePlugins(SbtScalajs)

}
