package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLInputFactory

/**
 * XML parser hardening utilities to reduce XXE exposure.
 */
@CompileStatic
final class XmlSecurityUtil {

  private static final Logger logger = LogManager.getLogger()

  private XmlSecurityUtil() {
    // utility class
  }

  /**
   * Create an {@link XMLInputFactory} with DTD and external entities disabled.
   */
  static XMLInputFactory newSecureInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance()
    configureXmlInputFactory(factory)
    factory
  }

  /**
   * Apply XXE hardening settings to an existing {@link XMLInputFactory}.
   */
  static void configureXmlInputFactory(XMLInputFactory factory) {
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
  }

  /**
   * Apply XXE hardening settings to a {@link DocumentBuilderFactory}.
   * Unsupported features are logged and ignored to keep parsing functional.
   */
  static void configureDocumentBuilderFactory(DocumentBuilderFactory factory) {
    setFeatureSafe(factory, "http://apache.org/xml/features/disallow-doctype-decl", true)
    setFeatureSafe(factory, "http://xml.org/sax/features/external-general-entities", false)
    setFeatureSafe(factory, "http://xml.org/sax/features/external-parameter-entities", false)
    setFeatureSafe(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
  }

  private static void setFeatureSafe(DocumentBuilderFactory factory, String feature, boolean value) {
    try {
      factory.setFeature(feature, value)
    } catch (Exception ex) {
      logger.warn("Failed to set XML parser feature {} to {}. Continuing without it.", feature, value, ex)
    }
  }
}
