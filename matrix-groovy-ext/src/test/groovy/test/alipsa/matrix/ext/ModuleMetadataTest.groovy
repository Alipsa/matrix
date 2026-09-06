package test.alipsa.matrix.ext

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

class ModuleMetadataTest {

  @Test
  void testExtensionModuleVersion() {
    Properties properties = new Properties()
    getClass().getResourceAsStream('/META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule').withCloseable {
      properties.load(it)
    }

    assertEquals('0.4.0-snapshot', properties.moduleVersion)
  }
}
