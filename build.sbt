import sbtcrossproject.{crossProject, CrossType}

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.3")

resolvers in ThisBuild += Resolver.sonatypeRepo("releases")

lazy val scalatags = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .settings(
    organization := "com.lihaoyi",
    name := "scalatags",
    scalaVersion := "2.11.11",

    autoCompilerPlugins := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "acyclic" % "0.1.7" % "provided",
      "com.lihaoyi" %%% "utest" % "0.5.3" % "test",
      "com.lihaoyi" %%% "sourcecode" % "0.1.4",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    ) ++ (
      if (scalaVersion.value startsWith "2.10.") Seq(
          "org.scalamacros" %% s"quasiquotes" % "2.1.0" % "provided",
          compilerPlugin("org.scalamacros" % s"paradise" % "2.1.0" cross CrossVersion.full)
      ) else Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.6" % "test"
      )
    ),
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.7"),
    unmanagedSourceDirectories in Compile ++= {
      if (scalaVersion.value startsWith "2.12.") Seq(baseDirectory.value / ".."/"shared"/"src"/ "main" / "scala-2.11")
      else Seq()
    },
    testFrameworks += new TestFramework("utest.runner.Framework"),
    // Sonatype
    version := _root_.scalatags.Constants.version,
    publishTo := Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),

    pomExtra :=
      <url>https://github.com/lihaoyi/scalatags</url>
        <licenses>
          <license>
            <name>MIT license</name>
            <url>http://www.opensource.org/licenses/mit-license.php</url>
          </license>
        </licenses>
        <scm>
          <url>git://github.com/lihaoyi/scalatags.git</url>
          <connection>scm:git://github.com/lihaoyi/scalatags.git</connection>
        </scm>
        <developers>
          <developer>
            <id>lihaoyi</id>
            <name>Li Haoyi</name>
            <url>https://github.com/lihaoyi</url>
          </developer>
        </developers>
  )
  .jvmSettings(fortifySettings)
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.3"
    ),
    scalaJSStage in Test := FullOptStage,
    jsEnv := new org.scalajs.jsenv.phantomjs.PhantomJSEnv(),
    scalacOptions ++= (if (isSnapshot.value) Seq.empty else Seq({
      val a = baseDirectory.value.toURI.toString.replaceFirst("[^/]+/?$", "")
      val g = "https://raw.githubusercontent.com/lihaoyi/scalatags"
      s"-P:scalajs:mapSourceURI:$a->$g/${version.value}/scalatags/"
    }))
  )
  .nativeSettings(
    nativeLinkStubs := true
  )


// Needed, so sbt finds the projects
lazy val scalatagsJVM = scalatags.jvm
lazy val scalatagsJS = scalatags.js
lazy val scalatagsNative = scalatags.native

lazy val example = project.in(file("example"))
  .dependsOn(scalatagsJS)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := "2.11.11",
    scalacOptions ++= Seq(
      "-deprecation", // warning and location for usages of deprecated APIs
      "-feature", // warning and location for usages of features that should be imported explicitly
      "-unchecked", // additional warnings where generated code depends on assumptions
      "-Xlint", // recommended additional warnings
      "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
      "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
      "-Ywarn-inaccessible",
      "-Ywarn-dead-code"
    )
  )

lazy val readme = scalatex.ScalatexReadme(
  projectId = "readme",
  wd = file(""),
  url = "https://github.com/lihaoyi/scalatags/tree/master",
  source = "Readme",
  autoResources = Seq("Autocomplete.png", "ErrorHighlighting.png", "InlineDocs.png", "example-opt.js")
).settings(
  scalaVersion := "2.11.11",
  (unmanagedSources in Compile) += baseDirectory.value/".."/"project"/"Constants.scala",
  (resources in Compile) += (fullOptJS in (example, Compile)).value.data,
  (resources in Compile) += (doc in (scalatagsJS, Compile)).value,
  (run in Compile) := (run in Compile).dependsOn(Def.task{
    sbt.IO.copyDirectory(
      (doc in (scalatagsJS, Compile)).value,
      (target in Compile).value/"scalatex"/"api",
      overwrite = true
    )
  }).evaluated
)

lazy val fortifySettings = Seq(
  credentials += Credentials(
    Path.userHome / ".lightbend" / "commercial.credentials"),
  resolvers += Resolver.url(
    "lightbend-commercial-releases",
    new URL("http://repo.lightbend.com/commercial-releases/"))(
    Resolver.ivyStylePatterns),
  libraryDependencies += compilerPlugin(
    "com.lightbend" %% "scala-fortify" % "deb2b0d3"
      classifier "assembly" cross CrossVersion.patch),
  scalacOptions += s"-P:fortify:build=scalatags")
