#!/usr/bin/env groovy
import groovy.json.JsonOutput
import groovy.xml.XmlSlurper
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest

// Real API/japicmp acceptance against unreleased release-coordinate fixtures, never a real deployment.
File root = new File(args ? args[0] : '.').canonicalFile
File warm = new File(args.size() > 1 ? args[1] : 'build/releaseAll/bom-verify').canonicalFile
File output = new File(root, 'build/releaseAll'); output.mkdirs()
File fixture = Files.createTempDirectory(output.toPath(), 'manifest-verifier-').toFile()
File staging = new File(fixture, 'staging with spaces')
def copyTree = { File source, File target ->
  Files.walk(source.toPath()).withCloseable { stream ->
    stream.forEach { path ->
      File destination = new File(target, source.toPath().relativize(path).toString())
      if (Files.isDirectory(path)) destination.mkdirs()
      else { destination.parentFile.mkdirs(); Files.copy(path, destination.toPath()) }
    }
  }
}
def fingerprint = { File directory ->
  Map<String, String> values = [:]
  directory.eachFileRecurse { File file ->
    if (file.isFile()) {
      MessageDigest digest = MessageDigest.getInstance('SHA-256')
      file.withInputStream { input ->
        byte[] buffer = new byte[65536]
        int count
        while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count)
      }
      values[directory.toPath().relativize(file.toPath()).toString()] = digest.digest().encodeHex().toString()
    }
  }
  values
}
try {
  File bom = new File(fixture, 'matrix-bom'); bom.mkdirs()
  ['bom.xml', 'pom.xml', 'verify-settings.xml', 'verifyBomApi.sh', 'BomSnapshots.groovy', 'version-plugin-rules.xml'].each { String name ->
    String text = new File(root, 'matrix-bom/' + name).text
    new File(bom, name).text = name.endsWith('.xml') ? text.replace('-SNAPSHOT', '-release-all-fixture') : text
  }
  copyTree(new File(root, 'matrix-bom/src'), new File(bom, 'src'))
  new File(bom, 'japicmp').mkdirs()
  new File(bom, 'japicmp/pom.xml.template').bytes = new File(root, 'matrix-bom/japicmp/pom.xml.template').bytes
  copyTree(new File(root, 'scripts/release-all'), new File(fixture, 'scripts/release-all'))
  new File(fixture, 'settings.gradle').bytes = new File(root, 'settings.gradle').bytes
  def original = new XmlSlurper().parse(new File(root, 'matrix-bom/bom.xml'))
  def properties = original.properties.children().collectEntries { [it.name().toString(), it.text()] }
  List<Map> components = []
  original.dependencyManagement.dependencies.dependency.each { dependency ->
    String artifact = dependency.artifactId.text()
    String expression = dependency.version.text()
    String version = properties[expression.substring(2, expression.length() - 1)]
    if (!version.endsWith('-SNAPSHOT')) return
    String release = version.replace('-SNAPSHOT', '-release-all-fixture')
    File source = new File(warm, "se/alipsa/matrix/${artifact}/${version}")
    assert source.isDirectory(): "Warm isolated artifact missing: ${artifact}:${version}; run verifyBomApi.sh first"
    File destination = new File(staging, "se/alipsa/matrix/${artifact}/${release}"); destination.mkdirs()
    List<String> files = []
    source.listFiles().findAll { it.name.endsWith('.pom') || it.name.endsWith('.jar') }.each { File artifactFile ->
      String name = artifactFile.name.replace('-SNAPSHOT', '-release-all-fixture')
      File target = new File(destination, name)
      if (name.endsWith('.pom')) target.text = artifactFile.text.replace('-SNAPSHOT', '-release-all-fixture')
      else target.bytes = artifactFile.bytes
      files.add(name)
    }
    components.add([projectPath: ':' + artifact, groupId: dependency.groupId.text(), artifactId: artifact, version: release, files: files])
  }
  def fixtureBom = new XmlSlurper().parse(new File(bom, 'bom.xml'))
  components.add([groupId: fixtureBom.groupId.text(), artifactId: 'matrix-bom', version: fixtureBom.version.text(), files: []])
  File manifest = new File(fixture, 'manifest.json'); manifest.text = JsonOutput.toJson([selected: components])
  File cache = new File(fixture, 'download-cache')
  copyTree(warm, cache)
  // This is a new fixture copy, never the user's Maven Local or the warm repository.
  File baseline = new File(fixture, 'baseline')
  File baselineSource = new File(warm, 'se/alipsa/matrix/matrix-core/' + properties.matrixCoreBaselineVersion)
  if (baselineSource.exists()) copyTree(baselineSource, baseline)
  new File(cache, 'se/alipsa/matrix').deleteDir()
  if (baseline.exists()) copyTree(baseline, new File(cache, 'se/alipsa/matrix/matrix-core/' + properties.matrixCoreBaselineVersion))
  Map before = fingerprint(staging)
  Files.walk(staging.toPath()).withCloseable { stream ->
    stream.forEach { path -> Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(Files.isDirectory(path) ? 'r-x------' : 'r--------')) }
  }
  def builder = new ProcessBuilder(['bash', new File(bom, 'verifyBomApi.sh').path, '--manifest', manifest.path, '--staging', staging.path, '--local', cache.path])
    .directory(fixture).redirectErrorStream(true)
  builder.environment().putAll([RUN_EXTERNAL_TESTS: 'false', RUN_SLOW_TESTS: 'false'])
  Process process = builder.start()
  File log = new File(output, 'manifest-verifier-test.log')
  log.withOutputStream { process.inputStream.transferTo(it) }
  assert process.waitFor() == 0: "Manifest verifier failed; inspect ${log}"
  assert fingerprint(staging) == before: 'Maven modified the read-only staged artifacts'
  assert log.text.contains('no Gradle publishing')
  assert !log.text.contains('maven-deploy-plugin')
  assert log.text.contains('japicmp: comparing')
  println "Manifest-mode verifier passed for ${components.size() - 1} unreleased release-coordinate fixtures, read-only staging, separate cache, API tests and japicmp. Log: ${log}"
} finally {
  if (staging.exists()) {
    Files.walk(staging.toPath()).withCloseable { stream ->
      stream.filter { Files.isDirectory(it) }.forEach { Files.setPosixFilePermissions(it, PosixFilePermissions.fromString('rwx------')) }
    }
  }
  fixture.deleteDir()
}
