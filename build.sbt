
name := "panoptes-scala-play"
version := "1.0.0"
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

pomExtra := (
  <url>http://bcatarino.com/panoptes-scala-play</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:bcatarino/panoptes-scala-play.git</url>
    <connection>scm:git:git@github.com:bcatarino/panoptes-scala-play.git</connection>
  </scm>
  <developers>
    <developer>
      <id>bcatarino</id>
      <name>Bruno Catarino</name>
      <url>https://github.com/bcatarino</url>
    </developer>
  </developers>)