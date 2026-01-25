package se.alipsa.matrix.spreadsheet.fastexcel

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.XmlSecurityUtil

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.io.StringReader
import java.util.regex.Pattern

@CompileStatic
class FExcelAppender {

  private static final String WORKBOOK_PATH = "xl/workbook.xml"
  private static final String RELS_PATH = "xl/_rels/workbook.xml.rels"
  private static final String CONTENT_TYPES = "[Content_Types].xml"
  private static final String APP_PATH = "docProps/app.xml"
  private static final String WORKSHEET_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"
  private static final String WORKSHEET_REL_TYPE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"

  static List<String> appendOrReplaceSheets(File file, List<Matrix> data, List<String> sheetNames) {
    if (!file.exists() || file.length() == 0) {
      return FExcelExporter.exportExcelSheets(file, data, sheetNames)
    }
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Matrices and sheet names lists must have the same size")
    }
    Map<String, Matrix> requested = buildRequestedMap(data, sheetNames)
    File tmp = File.createTempFile("matrix-xlsx", ".xlsx", file.parentFile)
    boolean moved = false
    try {
      try (ZipFile zip = new ZipFile(file); FileOutputStream fos = new FileOutputStream(tmp); ZipOutputStream zos = new ZipOutputStream(fos)) {
        WorkbookState state = readWorkbookState(zip)
        WorkbookPlan plan = buildPlan(state, requested)

        Enumeration<? extends ZipEntry> entries = zip.entries()
        Set<String> written = new HashSet<>()
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement()
          String name = entry.name
          if (name == WORKBOOK_PATH) {
            writeEntry(zos, WORKBOOK_PATH, plan.workbookXml)
            written.add(WORKBOOK_PATH)
            continue
          }
          if (name == RELS_PATH) {
            writeEntry(zos, RELS_PATH, plan.relsXml)
            written.add(RELS_PATH)
            continue
          }
          if (name == CONTENT_TYPES) {
            writeEntry(zos, CONTENT_TYPES, plan.contentTypesXml)
            written.add(CONTENT_TYPES)
            continue
          }
          if (name == APP_PATH) {
            writeEntry(zos, APP_PATH, plan.appXml)
            written.add(APP_PATH)
            continue
          }
          if (plan.replacements.containsKey(name)) {
            writeEntry(zos, name, plan.replacements.get(name))
            written.add(name)
            continue
          }
          copyEntry(zip, entry, zos)
          written.add(name)
        }
        plan.additions.each { String path, String xml ->
          if (!written.contains(path)) {
            writeEntry(zos, path, xml)
          }
        }
      }
      Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
      moved = true
      return requested.keySet().toList()
    } finally {
      if (!moved && tmp.exists()) {
        tmp.delete()
      }
    }
  }

  private static Map<String, Matrix> buildRequestedMap(List<Matrix> data, List<String> sheetNames) {
    Map<String, Matrix> requested = new LinkedHashMap<>()
    for (int i = 0; i < data.size(); i++) {
      String name = SpreadsheetUtil.createValidSheetName(sheetNames.get(i))
      requested.put(name, data.get(i))
    }
    requested
  }

  private static WorkbookState readWorkbookState(ZipFile zip) {
    String workbookXml = readEntry(zip, WORKBOOK_PATH)
    String relsXml = readEntry(zip, RELS_PATH)
    String contentXml = readEntry(zip, CONTENT_TYPES)
    String appXml = readEntry(zip, APP_PATH)
    return new WorkbookState(workbookXml, relsXml, contentXml, appXml, zip)
  }

  private static WorkbookPlan buildPlan(WorkbookState state, Map<String, Matrix> requested) {
    Document workbook = parseXml(state.workbookXml)
    Document rels = parseXml(state.relsXml)
    Document types = parseXml(state.contentTypesXml)
    Document app = state.appXml ? parseXml(state.appXml) : null

    Map<String, SheetInfo> existing = readSheets(workbook, rels)
    List<Integer> sheetIds = existing.values().collect { it.sheetId } as List<Integer>
    List<Integer> sheetIndexes = existing.values().collect { it.sheetIndex } as List<Integer>
    int nextSheetId = nextValue(sheetIds, 1)
    int nextRelId = maxRelId(rels) + 1
    int nextSheetIndex = nextValue(sheetIndexes, 1)

    Map<String, String> replacements = new LinkedHashMap<>()
    Map<String, String> additions = new LinkedHashMap<>()
    Map<String, SheetTemplate> templateCache = [:]
    SheetTemplate baseTemplate = readBaseTemplate(state.zip, existing)
    baseTemplate = ensureTemplate(state.zip, existing, baseTemplate)

    requested.each { String name, Matrix matrix ->
      SheetInfo info = existing.get(name)
      if (info != null) {
        SheetTemplate template = templateForPath(state.zip, templateCache, info.path) ?: baseTemplate
        replacements.put(info.path, buildSheetXml(matrix, template))
      } else {
        String sheetPath = "xl/worksheets/sheet${nextSheetIndex++}.xml"
        String relId = "rId${nextRelId++}"
        int sheetId = nextSheetId++
        addSheet(workbook, name, sheetId, relId)
        addRelationship(rels, relId, sheetPath)
        addContentType(types, sheetPath)
        additions.put(sheetPath, buildSheetXml(matrix, baseTemplate))
        existing.put(name, new SheetInfo(name, sheetId, relId, sheetPath, sheetId, relIdNumber(relId), sheetNumberFromPath(sheetPath)))
      }
    }

    String workbookXml = serialize(workbook)
    String relsXml = serialize(rels)
    String contentTypesXml = serialize(types)
    String appXml = app ? updateAppXml(app, existing.keySet().toList()) : null

    return new WorkbookPlan(workbookXml, relsXml, contentTypesXml, appXml, replacements, additions)
  }

  private static Map<String, SheetInfo> readSheets(Document workbook, Document rels) {
    Map<String, String> relTargets = [:]
    NodeList relNodes = rels.getElementsByTagName("Relationship")
    for (int i = 0; i < relNodes.length; i++) {
      Element rel = (Element) relNodes.item(i)
      relTargets[rel.getAttribute("Id")] = rel.getAttribute("Target")
    }

    Map<String, SheetInfo> sheets = [:]
    Element sheetsNode = firstElementByTag(workbook, "sheets")
    if (sheetsNode != null) {
      NodeList sheetNodes = sheetsNode.getElementsByTagName("sheet")
      for (int i = 0; i < sheetNodes.length; i++) {
        Element sheet = (Element) sheetNodes.item(i)
        String name = sheet.getAttribute("name")
        int sheetId = Integer.parseInt(sheet.getAttribute("sheetId"))
        String relId = sheet.getAttribute("r:id")
        String target = relTargets[relId]
        String path = target ? "xl/${target}" : null
        int sheetIndex = sheetNumberFromPath(path)
        sheets[name] = new SheetInfo(name, sheetId, relId, path, sheetId, relIdNumber(relId), sheetIndex)
      }
    }
    return sheets
  }

  private static int maxRelId(Document rels) {
    int maxId = 0
    NodeList relNodes = rels.getElementsByTagName("Relationship")
    for (int i = 0; i < relNodes.length; i++) {
      Element rel = (Element) relNodes.item(i)
      String id = rel.getAttribute("Id")
      if (id?.startsWith("rId")) {
        try {
          int num = Integer.parseInt(id.substring(3))
          maxId = Math.max(maxId, num)
        } catch (NumberFormatException ignored) {
          // ignore non-numeric rIds
        }
      }
    }
    return maxId
  }

  private static void addSheet(Document workbook, String name, int sheetId, String relId) {
    Element sheetsNode = firstElementByTag(workbook, "sheets")
    Element sheet = workbook.createElement("sheet")
    sheet.setAttribute("name", name)
    sheet.setAttribute("sheetId", String.valueOf(sheetId))
    sheet.setAttribute("r:id", relId)
    sheetsNode.appendChild(sheet)
  }

  private static void addRelationship(Document rels, String relId, String sheetPath) {
    Element rel = rels.createElement("Relationship")
    rel.setAttribute("Id", relId)
    rel.setAttribute("Type", WORKSHEET_REL_TYPE)
    rel.setAttribute("Target", sheetPath.replaceFirst("^xl/", ""))
    rels.documentElement.appendChild(rel)
  }

  private static void addContentType(Document types, String sheetPath) {
    Element override = types.createElement("Override")
    override.setAttribute("PartName", "/" + sheetPath)
    override.setAttribute("ContentType", WORKSHEET_CONTENT_TYPE)
    types.documentElement.appendChild(override)
  }

  private static String updateAppXml(Document app, List<String> sheetNames) {
    Element titles = firstChildElementBySuffix(app.documentElement, "TitlesOfParts")
    Element titlesVector = titles ? firstChildElementBySuffix(titles, "vector") : null
    if (titlesVector != null) {
      titlesVector.setAttribute("size", String.valueOf(sheetNames.size()))
      removeAllChildElements(titlesVector)
      String lpstrTag = tagWithSamePrefix(titlesVector, "lpstr")
      sheetNames.each { String name ->
        Element lpstr = app.createElement(lpstrTag)
        lpstr.setTextContent(name)
        titlesVector.appendChild(lpstr)
      }
    }
    Element headingPairs = firstChildElementBySuffix(app.documentElement, "HeadingPairs")
    Element headingVector = headingPairs ? firstChildElementBySuffix(headingPairs, "vector") : null
    if (headingVector != null) {
      Element countNode = firstChildElementBySuffix(headingVector, "i4")
      if (countNode != null) {
        countNode.setTextContent(String.valueOf(sheetNames.size()))
      }
    }
    return serialize(app)
  }

  private static Element firstElementByTag(Document doc, String tagName) {
    NodeList nodes = doc.getElementsByTagName(tagName)
    return nodes.length > 0 ? (Element) nodes.item(0) : null
  }

  private static Element firstChildElementBySuffix(Element parent, String suffix) {
    NodeList nodes = parent.getChildNodes()
    for (int i = 0; i < nodes.length; i++) {
      Node node = nodes.item(i)
      if (node instanceof Element) {
        String tag = ((Element) node).getTagName()
        if (tag.endsWith(suffix)) {
          return (Element) node
        }
      }
    }
    return null
  }

  private static String tagWithSamePrefix(Element template, String localName) {
    String tag = template.getTagName()
    int idx = tag.indexOf(':')
    return idx > 0 ? tag.substring(0, idx + 1) + localName : localName
  }

  private static void removeAllChildElements(Element parent) {
    NodeList nodes = parent.getChildNodes()
    for (int i = nodes.length - 1; i >= 0; i--) {
      Node node = nodes.item(i)
      parent.removeChild(node)
    }
  }

  private static int relIdNumber(String relId) {
    if (relId == null) return 0
    return relId.replace("rId", "").toInteger()
  }

  private static int sheetNumberFromPath(String path) {
    if (path == null) return 0
    def m = path =~ /sheet(\d+)\.xml/
    if (m.find()) {
      return Integer.parseInt(m.group(1))
    }
    return 0
  }

  private static String readEntry(ZipFile zip, String name) {
    ZipEntry entry = zip.getEntry(name)
    if (entry == null) {
      throw new IllegalArgumentException("Missing entry $name in xlsx file")
    }
    new String(zip.getInputStream(entry).bytes, StandardCharsets.UTF_8)
  }

  private static void copyEntry(ZipFile zip, ZipEntry entry, ZipOutputStream zos) {
    ZipEntry out = new ZipEntry(entry.name)
    zos.putNextEntry(out)
    zos.write(zip.getInputStream(entry).bytes)
    zos.closeEntry()
  }

  private static void writeEntry(ZipOutputStream zos, String name, String content) {
    ZipEntry entry = new ZipEntry(name)
    zos.putNextEntry(entry)
    zos.write(content.getBytes(StandardCharsets.UTF_8))
    zos.closeEntry()
  }

  private static Document parseXml(String xml) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(false)
    XmlSecurityUtil.configureDocumentBuilderFactory(factory)
    def builder = factory.newDocumentBuilder()
    return builder.parse(new InputSource(new StringReader(xml)))
  }

  private static String serialize(Document doc) {
    TransformerFactory tf = TransformerFactory.newInstance()
    Transformer transformer = tf.newTransformer()
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    transformer.setOutputProperty(OutputKeys.INDENT, "no")
    StringWriter out = new StringWriter()
    transformer.transform(new DOMSource(doc), new StreamResult(out))
    out.toString()
  }

  private static String buildSheetXml(Matrix matrix, SheetTemplate template) {
    StringBuilder sb = new StringBuilder()
    sb.append('<?xml version="1.0" encoding="UTF-8"?>')
    sb.append('<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" ')
      .append('xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">')
    sb.append('<dimension ref="').append(sheetDimension(matrix)).append('"/>')
    if (template?.sheetFormatXml) {
      sb.append(template.sheetFormatXml)
    } else if (template?.sheetFormatAttributes && !template.sheetFormatAttributes.isEmpty()) {
      sb.append('<sheetFormatPr')
      template.sheetFormatAttributes.each { String key, String value ->
        sb.append(' ').append(key).append('="').append(escapeXml(value)).append('"')
      }
      sb.append('/>')
    }
    if (template?.colsXml) {
      sb.append(template.colsXml)
    }
    sb.append('<sheetData>')
    writeRow(sb, 1, matrix.columnNames())
    List<Column> columns = matrix.columns()
    int rowCount = matrix.rowCount()
    int colCount = columns.size()
    for (int r = 0; r < rowCount; r++) {
      List<Object> row = new ArrayList<>(colCount)
      for (int c = 0; c < colCount; c++) {
        row.add(columns.get(c).get(r))
      }
      writeRow(sb, r + 2, row)
    }
    sb.append('</sheetData>')
    if (template?.pageMarginsXml) {
      sb.append(template.pageMarginsXml)
    }
    sb.append('</worksheet>')
    sb.toString()
  }

  private static String sheetDimension(Matrix matrix) {
    int rowCount = Math.max(1, matrix.rowCount() + 1)
    int colCount = Math.max(1, matrix.columnCount())
    String lastCol = SpreadsheetUtil.asColumnName(colCount)
    return "A1:${lastCol}${rowCount}"
  }

  private static void writeRow(StringBuilder sb, int rowNum, List<?> values) {
    sb.append('<row r="').append(rowNum).append('">')
    values.each { Object value ->
      appendCell(sb, value)
    }
    sb.append('</row>')
  }

  private static void appendCell(StringBuilder sb, Object value) {
    if (value == null) {
      sb.append('<c/>')
      return
    }
    if (value instanceof Boolean) {
      sb.append('<c t="b"><v>').append(((Boolean) value) ? '1' : '0').append('</v></c>')
      return
    }
    if (value instanceof Number) {
      String v = ValueConverter.asBigDecimal(value).toPlainString()
      sb.append('<c t="n"><v>').append(v).append('</v></c>')
      return
    }
    if (value instanceof LocalDate) {
      String v = ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE)
      sb.append('<c t="d"><v>').append(v).append('</v></c>')
      return
    }
    if (value instanceof LocalDateTime) {
      String v = ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      sb.append('<c t="d"><v>').append(v).append('</v></c>')
      return
    }
    if (value instanceof ZonedDateTime) {
      String v = ((ZonedDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      sb.append('<c t="d"><v>').append(v).append('</v></c>')
      return
    }
    if (value instanceof OffsetDateTime) {
      String v = ((OffsetDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      sb.append('<c t="d"><v>').append(v).append('</v></c>')
      return
    }
    if (value instanceof Date) {
      String v = ((Date) value).toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      sb.append('<c t="d"><v>').append(v).append('</v></c>')
      return
    }
    appendInlineString(sb, String.valueOf(value))
  }

  private static void appendInlineString(StringBuilder sb, String value) {
    String text = escapeXml(value)
    if (text.startsWith(' ') || text.endsWith(' ')) {
      sb.append('<c t="inlineStr"><is><t xml:space="preserve">').append(text).append('</t></is></c>')
    } else {
      sb.append('<c t="inlineStr"><is><t>').append(text).append('</t></is></c>')
    }
  }

  private static String escapeXml(String value) {
    if (value == null) {
      return ''
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
  }

  private static int nextValue(List<Integer> values, int defaultValue) {
    if (values == null || values.isEmpty()) {
      return defaultValue
    }
    Integer maxValue = values.max()
    return (maxValue == null ? defaultValue : maxValue + 1)
  }

  private static SheetTemplate readBaseTemplate(ZipFile zip, Map<String, SheetInfo> existing) {
    SheetInfo first = existing?.values()?.find { it.path }
    SheetTemplate template = first ? readSheetTemplate(zip, first.path) : null
    if (template != null) {
      return template
    }
    String fallback = findFirstWorksheetPath(zip)
    return fallback ? readSheetTemplate(zip, fallback) : null
  }

  private static SheetTemplate ensureTemplate(ZipFile zip, Map<String, SheetInfo> existing, SheetTemplate template) {
    if (template != null && template.sheetFormatXml && template.colsXml && template.pageMarginsXml) {
      return template
    }
    String path = existing?.values()?.find { it.path }?.path ?: findFirstWorksheetPath(zip)
    if (path == null) {
      return template
    }
    SheetTemplate fallback = readSheetTemplate(zip, path)
    if (fallback == null) {
      return template
    }
    if (template == null) {
      return fallback
    }
    return new SheetTemplate(
        template.sheetFormatAttributes ?: fallback.sheetFormatAttributes,
        template.sheetFormatXml ?: fallback.sheetFormatXml,
        template.colsXml ?: fallback.colsXml,
        template.pageMarginsXml ?: fallback.pageMarginsXml
    )
  }

  private static SheetTemplate templateForPath(ZipFile zip, Map<String, SheetTemplate> cache, String path) {
    if (path == null) {
      return null
    }
    SheetTemplate cached = cache.get(path)
    if (cached != null) {
      return cached
    }
    SheetTemplate template = readSheetTemplate(zip, path)
    cache.put(path, template)
    return template
  }

  private static SheetTemplate readSheetTemplate(ZipFile zip, String sheetPath) {
    if (sheetPath == null) {
      return null
    }
    String sheetXml = readEntry(zip, sheetPath)
    String sheetFormatXml = extractElementXml(sheetXml, "sheetFormatPr")
    Map<String, String> sheetFormatAttributes = sheetFormatXml ? readSheetFormatAttributesFallback(sheetXml) : null
    String colsXml = extractElementXml(sheetXml, "cols")
    String pageMarginsXml = extractElementXml(sheetXml, "pageMargins")
    if ((sheetFormatAttributes == null || sheetFormatAttributes.isEmpty()) && sheetFormatXml == null && colsXml == null && pageMarginsXml == null) {
      return null
    }
    return new SheetTemplate(sheetFormatAttributes, sheetFormatXml, colsXml, pageMarginsXml)
  }

  private static Map<String, String> readAttributes(Element sheet, String nodeName) {
    Element node = firstChildElementByTag(sheet, nodeName)
    if (node == null) {
      return null
    }
    Map<String, String> attrs = new LinkedHashMap<>()
    def attributes = node.getAttributes()
    for (int i = 0; i < attributes.length; i++) {
      def attr = attributes.item(i)
      String name = attr.nodeName
      if (!name.contains(":")) {
        attrs.put(name, attr.nodeValue)
      }
    }
    return attrs
  }

  private static String buildSimpleElementXml(Element sheet, String nodeName) {
    Element node = firstChildElementByTag(sheet, nodeName)
    if (node == null) {
      return null
    }
    StringBuilder sb = new StringBuilder()
    sb.append('<').append(nodeName)
    def attributes = node.getAttributes()
    for (int i = 0; i < attributes.length; i++) {
      def attr = attributes.item(i)
      String name = attr.nodeName
      if (!name.contains(":")) {
        sb.append(' ').append(name).append('="').append(escapeXml(attr.nodeValue)).append('"')
      }
    }
    sb.append('/>')
    sb.toString()
  }

  private static String buildColsXml(Element sheet) {
    Element cols = firstChildElementByTag(sheet, "cols")
    if (cols == null) {
      return null
    }
    StringBuilder sb = new StringBuilder()
    sb.append('<cols>')
    NodeList colNodes = cols.getElementsByTagName("col")
    for (int i = 0; i < colNodes.length; i++) {
      Element col = (Element) colNodes.item(i)
      sb.append('<col')
      def attributes = col.getAttributes()
      for (int j = 0; j < attributes.length; j++) {
        def attr = attributes.item(j)
        String name = attr.nodeName
        if (!name.contains(":")) {
          sb.append(' ').append(name).append('="').append(escapeXml(attr.nodeValue)).append('"')
        }
      }
      sb.append('/>')
    }
    sb.append('</cols>')
    sb.toString()
  }

  private static Element firstChildElementByTag(Element parent, String tagName) {
    NodeList nodes = parent.getChildNodes()
    for (int i = 0; i < nodes.length; i++) {
      Node node = nodes.item(i)
      if (node instanceof Element && ((Element) node).getTagName() == tagName) {
        return (Element) node
      }
    }
    return null
  }

  private static Map<String, String> readSheetFormatAttributesFallback(String sheetXml) {
    Pattern pattern = Pattern.compile("<sheetFormatPr\\b([^>]*)/?>")
    def matcher = pattern.matcher(sheetXml)
    if (!matcher.find()) {
      return null
    }
    String attrs = matcher.group(1)
    Map<String, String> result = new LinkedHashMap<>()
    Pattern attrPattern = Pattern.compile("\\b([A-Za-z0-9_-]+)=\"([^\"]*)\"")
    def attrMatcher = attrPattern.matcher(attrs)
    while (attrMatcher.find()) {
      String name = attrMatcher.group(1)
      if (!name.contains(":")) {
        result.put(name, attrMatcher.group(2))
      }
    }
    return result
  }

  private static String extractElementXml(String xml, String tagName) {
    Pattern fullPattern = Pattern.compile("(?s)<${tagName}\\b[^>]*>.*?</${tagName}>")
    def fullMatcher = fullPattern.matcher(xml)
    if (fullMatcher.find()) {
      return fullMatcher.group()
    }
    Pattern selfPattern = Pattern.compile("<${tagName}\\b[^>]*/>")
    def selfMatcher = selfPattern.matcher(xml)
    return selfMatcher.find() ? selfMatcher.group() : null
  }

  private static String findFirstWorksheetPath(ZipFile zip) {
    List<String> candidates = []
    Enumeration<? extends ZipEntry> entries = zip.entries()
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement()
      if (entry.name.startsWith("xl/worksheets/") && entry.name.endsWith(".xml")) {
        candidates.add(entry.name)
      }
    }
    if (candidates.isEmpty()) {
      return null
    }
    candidates.sort()
    return candidates.first()
  }

  private static class WorkbookState {
    final String workbookXml
    final String relsXml
    final String contentTypesXml
    final String appXml
    final ZipFile zip

    WorkbookState(String workbookXml, String relsXml, String contentTypesXml, String appXml, ZipFile zip) {
      this.workbookXml = workbookXml
      this.relsXml = relsXml
      this.contentTypesXml = contentTypesXml
      this.appXml = appXml
      this.zip = zip
    }
  }

  private static class WorkbookPlan {
    final String workbookXml
    final String relsXml
    final String contentTypesXml
    final String appXml
    final Map<String, String> replacements
    final Map<String, String> additions

    WorkbookPlan(String workbookXml, String relsXml, String contentTypesXml, String appXml,
                 Map<String, String> replacements, Map<String, String> additions) {
      this.workbookXml = workbookXml
      this.relsXml = relsXml
      this.contentTypesXml = contentTypesXml
      this.appXml = appXml
      this.replacements = replacements
      this.additions = additions
    }
  }

  private static class SheetInfo {
    final String name
    final int sheetId
    final String relId
    final String path
    final int sheetIdNum
    final int relIdNum
    final int sheetIndex

    SheetInfo(String name, int sheetId, String relId, String path, int sheetIdNum, int relIdNum, int sheetIndex) {
      this.name = name
      this.sheetId = sheetId
      this.relId = relId
      this.path = path
      this.sheetIdNum = sheetIdNum
      this.relIdNum = relIdNum
      this.sheetIndex = sheetIndex
    }
  }

  private static class SheetTemplate {
    final Map<String, String> sheetFormatAttributes
    final String sheetFormatXml
    final String colsXml
    final String pageMarginsXml

    SheetTemplate(Map<String, String> sheetFormatAttributes, String sheetFormatXml, String colsXml, String pageMarginsXml) {
      this.sheetFormatAttributes = sheetFormatAttributes
      this.sheetFormatXml = sheetFormatXml
      this.colsXml = colsXml
      this.pageMarginsXml = pageMarginsXml
    }
  }
}
