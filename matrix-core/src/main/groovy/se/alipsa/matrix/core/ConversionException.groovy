package se.alipsa.matrix.core

import groovy.transform.CompileStatic

@CompileStatic
class ConversionException extends RuntimeException {
    ConversionException(String message, Throwable cause) {
        super(message, cause)
    }

    ConversionException(GString message, Throwable cause) {
        super(message.toString(), cause)
    }
}
