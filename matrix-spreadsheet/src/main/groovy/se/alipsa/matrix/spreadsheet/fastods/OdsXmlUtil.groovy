package se.alipsa.matrix.spreadsheet.fastods


import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class OdsXmlUtil {

  static final String OPENDOCUMENT_MIMETYPE = "application/vnd.oasis.opendocument.spreadsheet";
  static final String tableUrn = 'urn:oasis:names:tc:opendocument:xmlns:table:1.0'
  static final String officeUrn = 'urn:oasis:names:tc:opendocument:xmlns:office:1.0'

  static String attributes(StartElement startElement) {
    StringBuilder sb = new StringBuilder()
    startElement.attributes.each {
      sb.append(it.name.localPart)
          .append('=')
          .append(it.value)
          .append(' ')
    }
    sb.toString()
  }

  static String attributes(XMLStreamReader reader) {
    StringBuilder sb = new StringBuilder()
    if (reader.isStartElement())
      for (int i = 0; i < reader.getAttributeCount(); i++) {
        sb.append(reader.getAttributeLocalName(i))
            .append('=')
            .append(reader.getAttributeValue(i))
            .append(' ')
      }
    sb.toString()
  }

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

  static String elementName(XMLEvent event) {
    if (event.isStartElement()) {
      return event.asStartElement().name.localPart
    }
    if (event.isEndElement()) {
      return event.asEndElement().name.localPart
    }
    ''
  }

  static QName oqn(String localPart) {
    new QName(officeUrn, localPart)
  }

  static QName tqn(String localPart) {
    new QName(tableUrn, localPart)
  }
}
