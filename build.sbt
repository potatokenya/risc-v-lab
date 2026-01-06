ThisBuild / scalaVersion := "2.13.12"

ThisBuild / version := "0.1.0"
ThisBuild / organization := "edu.dtu"

lazy val root = (project in file("."))
  .settings(
    name := "riscv-cpu",

    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.6.0",
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0"
    )
  )


