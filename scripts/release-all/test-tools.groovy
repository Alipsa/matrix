#!/usr/bin/env groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Files

File root = new File(args ? args[0] : '.').canonicalFile
File temporary = Files.createTempDirectory('matrix-release-tools').toFile().canonicalFile
int assertions = 0
try {
  def run = { List<String> command, Map<String, String> environment = [:] ->
    def builder = new ProcessBuilder(command).redirectErrorStream(true)
    builder.environment().putAll(environment)
    Process process = builder.start()
    String text = process.inputStream.text
    int code = process.waitFor()
    [code: code, text: text]
  }
  File preflight = new File(root, 'scripts/release-all/preflight.sh')
  ['release', 'bom'].each { mode ->
    ['21', '21.0.12.1', '20.0.2', '22', '210'].each { version ->
      def checked = run(['bash', '-c', 'source "$1"; fixture_version=$2; java() { echo "openjdk version \\"$fixture_version\\"" >&2; }; mvn() { echo "Apache Maven 3.9.9"; }; groovy() { :; }; RELEASE_ROOT=$3; release_tools_preflight "$4"', 'fixture', preflight.path, version, root.path, mode])
      boolean accepted = mode == 'release' ? version in ['21', '21.0.12.1'] : version != '20.0.2'
      assert (checked.code == 0) == accepted: checked.text
      assertions++
    }
  }
  File paths = new File(root, 'scripts/release-all/Paths.groovy')
  File withSpace = new File(temporary, 'with space'); withSpace.mkdirs()
  new File(withSpace, 'module').mkdirs()
  new File(withSpace, 'artifact.pom').text = 'pom'
  def result = run(['groovy', paths.path, withSpace.path, new File(temporary, 'missing/child').path])
  assert result.code == 0
  def normalized = new JsonSlurper().parseText(result.text)
  assert normalized[0].entries == ['artifact.pom', 'module']
  assert normalized[0].directories == ['module']
  assert normalized[1].path.endsWith('/missing/child')
  assertions += 4
  assert run(['groovy', paths.path, temporary.path + '/../unsafe']).code != 0
  File link = new File(temporary, 'link')
  Files.createSymbolicLink(link.toPath(), withSpace.toPath())
  assert run(['groovy', paths.path, new File(link, 'child').path]).code != 0
  def writableAndProtected = run(['groovy', paths.path, withSpace.path, link.path], [RELEASE_PATHS_STRICT_COUNT: '1'])
  assert writableAndProtected.code == 0
  assert new JsonSlurper().parseText(writableAndProtected.text)[1].path == withSpace.path
  def rejectedLink = run(['groovy', paths.path, new File(link, 'child').path])
  assert rejectedLink.text.contains(link.path)
  assertions += 5

  // Exercise the verifier's actual guards without including or executing any deletion/build code.
  String verifier = new File(root, 'matrix-bom/verifyBomApi.sh').text
  String guards = verifier.substring(verifier.indexOf('reject_dotdot() {'), verifier.indexOf('\nnorm() {'))
  File guardScript = new File(temporary, 'guards.sh')
  guardScript.text = '''#!/usr/bin/env bash
set -euo pipefail
REPO=$1
ROOT_DIR=$2
BOM_DIR="$ROOT_DIR/matrix-bom"
USER_REPO_SET=x
''' + guards + '\nassert_safe_repo_path\n'
  File homeDirectory = new File(System.getProperty('user.home'))
  [root, root.parentFile, new File(root, 'matrix-bom'), homeDirectory, new File(homeDirectory, '.m2/repository'), new File('/')].each { protectedPath ->
    assert run(['bash', guardScript.path, protectedPath.path, root.path]).code != 0
  }
  File repository = new File(temporary, 'guard repository'); repository.mkdirs()
  assert run(['bash', guardScript.path, repository.path, root.path]).code != 0
  new File(repository, '.matrix-bom-verify-repo').text = ''
  assert run(['bash', guardScript.path, repository.path, root.path]).code == 0
  new File(repository, 'settings.gradle').text = '// protected project'
  assert run(['bash', guardScript.path, repository.path, root.path]).code != 0
  assert run(['bash', guardScript.path, repository.path + '/../unsafe', root.path]).code != 0
  assert run(['bash', guardScript.path, new File(link, 'repository').path, root.path]).code != 0
  assertions += 11

  File settings = new File(temporary, 'settings.xml')
  settings.text = '''<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><servers><server><id>existing</id><password>fixture-only</password></server></servers><profiles><profile><id>signing</id><properties><gpg.keyname>fixture-key</gpg.keyname></properties></profile></profiles><activeProfiles><activeProfile>signing</activeProfile></activeProfiles></settings>'''
  String originalSettings = settings.text
  File generated = new File(temporary, 'generated-settings.xml')
  File settingsHelper = new File(root, 'scripts/release-all/staged-settings.groovy')
  result = run(['groovy', settingsHelper.path, settings.path, withSpace.path, generated.path, 'matrix-release-staging'])
  assert result.code == 0
  def merged = new groovy.xml.XmlParser(false, false).parse(generated)
  assert merged.servers.server.password.text() == 'fixture-only'
  assert merged.profiles.profile.find { it.id.text() == 'signing' }.properties.'gpg.keyname'.text() == 'fixture-key'
  assert merged.activeProfiles.activeProfile*.text() == ['signing', 'matrix-release-staging']
  assert merged.profiles.profile.find { it.id.text() == 'matrix-release-staging' }.repositories.repository.url.text() == withSpace.toURI().toString()
  assert Files.getPosixFilePermissions(generated.toPath()) == java.nio.file.attribute.PosixFilePermissions.fromString('rw-------')
  assert settings.text == originalSettings
  assert run(['groovy', settingsHelper.path, generated.path, withSpace.path, new File(temporary, 'conflict.xml').path, 'matrix-release-staging']).code != 0
  assertions += 8

  File bom = new File(temporary, 'bom.xml')
  bom.text = '<project><properties><arff.version>0.3.0</arff.version></properties><dependencyManagement><dependencies><dependency><artifactId>matrix-arff</artifactId><version>${arff.version}</version></dependency></dependencies></dependencyManagement></project>'
  File bomManifest = new File(temporary, 'bom-manifest.json')
  Map selectedArff = [projectPath: ':matrix-arff', groupId: 'se.alipsa.matrix', artifactId: 'matrix-arff', version: '0.3.2', files: []]
  bomManifest.text = JsonOutput.toJson([selected: [selectedArff]])
  File bomHelper = new File(root, 'scripts/release-all/manifest-bom.groovy')
  result = run(['groovy', bomHelper.path, bomManifest.path, bom.path, temporary.path])
  assert result.code == 0 && result.text.trim() == '__bom_selected=false'
  bomManifest.text = JsonOutput.toJson([selected: [selectedArff, [artifactId: 'matrix-bom']]])
  assert run(['groovy', bomHelper.path, bomManifest.path, bom.path, temporary.path]).code != 0
  assertions += 2

  File project = new File(temporary, 'fixture'); project.mkdirs()
  File mapping = new File(project, 'scripts/release-all/external-requirements.json')
  mapping.parentFile.mkdirs(); mapping.bytes = new File(root, 'scripts/release-all/external-requirements.json').bytes
  File test = new File(project, 'matrix-gsheets/src/test/groovy/Test.groovy'); test.parentFile.mkdirs()
  test.text = "package test.alipsa.matrix.gsheets\nimport org.junit.jupiter.api.Tag\n@Tag('external') class GsTest { @org.junit.jupiter.api.Test void testExport() {} }"
  File manifest = new File(temporary, 'manifest.json')
  manifest.text = JsonOutput.toJson([verificationScope: [':matrix-gsheets']])
  File inventory = new File(root, 'scripts/release-all/external-inventory.groovy')
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code == 0 && result.text.readLines().last() == 'gsheets'
  test.text = "package test.alipsa.matrix.gsheets\n// @Tag('external')\nclass GsAuthenticatorTest {}"
  assert run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true']).code == 0
  test.text = "import org.junit.jupiter.api.Tag\n@Tag('external') class UnmappedTest { @org.junit.jupiter.api.Test void testUnknown() {} }"
  assert run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true']).code != 0
  test.text = "package test.alipsa.matrix.gsheets\nimport org.junit.jupiter.api.*\n@Tag('external') class GsTest { @Test @Tag('flaky') void testExport() {} }"
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code == 0 && !result.text.trim()
  test.text = "package test.alipsa.matrix.gsheets\nimport org.junit.jupiter.api.*\n@Tag('external') class GsTest { @Test @Disabled void testExport() {} }"
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code == 0 && !result.text.trim()
  test.text = "package test.alipsa.matrix.gsheets\nimport org.junit.jupiter.api.*\n@Tag('external') class GsTest { @Test @Tag('slow') void testExport() {} }"
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true', RUN_SLOW_TESTS: 'false'])
  assert result.code == 0 && !result.text.trim()
  test.text = "package test.alipsa.matrix.gsheets\nimport org.junit.jupiter.api.Tag as TestTag\n@TestTag('external') class GsTest { @org.junit.jupiter.api.Test void testExport() {} }"
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code == 0 && result.text.readLines().last() == 'gsheets'
  assertions += 4
  assert run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'false']).code == 0
  assert run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'false', RELEASE_ALL_DEDICATED_EXTERNAL_TESTS: 'true']).code != 0
  File composed = new File(test.parentFile, 'Slow.groovy')
  composed.text = "package testutil\n@org.junit.jupiter.api.Tag('slow') @interface Slow {}"
  test.text = "package test.alipsa.matrix.gsheets\nimport testutil.Slow\n@org.junit.jupiter.api.Tag('external') @Slow class GsTest { @org.junit.jupiter.api.Test void testExport() {} }"
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true', RUN_SLOW_TESTS: 'false'])
  assert result.code == 0 && !result.text.trim(): result.text
  composed.delete()
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code != 0 && result.text.contains('Cannot resolve test annotation testutil.Slow')
  File base = new File(test.parentFile, 'Base.groovy')
  base.text = "@org.junit.jupiter.api.Tag('external') class Base {}"
  test.text = 'class Child extends Base { @org.junit.jupiter.api.Test void inherited() {} }'
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code != 0 && result.text.contains('inherited test tags')
  base.delete()
  assertions += 3
  test.delete()
  File javaTest = new File(project, 'matrix-gsheets/src/test/java/JavaFixture.java')
  javaTest.parentFile.mkdirs()
  javaTest.text = '// @Tag("external")\nclass JavaFixture {}'
  assert run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true']).code == 0
  javaTest.text = 'import org.junit.jupiter.api.Tag; @Tag("external") class JavaFixture { @org.junit.jupiter.api.Test void testUnknown() {} }'
  assert run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true']).code != 0
  javaTest.text = 'import custom.Slow; @Slow class JavaFixture { @org.junit.jupiter.api.Test void testUnknown() {} }'
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code != 0 && result.text.contains('Cannot resolve test annotation custom.Slow')
  // The base name intentionally differs from its filename to exercise AST indexing.
  javaTest.text = '@org.junit.jupiter.api.Tag("external") class TaggedBase {} class Child extends TaggedBase {}'
  result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code != 0 && result.text.contains('inherited test tags')
  assertions += 2
  javaTest.delete()
  Map fixtureMappings = new JsonSlurper().parse(mapping) as Map
  fixtureMappings['test.alipsa.matrix.gsheets.Outer$Inner#nested'] = [module: ':matrix-gsheets', requirements: ['gsheets']]
  mapping.text = JsonOutput.toJson(fixtureMappings)
  ['groovy', 'java'].each { language ->
    File nested = language == 'java' ? javaTest : test
    nested.text = language == 'java' ?
      'package test.alipsa.matrix.gsheets; import org.junit.jupiter.api.*; @Tag("external") class Outer { @Nested class Inner { @Test void nested() {} } }' :
      "package test.alipsa.matrix.gsheets\nimport org.junit.jupiter.api.*\n@Tag('external') class Outer { @Nested class Inner { @Test void nested() {} } }"
    result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
    assert result.code == 0 && result.text.readLines().last() == 'gsheets': result.text
    nested.text = nested.text.replace('class Outer', '@Disabled class Outer')
    result = run(['groovy', inventory.path, manifest.path, project.path], [RUN_EXTERNAL_TESTS: 'true'])
    assert result.code == 0 && !result.text.trim(): result.text
    nested.delete()
    assertions += 2
  }
  javaTest.delete()
  assertions += 2
  assertions += 5
  // Current BOM tests contain no external tags and add no credentials or endpoint probes.
  manifest.text = JsonOutput.toJson([selected: [[artifactId: 'matrix-bom']], verificationScope: []])
  result = run(['groovy', inventory.path, manifest.path, root.path], [RUN_EXTERNAL_TESTS: 'true'])
  assert result.code == 0 && !result.text.trim()
  assertions++
  println "Portable path and external annotation inventory checks passed (${assertions} assertions)."
} finally {
  // Only the freshly created test fixture is removed; production helpers never delete paths.
  temporary.deleteDir()
}
