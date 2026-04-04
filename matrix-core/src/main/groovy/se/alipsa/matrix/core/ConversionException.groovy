package se.alipsa.matrix.core

import groovy.transform.CompileStatic

/**
 * Exception thrown when a matrix value cannot be converted to the requested type.
 *
 * <p>Conversion utilities wrap lower-level parsing or coercion failures in this
 * exception to preserve the original cause while adding matrix-specific context.</p>
 */
@CompileStatic
class ConversionException extends RuntimeException {

    ConversionException(String message, Throwable cause) {
        super(message, cause)
    }

    ConversionException(GString message, Throwable cause) {
        super(message.toString(), cause)
    }

}
