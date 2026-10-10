#!/usr/bin/env groovy
import groovy.json.JsonOutput
import java.nio.file.Files

File root = new File(args ? args[0] : '.').canonicalFile
File temporary = Files.createTempDirectory('matrix-release-workflow').toFile().canonicalFile
try {
  File fixture = new File(temporary, 'matrix'); fixture.mkdirs()
  ['releaseAll.sh', 'scripts/release-all/Paths.groovy', 'scripts/release-all/preflight.sh', 'scripts/release-all/external-inventory.groovy', 'scripts/release-all/external-requirements.json'].each { String path ->
    File destination = new File(fixture, path); destination.parentFile.mkdirs()
    destination.bytes = new File(root, path).bytes
  }
  File plugin = new File(temporary, 'plugin/src/main/groovy/se/alipsa/gradle/plugin/release/ReleaseAllPlanTask.groovy')
  plugin.parentFile.mkdirs(); plugin.text = '// workflow fixture; no real builds or uploads'
  File gradle = new File(fixture, 'gradlew')
  gradle.text = '''#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/releaseAll
printf '%s\\n' "$*" >> build/commands.txt
for argument in "$@"; do
  if [[ "$argument" == releaseAllPlan ]]; then cp manifest-seed.json build/releaseAll/manifest.json; fi
  if [[ "$argument" == :matrix-gsheets:releaseAuthPreflight && "${FAIL_AUTH_PREFLIGHT:-false}" == true ]]; then echo 'Missing or expired ADC'; exit 1; fi
done
'''
  gradle.setExecutable(true)
  new File(fixture, '.gitignore').text = 'build/\n'
  File seed = new File(fixture, 'manifest-seed.json')
  seed.text = JsonOutput.toJson([selected: [], verificationScope: []])
  def execute = { List<String> command, Map<String, String> environment = [:], File directory = temporary ->
    def builder = new ProcessBuilder(command).directory(directory).redirectErrorStream(true)
    builder.environment().putAll(environment)
    Process process = builder.start()
    String output = process.inputStream.text
    [code: process.waitFor(), output: output]
  }
  execute(['git', '-C', fixture.path, 'init'])
  execute(['git', '-C', fixture.path, 'add', '.'])
  execute(['git', '-C', fixture.path, '-c', 'user.name=Test', '-c', 'user.email=test@example.org', 'commit', '-m', 'Workflow fixture'])
  def release = { List<String> options, Map<String, String> overrides = [:] ->
    execute(['bash', new File(fixture, 'releaseAll.sh').path] + options,
      [RUN_EXTERNAL_TESTS: 'false', RUN_SLOW_TESTS: 'false', RELEASE_ALL_DEDICATED_EXTERNAL_TESTS: 'false', RELEASE_ALL_PLUGIN_DIR: new File(temporary, 'plugin').path] + overrides)
  }
  [['', 'foo'], ['--dry-run', 'x'], ['--bundle-only', 'x'], ['--resume'], ['--unknown']].each { options ->
    def invalid = release(options)
    assert invalid.code == 2 && invalid.output.contains('usage: ./releaseAll.sh'): invalid.output
  }
  def commands = { new File(fixture, 'build/commands.txt').readLines() }
  def result = release(['--dry-run'])
  assert result.code == 0: result.output
  assert result.output.contains('No eligible unreleased components')
  assert commands().size() == 1 && commands().first().contains('releaseAllPlan')
  new File(fixture, 'build/commands.txt').delete()
  seed.text = JsonOutput.toJson([selected: [[projectPath: ':matrix-core']], verificationScope: [':matrix-core', ':matrix-csv']])
  execute(['git', '-C', fixture.path, 'add', '.'])
  execute(['git', '-C', fixture.path, '-c', 'user.name=Test', '-c', 'user.email=test@example.org', 'commit', '-m', 'Selected fixture'])
  result = release(['--bundle-only'])
  assert result.code == 0: result.output
  assert commands().any { it.contains(':matrix-core:build') && it.contains(':matrix-csv:build') && it.contains('-Pheadless=true') }
  assert commands().findIndexOf { it.contains('codenarcMain') } < commands().findIndexOf { it.contains('spotlessCheck') }
  assert commands().findIndexOf { it.contains('spotlessCheck') } < commands().findIndexOf { it.contains(':matrix-core:build') }
  assert !commands().any { it.contains('externalTest') || it.endsWith(' releaseAll') }
  assert commands().any { it.contains('releaseAllStage') }
  assert commands().any { it.contains('releaseAllPrepareMaven') }
  assert commands().last().endsWith('bundleAll')
  new File(fixture, 'build/commands.txt').delete()
  result = release([])
  assert result.code == 0: result.output
  assert commands().count { it.endsWith(' releaseAll') } == 1
  new File(fixture, 'build/commands.txt').delete()
  File bin = new File(fixture, 'bin'); bin.mkdirs()
  String realGroovy = execute(['bash', '-c', 'command -v groovy']).output.trim()
  File groovy = new File(bin, 'groovy')
  groovy.text = '''#!/usr/bin/env bash
if [[ "$*" == *raw.githubusercontent.com* ]]; then echo 'Fixture endpoint unavailable' >&2; exit 1; fi
exec "$REAL_GROOVY" "$@"
'''
  groovy.setExecutable(true)
  File docker = new File(bin, 'docker'); docker.text = '#!/usr/bin/env bash\nexit "${DOCKER_FIXTURE_STATUS:-0}"\n'; docker.setExecutable(true)
  Map<String, String> external = [RUN_EXTERNAL_TESTS: 'true', REAL_GROOVY: realGroovy, PATH: bin.path + File.pathSeparator + System.getenv('PATH')]
  def selectModule = { String module, String className, String method ->
    File test = new File(fixture, "${module.substring(1)}/src/test/groovy/Fixture.groovy"); test.parentFile.mkdirs()
    String packageName = className.substring(0, className.lastIndexOf('.'))
    String simpleName = className.substring(className.lastIndexOf('.') + 1)
    test.text = "package ${packageName}\nimport org.junit.jupiter.api.*\n@Tag('external') class ${simpleName} { @Test void ${method}() {} }"
    seed.text = JsonOutput.toJson([selected: [[projectPath: module]], verificationScope: [module]])
    execute(['git', '-C', fixture.path, 'add', '.'])
    execute(['git', '-C', fixture.path, '-c', 'user.name=Test', '-c', 'user.email=test@example.org', 'commit', '-m', 'External fixture'])
  }
  selectModule(':matrix-gsheets', 'test.alipsa.matrix.gsheets.GsTest', 'testExport')
  ['--dry-run', '--bundle-only'].each { mode ->
    result = release([mode], external + [FAIL_AUTH_PREFLIGHT: 'true'])
    assert result.code != 0 && result.output.contains('Missing or expired ADC')
    assert commands().any { it.contains(':matrix-gsheets:releaseAuthPreflight') }
    assert !commands().any { it.contains('codenarcMain') || it.contains(':build') || it.contains('releaseAllStage') }
    new File(fixture, 'build/commands.txt').delete()
  }
  selectModule(':matrix-datasets', 'datasets.RdatasetsTest', 'fetchInfo')
  result = release(['--dry-run'], external)
  assert result.code != 0 && result.output.contains('Dataset tests require reachable')
  assert !commands().any { it.contains('releaseAuthPreflight') || it.contains(':build') }
  new File(fixture, 'build/commands.txt').delete()
  selectModule(':matrix-bigquery', 'test.alipsa.matrix.bigquery.BqDataTypesTest', 'testBigDecimal')
  result = release(['--dry-run'], external)
  assert result.code == 0 && !commands().any { it.contains('releaseExternalTest') }
  new File(fixture, 'build/commands.txt').delete()
  Map dedicated = external + [RELEASE_ALL_DEDICATED_EXTERNAL_TESTS: 'true', GOOGLE_APPLICATION_CREDENTIALS: new File(temporary, 'missing-adc.json').path]
  [[GOOGLE_CLOUD_PROJECT: ''], [GOOGLE_CLOUD_PROJECT: 'fixture', DOCKER_FIXTURE_STATUS: '1'], [GOOGLE_CLOUD_PROJECT: 'fixture', DOCKER_FIXTURE_STATUS: '0']].each { prerequisites ->
    result = release(['--dry-run'], dedicated + prerequisites)
    assert result.code != 0: result.output
    assert !commands().any { it.contains(':build') || it.contains('releaseAllStage') }
    new File(fixture, 'build/commands.txt').delete()
  }
  // Resume must ignore source dirtiness and test prerequisites.
  seed.append('dirty')
  result = release(['--resume', 'saved-id'], [RUN_EXTERNAL_TESTS: 'false', RELEASE_ALL_DEDICATED_EXTERNAL_TESTS: 'true'])
  assert result.code == 0: result.output
  assert commands().size() == 1 && commands().first().contains('releaseAllResume=saved-id')
  new File(fixture, 'build/commands.txt').delete()
  result = release(['--dry-run'], [RELEASE_ALL_DEDICATED_EXTERNAL_TESTS: 'true'])
  assert result.code != 0 && result.output.contains('Dedicated external tests require')
  assert !new File(fixture, 'build/commands.txt').exists()
  println 'Workflow acceptance passed: empty selection, caller directory, scoped build/order, bundle-only, upload once, early authentication/endpoint failures, opt-in BigQuery prerequisites, resume and contradictory flags.'
} finally {
  temporary.deleteDir()
}
