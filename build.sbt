
name := "panoptes-scala-play"
version := "1.0"
scalaVersion := "2.11.8"




libraryDependencies ++= Seq("com.typesafe.play" %% "play" % "2.4.3")




publishMavenStyle := true
publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

