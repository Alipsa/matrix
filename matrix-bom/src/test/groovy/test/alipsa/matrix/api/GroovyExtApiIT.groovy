package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.ext.NumberExtension

import java.math.RoundingMode

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/** Verifies the registered NumberExtension methods through a transitive core dependency. */
@Tag('groovy-ext')
class GroovyExtApiIT implements ApiItSupport {

  @Test
  void numberExtensionCatalogAndRuntimeRegistration() {
    ['ceil', 'cos', 'exp', 'floor', 'log', 'log10', 'sin', 'sqrt', 'tan'].each { methodName ->
      assertTrue(NumberExtension.declaredMethods.any { it.name == methodName }, "missing extension $methodName")
    }
    assertEquals(2.0G, (100).log10())
    assertEquals(3G, 3.7G.floor())
    assertEquals(5G, 25G.sqrt())
    assertEquals(1G, (Math.PI / 2 as BigDecimal).sin().setScale(0, RoundingMode.HALF_UP))
  }
}
