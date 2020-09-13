import java.nio.file.StandardCopyOption
import java.nio.file.Files
def scala212 = "2.12.12"
inThisBuild(
  List(
    organization := "org.scalameta",
    homepage := Some(url("https://github.com/scalameta/sbt-native-image")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@gmail.com",
        url("https://geirsson.com")
      )
    ),
    scalaVersion := scala212,
    crossScalaVersions := List(scala212)
  )
)

crossScalaVersions := Nil
skip.in(publish) := true

lazy val subs = project
  .in(file("native-image-substitutions"))
  .settings(
    moduleName := "native-image-substitutions",
    crossVersion := CrossVersion.disabled,
    libraryDependencies += "org.graalvm.nativeimage" % "svm" % nativeImageDefaults.version % "compile-internal",
    sources in (Compile, doc) := Seq.empty,
    logLevel := Level.Error,
    javaHome in Compile := {
      // force javac to fork by setting javaHome to workaround https://github.com/sbt/zinc/issues/520
      val home = file(sys.props("java.home"))
      val actualHome =
        if (System.getProperty("java.version").startsWith("1.8"))
          home.getParentFile
        else home
      Some(actualHome)
    }
  )

lazy val plugin = project
  .in(file("plugin"))
  .settings(
    moduleName := "sbt-native-image",
    sbtPlugin := true,
    sbtVersion.in(pluginCrossBuild) := "1.0.0",
    buildInfoPackage := "sbtnativeimage",
    buildInfoKeys := Seq[BuildInfoKey](
      version
    ),
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      s"-Dplugin.version=${version.value}"
    ),
    resourceGenerators.in(Compile) += Def.task {
      val out = managedResourceDirectories.in(Compile).value.head /
        "sbt-native-image" / "native-image-substitutions.jar"
      val pkg = Keys.`package`.in(subs, Compile).value
      out.getParentFile().mkdirs()
      IO.copyFile(pkg, out)
      List(out)
    }
  )
  .enablePlugins(ScriptedPlugin, BuildInfoPlugin)

lazy val example = project
  .in(file("example"))
  .settings(
    skip.in(publish) := true,
    mainClass.in(Compile) := Some("example.Hello"),
    // This line is only necessary because this build depends on
    // sbt-native-image via source instead of `addSbtPlugin()`.
    nativeImageSubstitutions := List(Keys.`package`.in(subs, Compile).value),
    test := {
      val binary = nativeImage.value
      val output = scala.sys.process.Process(List(binary.toString)).!!.trim
      assert(output == "List(1, 2, 3)", output)
    }
  )
  .enablePlugins(NativeImagePlugin)
