#!/usr/bin/env groovy
// gmd-core 3.2.0 has not been released yet, so this script needs a locally published GMD snapshot.
@Grab('se.alipsa.gmd:gmd-core:3.2.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.5.3')
@GrabExclude(group='xml-apis', module='xml-apis')
@GrabConfig(systemClassLoader=true)
import se.alipsa.gmd.core.*

Gmd gmd = new Gmd()
def outputDir = new File("build")
def htmlFile = new File(outputDir, "whiskeyAnalysis.html")
def gmdFile = new File("src/main/gmd/WhiskeyAnalysis.gmd")
gmd.gmdToHtml(gmdFile.text, htmlFile, [:])
