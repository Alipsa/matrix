package se.alipsa.matrix.spreadsheet.fastexcel

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetWriteUtil
import se.alipsa.matrix.spreadsheet.XmlSecurityUtil
import se.alipsa.matrix.spreadsheet.ZipUtil

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Appends Matrix data to an existing Excel (.xlsx) workbook by manipulating its underlying XML entries.
 */
class FExcelAppender {

  private static final String WORKBOOK_PATH = 'xl/workbook.xml'
  private static final String RELS_PATH = 'xl/_rels/workbook.xml.rels'
  private static final String CONTENT_TYPES = '[Content_Types].xml'
  private static final String APP_PATH = 'docProps/app.xml'
  private static final String WORKSHEET_CONTENT_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml'
  private static final String WORKSHEET_REL_TYPE = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet'
  private static final String DEFAULT_START = 'A1'
  private static final String RELATIONSHIP_TAG = 'Relationship'
  private static final String ATTR_ID = 'Id'
  private static final String ATTR_NAME = 'name'
  private static final String ATTR_SHEET_ID = 'sheetId'
  private static final String TAG_SHEETS = 'sheets'
  private static final String TAG_SHEET = 'sheet'
  private static final String ATTR_R_ID = 'r:id'
  private static final String ATTR_TARGET = 'Target'
  private static final String TAG_VECTOR = 'vector'
  private static final String RID_PREFIX = 'rId'
  private static final String PROP_NO = 'no'
  private static final String EMPTY_CELL = '<c/>'
  private static final String ROW_START = '<row r="'
  private static final String ROW_END = '"/>'
  private static final String DATE_CELL_START = '<c t="d"><v>'
  private static final String VALUE_CELL_END = '</v></c>'
  private static final String INLINE_STR_END = '</t></is></c>'
  private static final String COLON = ':'
  private static final String SPACE = ' '
  private static final String DOUBLE_QUOTE = '"'

  static List<String> appendOrReplaceSheets(File file, List<Matrix> data, List<String> sheetNames) {
    return appendOrReplaceSheets(file, data, sheetNames, null)
  }

