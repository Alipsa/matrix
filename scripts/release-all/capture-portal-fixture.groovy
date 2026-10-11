#!/usr/bin/env groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.http.*
import java.time.Duration

Properties properties = new Properties()
File configuration = new File(System.getProperty('user.home'), '.gradle/gradle.properties')
if (configuration.exists()) configuration.withInputStream { properties.load(it) }
String user = System.getenv('SONATYPE_USERNAME') ?: properties.getProperty('sonatypeUsername')
String password = System.getenv('SONATYPE_PASSWORD') ?: properties.getProperty('sonatypePassword')
if (!user || !password) throw new IllegalStateException('Set SONATYPE_USERNAME/SONATYPE_PASSWORD or Gradle sonatypeUsername/sonatypePassword')
String namespace = 'se.alipsa.matrix'
def client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
def request = { String name, String version ->
  String query = [namespace: namespace, name: name, version: version].collect { k, v -> "${k}=${URLEncoder.encode(v, 'UTF-8')}" }.join('&')
  def response = client.send(HttpRequest.newBuilder(URI.create('https://central.sonatype.com/api/v1/publisher/published?' + query))
    .timeout(Duration.ofSeconds(30)).header('Authorization', 'Bearer ' + Base64.encoder.encodeToString("${user}:${password}".getBytes('UTF-8'))).GET().build(), HttpResponse.BodyHandlers.ofString())
  if (response.statusCode() != 200) throw new IllegalStateException("Fixture request returned HTTP ${response.statusCode()}")
  def body = new JsonSlurper().parseText(response.body())
  if (!(body.published instanceof Boolean)) throw new IllegalStateException('Fixture response missing boolean published')
  [parameters: [namespace: namespace, name: name, version: version], status: response.statusCode(), body: [published: body.published]]
}
def fixture = [source: 'https://central.sonatype.com/api-doc#checkComponentPublished', captured: java.time.Instant.now().toString(),
  namespacePolicy: 'groupId', published: request('matrix-core', '3.9.0'), absent: request('matrix-core', '0.0.0-release-all-never-uploaded')]
if (fixture.published.body.published != true || fixture.absent.body.published != false) throw new IllegalStateException('Portal semantics not confirmed; discovery remains blocked')
new File(args ? args[0] : 'req/releaseAll-portal-fixture.json').text = JsonOutput.prettyPrint(JsonOutput.toJson(fixture))
println 'Confirmed exact Maven groupId namespace and published:false for never-uploaded coordinate; saved sanitized fixture.'
