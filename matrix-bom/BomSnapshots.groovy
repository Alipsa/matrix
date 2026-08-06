#!/usr/bin/env groovy

import groovy.xml.XmlSlurper

if (args.size() < 1 || args.size() > 2) {
  throw new IllegalArgumentException('usage: groovy BomSnapshots.groovy bom.xml [--all]')
}

boolean all = args.size() == 2 && args[1] == '--all'
if (args.size() == 2 && !all) {
  throw new IllegalArgumentException("unknown option: ${args[1]}")
}

def document = new XmlSlurper().parse(new File(args[0]))
document.children().findAll { it.name().toString() == 'properties' }.each { properties ->
  properties.children().each { property ->
    String name = property.name().toString()
    String value = property.text().trim()
    if (name.startsWith('matrix') && name.endsWith('Version') && (all || value.endsWith('-SNAPSHOT'))) {
      println "${name}=${value}"
    }
  }
}
