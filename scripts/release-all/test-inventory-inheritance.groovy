#!/usr/bin/env groovy
import groovy.json.JsonOutput
import java.nio.file.Files

File root = new File(args ? args[0] : '.').canonicalFile
File temporary = Files.createTempDirectory('matrix-inventory-inheritance').toFile()
try {
  File mapping = new File(temporary, 'scripts/release-all/external-requirements.json')
  mapping.parentFile.mkdirs()
  mapping.text = '{}'
  File manifest = new File(temporary, 'manifest.json')
  manifest.text = JsonOutput.toJson([verificationScope: [':fixture']])
  Closure scan = { File sourceRoot, boolean slow ->
    def builder = new ProcessBuilder('groovy', new File(root, 'scripts/release-all/external-inventory.groovy').path,
      manifest.path, sourceRoot.path).redirectErrorStream(true)
    builder.environment().putAll([RUN_EXTERNAL_TESTS: 'true', RUN_SLOW_TESTS: slow.toString(),
      RELEASE_ALL_DEDICATED_EXTERNAL_TESTS: 'true'])
    Process process = builder.start()
    String output = process.inputStream.text
    [code: process.waitFor(), output: output]
  }
  ['groovy', 'java'].each { language ->
    File sources = new File(temporary, "fixture/src/test/${language}")
    sources.mkdirs()
    File base = new File(sources, "Base.${language}")
    File child = new File(sources, "Child.${language}")
    File unrelated = new File(sources, "Other.${language}")
    base.text = 'package fixture; class Base<T> {}'
    child.text = 'package fixture; class Child extends Base<String> { @org.junit.jupiter.api.Test void test() {} }'
    def result = scan(temporary, true)
    assert result.code == 0: result.output

    // Follow a grandparent even when the intermediate class has no annotations.
    unrelated.text = 'package fixture; @org.junit.jupiter.api.Tag("external") class Ancestor {}'
    base.text = 'package fixture; class Base<T> extends Ancestor {}'
    result = scan(temporary, true)
    assert result.code != 0 && result.output.contains('inherited test tags') && result.output.contains('fixture.Ancestor'): result.output

    unrelated.delete()
    base.text = 'package fixture; @org.junit.jupiter.api.Disabled class Base<T> {}'
    result = scan(temporary, true)
    assert result.code != 0 && result.output.contains('inherited test tags'): result.output

    // A tagged class in a different package cannot turn a library base into a local one.
    base.text = 'package unrelated; @org.junit.jupiter.api.Tag("external") class ArrayList {}'
    child.text = 'package fixture; import java.util.ArrayList; class Child extends ArrayList<String> {}'
    result = scan(temporary, true)
    assert result.code == 0: result.output
    child.text = 'package fixture; class Child extends java.util.ArrayList<String> {}'
    result = scan(temporary, true)
    assert result.code == 0: result.output

    // A composed tag on a superclass must also trigger the diagnostic.
    base.text = 'package fixture; @testutil.Slow class Base<T> {}'
    child.text = 'package fixture; class Child extends Base<String> {}'
    File annotation = new File(temporary, 'fixture/src/test/groovy/Slow.groovy')
    annotation.parentFile.mkdirs()
    annotation.text = 'package testutil; @org.junit.jupiter.api.Tag("slow") @interface Slow {}'
    result = scan(temporary, false)
    assert result.code != 0 && result.output.contains('inherited test tags'): result.output
    annotation.delete()
    sources.deleteDir()
  }
  // Exercise the actual repository, including untagged test bases and nested helpers.
  List<String> modules = root.listFiles().findAll { it.directory && it.name.startsWith('matrix-') && it.name != 'matrix-examples' }.collect { ':' + it.name }
  assert !modules.empty
  manifest.text = JsonOutput.toJson([verificationScope: modules])
  [true, false].each { slow ->
    def result = scan(root, slow)
    assert result.code == 0: result.output
  }
  println 'Inheritance inventory checks passed: untagged helpers, ancestor tags, disabled bases, generic Java bases, qualified/imported library names, composed tags and all repository modules with slow tests on/off.'
} finally {
  temporary.deleteDir()
}
