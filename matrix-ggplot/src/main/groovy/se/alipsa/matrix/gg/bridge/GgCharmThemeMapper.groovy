package se.alipsa.matrix.gg.bridge

import groovy.transform.PackageScope

import se.alipsa.matrix.charm.LegendDirection
import se.alipsa.matrix.charm.LegendPosition
import se.alipsa.matrix.charm.Theme
import se.alipsa.matrix.gg.theme.Theme as GgTheme

/**
 * Maps gg theme elements ({@link GgTheme}) to their Charm ({@link Theme}) equivalents.
 */
@PackageScope
class GgCharmThemeMapper {

  static Theme mapTheme(GgTheme source) {
    Theme theme = new Theme()
    if (source == null) {
      return theme
    }
    mapPlotTheme(theme, source)
    mapPanelTheme(theme, source)
    mapAxisTheme(theme, source)
    mapLegendTheme(theme, source)
    mapStripAndBaseTheme(theme, source)
    theme
  }

  private static void mapPlotTheme(Theme theme, GgTheme source) {
    theme.with {
      plotBackground = mapRect(source.plotBackground)
      plotTitle = mapText(source.plotTitle)
      plotSubtitle = mapText(source.plotSubtitle)
      plotCaption = mapText(source.plotCaption)
      plotMargin = source.plotMargin != null ? new ArrayList<>(source.plotMargin) : null
    }
  }

  private static void mapPanelTheme(Theme theme, GgTheme source) {
    theme.with {
      panelBackground = mapRect(source.panelBackground)
      panelBorder = mapRect(source.panelBorder)
      panelGridMajor = mapLine(source.panelGridMajor)
      panelGridMinor = mapLine(source.panelGridMinor)
      panelSpacing = source.panelSpacing != null ? new ArrayList<>(source.panelSpacing) : null
    }
  }

  private static void mapAxisTheme(Theme theme, GgTheme source) {
    theme.with {
      axisLineX = mapLine(source.axisLineX)
      axisLineY = mapLine(source.axisLineY)
      axisTicksX = mapLine(source.axisTicksX)
      axisTicksY = mapLine(source.axisTicksY)
      axisTextX = mapText(source.axisTextX)
      axisTextY = mapText(source.axisTextY)
      axisTitleX = mapText(source.axisTitleX)
      axisTitleY = mapText(source.axisTitleY)
      axisTickLength = source.axisTickLength
    }
  }

  private static void mapLegendTheme(Theme theme, GgTheme source) {
    Object normalizedLegendPos = LegendPosition.normalize(source.legendPosition)
    Object dirNormalized = LegendDirection.normalize(source.legendDirection)
    theme.with {
      legendPosition = normalizedLegendPos instanceof LegendPosition
          ? normalizedLegendPos as LegendPosition
          : LegendPosition.RIGHT
      if (source.legendPositionCoords != null) {
        legendPositionCoords = new ArrayList<>(source.legendPositionCoords)
      }
      legendDirection = dirNormalized instanceof LegendDirection
          ? dirNormalized as LegendDirection
          : LegendDirection.VERTICAL
      legendBackground = mapRect(source.legendBackground)
      legendKey = mapRect(source.legendKey)
      legendKeySize = source.legendKeySize != null ? new ArrayList<>(source.legendKeySize) : null
      legendTitle = mapText(source.legendTitle)
      legendText = mapText(source.legendText)
      legendMargin = source.legendMargin != null ? new ArrayList<>(source.legendMargin) : null
    }
  }

  private static void mapStripAndBaseTheme(Theme theme, GgTheme source) {
    theme.with {
      stripBackground = mapRect(source.stripBackground)
      stripText = mapText(source.stripText)
      discreteColors = source.discreteColors != null ? new ArrayList<>(source.discreteColors) : null
      gradientColors = source.gradientColors != null ? new ArrayList<>(source.gradientColors) : null
      baseFamily = source.baseFamily
      baseSize = source.baseSize
      baseLineHeight = source.baseLineHeight
      themeName = source.themeName
      explicitNulls = copyExplicitNulls(source)
    }
  }

  private static Set<String> copyExplicitNulls(GgTheme source) {
    Set<String> copy = [] as Set<String>
    if (source.explicitNulls) {
      copy.addAll(source.explicitNulls)
    }
    copy
  }

  private static se.alipsa.matrix.charm.theme.ElementText mapText(se.alipsa.matrix.gg.theme.ElementText source) {
    if (source == null) {
      return null
    }
    new se.alipsa.matrix.charm.theme.ElementText(
        family: source.family,
        face: source.face,
        size: source.size,
        color: source.color,
        hjust: source.hjust,
        vjust: source.vjust,
        angle: source.angle,
        lineheight: source.lineheight,
        margin: source.margin != null ? new ArrayList<>(source.margin) : null
    )
  }

  private static se.alipsa.matrix.charm.theme.ElementLine mapLine(se.alipsa.matrix.gg.theme.ElementLine source) {
    if (source == null) {
      return null
    }
    new se.alipsa.matrix.charm.theme.ElementLine(
        color: source.color,
        size: source.size,
        linetype: source.linetype,
        lineend: source.lineend
    )
  }

  private static se.alipsa.matrix.charm.theme.ElementRect mapRect(se.alipsa.matrix.gg.theme.ElementRect source) {
    if (source == null) {
      return null
    }
    new se.alipsa.matrix.charm.theme.ElementRect(
        fill: source.fill,
        color: source.color,
        size: source.size,
        linetype: source.linetype
    )
  }
}
