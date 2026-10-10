#!/usr/bin/env groovy
import groovy.json.JsonOutput
import java.nio.file.Files
import java.nio.file.Path

// One invocation handles every path/listing in a workflow phase; no GNU utilities.
// Only writable targets reject links; protected read-only paths are canonicalized.
String configuredCount = System.getenv('RELEASE_PATHS_STRICT_COUNT')
int strictCount = configuredCount == null ?
  (System.getenv('RELEASE_PATHS_RELEASE') == 'true' ? 2 : args.length) : configuredCount.toInteger()
int index = 0
def result = args.collect { String input ->
  boolean strict = index++ < strictCount
  Path path = Path.of(input).toAbsolutePath()
  if (path.any { it.toString() == '..' }) throw new IllegalArgumentException("Path contains dot-dot: ${input}")
  Path cursor = path
  List<String> missing = []
  while (!Files.exists(cursor)) {
    if (Files.isSymbolicLink(cursor)) throw new IllegalArgumentException("Symlink path component: ${cursor} (input: ${input})")
    missing.add(0, cursor.fileName.toString())
    cursor = cursor.parent
  }
  if (missing && !Files.isDirectory(cursor)) throw new IllegalArgumentException("Existing path ancestor is not a directory: ${input}")
  Path check = cursor
  while (check != null) {
    if (strict && Files.isSymbolicLink(check)) throw new IllegalArgumentException("Symlink path component: ${check} (input: ${input})")
    check = check.parent
  }
  Path canonical = cursor.toRealPath()
  missing.each { canonical = canonical.resolve(it) }
  List<String> entries = []
  List<String> directories = []
  if (Files.isDirectory(canonical)) {
    Files.list(canonical).withCloseable { stream ->
      stream.forEach { child ->
        entries.add(child.fileName.toString())
        if (Files.isDirectory(child)) directories.add(child.fileName.toString())
      }
    }
  }
  [path: canonical.toString(), entries: entries.sort(), directories: directories.sort()]
}
if (System.getenv('RELEASE_PATHS_RELEASE') == 'true') {
  if (result.size() != 6 || !args.take(2).every { Path.of(it).isAbsolute() }) {
    throw new IllegalArgumentException('Release staging and cache must be absolute paths')
  }
  List<Path> repositories = result.take(2).collect { Path.of(it.path) }
  List<Path> protectedPaths = result.drop(2).collect { Path.of(it.path) }
  repositories.each { Path repository ->
    if (repository.parent == null || protectedPaths.any { it == repository || it.startsWith(repository) } || repository.startsWith(protectedPaths.last())) {
      throw new IllegalArgumentException('Release repository overlaps protected project/home/Maven cache paths')
    }
  }
  if (repositories[0].startsWith(repositories[1]) || repositories[1].startsWith(repositories[0])) {
    throw new IllegalArgumentException('Release staging and download cache must be separate')
  }
}
if (System.getenv('RELEASE_PATHS_TSV') == 'true') {
  result.each { println([it.path, it.entries.join(','), it.directories.join(',')].join('|')) }
} else if (System.getenv('RELEASE_PATHS_LINES') == 'true') {
  result.each { println it.path }
} else if (System.getenv('RELEASE_PATHS_LIST') == 'true') {
  result.each { it.entries.each { println it } }
} else if (System.getenv('RELEASE_PATHS_DIRS') == 'true') {
  result.each { it.directories.each { println it } }
} else {
  println JsonOutput.toJson(result)
}
