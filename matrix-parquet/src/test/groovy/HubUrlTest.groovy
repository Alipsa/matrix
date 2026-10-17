import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.parquet.HubUrl

/** Dataset cases pinned to huggingface_hub v2.2.0 parse_hf_uri and file URL quoting. */
class HubUrlTest {
  @Test
  void revisionsAndLiteralFileNames() {
    new File('src/test/resources/optimized/hub-urls.tsv').readLines('UTF-8')
        .findAll { !it.startsWith('#') }.each { String line ->
          String[] parts = line.split('\t', 2)
          assertEquals(parts[1], HubUrl.resolve(parts[0]).toString())
        }
    ['hf://models/owner/repo/file', 'hf://datasets/owner/repo@/file', 'hf://datasets/owner/repo'].each { value ->
      assertThrows(IllegalArgumentException) { HubUrl.resolve(value) }
    }
  }
}
