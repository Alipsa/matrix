#!/usr/bin/env groovy
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions

// Shared by isolated verification and signing: preserve input settings and add a file remote.
if (args.size() < 3 || args.size() > 4) throw new IllegalArgumentException('usage: source-settings staging output-settings [repository-id=release-staging]')
File input = new File(args[0])
File output = new File(args[2])
String repositoryId = args.size() == 4 ? args[3] : 'release-staging'
File temporary
try {
  // Namespace-unaware parsing preserves default xmlns while allowing ordinary child names.
  Node settings = input.exists() ? new XmlParser(false, false).parse(input) : new Node(null, 'settings')
  Node profiles = settings.children().find { it instanceof Node && it.name().toString().tokenize(':').last() == 'profiles' } as Node
  if (profiles == null) profiles = settings.appendNode('profiles')
  if (profiles.children().any { it instanceof Node && it.children().any { child -> child instanceof Node && child.name().toString().tokenize(':').last() == 'id' && child.text() == repositoryId } }) {
    throw new IllegalArgumentException('Reserved staging profile already exists in input settings')
  }
  Node profile = profiles.appendNode('profile')
  profile.appendNode('id', repositoryId)
  Node repository = profile.appendNode('repositories').appendNode('repository')
  repository.appendNode('id', repositoryId)
  repository.appendNode('url', new File(args[1]).toURI().toString())
  repository.appendNode('releases').appendNode('enabled', 'true')
  repository.appendNode('snapshots').appendNode('enabled', 'false')
  Node active = settings.children().find { it instanceof Node && it.name().toString().tokenize(':').last() == 'activeProfiles' } as Node
  if (active == null) active = settings.appendNode('activeProfiles')
  active.appendNode('activeProfile', repositoryId)
  output.parentFile.mkdirs()
  temporary = Files.createTempFile(output.parentFile.toPath(), 'maven-settings-', '.xml', PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString('rw-------'))).toFile()
  temporary.setText(XmlUtil.serialize(settings), 'UTF-8')
  Files.move(temporary.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
} catch (Exception ignored) {
  throw new IllegalStateException('Cannot prepare isolated Maven settings; check the source XML, reserved staging profile and writable output directory')
} finally {
  temporary?.delete()
}
