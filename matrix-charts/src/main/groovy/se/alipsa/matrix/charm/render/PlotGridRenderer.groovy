package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import org.dom4j.Attribute
import org.dom4j.Element
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotGrid

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Renders a {@link PlotGrid} into a single SVG using nested {@code <svg>} elements
 * for viewport isolation.
 *
 * <p>Each subplot is rendered independently via {@link CharmRenderer}, then embedded
 * as a nested {@code <svg x=… y=… width=… height=…>} positioned at its grid slot.
 * SVG IDs are rewritten with per-cell prefixes to prevent collisions, since SVG IDs
 * are document-global even within nested {@code <svg>} elements.</p>
 */
@CompileStatic
class PlotGridRenderer {

  /** Matches url(#someId) references in attribute values. */
  private static final Pattern URL_REF_PATTERN = Pattern.compile('url\\(#([^)]+)\\)')

  /** Matches #someId href references. */
  private static final Pattern HREF_REF_PATTERN = Pattern.compile('^#(.+)$')

  /** Attribute names that can contain href-style references. */
  private static final Set<String> HREF_ATTRS = ['href', 'xlink:href'] as Set<String>

  /** Title area height in pixels when a title is present. */
  private static final int TITLE_HEIGHT = 30

  /**
   * Renders the given plot grid to SVG.
   *
   * @param grid the plot grid specification
   * @param totalWidth total SVG width in pixels
   * @param totalHeight total SVG height in pixels
   * @return rendered SVG
   */
  Svg render(PlotGrid grid, int totalWidth, int totalHeight) {
    Svg outerSvg = new Svg()
    outerSvg.width(totalWidth)
    outerSvg.height(totalHeight)
    outerSvg.viewBox("0 0 $totalWidth $totalHeight")

    int titleOffset = 0
    if (grid.title) {
      outerSvg.addText(grid.title)
          .x(totalWidth / 2 as Number)
          .y(22)
          .textAnchor('middle')
          .fontSize(15)
          .fill('#222222')
          .styleClass('charm-grid-title')
      titleOffset = TITLE_HEIGHT
    }

    int usableWidth = totalWidth - (grid.ncol - 1) * grid.spacing
    int usableHeight = totalHeight - titleOffset - (grid.nrow - 1) * grid.spacing
    List<Integer> colWidths = distributeSpace(usableWidth, grid.ncol, grid.widths)
    List<Integer> rowHeights = distributeSpace(usableHeight, grid.nrow, grid.heights)

    CharmRenderer renderer = new CharmRenderer()

    grid.charts.eachWithIndex { Chart chart, int index ->
      int row = index.intdiv(grid.ncol) as int
      int col = index % grid.ncol

      int cellW = colWidths[col]
      int cellH = rowHeights[row]
      int cellX = computeOffset(colWidths, col, grid.spacing)
      int cellY = titleOffset + computeOffset(rowHeights, row, grid.spacing)

      RenderConfig cellConfig = new RenderConfig(width: cellW, height: cellH)
      Svg cellSvg = renderer.render(chart, cellConfig)

      String prefix = "g${row}c${col}-"
      rewriteDomIds(cellSvg.element, prefix)

      Svg nested = outerSvg.addSvg()
      nested.addAttribute('x', cellX)
      nested.addAttribute('y', cellY)
      nested.width(cellW)
      nested.height(cellH)
      nested.viewBox("0 0 $cellW $cellH")

      // Clone DOM children from cellSvg into nested SVG (avoids the
      // duplicate-element issue that SvgElementFactory.copyChildren can cause
      // when adopting constructors re-add already-present child DOM nodes)
      cellSvg.element.elements().each { Element childElem ->
        Element cloned = childElem.createCopy()
        nested.element.add(cloned)
      }
    }

    outerSvg
  }

  /**
   * Distributes available space among cells according to optional weights.
   * The last cell absorbs rounding remainder to avoid gaps.
   *
   * @param totalSpace total available pixels
   * @param count number of cells
   * @param weights optional fractional weights (null means equal)
   * @return list of cell sizes in pixels
   */
  private static List<Integer> distributeSpace(int totalSpace, int count, List<BigDecimal> weights) {
    if (count <= 0) {
      return []
    }

    List<BigDecimal> normalized = normalizeWeights(count, weights)
    List<Integer> sizes = []
    int allocated = 0

    for (int i = 0; i < count; i++) {
      if (i == count - 1) {
        // Last cell gets remainder to avoid rounding gaps
        sizes << (totalSpace - allocated)
      } else {
        int size = (totalSpace * normalized[i]) as int
        sizes << size
        allocated += size
      }
    }
    sizes
  }

