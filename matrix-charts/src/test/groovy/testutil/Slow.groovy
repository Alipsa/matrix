package testutil

import org.junit.jupiter.api.Tag

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Marks a test or test class as slow (involves chart rendering or file I/O).
 * Slow tests are excluded from the {@code testFast} Gradle task and run only
 * as part of the full {@code test} task.
 *
 * Usage on a whole class (every test in the class is slow):
 * <pre>
 * {@literal @}Slow
 * class GeomFunctionTest { ... }
 * </pre>
 *
 * Usage on individual methods in a mixed class:
 * <pre>
 * class GeomBarColTest {
 *   {@literal @}Test
 *   void testDefaults() { ... }   // fast — no @Slow
 *
 *   {@literal @}Slow
 *   {@literal @}Test
 *   void testSimpleBarChart() { ... }   // slow — chart.render()
 * }
 * </pre>
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@Tag('slow')
@interface Slow {}
