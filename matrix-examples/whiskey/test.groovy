#!/usr/bin/env groovy
@Grab(group='se.alipsa.matrix', module='matrix-bom', version='2.1.0', type='pom')
@Grab('se.alipsa.matrix:matrix-core')

def userHome = System.getProperty("user.home")
File cacheDir = new File(userHome, ".groovy/grapes/se.alipsa.matrix/matrix-core/jars")
boolean coreJarExists = false
println "Grape cache: ${cacheDir.absolutePath} jars:"
cacheDir.listFiles().eachWithIndex { it, idx ->
  println idx + ". " + it.name
  if (it.name.contains("matrix-core-3.1.0.jar")) {
    coreJarExists = true
  }
}
println "\nmatrix-core-3.1.0.jar was ${coreJarExists ? 'found' : 'NOT found'} in ${cacheDir.absolutePath}"