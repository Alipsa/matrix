# GG-Charm Parity Matrix

This matrix tracks feature parity progress for the migration plan in `matrix-charts/docs/gg-charm-migration.md`.

## Completion Rule

A feature row may be marked `[x]` only when:
- `Charm Implementation` references the implemented class(es)/package(s)
- `Tests` references dedicated test class(es)/method(s)
- a command in **Recorded Test Commands** is checked `[x]`

## Test Taxonomy

- `gg integration tests`: validate `gg -> charm` delegation behavior through the public gg API
- `charm direct rendering tests`: validate charm rendering/model behavior directly
- `charts facade tests`: validate `se.alipsa.matrix.charts` compatibility through `CharmBridge`

## Feature Rows

| Feature | Priority | Charm Implementation | Tests | Status |
|---|---|---|---|---|
| Geom: GeomPoint | P0 | `charm/render/geom/PointRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testPointRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomLine | P0 | `charm/render/geom/LineRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testLineRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomBar | P0 | `charm/render/geom/BarRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testBarRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomCol | P0 | `charm/render/geom/BarRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testColRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomHistogram | P0 | `charm/render/geom/HistogramRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testHistogramRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomBoxplot | P0 | `charm/render/geom/BoxplotRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testBoxplotRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomArea | P0 | `charm/render/geom/AreaRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testAreaRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomSmooth | P0 | `charm/render/geom/SmoothRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testSmoothRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomDensity | P0 | `charm/render/geom/DensityRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testDensityRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomViolin | P0 | `charm/render/geom/ViolinRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testViolinRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomTile | P0 | `charm/render/geom/TileRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testTileRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomText | P0 | `charm/render/geom/TextRenderer`, `charm/render/geom/GeomEngine`, `CharmRenderer` dispatch | `P0GeomRendererTest.testTextRepresentativeAndEmptyEdge` | [x] |
| Geom: GeomJitter | P1 | - | - | [ ] |
| Geom: GeomStep | P1 | - | - | [ ] |
| Geom: GeomErrorbar | P1 | - | - | [ ] |
| Geom: GeomErrorbarh | P1 | - | - | [ ] |
| Geom: GeomRibbon | P1 | - | - | [ ] |
| Geom: GeomSegment | P1 | - | - | [ ] |
| Geom: GeomHline | P1 | - | - | [ ] |
| Geom: GeomVline | P1 | - | - | [ ] |
| Geom: GeomAbline | P1 | - | - | [ ] |
| Geom: GeomLabel | P1 | - | - | [ ] |
| Geom: GeomRug | P1 | - | - | [ ] |
| Geom: GeomFreqpoly | P1 | - | - | [ ] |
| Geom: GeomPath | P1 | - | - | [ ] |
| Geom: GeomRect | P1 | - | - | [ ] |
| Geom: GeomPolygon | P1 | - | - | [ ] |
| Geom: GeomCrossbar | P1 | - | - | [ ] |
| Geom: GeomLinerange | P1 | - | - | [ ] |
| Geom: GeomPointrange | P1 | - | - | [ ] |
| Geom: GeomHex | P1 | - | - | [ ] |
| Geom: GeomContour | P1 | - | - | [ ] |
| Geom: GeomBin2d | P2 | - | - | [ ] |
| Geom: GeomBlank | P2 | - | - | [ ] |
| Geom: GeomContourFilled | P2 | - | - | [ ] |
| Geom: GeomCount | P2 | - | - | [ ] |
| Geom: GeomCurve | P2 | - | - | [ ] |
| Geom: GeomCustom | P2 | - | - | [ ] |
| Geom: GeomDensity2d | P2 | - | - | [ ] |
| Geom: GeomDensity2dFilled | P2 | - | - | [ ] |
| Geom: GeomDotplot | P2 | - | - | [ ] |
| Geom: GeomFunction | P2 | - | - | [ ] |
| Geom: GeomLogticks | P2 | - | - | [ ] |
| Geom: GeomMag | P2 | - | - | [ ] |
| Geom: GeomMap | P2 | - | - | [ ] |
| Geom: GeomParallel | P2 | - | - | [ ] |
| Geom: GeomQq | P2 | - | - | [ ] |
| Geom: GeomQqLine | P2 | - | - | [ ] |
| Geom: GeomQuantile | P2 | - | - | [ ] |
| Geom: GeomRaster | P2 | - | - | [ ] |
| Geom: GeomRasterAnn | P2 | - | - | [ ] |
| Geom: GeomSpoke | P2 | - | - | [ ] |
| Geom: GeomSf | P2 | - | - | [ ] |
| Geom: GeomSfLabel | P2 | - | - | [ ] |
| Geom: GeomSfText | P2 | - | - | [ ] |
| Stat: IDENTITY | P0 | `charm/render/stat/IdentityStat`, `StatEngine` dispatch | `IdentityStatTest`, `StatEngineTest` | [x] |
| Stat: COUNT | P0 | `charm/render/stat/CountStat`, `StatEngine` dispatch | `CountStatTest`, `StatEngineTest` | [x] |
| Stat: BIN | P0 | `charm/render/stat/BinStat`, `StatEngine` dispatch | `BinStatTest`, `StatEngineTest` | [x] |
| Stat: BOXPLOT | P0 | `charm/render/stat/BoxplotStat`, `StatEngine` dispatch | `BoxplotStatTest`, `StatEngineTest` | [x] |
| Stat: SMOOTH | P0 | `charm/render/stat/SmoothStat`, `StatEngine` dispatch | `SmoothStatTest`, `StatEngineTest` | [x] |
| Stat: DENSITY | P0 | `charm/render/stat/DensityStat`, `StatEngine` dispatch | `DensityStatTest`, `StatEngineTest` | [x] |
| Stat: YDENSITY | P0 | `charm/render/stat/YDensityStat`, `StatEngine` dispatch | `YDensityStatTest`, `StatEngineTest` | [x] |
| Stat: SUMMARY | P1 | - | - | [ ] |
| Stat: BIN2D | P1 | - | - | [ ] |
| Stat: CONTOUR | P1 | - | - | [ ] |
| Stat: ECDF | P1 | - | - | [ ] |
| Stat: QQ | P1 | - | - | [ ] |
| Stat: QQ_LINE | P1 | - | - | [ ] |
| Stat: FUNCTION | P1 | - | - | [ ] |
| Stat: SUMMARY_BIN | P1 | - | - | [ ] |
| Stat: UNIQUE | P1 | - | - | [ ] |
| Stat: QUANTILE | P1 | - | - | [ ] |
| Stat: DENSITY_2D | P2 | - | - | [ ] |
| Stat: BIN_HEX | P2 | - | - | [ ] |
| Stat: SUMMARY_HEX | P2 | - | - | [ ] |
| Stat: SUMMARY_2D | P2 | - | - | [ ] |
| Stat: ELLIPSE | P2 | - | - | [ ] |
| Stat: SF | P2 | - | - | [ ] |
| Stat: SF_COORDINATES | P2 | - | - | [ ] |
| Stat: SPOKE | P2 | - | - | [ ] |
| Stat: ALIGN | P2 | - | - | [ ] |
| Position: IDENTITY | P0 | `charm/render/position/IdentityPosition`, `PositionEngine` dispatch | `IdentityPositionTest`, `PositionEngineTest` | [x] |
| Position: DODGE | P0 | `charm/render/position/DodgePosition`, `PositionEngine` dispatch | `DodgePositionTest`, `PositionEngineTest` | [x] |
| Position: STACK | P0 | `charm/render/position/StackPosition`, `PositionEngine` dispatch | `StackPositionTest`, `PositionEngineTest` | [x] |
| Position: FILL | P0 | `charm/render/position/FillPosition`, `PositionEngine` dispatch | `FillPositionTest`, `PositionEngineTest` | [x] |
| Position: JITTER | P1 | - | - | [ ] |
| Position: DODGE2 | P1 | - | - | [ ] |
| Position: NUDGE | P1 | - | - | [ ] |
| Coord: CoordCartesian | P0 | `charm/render/coord/CartesianCoord`, `CoordEngine` dispatch | `CartesianCoordTest`, `CoordEngineTest` | [x] |
| Coord: CoordFlip | P0 | `charm/render/coord/FlipCoord`, `CoordEngine` dispatch | `FlipCoordTest`, `CoordEngineTest` | [x] |
| Coord: CoordFixed | P0 | `charm/render/coord/FixedCoord`, `CoordEngine` dispatch | `FixedCoordTest`, `CoordEngineTest` | [x] |
| Coord: CoordPolar | P1 | - | - | [ ] |
| Coord: CoordRadial | P1 | - | - | [ ] |
| Coord: CoordTrans | P1 | - | - | [ ] |
| Coord: CoordMap | P2 | - | - | [ ] |
| Coord: CoordQuickmap | P2 | - | - | [ ] |
| Coord: CoordSf | P2 | - | - | [ ] |
| Scale: ScaleXContinuous / ScaleYContinuous | P0 | `charm/render/scale/ContinuousCharmScale`, `charm/Scale.continuous()`, `gg/scale/ScaleX/YContinuous.toCharmScale()` | `ContinuousCharmScaleTest`, `ScaleEngineTest` | [x] |
| Scale: ScaleXDiscrete / ScaleYDiscrete | P0 | `charm/render/scale/DiscreteCharmScale`, `charm/Scale.discrete()`, `gg/scale/ScaleX/YDiscrete.toCharmScale()` | `DiscreteCharmScaleTest`, `ScaleEngineTest` | [x] |
| Scale: ScaleXLog10 / ScaleYLog10 | P0 | `charm/render/scale/ContinuousCharmScale` + `Log10ScaleTransform`, `charm/Scale.transform('log10')`, `gg/scale/ScaleXLog10.toCharmScale()` | `ContinuousCharmScaleTest.testLog10*`, `ScaleEngineTest.testLog10ScaleTraining` | [x] |
| Scale: ScaleXReverse / ScaleYReverse | P0 | `charm/render/scale/ContinuousCharmScale` + `ReverseScaleTransform`, `charm/Scale.transform('reverse')`, `gg/scale/ScaleXReverse.toCharmScale()` | `ContinuousCharmScaleTest.testReverse*`, `ScaleEngineTest.testReverseScaleTraining` | [x] |
| Scale: ScaleColorManual | P0 | `charm/render/scale/ColorCharmScale` (manual strategy), `charm/Scale.manual()`, `gg/scale/ScaleColorManual.toCharmScale()` | `ColorCharmScaleTest.testManual*` | [x] |
| Scale: ScaleColorBrewer | P0 | `charm/render/scale/ColorCharmScale` (brewer strategy), `charm/Scale.brewer()`, `gg/scale/ScaleColorBrewer.toCharmScale()` | `ColorCharmScaleTest.testBrewer*` | [x] |
| Scale: ScaleColorGradient / ScaleColorGradientN | P0 | `charm/render/scale/ColorCharmScale` (gradient/gradientN strategies), `charm/Scale.gradient()/gradientN()`, `gg/scale/ScaleColorGradient/N.toCharmScale()` | `ColorCharmScaleTest.testGradient*` | [x] |
| Scale: ScaleColorViridis / ScaleColorViridisC | P0 | `charm/render/scale/ColorCharmScale` (viridis_d strategy) + `ViridisProvider`, `charm/Scale.viridis()`, `gg/scale/ScaleColorViridis.toCharmScale()` | `ColorCharmScaleTest.testViridis*` | [x] |
| Scale: ScaleFillIdentity | P0 | `charm/render/scale/ColorCharmScale` (identity strategy), `charm/Scale.identity()`, `gg/scale/ScaleFillIdentity.toCharmScale()` | `ColorCharmScaleTest.testIdentity*` | [x] |
| Scale: ScaleXSqrt / ScaleYSqrt | P1 | - | - | [ ] |
| Scale: ScaleXDate / ScaleYDate | P1 | - | - | [ ] |
| Scale: ScaleXDatetime / ScaleYDatetime | P1 | - | - | [ ] |
| Scale: ScaleXTime / ScaleYTime | P1 | - | - | [ ] |
| Scale: ScaleXBinned / ScaleYBinned | P1 | - | - | [ ] |
| Scale: ScaleColorDistiller / ScaleColorFermenter | P1 | - | - | [ ] |
| Scale: ScaleColorGrey / ScaleColorHue | P1 | - | - | [ ] |
| Scale: ScaleColorIdentity | P1 | - | - | [ ] |
| Scale: ScaleColorSteps / ScaleColorSteps2 / ScaleColorStepsN | P1 | - | - | [ ] |
| Scale: ScaleSizeContinuous / ScaleSizeDiscrete / ScaleSizeBinned / ScaleSizeArea / ScaleSizeIdentity | P1 | - | - | [ ] |
| Scale: ScaleAlphaContinuous / ScaleAlphaDiscrete / ScaleAlphaBinned / ScaleAlphaIdentity | P1 | - | - | [ ] |
| Scale: ScaleShape / ScaleShapeIdentity / ScaleShapeManual / ScaleShapeBinned | P1 | - | - | [ ] |
| Scale: ScaleLinetype / ScaleLinetypeIdentity / ScaleLinetypeManual | P1 | - | - | [ ] |
| Scale: ScaleRadius | P1 | - | - | [ ] |
| Scale: SecondaryAxis | P1 | - | - | [ ] |
| Annotation: Annotate (annotate() factory) | Cross-cutting | - | - | [ ] |
| Annotation: AnnotationCustom | Cross-cutting | - | - | [ ] |
| Annotation: AnnotationLogticks | Cross-cutting | - | - | [ ] |
| Annotation: AnnotationRaster | Cross-cutting | - | - | [ ] |
| Annotation: AnnotationMap (`annotation_map`) | Cross-cutting | - | - | [ ] |
| Guide: legend | Cross-cutting | `charm/GuideType.LEGEND`, `charm/render/LegendRenderer.renderDiscreteLegend` | `CharmLegendRendererTest`, `CharmGuideModelTest` | [x] |
| Guide: colorbar | Cross-cutting | `charm/GuideType.COLORBAR`, `charm/render/LegendRenderer.renderColorbar` | `CharmLegendRendererTest`, `CharmGuideModelTest` | [x] |
| Guide: coloursteps / colorsteps | Cross-cutting | `charm/GuideType.COLORSTEPS`, `charm/render/LegendRenderer.renderColorSteps` | `CharmLegendRendererTest`, `CharmGuideModelTest` | [x] |
| Guide: none | Cross-cutting | `charm/GuideType.NONE`, `charm/render/LegendRenderer` (filtered out) | `CharmLegendRendererTest.testGuideNoneSuppressesLegend`, `CharmGuideModelTest` | [x] |
| Guide: axis | Cross-cutting | `charm/GuideType.AXIS`, `charm/render/AxisRenderer.renderStandardXAxis` (with angle/overlap params) | `CharmAxisGuideTest.testAxisGuideWithLabelRotation`, `CharmAxisGuideTest.testAxisGuideDefault` | [x] |
| Guide: axis_logticks | Cross-cutting | `charm/GuideType.AXIS_LOGTICKS`, `charm/render/AxisRenderer.renderAxisLogticks` | `CharmAxisGuideTest.testAxisLogticksGuide` | [x] |
| Guide: axis_theta | Cross-cutting | `charm/GuideType.AXIS_THETA`, `charm/GuideSpec.axisTheta` (stub, coord_polar not in charm) | `CharmGuideModelTest` | [x] |
| Guide: axis_stack | Cross-cutting | `charm/GuideType.AXIS_STACK`, `charm/render/AxisRenderer.renderStackedAxes` | `CharmAxisGuideTest.testAxisStackGuide` | [x] |
| Guide: custom | Cross-cutting | `charm/GuideType.CUSTOM`, `charm/render/LegendRenderer.renderCustomGuide` | `CharmCustomGuideTest` | [x] |
| Expression: Factor | Cross-cutting | - | - | [ ] |
| Expression: CutWidth | Cross-cutting | - | - | [ ] |
| Expression: Expression | Cross-cutting | - | - | [ ] |
| Expression: AfterStat | Cross-cutting | - | - | [ ] |
| Expression: AfterScale | Cross-cutting | - | - | [ ] |
| Expression: Identity | Cross-cutting | - | - | [ ] |
| Helper: `ggsave` overloads (single chart, multiple charts, svg inputs) | Cross-cutting | - | - | [ ] |
| Helper: `borders` helpers (`borders(String, ...)`, `borders(Matrix, ...)`) | Cross-cutting | - | - | [ ] |
| Helper: `xlim` / `ylim` wrappers | Cross-cutting | - | - | [ ] |
| Helper: Global theme functions (`theme_get`, `theme_set`, `theme_update`, `theme_replace`) | Cross-cutting | - | - | [ ] |
| Helper: Utility wrappers (`position_nudge`, `expansion`, `vars`) | Cross-cutting | - | - | [ ] |
| Theme: Theme presets: `theme_gray`/`theme_grey`, `theme_bw`, `theme_minimal`, `theme_classic`, `theme_dark`, `theme_light`, `theme_linedraw`, `theme_void`, `theme_test` | Cross-cutting | `charm/theme/CharmThemes` | `CharmThemeElementTest` | [x] |
| Theme: Theme state helpers: `theme_get`, `theme_set`, `theme_update`, `theme_replace` | Cross-cutting | - | - | [ ] |
| Theme: Theme element parity: `ElementLine`, `ElementRect`, `ElementText`, `ElementBlank` | Cross-cutting | `charm/theme/ElementLine`, `charm/theme/ElementRect`, `charm/theme/ElementText`, `charm/Theme` | `CharmThemeElementTest` | [x] |
| Charm DSL: `Charts.plot(...)` and `PlotSpec` fluent API | Cross-cutting | - | - | [ ] |
| Charm DSL: Layer/aesthetic DSLs: `Aes`, `AesDsl`, `Layer`, `LayerDsl` | Cross-cutting | - | - | [ ] |
| Charm DSL: Structural DSLs: `Facet`, `Coord`, `Theme`, `Labels` | Cross-cutting | - | - | [ ] |
| Charm DSL: Column/mapping DSLs: `Cols`, `ColumnRef`, `ColumnExpr`, `MapDsl` | Cross-cutting | - | - | [ ] |
| Phase Task 5.9.1: Implement FacetWrap parity in charm, including free scales (`fixed`, `free`, `free_x`, `free_y`), `ncol`, `nrow`, `dir`, multi-variable composite keys, labeller support. | Cross-cutting | `charm/render/FacetRenderer`, `charm/render/CharmRenderer` | `CharmFacetThemeTest` | [x] |
| Phase Task 5.9.2: Implement FacetGrid parity in charm, including `rows`, `cols`, margin panels, multi-variable composite keys, labeller support. | Cross-cutting | `charm/render/FacetRenderer` | `CharmFacetThemeTest` | [x] |
| Phase Task 5.9.3: Move `FormulaParser` and `Labeller` from `gg/facet/` to `charm/facet/`. Original gg classes delegate/extend charm implementations for backward compatibility. | Cross-cutting | `charm/facet/FormulaParser`, `charm/facet/Labeller` | `CharmFacetThemeTest` | [x] |
| Phase Task 5.9.4: Implement strip rendering parity (column strips top, row strips right-side with 90-degree rotation, theme-driven styling via `stripBackground`/`stripText`). | Cross-cutting | `charm/render/CharmRenderer` | `CharmFacetThemeTest` | [x] |
| Phase Task 5.9.5: Implement full label parity: `title`, `subtitle`, `caption`, axis labels with theme-driven styling (`plotTitle`, `plotSubtitle`, `plotCaption`, `axisTitleX`, `axisTitleY`), explicit null suppression, and hjust-based alignment. | Cross-cutting | `charm/render/CharmRenderer` | `CharmLabelTest` | [x] |
| Phase Task 5.9.6: Implement charm theme element model: `ElementText`, `ElementLine`, `ElementRect`, `ElementBlank` in `charm/theme/` package. Replaced map-based fields in `Theme`/`ThemeSpec` with ~30 typed fields, `explicitNulls` set, `copy()`, and `plus()` merging. | Cross-cutting | `charm/theme/ElementText`, `charm/theme/ElementLine`, `charm/theme/ElementRect`, `charm/Theme` | `CharmThemeElementTest` | [x] |
| Phase Task 5.9.7: Port all 9 predefined themes to `charm/theme/CharmThemes.groovy`: `gray()`, `classic()`, `bw()`, `minimal()`, `void_()`, `light()`, `dark()`, `linedraw()`, `test()`. Rewrote `GgCharmAdapter.mapTheme()` for field-by-field typed mapping. Removed theme gate, label gate, and facet gate from adapter. | Cross-cutting | `charm/theme/CharmThemes`, `GgCharmAdapter.mapTheme()` | `CharmThemeElementTest` | [x] |
| Phase Task 5.9.8: Add tests: `CharmThemeElementTest` (18 tests), `CharmLabelTest` (6 tests), `CharmFacetThemeTest` (11 tests). All 2049 matrix-charts tests pass. | Cross-cutting | `CharmThemeElementTest`, `CharmLabelTest`, `CharmFacetThemeTest` | `CharmThemeElementTest`, `CharmLabelTest`, `CharmFacetThemeTest` | [x] |
| Phase Task 5.10.1: Implement guide types in charm: legend, colorbar, coloursteps/colorsteps, none (3.7.1-3.7.4). | Cross-cutting | `charm/GuideType`, `charm/GuideSpec`, `charm/GuidesSpec`, `charm/render/LegendRenderer` | `CharmGuideModelTest`, `CharmLegendRendererTest` | [x] |
| Phase Task 5.10.2: Implement axis guide variants: axis, axis_logticks, axis_theta, axis_stack (3.7.5-3.7.8). | Cross-cutting | `charm/render/AxisRenderer` (logticks, axis params, stack) | `CharmAxisGuideTest` | [x] |
| Phase Task 5.10.3: Implement custom guide support (3.7.9). | Cross-cutting | `charm/render/LegendRenderer.renderCustomGuide` | `CharmCustomGuideTest` | [x] |
| Phase Task 5.10.4: Implement legend merging behavior across aesthetics. | Cross-cutting | `charm/render/LegendRenderer` (shape+color merging) | `CharmLegendRendererTest` | [x] |
| Phase Task 5.10.5: Port guide parameter handling and defaults. | Cross-cutting | `charm/render/LegendRenderer`, `charm/render/AxisRenderer`, `gg/adapter/GgCharmAdapter.mapGuides()` | `GgCharmAdapterGuideTest` | [x] |
| Phase Task 5.10.6: Add tests for each guide type and mixed-guide charts. | Cross-cutting | `CharmGuideModelTest` (17), `CharmLegendRendererTest` (10), `CharmAxisGuideTest` (5), `CharmCustomGuideTest` (3), `GgCharmAdapterGuideTest` (11) | All Phase 10 test classes | [x] |
| Phase Task 5.11.1: Implement `CssAttributeConfig` handling in charm's renderer pipeline. | Cross-cutting | - | - | [ ] |
| Phase Task 5.11.2: Implement parity for `enabled`, `includeClasses`, `includeIds`, `includeDataAttributes`, `chartIdPrefix`, `idPrefix`. | Cross-cutting | - | - | [ ] |
| Phase Task 5.11.3: Implement ID naming and panel/layer indexing behavior for single and faceted charts. | Cross-cutting | - | - | [ ] |
| Phase Task 5.11.4: Implement data attribute emission behavior and defaults. | Cross-cutting | - | - | [ ] |
| Phase Task 5.11.5: Add structural SVG tests that assert CSS class/id/data-* behavior. | Cross-cutting | - | - | [ ] |
| Phase Task 5.12.1: Implement annotation rendering in charm: custom, logticks, raster. | Cross-cutting | - | - | [ ] |
| Phase Task 5.12.2: Wire `GgChart.plus(Annotate)` to produce charm `AnnotationSpec` objects. | Cross-cutting | - | - | [ ] |
| Phase Task 5.12.3: Move `AnnotationConstants` to charm or make shared. | Cross-cutting | - | - | [ ] |
| Phase Task 5.12.4: Add tests for each annotation type. | Cross-cutting | - | - | [ ] |
| Phase Task 5.12.5: Add tests for `annotation_map` parity. | Cross-cutting | - | - | [ ] |

## Recorded Test Commands

- [x] `./gradlew :matrix-charts:compileGroovy`
- [x] `./gradlew :matrix-charts:test -Pheadless=true`
- [x] `./gradlew test -Pheadless=true`
