enablePlugins(NativeImagePlugin)
crossScalaVersions := List(
  // "2.11.10",
  // "2.12.10",
  // "2.12.12",
  // "2.13.2",
  "2.13.3"
)
mainClass.in(Compile) := Some("Prog")
nativeImageOptions := List(
  "--no-fallback"
)
