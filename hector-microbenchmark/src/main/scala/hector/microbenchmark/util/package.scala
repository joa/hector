package hector.microbenchmark

import java.io.File

/**
 */
package object util {
  def classesOf(packageName: String): Seq[Class[_]] ={
    val classLoader = Thread.currentThread().getContextClassLoader
    val path = packageName.replace(".", "/")
    val resources = classLoader.getResources(path)
    var dirs = Seq.empty[File]

    while(resources.hasMoreElements) {
      val resource = resources.nextElement()
      dirs = dirs :+ new File(resource.getFile)
    }

    (for {
      dir ← dirs
    } yield {
      findClasses(dir, packageName)
    }).flatten
  }

  private[this] def findClasses(directory: File, packageName: String): Seq[Class[_]] =
    if(directory.exists()) {
      val files = directory.listFiles()
      val n = files.length

      var result = Seq.empty[Class[_]]
      var i = 0

      while(i < n) {
        val file = files(i)
        val name = file.getName

        if(file.isDirectory) {
          result = result ++ findClasses(file, packageName+"."+name)
        } else if(file.getName.endsWith(".class")) {
          result = result :+ Class.forName(packageName+"."+name.substring(0, name.length - 6))
        }

        i += 1
      }

      result
    } else {
      Seq.empty
    }
}
