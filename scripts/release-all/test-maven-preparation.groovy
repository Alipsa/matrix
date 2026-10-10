#!/usr/bin/env groovy
import groovy.json.JsonOutput
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Signs only disposable fixture coordinates. No deploy goal or publishing endpoint is configured.
File root = new File(args ? args[0] : '.').canonicalFile
File plugin = new File(args.size() > 1 ? args[1] : '../nexus-release-plugin').canonicalFile
File output = new File(root, 'build/releaseAll'); output.mkdirs()
File fixture = Files.createTempDirectory(output.toPath(), 'signed-maven-').toFile()
File keys = new File(fixture, 'gpg'); keys.mkdirs()
Files.setPosixFilePermissions(keys.toPath(), PosixFilePermissions.fromString('rwx------'))
File log = new File(output, 'signed-maven-test.log')
log.text = ''
def run = { List<String> command, File directory = fixture ->
  def builder = new ProcessBuilder(command).directory(directory).redirectErrorStream(true)
  builder.environment().put('GNUPGHOME', keys.path)
  Process process = builder.start()
  String text = process.inputStream.text
  log.append(text)
  assert process.waitFor() == 0: "Fixture command failed; inspect ${log}"
  text.trim()
}
try {
  run(['gpg', '--batch', '--pinentry-mode', 'loopback', '--passphrase', '', '--quick-generate-key', 'Release fixture <fixture@example.invalid>', 'rsa2048', 'sign', '1d'])
  String fingerprint = run(['gpg', '--batch', '--with-colons', '--list-secret-keys']).readLines().find { it.startsWith('fpr:') }.split(':')[9]
  String group = 'se.alipsa.matrix.releasefixture'
  String version = '0.0.0-fixture'
  String metadata = '<name>Release fixture</name><description>Offline publication preparation test</description><url>https://example.invalid</url><licenses><license><name>MIT</name><url>https://example.invalid/license</url></license></licenses><developers><developer><name>Fixture</name></developer></developers><scm><url>https://example.invalid/repo</url><connection>scm:git:https://example.invalid/repo</connection></scm>'
  String signing = '<profiles><profile><id>release</id><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-gpg-plugin</artifactId><version>3.2.8</version><executions><execution><phase>verify</phase><goals><goal>sign</goal></goals></execution></executions><configuration><gpgArguments><arg>--pinentry-mode</arg><arg>loopback</arg></gpgArguments></configuration></plugin></plugins></build></profile></profiles>'
  String library = "<dependency><groupId>${group}</groupId><artifactId>library</artifactId><version>1</version></dependency>"
  File bom = new File(fixture, 'bom.xml')
  bom.text = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion><groupId>${group}</groupId><artifactId>bom</artifactId><version>${version}</version><packaging>pom</packaging>${metadata}<dependencyManagement><dependencies>${library}</dependencies></dependencyManagement>${signing}</project>"
  File all = new File(fixture, 'pom.xml')
  String flatten = '<build><plugins><plugin><groupId>org.codehaus.mojo</groupId><artifactId>flatten-maven-plugin</artifactId><version>1.7.3</version><configuration><flattenMode>ossrh</flattenMode></configuration><executions><execution><phase>process-resources</phase><goals><goal>flatten</goal></goals></execution></executions></plugin><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-jar-plugin</artifactId><version>3.4.2</version></plugin></plugins></build>'
  all.text = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion><groupId>${group}</groupId><artifactId>all</artifactId><version>${version}</version>${metadata}<dependencyManagement><dependencies><dependency><groupId>${group}</groupId><artifactId>bom</artifactId><version>${version}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement><dependencies>${library.replace('<version>1</version>', '')}</dependencies>${flatten}${signing}</project>"
  File settings = new File(fixture, 'user-settings.xml')
  settings.text = "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\"><profiles><profile><id>fixture-signing</id><properties><gpg.keyname>${fingerprint}</gpg.keyname></properties></profile></profiles><activeProfiles><activeProfile>fixture-signing</activeProfile></activeProfiles></settings>"
  String originalSettings = settings.text
  File helper = new File(fixture, 'settings-helper.groovy'); helper.bytes = new File(root, 'scripts/release-all/staged-settings.groovy').bytes
  File verifier = new File(fixture, 'verify.sh')
  verifier.text = '#!/usr/bin/env bash\nset -euo pipefail\n[[ "$1" == --manifest && "$3" == --staging && "$5" == --local ]]\necho fixture-verifier-called\n'
  new File(fixture, '.gitignore').text = 'build/\n.gradle/\ngpg/\ntarget/\n.flattened-pom.xml\n'
  new File(fixture, 'settings.gradle').text = "pluginManagement { includeBuild(${JsonOutput.toJson(plugin.path)}) }; rootProject.name='signed-preparation-fixture'"
  new File(fixture, 'build.gradle').text = '''plugins { id 'se.alipsa.nexus-release-all' }
releaseAllPrepareMaven {
  sourceDirectory = layout.projectDirectory
  manifestFile = layout.buildDirectory.file('manifest.json')
  stagingDirectory = layout.buildDirectory.dir('staging')
  localRepository = layout.buildDirectory.dir('maven-local')
  mavenWorkDirectory = layout.projectDirectory
  verifierFile = layout.projectDirectory.file('verify.sh')
  settingsHelperFile = layout.projectDirectory.file('settings-helper.groovy')
  userSettingsFile = layout.projectDirectory.file('user-settings.xml')
}
bundleAll {
  sourceDirectory = layout.projectDirectory
  manifestFile = layout.buildDirectory.file('manifest.json')
  stagingDirectory = layout.buildDirectory.dir('staging')
  bundleFile = layout.buildDirectory.file('bundle.zip')
  receiptFile = layout.buildDirectory.file('receipt.json')
}
'''
  run(['git', 'init']); run(['git', 'add', '.'])
  run(['git', '-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-m', 'Signed preparation fixture'])
  File stage = new File(fixture, 'build/staging')
  File libraryDir = new File(stage, group.replace('.', '/') + '/library/1'); libraryDir.mkdirs()
  new File(libraryDir, 'library-1.pom').text = "<project><modelVersion>4.0.0</modelVersion><groupId>${group}</groupId><artifactId>library</artifactId><version>1</version></project>"
  new ZipOutputStream(new FileOutputStream(new File(libraryDir, 'library-1.jar'))).withCloseable { zip ->
    zip.putNextEntry(new ZipEntry('fixture.txt')); zip.write('fixture'.bytes); zip.closeEntry()
  }
  Map edge = [groupId: group, artifactId: 'library', version: '1', scope: 'compile', type: 'jar', classifier: '']
  List<Map> selected = [[groupId: group, artifactId: 'bom', version: version, packaging: 'pom', role: 'bom', pomFile: 'bom.xml', files: ["bom-${version}.pom"], publishedDependencies: [], publishedManagement: [edge]],
    [groupId: group, artifactId: 'all', version: version, packaging: 'jar', role: 'aggregate', sourcesAndJavadocRequired: false, pomFile: 'pom.xml', files: ["all-${version}.pom", "all-${version}.jar"], publishedDependencies: [edge], publishedManagement: []]]
  new File(fixture, 'build/manifest.json').text = JsonOutput.toJson([revision: run(['git', 'rev-parse', 'HEAD']), hashes: [:], versions: [:], selected: selected])
  run([new File(root, 'gradlew').path, '-p', fixture.path, 'releaseAllPrepareMaven', 'bundleAll', '--no-configuration-cache', '--max-workers=2'])
  selected.each { component ->
    component.files.each { name ->
      File artifact = new File(stage, "${group.replace('.', '/')}/${component.artifactId}/${version}/${name}")
      assert artifact.isFile() && new File(artifact.path + '.asc').isFile()
      run(['gpg', '--batch', '--verify', artifact.path + '.asc', artifact.path])
    }
  }
  assert settings.text == originalSettings
  assert !new File(fixture, 'build').listFiles().any { it.name.startsWith('signing-settings-') }
  assert log.text.contains('fixture-verifier-called') && log.text.contains('flatten:')
  assert !log.text.contains('maven-deploy-plugin')
  assert new File(fixture, 'build/bundle.zip').isFile()
  assert run(['git', 'status', '--porcelain']).empty
  println "Signed Maven preparation passed: exact staged dependency, BOM install, flattened aggregate POM, GPG signatures, allow-listed bundle and preserved user settings. Log: ${log}"
} finally {
  try {
    def shutdown = new ProcessBuilder('gpgconf', '--kill', 'gpg-agent')
    shutdown.environment().put('GNUPGHOME', keys.path)
    shutdown.redirectErrorStream(true).start().waitFor()
  } finally {
    fixture.deleteDir()
  }
}
