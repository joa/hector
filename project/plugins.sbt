resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"

// Wait for plugins to adapt SBT 0.12
//addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.0.0", sbtVersion = "0.12.0-M2")
//libraryDependencies <+= sbtVersion(v ⇒ "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.11"))
