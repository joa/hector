resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.0.0")

libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.10"))
