package se.alipsa.matrix.spreadsheet.fastods.reader

import com.fasterxml.aalto.stax.InputFactoryImpl
import groovy.transform.CompileStatic

import javax.xml.stream.XMLInputFactory

/**
 * Provides a high-performance, thread-safe XMLInputFactory for ODS parsing.
 * <p>
 * Uses the Aalto StAX parser which is 10-30% faster than the JDK default StAX implementation
 * (Woodstox or com.sun.xml). The factory is configured for security (no DTD, no external entities)
 * and optimized for throughput.
 * <p>
 * The factory instance is shared across all ODS imports and is thread-safe.
 * <p>
 * Based on fastexcel's approach (org.dhatim.fastexcel.reader.DefaultXMLInputFactory).
 *
 * @see <a href="https://github.com/FasterXML/aalto-xml">Aalto XML</a>
 */
@CompileStatic
final class OptimizedXMLInputFactory {

    /**
     * Shared, thread-safe XMLInputFactory configured for maximum performance.
     * Uses Aalto StAX parser with security hardening (DTD and external entities disabled).
     */
    static final XMLInputFactory INSTANCE = createFactory()

    private static XMLInputFactory createFactory() {
        XMLInputFactory factory = new InputFactoryImpl()

        // Security: disable DTD processing and external entity resolution to prevent XXE attacks
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE)
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE)

        return factory
    }

    private OptimizedXMLInputFactory() {
        // Utility class - prevent instantiation
        throw new AssertionError('Utility class should not be instantiated')
    }
}
