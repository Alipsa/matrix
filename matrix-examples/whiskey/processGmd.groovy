#!/usr/bin/env groovy
// TODO: pin to released versions once matrix-stats 2.5.3 and gmd-core 3.2.0 are published;
//       SNAPSHOT coordinates only resolve after a local publishToMavenLocal
@Grab('se.alipsa.gmd:gmd-core:3.2.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.5.3-SNAPSHOT')
@GrabExclude(group='xml-apis', module='xml-apis')
@groovy.lang.GrabConfig(systemClassLoader=true)
import se.alipsa.gmd.core.*

Gmd gmd = new Gmd()
def outputDir = new File("build")
def htmlFile = new File(outputDir, "whiskeyAnalysis.html")
def gmdFile = new File("src/main/gmd/WhiskeyAnalysis.gmd")
gmd.gmdToHtml(gmdFile.text, htmlFile, [:])
