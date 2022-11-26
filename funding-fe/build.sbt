import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtcrossproject.CrossPlugin.autoImport.CrossType
import scalapb.compiler.Version.scalapbVersion

inThisBuild(
  List(
    organization := "my-organization",
    version := "0.0.1",
    scalaVersion := DependencyVersions.scala,
    scalafmtOnCompile := true
  )
)
// no effect ! ThisBuild / libraryDependencySchemes += "io.circe" %% "circe-core" % "early-semver"

name := "funding-fe"

val sharedSettings = Seq.concat(
  ScalaOptions.fixOptions,
  Seq(
    addCompilerPlugin(
      ("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full)
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    // for Scala 2.12 and lower
    // addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
    // ---
  )
)

lazy val shared =
  (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file(
    "modules/shared"
  )).disablePlugins(RevolverPlugin)
    .settings(
      libraryDependencies ++= Seq.concat(
        Dependencies.scribe.value,
        Dependencies.circe.value,
        Dependencies.newtype.value,
        Dependencies.unindent.value
      )
    )
    .settings(sharedSettings)

val TEST_FILE = s"./sjs.test.js"

val testDev  = Def.taskKey[Unit]("test in dev mode")
val testProd = Def.taskKey[Unit]("test in prod mode")

def runJest(): Unit = {
  import sys.process._
  val jestResult = """yarn test --colors""".!
  if (jestResult != 0) throw new IllegalStateException("Jest Suite failed")
}

lazy val jest =
  project
    .in(file("modules/jest"))
    .disablePlugins(RevolverPlugin)
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
    )
    .settings(sharedSettings)
    .settings(
      libraryDependencies ++= Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
    )
    .dependsOn(shared.js)

lazy val proto = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("modules/proto"))
  .settings(
    Compile / PB.protocOptions += "--experimental_allow_proto3_optional",
    Compile / PB.targets := Seq(
      scalapb.gen(flatPackage = true) -> (Compile / sourceManaged).value / "protos"
    ),
    // The trick is in this line:
    Compile / PB.protoSources := Seq(file("modules/proto/src/main/protobuf")),
    scalaJSUseMainModuleInitializer                := true,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  )



lazy val frontend =
  project
    .in(
      file("modules/frontend")
    )
    .enablePlugins(ScalaJSPlugin)
    .disablePlugins(RevolverPlugin)
    .settings(
      scalacOptions ++= Seq(
        "-P:scalajs:nowarnGlobalExecutionContext"
      ),
      scalaJSUseMainModuleInitializer := true,
//      Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
//      Test / scalaJSUseTestModuleInitializer := false,
//      Test / scalaJSUseMainModuleInitializer := true,
      Test / fastOptJS / artifactPath :=
        ((fastOptJS / crossTarget).value /
          ((fastOptJS / moduleName).value + "-fastopt.test.js")),
      Test / fullOptJS / artifactPath :=
        ((fullOptJS / crossTarget).value /
          ((fullOptJS / moduleName).value + "-opt.test.js")),
      testDev := {
        (Test / fastOptJS).value
        runJest()
      },
      testProd := {
        (Test / fullOptJS).value
        runJest()
      },
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
      libraryDependencies ++= Seq.concat(
        Dependencies.laminar.value,
        Dependencies.laminext.value,
        Dependencies.`url-dsl`.value,
        Dependencies.waypoint.value,
        Dependencies.quicklens.value,
        Dependencies.`dom-test-utils`.value
      ),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.1.0",
        "com.raquo" %%% "airstream" % "0.14.5",
        "io.github.cquiroz" %%% "scala-java-time" % "2.2.2",
        "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,

        // The following needed only if you include scalapb/scalapb.proto:
        //"com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

        "io.github.scalapb-json" %%% "scalapb-circe" % "0.12.1",
        "io.github.scalapb-json" %% "scalapb-circe-macros" % "0.12.1"
      )
    )
    .settings(sharedSettings)
    .dependsOn(shared.js, jest % Test, proto.js)


val AkkaVersion = "2.6.18"


lazy val backend =
  project
    .in(file("modules/backend"))
    .enablePlugins(JavaAppPackaging)
    .settings(Revolver.settings.settings)
    .settings(
      libraryDependencies ++= Seq.concat(
        Dependencies.`akka-http`.value,
        Dependencies.`akka-http-json`.value,
        Dependencies.akka.value,
        Dependencies.`typesafe-config`.value,
        Dependencies.`circe-config`.value
      ),
      libraryDependencies ++= Seq(
        "com.lightbend.akka" %% "akka-stream-alpakka-google-cloud-pub-sub-grpc" % "3.0.3",
        "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        //"io.confluent" %% "kafka-protobuf-serializer" % "5.5.1",
        "com.typesafe.akka" %% "akka-http2-support" % "10.2.6",
        "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6",
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream-typed"        % AkkaVersion,
        "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
        "com.lihaoyi" %% "upickle" % "1.3.8"
      ),
      reStart / mainClass := Some("starter.boot.Boot"),
      reLogTag := ""
    )
    .settings(sharedSettings)
    .dependsOn(shared.jvm)