  @SuppressWarnings('NestedBlockDepth')
  static List<String> appendOrReplaceSheets(File file, List<Matrix> data, List<String> sheetNames, List<String> startPositions) {
    if (!file.exists() || file.length() == 0) {
      return FExcelExporter.exportExcelSheets(file, data, sheetNames, startPositions)
    }
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException('Matrices and sheet names lists must have the same size')
    }
    Map<String, Matrix> requested = SpreadsheetWriteUtil.buildRequestedMap(data, sheetNames)
    Map<String, String> positions = buildPositionMap(sheetNames, startPositions)
    File tmp = File.createTempFile('matrix-xlsx', '.xlsx', file.parentFile)
    boolean moved = false
    try {
      try (ZipFile zip = new ZipFile(file); FileOutputStream fos = new FileOutputStream(tmp); ZipOutputStream zos = new ZipOutputStream(fos)) {
        WorkbookState state = readWorkbookState(zip)
        WorkbookPlan plan = buildPlan(state, requested, positions)

        Set<String> written = writeModifiedEntries(zip, zos, plan)
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

  private static Map<String, String> buildPositionMap(List<String> sheetNames, List<String> startPositions) {
    List<String> positions = startPositions ?: Collections.nCopies(sheetNames.size(), DEFAULT_START)
    if (sheetNames.size() != positions.size()) {
      throw new IllegalArgumentException('Sheet names and start positions lists must have the same size')
    }
    List<String> uniqueNames = SpreadsheetUtil.createUniqueSheetNames(sheetNames)
    Map<String, String> result = [:]
    for (int i = 0; i < uniqueNames.size(); i++) {
      result.put(uniqueNames.get(i), positions.get(i) ?: DEFAULT_START)
    }
    result
  }

  private static Set<String> writeModifiedEntries(ZipFile zip, ZipOutputStream zos, WorkbookPlan plan) {
    Map<String, String> entryOverrides = [
        (WORKBOOK_PATH): plan.workbookXml,
        (RELS_PATH)    : plan.relsXml,
        (CONTENT_TYPES): plan.contentTypesXml,
        (APP_PATH)     : plan.appXml,
    ]
    Set<String> written = [] as Set
    Enumeration<? extends ZipEntry> entries = zip.entries()
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement()
      String name = entry.name
      String override = entryOverrides[name] ?: plan.replacements[name]
      if (override != null) {
        writeEntry(zos, name, override)
      } else {
        ZipUtil.copyEntry(zip, entry, zos)
      }
      written.add(name)
    }
    written
  }

  private static WorkbookState readWorkbookState(ZipFile zip) {
    String workbookXml = readEntry(zip, WORKBOOK_PATH)
    String relsXml = readEntry(zip, RELS_PATH)
    String contentXml = readEntry(zip, CONTENT_TYPES)
    String appXml = readEntry(zip, APP_PATH)
    return new WorkbookState(workbookXml, relsXml, contentXml, appXml, zip)
  }

  private static WorkbookPlan buildPlan(WorkbookState state, Map<String, Matrix> requested, Map<String, String> positions) {
    Document workbook = parseXml(state.workbookXml)
    Document rels = parseXml(state.relsXml)
    Document types = parseXml(state.contentTypesXml)
    Document app = state.appXml ? parseXml(state.appXml) : null

    Map<String, SheetInfo> existing = readSheets(workbook, rels)
    List<Integer> sheetIds = existing.values()*.sheetId
    List<Integer> sheetIndexes = existing.values()*.sheetIndex
    int nextSheetId = nextValue(sheetIds, 1)
    int nextRelId = maxRelId(rels) + 1
    int nextSheetIndex = nextValue(sheetIndexes, 1)

    Map<String, String> replacements = [:]
    Map<String, String> additions = [:]
    Map<String, SheetTemplate> templateCache = [:]
    SheetTemplate baseTemplate = readBaseTemplate(state.zip, existing)
    baseTemplate = mergeTemplate(state.zip, existing, baseTemplate)

    requested.each { String name, Matrix matrix ->
      String startPosition = positions.get(name) ?: DEFAULT_START
      SheetInfo info = existing.get(name)
      if (info != null) {
        SheetTemplate template = templateForPath(state.zip, templateCache, info.path) ?: baseTemplate
        replacements.put(info.path, buildSheetXml(matrix, template, startPosition))
      } else {
        String sheetPath = "xl/worksheets/sheet${nextSheetIndex++}.xml"
        String relId = "rId${nextRelId++}"
        int sheetId = nextSheetId++
        addSheet(workbook, name, sheetId, relId)
        addRelationship(rels, relId, sheetPath)
        addContentType(types, sheetPath)
        additions.put(sheetPath, buildSheetXml(matrix, baseTemplate, startPosition))
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
    NodeList relNodes = rels.getElementsByTagName(RELATIONSHIP_TAG)
    for (int i = 0; i < relNodes.length; i++) {
      Element rel = (Element) relNodes.item(i)
      relTargets[rel.getAttribute(ATTR_ID)] = rel.getAttribute(ATTR_TARGET)
    }

    Map<String, SheetInfo> sheets = [:]
    Element sheetsNode = firstElementByTag(workbook, TAG_SHEETS)
    if (sheetsNode != null) {
      NodeList sheetNodes = sheetsNode.getElementsByTagName(TAG_SHEET)
      for (int i = 0; i < sheetNodes.length; i++) {
        Element sheet = (Element) sheetNodes.item(i)
        String name = sheet.getAttribute(ATTR_NAME)
        int sheetId = Integer.parseInt(sheet.getAttribute(ATTR_SHEET_ID))
        String relId = sheet.getAttribute(ATTR_R_ID)
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
    NodeList relNodes = rels.getElementsByTagName(RELATIONSHIP_TAG)
    for (int i = 0; i < relNodes.length; i++) {
      Element rel = (Element) relNodes.item(i)
      String id = rel.getAttribute(ATTR_ID)
      if (id?.startsWith(RID_PREFIX)) {
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
    Element sheetsNode = firstElementByTag(workbook, TAG_SHEETS)
    Element sheet = workbook.createElement(TAG_SHEET)
    sheet.setAttribute(ATTR_NAME, name)
    sheet.setAttribute(ATTR_SHEET_ID, String.valueOf(sheetId))
    sheet.setAttribute(ATTR_R_ID, relId)
    sheetsNode.appendChild(sheet)
  }

  private static void addRelationship(Document rels, String relId, String sheetPath) {
    Element rel = rels.createElement(RELATIONSHIP_TAG)
    rel.setAttribute(ATTR_ID, relId)
    rel.setAttribute('Type', WORKSHEET_REL_TYPE)
    rel.setAttribute(ATTR_TARGET, sheetPath.replaceFirst('^xl/', ''))
    rels.documentElement.appendChild(rel)
  }

  private static void addContentType(Document types, String sheetPath) {
    Element override = types.createElement('Override')
    override.setAttribute('PartName', '/' + sheetPath)
    override.setAttribute('ContentType', WORKSHEET_CONTENT_TYPE)
    types.documentElement.appendChild(override)
  }

  private static String updateAppXml(Document app, List<String> sheetNames) {
    Element titles = firstChildElementBySuffix(app.documentElement, 'TitlesOfParts')
    Element titlesVector = titles ? firstChildElementBySuffix(titles, TAG_VECTOR) : null
    if (titlesVector != null) {
      titlesVector.setAttribute('size', String.valueOf(sheetNames.size()))
      removeAllChildElements(titlesVector)
      String lpstrTag = tagWithSamePrefix(titlesVector, 'lpstr')
      sheetNames.each { String name ->
        Element lpstr = app.createElement(lpstrTag)
        lpstr.setTextContent(name)
        titlesVector.appendChild(lpstr)
      }
    }
    Element headingPairs = firstChildElementBySuffix(app.documentElement, 'HeadingPairs')
    Element headingVector = headingPairs ? firstChildElementBySuffix(headingPairs, TAG_VECTOR) : null
    if (headingVector != null) {
      Element countNode = firstChildElementBySuffix(headingVector, 'i4')
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
      if (Element.isInstance(node)) {
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
    int idx = tag.indexOf(COLON)
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
    if (relId == null) {
      return 0
    }
    return relId.replace(RID_PREFIX, '').toInteger()
  }

  private static int sheetNumberFromPath(String path) {
    if (path == null) {
      return 0
    }
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
    zip.getInputStream(entry).withCloseable { InputStream is ->
      new String(is.bytes, StandardCharsets.UTF_8)
    }
  }

  private static void writeEntry(ZipOutputStream zos, String name, String content) {
    ZipEntry entry = new ZipEntry(name)
    zos.putNextEntry(entry)
    zos.write(content.getBytes(StandardCharsets.UTF_8))
    zos.closeEntry()
  }

  private static Document parseXml(String xml) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(true)
    XmlSecurityUtil.configureDocumentBuilderFactory(factory)
    def builder = factory.newDocumentBuilder()
    return builder.parse(new InputSource(new StringReader(xml)))
  }

  private static String serialize(Document doc) {
    TransformerFactory tf = TransformerFactory.newInstance()
    Transformer transformer = tf.newTransformer()
    transformer.setOutputProperty(OutputKeys.ENCODING, 'UTF-8')
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, PROP_NO)
    transformer.setOutputProperty(OutputKeys.INDENT, PROP_NO)
    StringWriter out = new StringWriter()
    transformer.transform(new DOMSource(doc), new StreamResult(out))
    out.toString()
  }

  private static String buildSheetXml(Matrix matrix, SheetTemplate template, String startPosition) {
    SpreadsheetUtil.CellPosition position = SpreadsheetUtil.parseCellPosition(startPosition)
    StringBuilder sb = new StringBuilder()
    sb.append('<?xml version="1.0" encoding="UTF-8"?>')
    sb.append('<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" ')
      .append('xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">')
    sb.append('<dimension ref="').append(sheetDimension(matrix, position)).append(ROW_END)
    if (template?.sheetFormatXml) {
      sb.append(template.sheetFormatXml)
    } else if (template?.sheetFormatAttributes && !template.sheetFormatAttributes.isEmpty()) {
      sb.append('<sheetFormatPr')
      template.sheetFormatAttributes.each { String key, String value ->
        sb.append(SPACE).append(key).append('="').append(escapeXml(value)).append(DOUBLE_QUOTE)
      }
      sb.append('/>')
    }
    if (template?.colsXml) {
      sb.append(template.colsXml)
    }
    sb.append('<sheetData>')
    writeEmptyRows(sb, position.row - 1)
    writeRow(sb, position.row, matrix.columnNames(), position.column)
    List<Column> columns = matrix.columns()
    int rowCount = matrix.rowCount()
    int colCount = columns.size()
    for (int r = 0; r < rowCount; r++) {
      List<Object> row = collectRow(columns, colCount, r)
      writeRow(sb, position.row + r + 1, row, position.column)
    }
    sb.append('</sheetData>')
    if (template?.pageMarginsXml) {
      sb.append(template.pageMarginsXml)
    }
    sb.append('</worksheet>')
    sb.toString()
  }

  private static List<Object> collectRow(List<Column> columns, int colCount, int rowIndex) {
    List<Object> row = new ArrayList<>(colCount)
    for (int c = 0; c < colCount; c++) {
      row.add(columns.get(c).get(rowIndex))
    }
    row
  }

  private static String sheetDimension(Matrix matrix, SpreadsheetUtil.CellPosition position) {
    // Always include the header row, even if the matrix has no data rows.
    int rowCount = Math.max(1, matrix.rowCount() + 1)
    int colCount = Math.max(1, matrix.columnCount())
    int startRow = position.row
    int startCol = position.column
    int lastRow = startRow + rowCount - 1
    int lastColNum = startCol + colCount - 1
    String startColName = SpreadsheetUtil.asColumnName(startCol)
    String lastColName = SpreadsheetUtil.asColumnName(lastColNum)
    return "${startColName}${startRow}:${lastColName}${lastRow}"
  }

  private static void writeRow(StringBuilder sb, int rowNum, List<?> values, int startCol) {
    sb.append(ROW_START).append(rowNum).append('">')
    appendEmptyCells(sb, startCol - 1)
    values.each { Object value ->
      appendCell(sb, value)
    }
    sb.append('</row>')
  }

  private static void writeEmptyRows(StringBuilder sb, int count) {
    for (int i = 1; i <= count; i++) {
      sb.append(ROW_START).append(i).append(ROW_END)
    }
  }

  private static void appendEmptyCells(StringBuilder sb, int count) {
    for (int i = 0; i < count; i++) {
      sb.append(EMPTY_CELL)
    }
  }

  private static void appendCell(StringBuilder sb, Object value) {
    if (value == null) {
      sb.append(EMPTY_CELL)
      return
    }
    switch (value) {
      case Boolean -> sb.append('<c t="b"><v>').append(((Boolean) value) ? '1' : '0').append(VALUE_CELL_END)
      case Number -> {
        String v = ValueConverter.asBigDecimal(value).toPlainString()
        sb.append('<c t="n"><v>').append(v).append(VALUE_CELL_END)
      }
      case LocalDate -> sb.append(DATE_CELL_START).append(((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE)).append(VALUE_CELL_END)
      case LocalDateTime -> sb.append(DATE_CELL_START).append(((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(VALUE_CELL_END)
      case ZonedDateTime -> sb.append(DATE_CELL_START).append(((ZonedDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append(VALUE_CELL_END)
      case OffsetDateTime -> sb.append(DATE_CELL_START).append(((OffsetDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append(VALUE_CELL_END)
      case Date -> sb.append(DATE_CELL_START).append(((Date) value).toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append(VALUE_CELL_END)
      default -> appendInlineString(sb, String.valueOf(value))
    }
  }

  private static void appendInlineString(StringBuilder sb, String value) {
    String text = escapeXml(value)
    if (text.startsWith(SPACE) || text.endsWith(SPACE)) {
      sb.append('<c t="inlineStr"><is><t xml:space="preserve">').append(text).append(INLINE_STR_END)
    } else {
      sb.append('<c t="inlineStr"><is><t>').append(text).append(INLINE_STR_END)
    }
  }

  private static String escapeXml(String value) {
    if (value == null) {
      return ''
    }
    return value
        .replace('&', '&amp;')
        .replace('<', '&lt;')
        .replace('>', '&gt;')
        .replace(DOUBLE_QUOTE, '&quot;')
        .replace("'", '&apos;')
  }

  private static int nextValue(List<Integer> values, int defaultValue) {
    if (values == null || values.isEmpty()) {
      return defaultValue
    }
    Integer maxValue = values.findAll { it != null }?.max()
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

  private static SheetTemplate mergeTemplate(ZipFile zip, Map<String, SheetInfo> existing, SheetTemplate template) {
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
    String sheetFormatXml = extractElementXml(sheetXml, 'sheetFormatPr')
    Map<String, String> sheetFormatAttributes = sheetFormatXml ? readSheetFormatAttributesFallback(sheetXml) : null
    String colsXml = extractElementXml(sheetXml, 'cols')
    String pageMarginsXml = extractElementXml(sheetXml, 'pageMargins')
    if ((sheetFormatAttributes == null || sheetFormatAttributes.isEmpty()) && sheetFormatXml == null && colsXml == null && pageMarginsXml == null) {
      return null
    }
    return new SheetTemplate(sheetFormatAttributes, sheetFormatXml, colsXml, pageMarginsXml)
  }

  /**
   * Fallback regex-based parser for sheetFormatPr attributes when DOM parsing is not available.
   *
   * <p><b>Limitations:</b></p>
   * <ul>
   *   <li>Only handles double-quoted attributes (not single-quoted)</li>
   *   <li>Assumes the element is on a single line or the regex can match across lines</li>
   *   <li>Ignores namespace-prefixed attributes (e.g., {@code x:attr="value"})</li>
   *   <li>Does not handle XML entities in attribute values</li>
   * </ul>
   *
   * <p>This is used as a fallback when the primary DOM-based extraction fails.
   * For most standard Excel files generated by modern applications, this works correctly.</p>
   *
   * @param sheetXml the raw XML content of the worksheet
   * @return a map of attribute names to values, or null if sheetFormatPr is not found
   */
  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  private static Map<String, String> readSheetFormatAttributesFallback(String sheetXml) {
    Pattern pattern = Pattern.compile('<sheetFormatPr\\b([^>]*)/?>')
    def matcher = pattern.matcher(sheetXml)
    if (!matcher.find()) {
      return null
    }
    String attrs = matcher.group(1)
    Map<String, String> result = [:]
    Pattern attrPattern = Pattern.compile('\\b([A-Za-z_][A-Za-z0-9_.:-]*)="([^"]*)"')
    def attrMatcher = attrPattern.matcher(attrs)
    while (attrMatcher.find()) {
      String name = attrMatcher.group(1)
      if (!name.contains(COLON)) {
        result.put(name, attrMatcher.group(2))
      }
    }
    return result
  }

  /**
   * Extracts a complete XML element (including its content) using regex.
   *
   * <p><b>Limitations:</b></p>
   * <ul>
   *   <li>Assumes no nested elements with the same tag name</li>
   *   <li>Uses non-greedy matching which may fail on complex nested structures</li>
   *   <li>Works for self-closing elements ({@code <tag/>}) and simple content elements</li>
   * </ul>
   *
   * <p>This is a fallback method used when DOM manipulation is not practical.
   * It handles common cases like {@code <cols>...</cols>} or {@code <pageMargins .../>}.</p>
   *
   * @param xml the XML string to search in
   * @param tagName the name of the element to extract
   * @return the complete element XML string, or null if not found
   */
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
      if (entry.name.startsWith('xl/worksheets/') && entry.name.endsWith('.xml')) {
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
