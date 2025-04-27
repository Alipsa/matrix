#!/usr/bin/env groovy
@Grab('se.alipsa.groovy:gmd:2.2.1-SNAPSHOT')
@Grab('org.slf4j:slf4j-simple:2.0.17')
@Grab(group='org.apache.logging.log4j', module='log4j-to-slf4j', version='2.24.3')
@groovy.lang.GrabConfig(systemClassLoader=true)
import se.alipsa.groovy.gmd.*

Gmd gmd = new Gmd()
def outputDir = new File("build")
def htmlFile = new File(outputDir, "whiskeyAnalysis.html")
def gmdFile = new File("src/main/gmd/WhiskeyAnalysis.gmd")
gmd.gmdToHtml(gmdFile.text, htmlFile, [:])