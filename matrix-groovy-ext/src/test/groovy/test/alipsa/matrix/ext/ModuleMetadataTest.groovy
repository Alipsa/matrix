package test.alipsa.matrix.ext

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assumptions.assumeTrue

import org.junit.jupiter.api.Test

class ModuleMetadataTest {

  @Test
  void testExtensionModuleVersion() {
    Properties properties = new Properties()
    getClass().getResourceAsStream('/META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule').withCloseable {
      properties.load(it)
    }

    String projectVersion = System.getProperty('project.version')
    assumeTrue(projectVersion != null, 'project.version not set; run via Gradle')
    assertEquals(projectVersion, properties.moduleVersion)
  }
}
