#!/usr/bin/env groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Files

// Independent Gradle source-set fixture: no root build's Groovy platform inheritance.
File root = new File(args ? args[0] : '.').canonicalFile
File output = new File(root, 'build/releaseAll'); output.mkdirs()
File fixture = Files.createTempDirectory(output.toPath(), 'auth-classpath-').toFile()
File log = new File(output, 'auth-classpath-test.log'); log.text = ''
def run = { List<String> command, File directory, boolean succeeds = true ->
  Process process = new ProcessBuilder(command).directory(directory).redirectErrorStream(true).start()
  String text = process.inputStream.text
  log.append(text)
  int status = process.waitFor()
  assert succeeds ? status == 0 : status != 0: "Unexpected fixture result; inspect ${log}"
  text
}
try {
  File lazyGuard = new File(fixture, 'lazy-classpath.init.gradle')
  lazyGuard.text = """gradle.beforeProject { module ->
  if (module.name == 'matrix-gsheets') {
    module.configurations.configureEach { configuration ->
      if (configuration.name == 'compileClasspath') {
        configuration.incoming.beforeResolve {
          throw new GradleException('GSheets compileClasspath resolved during help')
        }
      }
    }
  }
}"""
  run([new File(root, 'gradlew').path, 'help', '-I', lazyGuard.path, '--no-configuration-cache', '--offline'], root)
  File init = new File(fixture, 'classpath.init.gradle')
  File classpathFile = new File(fixture, 'classpath.json')
  init.text = """allprojects { module ->
  if (module.name == 'matrix-gsheets') {
    module.afterEvaluate {
      tasks.register('writeReleaseAuthClasspath') {
        dependsOn tasks.named('classes'), tasks.named('authPreflightClasses'), tasks.named('testClasses')
        doLast { new File(${JsonOutput.toJson(classpathFile.path)}).text = groovy.json.JsonOutput.toJson([runtime: sourceSets.authPreflight.runtimeClasspath.files.collect { it.absolutePath }, tests: sourceSets.test.runtimeClasspath.files.collect { it.absolutePath }, agent: configurations.mockitoAgent.singleFile.absolutePath]) }
      }
    }
  }
}"""
  run([new File(root, 'gradlew').path, ':matrix-gsheets:writeReleaseAuthClasspath', '-I', init.path, '--no-configuration-cache', '--max-workers=3'], root)
  Map resolved = new JsonSlurper().parse(classpathFile) as Map
  List<String> classpath = (resolved.runtime + resolved.tests).unique() as List<String>
  classpath = classpath.findAll { !new File(it).name.startsWith('groovy') && !it.contains('/authPreflight') }
  File source = new File(fixture, 'src/authPreflight/groovy/se/alipsa/matrix/gsheets/preflight'); source.mkdirs()
  ['AuthPreflight.groovy', 'ExternalAuthRequirements.groovy'].each { name ->
    new File(source, name).bytes = new File(root, 'matrix-gsheets/src/authPreflight/groovy/se/alipsa/matrix/gsheets/preflight/' + name).bytes
  }
  new File(source, 'ControlledPreflight.groovy').text = '''package se.alipsa.matrix.gsheets.preflight
import test.alipsa.matrix.gsheets.AuthPreflightTest
class ControlledPreflight {
  static void main(String[] args) {
    def checks = new AuthPreflightTest()
    checks.currentUnionIncludesCleanupAndAcceptsBroaderGrants()
    checks.entryPointUsesSharedScopesAndProbesSheetsAndDrive()
    checks.entryPointSanitizesCredentialFailures()
  }
}
'''
  new File(fixture, 'compileStatic.groovy').bytes = new File(root, 'config/groovy/compileStatic.groovy').bytes
  new File(fixture, 'settings.gradle').text = "rootProject.name='isolated-auth-preflight'"
  new File(fixture, 'build.gradle').text = """plugins { id 'groovy' }
repositories { mavenCentral() }
sourceSets { authPreflight }
configurations { authPreflightExecution }
dependencies {
  authPreflightImplementation platform('org.apache.groovy:groovy-bom:5.1.1')
  authPreflightRuntimeOnly platform('org.apache.groovy:groovy-bom:5.1.1')
  authPreflightImplementation 'org.apache.groovy:groovy'
  authPreflightImplementation 'org.apache.groovy:groovy-json'
  authPreflightImplementation files(${JsonOutput.toJson(classpath)})
  authPreflightExecution platform('org.apache.groovy:groovy-bom:5.1.1')
  authPreflightExecution 'org.apache.groovy:groovy'
  authPreflightExecution 'org.apache.groovy:groovy-json'
}
tasks.withType(GroovyCompile).configureEach { groovyOptions.configurationScript = file('compileStatic.groovy') }
tasks.register('releaseAuthPreflight', JavaExec) {
  dependsOn tasks.named('authPreflightClasses')
  classpath = sourceSets.authPreflight.runtimeClasspath + configurations.authPreflightExecution
  mainClass = 'se.alipsa.matrix.gsheets.preflight.AuthPreflight'
  args '--check-classpath'
}
tasks.register('controlledAuthPreflight', JavaExec) {
  dependsOn tasks.named('authPreflightClasses')
  classpath = sourceSets.authPreflight.runtimeClasspath + configurations.authPreflightExecution
  mainClass = 'se.alipsa.matrix.gsheets.preflight.ControlledPreflight'
  jvmArgs '-javaagent:' + ${JsonOutput.toJson(resolved.agent)}
}
"""
  String success = run([new File(root, 'gradlew').path, '-p', fixture.path, 'releaseAuthPreflight', 'controlledAuthPreflight', '--no-configuration-cache', '--max-workers=3'], fixture)
  assert success.contains('Authentication preflight runtime classpath resolved')
  assert success.contains('Sheets external test authentication and network preflight passed')
  File entry = new File(source, 'AuthPreflight.groovy')
  entry.text = entry.text.replace('GsAuthenticator.authenticate(ExternalAuthRequirements.scopes())', 'GsAuthenticator.authenticate(ExternalAuthRequirements.scopes(), 12345)')
  String failure = run([new File(root, 'gradlew').path, '-p', fixture.path, 'compileAuthPreflightGroovy', '--no-configuration-cache', '--max-workers=3'], fixture, false)
  assert failure.contains('Static type checking') && failure.contains('authenticate')
  println "Isolated authentication JavaExec passed with explicit Groovy BOM/runtime, JSON, main/core classes and controlled authentication/probe responses; static compilation rejected an invalid authenticator signature. Log: ${log}"
} finally {
  fixture.deleteDir()
}