  /**
   * Normalizes weight list so they sum to 1.0. Uses equal weights when null.
   *
   * @param count expected number of weights
   * @param weights optional raw weights
   * @return normalized weights summing to 1.0
   */
  private static List<BigDecimal> normalizeWeights(int count, List<BigDecimal> weights) {
    if (weights == null || weights.isEmpty()) {
      BigDecimal equal = 1.0 / count
      return (1..count).collect { equal }
    }

    // Pad or truncate to match count
    List<BigDecimal> effective = []
    for (int i = 0; i < count; i++) {
      effective << (i < weights.size() ? weights[i] : 1.0 as BigDecimal)
    }

    BigDecimal sum = effective.sum() as BigDecimal
    if (sum == 0) {
      BigDecimal equal = 1.0 / count
      return (1..count).collect { equal }
    }
    effective.collect { BigDecimal w -> w / sum }
  }

  /**
   * Computes the pixel offset for a cell at the given index.
   *
   * @param sizes list of cell sizes
   * @param index target cell index
   * @param spacing pixel gap between cells
   * @return pixel offset from origin
   */
  private static int computeOffset(List<Integer> sizes, int index, int spacing) {
    int offset = 0
    for (int i = 0; i < index; i++) {
      offset += sizes[i] + spacing
    }
    offset
  }

  /**
   * Rewrites all SVG IDs in a DOM element tree with a prefix to prevent collisions
   * when multiple subplots are composed into a single SVG document.
   *
   * <p>Two-phase approach operating directly on DOM4J elements:</p>
   * <ol>
   *   <li>Collect all existing IDs and build old-to-new mapping</li>
   *   <li>Rewrite {@code id} attributes and all references ({@code url(#...)},
   *       {@code href="#..."}, {@code xlink:href="#..."})</li>
   * </ol>
   *
   * @param root the root DOM element to rewrite
   * @param prefix the prefix to prepend to each ID
   */
  private static void rewriteDomIds(Element root, String prefix) {
    List<Element> allElements = collectDomDescendants(root)

    // Phase 1: collect all IDs
    Map<String, String> idMap = [:]
    for (Element elem : allElements) {
      String id = elem.attributeValue('id')
      if (id != null && !id.isEmpty()) {
        idMap[id] = "${prefix}${id}"
      }
    }

    if (idMap.isEmpty()) {
      return
    }

    // Phase 2: rewrite IDs and references
    for (Element elem : allElements) {
      String id = elem.attributeValue('id')
      if (id != null && idMap.containsKey(id)) {
        elem.addAttribute('id', idMap[id])
      }

      // Scan all attributes for url(#...) and href references
      List<Attribute> attrs = elem.attributes() as List<Attribute>
      for (Attribute attr : attrs) {
        String attrName = attr.name
        String attrValue = attr.value
        if (attrName == 'id' || attrValue == null) {
          continue
        }

        if (attrValue.contains('url(#')) {
          String rewritten = rewriteUrlRefs(attrValue, idMap)
          if (rewritten != attrValue) {
            attr.value = rewritten
          }
        }

        if (HREF_ATTRS.contains(attrName) || HREF_ATTRS.contains(attr.qualifiedName)) {
          String rewritten = rewriteHrefRef(attrValue, idMap)
          if (rewritten != attrValue) {
            attr.value = rewritten
          }
        }
      }
    }
  }

  /**
   * Collects an element and all its descendants into a flat list.
   *
   * @param root the root element
   * @return flat list of all elements (root + descendants)
   */
  private static List<Element> collectDomDescendants(Element root) {
    List<Element> result = [root]
    root.elements().each { Element child ->
      result.addAll(collectDomDescendants(child))
    }
    result
  }

  /**
   * Rewrites all url(#id) references in an attribute value.
   *
   * @param value the attribute value
   * @param idMap old-to-new ID mapping
   * @return rewritten value
   */
  private static String rewriteUrlRefs(String value, Map<String, String> idMap) {
    Matcher matcher = URL_REF_PATTERN.matcher(value)
    StringBuffer sb = new StringBuffer()
    while (matcher.find()) {
      String oldId = matcher.group(1)
      String newId = idMap[oldId]
      if (newId != null) {
        matcher.appendReplacement(sb, "url(#${Matcher.quoteReplacement(newId)})")
      }
    }
    matcher.appendTail(sb)
    sb.toString()
  }

  /**
   * Rewrites a #ref href value if it matches a known ID.
   *
   * @param value the href attribute value
   * @param idMap old-to-new ID mapping
   * @return rewritten value
   */
  private static String rewriteHrefRef(String value, Map<String, String> idMap) {
    Matcher matcher = HREF_REF_PATTERN.matcher(value)
    if (matcher.matches()) {
      String oldId = matcher.group(1)
      String newId = idMap[oldId]
      if (newId != null) {
        return "#${newId}"
      }
    }
    value
  }
}
