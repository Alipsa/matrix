#!/usr/bin/env groovy
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper

def manifest = new JsonSlurper().parse(new File(args[0]))
def pom = new XmlSlurper().parse(new File(args[1]))
def pins = pom.dependencyManagement.dependencies.dependency.collectEntries { [it.artifactId.text(), it.version.text()] }
def properties = pom.properties.children().collectEntries { [it.name().toString(), it.text()] }
boolean bomSelected = manifest.selected.any { it.artifactId == 'matrix-bom' }
println "__bom_selected=${bomSelected}"
manifest.selected.findAll { it.projectPath && pins[it.artifactId] }.each { component ->
  String expression = pins[component.artifactId]
  String property = expression.startsWith('${') ? expression.substring(2, expression.length() - 1) : null
  if (!property) throw new IllegalStateException('BOM pin must reference its version property')
  if (properties[property] != component.version) {
    if (bomSelected) throw new IllegalStateException('Selected module differs from BOM pin')
    // An already-released BOM can retain an older pin; resolve that exact pin from Central.
    return
  }
  File directory = new File(args[2], "${component.groupId.replace('.', '/')}/${component.artifactId}/${component.version}")
  component.files.each { String name ->
    if (!new File(directory, name).isFile()) throw new IllegalStateException("Missing staged artifact: ${name}")
  }
  println "${property}=${component.version}"
}
