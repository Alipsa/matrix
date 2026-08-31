package se.alipsa.matrix.charm.render

import org.dom4j.Attribute
import org.dom4j.Element

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.SvgIdRewriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotGrid

/**
 * Renders a {@link PlotGrid} into a single SVG using nested {@code <svg>} elements
 * for viewport isolation.
 *
 * <p>Each subplot is rendered independently via {@link CharmRenderer}, then embedded
 * as a nested {@code <svg x=… y=… width=… height=…>} positioned at its grid slot.
 * SVG IDs are rewritten with per-cell prefixes to prevent collisions, since SVG IDs
 * are document-global even within nested {@code <svg>} elements.</p>
 */
@SuppressWarnings('DuplicateNumberLiteral')
@SuppressWarnings('DuplicateStringLiteral')
@SuppressWarnings('UnnecessaryCast')
class PlotGridRenderer {

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

    if (usableWidth <= 0 || usableHeight <= 0) {
      throw new IllegalArgumentException(
          "Not enough space to render PlotGrid: usableWidth=${usableWidth}, usableHeight=${usableHeight}. " +
          'Increase totalWidth/totalHeight or reduce ncol/nrow, spacing, or title height.')
    }

    List<Integer> colWidths = distributeSpace(usableWidth, grid.ncol, grid.widths)
    List<Integer> rowHeights = distributeSpace(usableHeight, grid.nrow, grid.heights)

    CharmRenderer renderer = new CharmRenderer()

    grid.charts.eachWithIndex { Chart chart, int index ->
      int row = index.intdiv(grid.ncol) as int
      int col = index % grid.ncol

      int cellW = colWidths[col]
      int cellH = rowHeights[row]

      if (cellW <= 0 || cellH <= 0) {
        throw new IllegalArgumentException(
            "PlotGrid cell at row=${row}, col=${col} (chart index=${index}) has non-positive size: " +
            "width=${cellW}, height=${cellH}. " +
            'Increase totalWidth/totalHeight or adjust ncol/nrow, spacing, or relative widths/heights.')
      }

      int cellX = computeOffset(colWidths, col, grid.spacing)
      int cellY = titleOffset + computeOffset(rowHeights, row, grid.spacing)

      RenderConfig cellConfig = new RenderConfig(width: cellW, height: cellH)
      int plotW = cellConfig.plotWidth()
      int plotH = cellConfig.plotHeight()
      if (plotW <= 0 || plotH <= 0) {
        throw new IllegalArgumentException(
            "PlotGrid cell at row=${row}, col=${col} (chart index=${index}) is too small for default margins: " +
            "cellWidth=${cellW}, cellHeight=${cellH}, plotWidth=${plotW}, plotHeight=${plotH}. " +
            'Increase totalWidth/totalHeight or adjust ncol/nrow, spacing, or margins.')
      }
      Svg cellSvg = renderer.render(chart, cellConfig)

      String prefix = "g${row}c${col}-"
      SvgIdRewriter.prefixIds(cellSvg, prefix)

      Svg nested = outerSvg.addSvg()
      nested.addAttribute('x', cellX)
      nested.addAttribute('y', cellY)
      nested.width(cellW)
      nested.height(cellH)
      nested.viewBox("0 0 $cellW $cellH")
      cellSvg.element.attributes().each { Attribute attribute ->
        if (attribute.qualifiedName in ['id', 'class', 'style']) {
          nested.addAttribute(attribute.qualifiedName, attribute.value)
        }
      }

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
    if (weights == null) {
      BigDecimal equal = 1.0 / count
      return (1..count).collect { equal }
    }

    BigDecimal sum = weights.sum() as BigDecimal
    weights.collect { BigDecimal weight -> weight / sum }
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

}
