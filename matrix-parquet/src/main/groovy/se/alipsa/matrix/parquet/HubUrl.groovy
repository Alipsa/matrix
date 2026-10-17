package se.alipsa.matrix.parquet

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/** Resolves namespaced dataset hf:// URIs, following huggingface_hub v2.2.0 parsing. */
final class HubUrl {
  private static final String SLASH = '/'
  private static final String ERR_FILE_PATH = 'A dataset file path is required'
  private static final String PLUS = '+'
  private static final String PARENT = '..'

  private static final int PARTS = 3
  private static final String PREFIX = 'hf://datasets/'
  private static final Pattern SPECIAL = Pattern.compile('^refs/(?:convert/[\\w.-]+|pr/\\d+)')

  private HubUrl() {}

  /** Resolves an hf://datasets/owner/repo[@revision]/path URI without contacting the Hub. */
  static URL resolve(String uri) {
    if (uri == null || !uri.startsWith(PREFIX)) { throw new IllegalArgumentException('Expected hf://datasets/owner/repo[@revision]/path') }
    String location = uri.substring(PREFIX.length()).replaceAll('^/+|/+$', '')
    int at = location.indexOf('@')
    String repo
    String revision = 'main'
    String path
    if (at < 0 || location.substring(0, at).count(SLASH) > 1) {
      String[] parts = location.split(SLASH, PARTS)
      if (parts.length < PARTS) { throw new IllegalArgumentException(ERR_FILE_PATH) }
      repo = parts[0] + SLASH + parts[1]
      path = parts[2]
    } else {
      repo = location.substring(0, at)
      String tail = location.substring(at + 1)
      def special = SPECIAL.matcher(tail)
      if (special.find()) {
        revision = special.group()
        path = tail.substring(revision.length()).replaceFirst('^/', '')
      } else {
        int slash = tail.indexOf(SLASH)
        if (slash < 0) { throw new IllegalArgumentException(ERR_FILE_PATH) }
        revision = tail.substring(0, slash)
        path = tail.substring(slash + 1)
      }
      revision = URLDecoder.decode(revision.replace(PLUS, '%2B'), StandardCharsets.UTF_8)
    }
    if (!(repo ==~ /[^\/@]+\/[^\/@]+/) || revision.empty || path.empty) {
      throw new IllegalArgumentException('Invalid dataset repository, revision or file path')
    }
    if (repo.contains('--') || repo.contains(PARENT) || repo.endsWith('.git') ||
        repo.split(SLASH).any { !(it ==~ /[\w](?:[\w.-]*[\w])?/) } || path.startsWith(SLASH) || path.contains('//') || hasDotSegments(path) || hasDotSegments(revision)) {
      throw new IllegalArgumentException('Invalid repository id or empty path segment')
    }
    String encodedRevision = SPECIAL.matcher(revision).matches() ? revision : encode(revision)
    String encodedPath = path.split(SLASH, -1).collect { encode(it) }.join(SLASH)
    new URI("https://huggingface.co/datasets/$repo/resolve/$encodedRevision/$encodedPath").toURL()
  }

  private static boolean hasDotSegments(String value) {
    value.split(SLASH).any { it in ['.', PARENT] }
  }

  private static String encode(String value) {
    URLEncoder.encode(value, StandardCharsets.UTF_8).replace(PLUS, '%20').replace('%7E', '~').replace('*', '%2A')
  }
}
