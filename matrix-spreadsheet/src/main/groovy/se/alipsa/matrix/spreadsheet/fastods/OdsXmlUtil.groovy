package se.alipsa.matrix.spreadsheet.fastods

import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

/**
 * Constants and helpers for working with OpenDocument XML namespaces and qualified names.
 */
class OdsXmlUtil {

  static final String OPENDOCUMENT_MIMETYPE = 'application/vnd.oasis.opendocument.spreadsheet'
  static final String TABLE_URN = 'urn:oasis:names:tc:opendocument:xmlns:table:1.0'
  static final String OFFICE_URN = 'urn:oasis:names:tc:opendocument:xmlns:office:1.0'
  static final String TEXT_URN = 'urn:oasis:names:tc:opendocument:xmlns:text:1.0'

  private static final String EQUALS = '='
  private static final String SPACE = ' '

  static QName oqn(String localPart) {
    new QName(OFFICE_URN, localPart)
  }

  static QName tqn(String localPart) {
    new QName(TABLE_URN, localPart)
  }

  static QName textQn(String localPart) {
    new QName(TEXT_URN, localPart)
  }

  // Useful for debugging
  static String attributes(StartElement startElement) {
    StringBuilder sb = new StringBuilder()
    startElement.attributes.each {
      sb.append(it.name.localPart)
          .append(EQUALS)
          .append(it.value)
          .append(SPACE)
    }
    sb.toString()
  }

  // Useful for debugging
  static String attributes(XMLStreamReader reader) {
    StringBuilder sb = new StringBuilder()
    if (reader.isStartElement()) {
      for (int i = 0; i < reader.getAttributeCount(); i++) {
        sb.append(reader.getAttributeLocalName(i))
            .append(EQUALS)
            .append(reader.getAttributeValue(i))
            .append(SPACE)
      }
    }
    sb.toString()
  }

  // Useful for debugging
  static String eventTypeName(int eventTypeCode) {
    return switch (eventTypeCode) {
      case XMLStreamConstants.ATTRIBUTE -> 'ATTRIBUTE'
      case XMLStreamConstants.CDATA -> 'CDATA'
      case XMLStreamConstants.CHARACTERS -> 'CHARACTERS'
      case XMLStreamConstants.COMMENT -> 'COMMENT'
      case XMLStreamConstants.START_ELEMENT -> 'START_ELEMENT'
      case XMLStreamConstants.END_ELEMENT -> 'END_ELEMENT'
      case XMLStreamConstants.NAMESPACE -> 'NAMESPACE'
      case XMLStreamConstants.PROCESSING_INSTRUCTION -> 'PROCESSING_INSTRUCTION'
      case XMLStreamConstants.START_DOCUMENT -> 'START_DOCUMENT'
      case XMLStreamConstants.END_DOCUMENT -> 'END_DOCUMENT'
      case XMLStreamConstants.DTD -> 'DTD'
      default -> 'unknown'
    }
  }

  // Useful for debugging
  static String elementName(XMLEvent event) {
    if (event.isStartElement()) {
      return event.asStartElement().name.localPart
    }
    if (event.isEndElement()) {
      return event.asEndElement().name.localPart
    }
    ''
  }

}
