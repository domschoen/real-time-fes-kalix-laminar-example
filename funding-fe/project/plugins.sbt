addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.11.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.4")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0")


libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.12"
