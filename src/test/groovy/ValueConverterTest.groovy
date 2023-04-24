import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.ValueConverter

import static org.junit.jupiter.api.Assertions.*

class ValueConverterTest {

    @Test
    void testAsBoolean() {
        assertEquals(true, ValueConverter.asBoolean(1))
        assertEquals(true, ValueConverter.asBoolean(1.0))
        assertEquals(true, ValueConverter.asBoolean('YES'))
        assertEquals(true, ValueConverter.asBoolean('on'))
        assertEquals(true, ValueConverter.asBoolean('True'))

        assertEquals(false, ValueConverter.asBoolean(0))
        assertEquals(false, ValueConverter.asBoolean(-1.0))
        assertEquals(false, ValueConverter.asBoolean('NO'))
        assertEquals(false, ValueConverter.asBoolean('off'))
        assertEquals(false, ValueConverter.asBoolean('False'))
    }
}
