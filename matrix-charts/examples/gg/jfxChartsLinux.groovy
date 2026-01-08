@Grab('org.openjfx:javafx-base:23.0.2:linux')
@Grab('org.openjfx:javafx-graphics:23.0.2:linux')
@Grab('org.openjfx:javafx-controls:23.0.2:linux')

import groovy.transform.SourceURI

@SourceURI
URI sourceUri

File scriptDir = new File(sourceUri).parentFile
evaluate(new File(scriptDir, "jfxCharts.groovy"))