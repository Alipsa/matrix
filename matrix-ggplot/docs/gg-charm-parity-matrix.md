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
- `charts facade tests`: validate `se.alipsa.matrix.pict` compatibility through `CharmBridge`

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
| Geom: GeomJitter | P1 | `charm/render/geom/GeomEngine` (JITTER dispatch via `PointRenderer`) | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomStep | P1 | `charm/render/geom/StepRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomErrorbar | P1 | `charm/render/geom/IntervalRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomErrorbarh | P1 | `charm/render/geom/IntervalRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomRibbon | P1 | `charm/render/geom/RibbonRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomSegment | P1 | `charm/render/geom/SegmentRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomHline | P1 | `charm/render/geom/SegmentRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomVline | P1 | `charm/render/geom/SegmentRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomAbline | P1 | `charm/render/geom/SegmentRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomLabel | P1 | `charm/render/geom/LabelRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomRug | P1 | `charm/render/geom/RugRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomFreqpoly | P1 | `charm/render/geom/GeomEngine` (FREQPOLY dispatch via `LineRenderer`) | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomPath | P1 | `charm/render/geom/PathRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomRect | P1 | `charm/render/geom/RectRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomPolygon | P1 | `charm/render/geom/PolygonRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomCrossbar | P1 | `charm/render/geom/IntervalRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomLinerange | P1 | `charm/render/geom/IntervalRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomPointrange | P1 | `charm/render/geom/IntervalRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomHex | P1 | `charm/render/geom/HexRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomContour | P1 | `charm/render/geom/ContourRenderer`, `GeomEngine` dispatch | `P1GeomRendererTest.testP1GeomRendering` | [x] |
| Geom: GeomBin2d | P2 | `charm/render/geom/GeomEngine` (BIN2D dispatch via `TileRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomBlank | P2 | `charm/render/geom/BlankRenderer`, `GeomEngine` dispatch | `P2GeomRendererTest.testBlankAndCustomGeomDoNotRenderElements` | [x] |
| Geom: GeomContourFilled | P2 | `charm/render/geom/GeomEngine` (CONTOUR_FILLED dispatch via `ContourRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomCount | P2 | `charm/render/geom/GeomEngine` (COUNT dispatch via `PointRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomCurve | P2 | `charm/render/geom/CurveRenderer`, `GeomEngine` dispatch | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomCustom | P2 | `charm/render/geom/GeomEngine` (CUSTOM dispatch via `BlankRenderer`) | `P2GeomRendererTest.testBlankAndCustomGeomDoNotRenderElements` | [x] |
| Geom: GeomDensity2d | P2 | `charm/render/geom/GeomEngine` (DENSITY_2D dispatch via `ContourRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomDensity2dFilled | P2 | `charm/render/geom/GeomEngine` (DENSITY_2D_FILLED dispatch via `ContourRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomDotplot | P2 | `charm/render/geom/GeomEngine` (DOTPLOT dispatch via `PointRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomFunction | P2 | `charm/render/geom/GeomEngine` (FUNCTION dispatch via `LineRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomLogticks | P2 | `charm/render/geom/GeomEngine` (LOGTICKS dispatch via `RugRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomMag | P2 | `charm/render/geom/GeomEngine` (MAG dispatch via `PointRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomMap | P2 | `charm/render/geom/GeomEngine` (MAP dispatch via `PolygonRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomParallel | P2 | `charm/render/geom/GeomEngine` (PARALLEL dispatch via `PathRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomQq | P2 | `charm/render/geom/GeomEngine` (QQ dispatch via `PointRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomQqLine | P2 | `charm/render/geom/GeomEngine` (QQ_LINE dispatch via `LineRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomQuantile | P2 | `charm/render/geom/GeomEngine` (QUANTILE dispatch via `LineRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomRaster | P2 | `charm/render/geom/GeomEngine` (RASTER dispatch via `TileRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomRasterAnn | P2 | `charm/render/geom/GeomEngine` (RASTER_ANN dispatch via `TileRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomSpoke | P2 | `charm/render/geom/SpokeRenderer`, `GeomEngine` dispatch | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomSf | P2 | `charm/render/geom/SfRenderer`, `GeomEngine` dispatch | `P2GeomRendererTest.testP2GeomRendering`, `gg.geom.GeomSfTest` | [x] |
| Geom: GeomSfLabel | P2 | `charm/render/geom/GeomEngine` (SF_LABEL dispatch via `LabelRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Geom: GeomSfText | P2 | `charm/render/geom/GeomEngine` (SF_TEXT dispatch via `TextRenderer`) | `P2GeomRendererTest.testP2GeomRendering` | [x] |
| Stat: IDENTITY | P0 | `charm/render/stat/IdentityStat`, `StatEngine` dispatch | `IdentityStatTest`, `StatEngineTest` | [x] |
| Stat: COUNT | P0 | `charm/render/stat/CountStat`, `StatEngine` dispatch | `CountStatTest`, `StatEngineTest` | [x] |
| Stat: BIN | P0 | `charm/render/stat/BinStat`, `StatEngine` dispatch | `BinStatTest`, `StatEngineTest` | [x] |
| Stat: BOXPLOT | P0 | `charm/render/stat/BoxplotStat`, `StatEngine` dispatch | `BoxplotStatTest`, `StatEngineTest` | [x] |
| Stat: SMOOTH | P0 | `charm/render/stat/SmoothStat`, `StatEngine` dispatch | `SmoothStatTest`, `StatEngineTest` | [x] |
| Stat: DENSITY | P0 | `charm/render/stat/DensityStat`, `StatEngine` dispatch | `DensityStatTest`, `StatEngineTest` | [x] |
| Stat: YDENSITY | P0 | `charm/render/stat/YDensityStat`, `StatEngine` dispatch | `YDensityStatTest`, `StatEngineTest` | [x] |
| Stat: SUMMARY | P1 | `charm/render/stat/SummaryStat`, `StatEngine` dispatch | `P1StatTest.testSummaryStat` | [x] |
| Stat: BIN2D | P1 | `charm/render/stat/Bin2DStat`, `StatEngine` dispatch | `P1StatTest.testBin2DStat` | [x] |
| Stat: CONTOUR | P1 | `charm/render/stat/ContourStat`, `StatEngine` dispatch | `P1StatTest.testContourStat` | [x] |
| Stat: ECDF | P1 | `charm/render/stat/EcdfStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchEcdf` | [x] |
| Stat: QQ | P1 | `charm/render/stat/QqStat`, `StatEngine` dispatch | `P1StatTest.testQqStat` | [x] |
| Stat: QQ_LINE | P1 | `charm/render/stat/QqLineStat`, `StatEngine` dispatch | `P1StatTest.testQqLineStat` | [x] |
| Stat: FUNCTION | P1 | `charm/render/stat/FunctionStat`, `StatEngine` dispatch | `P1StatTest.testFunctionStat` | [x] |
| Stat: SUMMARY_BIN | P1 | `charm/render/stat/SummaryBinStat`, `StatEngine` dispatch | `P1StatTest.testSummaryBinStat` | [x] |
| Stat: UNIQUE | P1 | `charm/render/stat/UniqueStat`, `StatEngine` dispatch | `P1StatTest.testUniqueStat` | [x] |
| Stat: QUANTILE | P1 | `charm/render/stat/QuantileStat`, `StatEngine` dispatch | `P1StatTest.testQuantileStat` | [x] |
| Stat: DENSITY_2D | P2 | `charm/render/stat/Density2DStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchDensity2D` | [x] |
| Stat: BIN_HEX | P2 | `charm/render/stat/BinHexStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchBinHex` | [x] |
| Stat: SUMMARY_HEX | P2 | `charm/render/stat/SummaryHexStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchSummaryHexAndSummary2D` | [x] |
| Stat: SUMMARY_2D | P2 | `charm/render/stat/Summary2DStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchSummaryHexAndSummary2D` | [x] |
| Stat: ELLIPSE | P2 | `charm/render/stat/EllipseStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchEllipse` | [x] |
| Stat: SF | P2 | `charm/render/stat/SfStat`, `charm/sf/*`, `StatEngine` dispatch | `StatEngineTest.testDispatchSfAndSfCoordinates`, `gg.geom.GeomSfTest` | [x] |
| Stat: SF_COORDINATES | P2 | `charm/render/stat/SfCoordinatesStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchSfAndSfCoordinates` | [x] |
| Stat: SPOKE | P2 | `charm/render/stat/SpokeStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchSpoke` | [x] |
| Stat: ALIGN | P2 | `charm/render/stat/AlignStat`, `StatEngine` dispatch | `StatEngineTest.testDispatchAlign` | [x] |
| Position: IDENTITY | P0 | `charm/render/position/IdentityPosition`, `PositionEngine` dispatch | `IdentityPositionTest`, `PositionEngineTest` | [x] |
| Position: DODGE | P0 | `charm/render/position/DodgePosition`, `PositionEngine` dispatch | `DodgePositionTest`, `PositionEngineTest` | [x] |
| Position: STACK | P0 | `charm/render/position/StackPosition`, `PositionEngine` dispatch | `StackPositionTest`, `PositionEngineTest` | [x] |
| Position: FILL | P0 | `charm/render/position/FillPosition`, `PositionEngine` dispatch | `FillPositionTest`, `PositionEngineTest` | [x] |
| Position: JITTER | P1 | `charm/render/position/JitterPosition`, `PositionEngine` dispatch | `PositionEngineTest.testDispatchJitter` | [x] |
| Position: DODGE2 | P1 | `charm/render/position/Dodge2Position`, `PositionEngine` dispatch | `PositionEngineTest.testDispatchDodge2` | [x] |
| Position: NUDGE | P1 | `charm/render/position/NudgePosition`, `PositionEngine` dispatch | `PositionEngineTest.testDispatchNudge` | [x] |
| Coord: CoordCartesian | P0 | `charm/render/coord/CartesianCoord`, `CoordEngine` dispatch | `CartesianCoordTest`, `CoordEngineTest` | [x] |
| Coord: CoordFlip | P0 | `charm/render/coord/FlipCoord`, `CoordEngine` dispatch | `FlipCoordTest`, `CoordEngineTest` | [x] |
| Coord: CoordFixed | P0 | `charm/render/coord/FixedCoord`, `CoordEngine` dispatch | `FixedCoordTest`, `CoordEngineTest` | [x] |
| Coord: CoordPolar | P1 | `charm/render/coord/PolarCoord`, `CoordEngine` dispatch | `CoordEngineTest.testDispatchPolar` | [x] |
| Coord: CoordRadial | P1 | `charm/render/coord/RadialCoord`, `CoordEngine` dispatch | `CoordEngineTest.testDispatchRadial` | [x] |
| Coord: CoordTrans | P1 | `charm/render/coord/TransCoord`, `CoordEngine` dispatch | `CoordEngineTest.testDispatchTrans` | [x] |
| Coord: CoordMap | P2 | `charm/render/coord/MapCoord`, `CoordEngine` dispatch | `CoordEngineTest.testDispatchMap` | [x] |
| Coord: CoordQuickmap | P2 | `charm/render/coord/QuickmapCoord`, `CoordEngine` dispatch | `CoordEngineTest.testDispatchQuickmap` | [x] |
| Coord: CoordSf | P2 | `charm/render/coord/SfCoord`, `CoordEngine` dispatch | `CoordEngineTest.testDispatchSf` | [x] |
| Scale: ScaleXContinuous / ScaleYContinuous | P0 | `charm/render/scale/ContinuousCharmScale`, `charm/Scale.continuous()`, `gg/scale/ScaleX/YContinuous.toCharmScale()` | `ContinuousCharmScaleTest`, `ScaleEngineTest` | [x] |
| Scale: ScaleXDiscrete / ScaleYDiscrete | P0 | `charm/render/scale/DiscreteCharmScale`, `charm/Scale.discrete()`, `gg/scale/ScaleX/YDiscrete.toCharmScale()` | `DiscreteCharmScaleTest`, `ScaleEngineTest` | [x] |
| Scale: ScaleXLog10 / ScaleYLog10 | P0 | `charm/render/scale/ContinuousCharmScale` + `Log10ScaleTransform`, `charm/Scale.transform('log10')`, `gg/scale/ScaleXLog10.toCharmScale()` | `ContinuousCharmScaleTest.testLog10*`, `ScaleEngineTest.testLog10ScaleTraining` | [x] |
| Scale: ScaleXReverse / ScaleYReverse | P0 | `charm/render/scale/ContinuousCharmScale` + `ReverseScaleTransform`, `charm/Scale.transform('reverse')`, `gg/scale/ScaleXReverse.toCharmScale()` | `ContinuousCharmScaleTest.testReverse*`, `ScaleEngineTest.testReverseScaleTraining` | [x] |
| Scale: ScaleColorManual | P0 | `charm/render/scale/ColorCharmScale` (manual strategy), `charm/Scale.manual()`, `gg/scale/ScaleColorManual.toCharmScale()` | `ColorCharmScaleTest.testManual*` | [x] |
| Scale: ScaleColorBrewer | P0 | `charm/render/scale/ColorCharmScale` (brewer strategy), `charm/Scale.brewer()`, `gg/scale/ScaleColorBrewer.toCharmScale()` | `ColorCharmScaleTest.testBrewer*` | [x] |
| Scale: ScaleColorGradient / ScaleColorGradientN | P0 | `charm/render/scale/ColorCharmScale` (gradient/gradientN strategies), `charm/Scale.gradient()/gradientN()`, `gg/scale/ScaleColorGradient/N.toCharmScale()` | `ColorCharmScaleTest.testGradient*` | [x] |
| Scale: ScaleColorViridis / ScaleColorViridisC | P0 | `charm/render/scale/ColorCharmScale` (viridis_d strategy) + `ViridisProvider`, `charm/Scale.viridis()`, `gg/scale/ScaleColorViridis.toCharmScale()` | `ColorCharmScaleTest.testViridis*` | [x] |
| Scale: ScaleFillIdentity | P0 | `charm/render/scale/ColorCharmScale` (identity strategy), `charm/Scale.identity()`, `gg/scale/ScaleFillIdentity.toCharmScale()` | `ColorCharmScaleTest.testIdentity*` | [x] |
| Scale: ScaleXSqrt / ScaleYSqrt | P1 | `charm/ScaleTransform` (`SqrtScaleTransform`), `ScaleEngine.trainPositionalScale` | `ScaleEngineTest.testSqrtScaleTraining` | [x] |
| Scale: ScaleXDate / ScaleYDate | P1 | `charm/ScaleTransform` (`DateScaleTransform`), `NumberCoercionUtil` temporal coercion, `ScaleEngine` | `ScaleEngineTest.testDateScaleTrainingWithTemporalValues` | [x] |
| Scale: ScaleXDatetime / ScaleYDatetime | P1 | `DateScaleTransform` + temporal coercion for `LocalDateTime`/`Instant` types, `ScaleEngine` | `ScaleEngineTest.testDatetimeScaleTrainingWithTemporalValues` | [x] |
| Scale: ScaleXTime / ScaleYTime | P1 | `charm/ScaleTransform` (`TimeScaleTransform`), temporal coercion for `LocalTime`, `ScaleEngine` | `ScaleEngineTest.testTimeScaleTrainingWithTemporalValues` | [x] |
| Scale: ScaleXBinned / ScaleYBinned | P1 | `charm/render/scale/BinnedCharmScale`, `ScaleEngine.trainBinnedScale` | `ScaleEngineTest.testBinnedPositionalScaleTraining` | [x] |
| Scale: ScaleColorDistiller / ScaleColorFermenter | P1 | `charm/Scale.distiller()/fermenter()`, `ColorCharmScale` distiller/fermenter strategies | `ScaleEngineTest.testP1ColorScaleVariants` | [x] |
| Scale: ScaleColorGrey / ScaleColorHue | P1 | `charm/Scale.grey()/hue()`, `ColorCharmScale` grey/hue strategies | `ScaleEngineTest.testP1ColorScaleVariants` | [x] |
| Scale: ScaleColorIdentity | P1 | `charm/Scale.identity()`, `ColorCharmScale` identity strategy | `ColorCharmScaleTest.testIdentity*` | [x] |
| Scale: ScaleColorSteps / ScaleColorSteps2 / ScaleColorStepsN | P1 | `charm/Scale.steps()/steps2()/stepsN()`, `ColorCharmScale` stepped strategies | `ScaleEngineTest.testP1ColorScaleVariants` | [x] |
| Scale: ScaleSizeContinuous / ScaleSizeDiscrete / ScaleSizeBinned / ScaleSizeArea / ScaleSizeIdentity | P1 | `ScaleEngine.trainSizeScale` + `GeomUtils.resolveLineWidth` / `PointRenderer` mapped size application | `ScaleEngineTest.testTrainNonPositionalScalesRespectSpecTypes` | [x] |
| Scale: ScaleAlphaContinuous / ScaleAlphaDiscrete / ScaleAlphaBinned / ScaleAlphaIdentity | P1 | `ScaleEngine.trainAlphaScale`, `GeomUtils.resolveAlpha` mapped alpha application | `ScaleEngineTest.testTrainNonPositionalScalesRespectSpecTypes` | [x] |
| Scale: ScaleShape / ScaleShapeIdentity / ScaleShapeManual / ScaleShapeBinned | P1 | `ScaleEngine.trainShapeScale`, `GeomUtils.resolveShape` mapped/manual shape resolution | `ScaleEngineTest.testTrainNonPositionalScalesRespectSpecTypes` | [x] |
| Scale: ScaleLinetype / ScaleLinetypeIdentity / ScaleLinetypeManual | P1 | `ScaleEngine.trainLinetypeScale`, `GeomUtils.resolveLinetype` mapped/manual linetype resolution | `ScaleEngineTest.testTrainLinetypeChannel` | [x] |
| Scale: ScaleRadius | P1 | `charm/Scale.radius()`, `ScaleEngine.trainSizeScale` range-driven radius mapping | `ScaleEngineTest.testTrainNonPositionalScalesRespectSpecTypes` | [x] |
| Scale: SecondaryAxis | P1 | `charm/Scale.secondaryAxis(...)` metadata support preserved through trained scales | `ScaleEngineTest.testSecondaryAxisMetadataPreserved` | [x] |
| Annotation: Annotate (annotate() factory) | Cross-cutting | `gg/GgChart.plus(Annotate)`, `gg/adapter/GgCharmAdapter.mapInlineAnnotationLayer()`, `charm/AnnotationSpec` | `GgCharmAdapterAnnotationTest.testGgChartPlusAnnotateMapsToCharmAnnotationSpec` | [x] |
| Annotation: AnnotationCustom | Cross-cutting | `charm/render/annotation/AnnotationEngine.renderCustom`, `gg/adapter/GgCharmAdapter.mapCustomAnnotationLayer()`, `charm/CustomAnnotationSpec` | `CharmAnnotationRendererTest.testCustomAnnotationDslRendersCustomElements`, `GgCharmAdapterAnnotationTest.testAdapterDelegatesAnnotationCustom` | [x] |
| Annotation: AnnotationLogticks | Cross-cutting | `charm/render/annotation/AnnotationEngine.renderLogticks`, `gg/adapter/GgCharmAdapter.mapLogticksAnnotationLayer()`, `charm/LogticksAnnotationSpec` | `CharmAnnotationRendererTest.testLogticksAnnotationDslRendersLines`, `GgCharmAdapterAnnotationTest.testAdapterDelegatesAnnotationLogticks` | [x] |
| Annotation: AnnotationRaster | Cross-cutting | `charm/render/annotation/AnnotationEngine.renderRaster`, `gg/adapter/GgCharmAdapter.mapRasterAnnotationLayer()`, `charm/RasterAnnotationSpec` | `CharmAnnotationRendererTest.testRasterAnnotationDslRendersRasterCells`, `GgCharmAdapterAnnotationTest.testAdapterDelegatesAnnotationRaster` | [x] |
| Annotation: AnnotationMap (`annotation_map`) | Cross-cutting | `charm/render/annotation/AnnotationEngine.renderMap`, `gg/adapter/GgCharmAdapter.mapMapAnnotationLayer()`, `charm/MapAnnotationSpec` | `CharmAnnotationRendererTest.testMapAnnotationDslRendersPaths`, `GgCharmAdapterAnnotationTest.testAdapterDelegatesAnnotationMapParity` | [x] |
| Guide: legend | Cross-cutting | `charm/GuideType.LEGEND`, `charm/render/LegendRenderer.renderDiscreteLegend` | `CharmLegendRendererTest`, `CharmGuideModelTest` | [x] |
| Guide: colorbar | Cross-cutting | `charm/GuideType.COLORBAR`, `charm/render/LegendRenderer.renderColorbar` | `CharmLegendRendererTest`, `CharmGuideModelTest` | [x] |
| Guide: coloursteps / colorsteps | Cross-cutting | `charm/GuideType.COLORSTEPS`, `charm/render/LegendRenderer.renderColorSteps` | `CharmLegendRendererTest`, `CharmGuideModelTest` | [x] |
| Guide: none | Cross-cutting | `charm/GuideType.NONE`, `charm/render/LegendRenderer` (filtered out) | `CharmLegendRendererTest.testGuideNoneSuppressesLegend`, `CharmGuideModelTest` | [x] |
| Guide: axis | Cross-cutting | `charm/GuideType.AXIS`, `charm/render/AxisRenderer.renderStandardXAxis` (with angle/overlap params) | `CharmAxisGuideTest.testAxisGuideWithLabelRotation`, `CharmAxisGuideTest.testAxisGuideDefault` | [x] |
| Guide: axis_logticks | Cross-cutting | `charm/GuideType.AXIS_LOGTICKS`, `charm/render/AxisRenderer.renderAxisLogticks` | `CharmAxisGuideTest.testAxisLogticksGuide` | [x] |
| Guide: axis_theta | Cross-cutting | `charm/GuideType.AXIS_THETA`, `charm/GuideSpec.axisTheta` (stub, coord_polar not in charm) | `CharmGuideModelTest` | [x] |
| Guide: axis_stack | Cross-cutting | `charm/GuideType.AXIS_STACK`, `charm/render/AxisRenderer.renderStackedAxes` | `CharmAxisGuideTest.testAxisStackGuide` | [x] |
| Guide: custom | Cross-cutting | `charm/GuideType.CUSTOM`, `charm/render/LegendRenderer.renderCustomGuide` | `CharmCustomGuideTest` | [x] |
| Expression: Factor | Cross-cutting | `gg/aes/Factor`, `charm/bridge/GgCharmCompiler.mapAes(...)` | `FactorTest`, `CharmModelExpansionTest.testCharmExpressionImplementedByGgExpressionTypes` | [x] |
| Expression: CutWidth | Cross-cutting | `gg/aes/CutWidth`, `charm/bridge/GgCharmCompiler.mapAes(...)` | `CutWidthTest`, `CharmModelExpansionTest.testCharmExpressionImplementedByGgExpressionTypes` | [x] |
| Expression: Expression | Cross-cutting | `gg/aes/Expression`, `charm/bridge/GgCharmCompiler.mapAes(...)` | `ExpressionTest`, `CharmModelExpansionTest.testCharmExpressionImplementedByGgExpressionTypes` | [x] |
| Expression: AfterStat | Cross-cutting | `gg/aes/AfterStat`, `charm/bridge/GgCharmCompiler.mapAes(...)` | `ScaleIntegrationTest.testAfterStatFactoryMethod`, `CharmModelExpansionTest.testCharmExpressionImplementedByGgExpressionTypes` | [x] |
| Expression: AfterScale | Cross-cutting | `gg/aes/AfterScale`, `charm/bridge/GgCharmCompiler.mapAes(...)` | `GgPlotHelpersTest.testAfterScaleHelper`, `CharmModelExpansionTest.testCharmExpressionImplementedByGgExpressionTypes` | [x] |
| Expression: Identity | Cross-cutting | `gg/aes/Identity`, `charm/bridge/GgCharmCompiler.mapAes(...)` | `CharmModelExpansionTest.testCharmExpressionImplementedByGgExpressionTypes` | [x] |
| Helper: `ggsave` overloads (single chart, multiple charts, svg inputs) | Cross-cutting | `gg/GgPlot.groovy` (`ggsave(...)` overloads) | `GgSaveTest`, `GgPlotTest` (ggsave integration section) | [x] |
| Helper: `borders` helpers (`borders(String, ...)`, `borders(Matrix, ...)`) | Cross-cutting | `gg/GgPlot.groovy` (`borders(...)` overloads) | `GgPlotTest` | [x] |
| Helper: `xlim` / `ylim` wrappers | Cross-cutting | `gg/GgPlot.groovy` (`xlim(...)`, `ylim(...)`) | `GeomFunctionTest`, `ThemeTestTest`, `GgPlotTest` | [x] |
| Helper: Global theme functions (`theme_get`, `theme_set`, `theme_update`, `theme_replace`) | Cross-cutting | `gg/GgPlot.groovy` global theme API + thread-local storage | `gg/theme/GlobalThemeTest` | [x] |
| Helper: Utility wrappers (`position_nudge`, `expansion`, `vars`) | Cross-cutting | `gg/GgPlot.groovy` (`position_nudge(...)`, `expansion(...)`, `vars(...)`) | `GgPlotHelpersTest` | [x] |
| Theme: Theme presets: `theme_gray`/`theme_grey`, `theme_bw`, `theme_minimal`, `theme_classic`, `theme_dark`, `theme_light`, `theme_linedraw`, `theme_void`, `theme_test` | Cross-cutting | `charm/theme/CharmThemes` | `CharmThemeElementTest` | [x] |
| Theme: Theme state helpers: `theme_get`, `theme_set`, `theme_update`, `theme_replace` | Cross-cutting | `gg/GgPlot.groovy` global theme helpers | `gg/theme/GlobalThemeTest` | [x] |
| Theme: Theme element parity: `ElementLine`, `ElementRect`, `ElementText`, `ElementBlank` | Cross-cutting | `charm/theme/ElementLine`, `charm/theme/ElementRect`, `charm/theme/ElementText`, `charm/Theme` | `CharmThemeElementTest` | [x] |
| Charm DSL: `Charts.plot(...)` and `PlotSpec` fluent API | Cross-cutting | `charm/Charts`, `charm/PlotSpec` | `charm/api/CharmApiDesignTest`, `charm/core/CharmCoreModelTest` | [x] |
| Charm DSL: Layer/aesthetic DSLs: `Aes`, `AesDsl`, `Layer`, `LayerDsl` | Cross-cutting | `charm/Aes`, `charm/AesDsl`, `charm/Layer`, `charm/LayerDsl` | `charm/core/CharmCoreModelTest`, `charm/core/CharmModelExpansionTest` | [x] |
| Charm DSL: Structural DSLs: `Facet`, `Coord`, `Theme`, `Labels` | Cross-cutting | `charm/Facet`, `charm/Coord`, `charm/Theme`, `charm/Labels` | `charm/api/CharmApiDesignTest`, `charm/render/CharmFacetThemeTest`, `charm/render/CharmLabelTest` | [x] |
| Charm DSL: Column/mapping DSLs: `Cols`, `ColumnRef`, `ColumnExpr`, `MapDsl` | Cross-cutting | `charm/Cols`, `charm/ColumnRef`, `charm/ColumnExpr`, `charm/MapDsl` | `charm/api/CharmApiDesignTest`, `charm/core/CharmCoreModelTest`, `charm/core/CharmModelExpansionTest` | [x] |
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
| Phase Task 5.11.1: Implement `CssAttributeConfig` handling in charm's renderer pipeline. | Cross-cutting | `charm/CssAttributesSpec`, `charm/Chart`, `gg/adapter/GgCharmAdapter.mapCssAttributes()`, `charm/render/geom/GeomUtils.applyCssAttributes()` | `GgCharmAdapterTest.testAdapterDelegatesPointChartWhenCssAttributesEnabled` | [x] |
| Phase Task 5.11.2: Implement parity for `enabled`, `includeClasses`, `includeIds`, `includeDataAttributes`, `chartIdPrefix`, `idPrefix`. | Cross-cutting | `charm/render/geom/GeomUtils` (class/id/data-* toggles and prefix resolution) | `GeomCssAttributesTest`, `FacetedCssAttributesTest` | [x] |
| Phase Task 5.11.3: Implement ID naming and panel/layer indexing behavior for single and faceted charts. | Cross-cutting | `charm/render/RenderContext` (layer/panel indexes), `charm/render/CharmRenderer` (panel/layer assignment), `charm/render/geom/GeomUtils.generateElementId()` | `GeomCssAttributesTest.testMultipleLayersIncrementLayerIndex`, `FacetedCssAttributesTest.testFacetWrapWithCssAttributes` | [x] |
| Phase Task 5.11.4: Implement data attribute emission behavior and defaults. | Cross-cutting | `charm/render/geom/GeomUtils.applyDataAttributes()` | `GeomCssAttributesTest.testGeomPointWithDataAttributesEnabled`, `FacetedCssAttributesTest.testFacetedChartAddsDataPanelAttributes` | [x] |
| Phase Task 5.11.5: Add structural SVG tests that assert CSS class/id/data-* behavior. | Cross-cutting | `gg` integration coverage for CSS output via delegated charm render path | `GeomCssAttributesTest`, `FacetedCssAttributesTest`, `GeomUtilsCssTest`, `GgCharmAdapterTest` | [x] |
| Phase Task 5.12.1: Implement annotation rendering in charm: custom, logticks, raster. | Cross-cutting | `charm/render/annotation/AnnotationEngine`, `charm/render/CharmRenderer.renderAnnotations()` | `CharmAnnotationRendererTest` | [x] |
| Phase Task 5.12.2: Wire `GgChart.plus(Annotate)` to produce charm `AnnotationSpec` objects. | Cross-cutting | `gg/GgChart.plus(Annotate)`, `gg/adapter/GgCharmAdapter.mapAnnotationLayer()` | `GgCharmAdapterAnnotationTest.testGgChartPlusAnnotateMapsToCharmAnnotationSpec` | [x] |
| Phase Task 5.12.3: Move `AnnotationConstants` to charm or make shared. | Cross-cutting | `charm/AnnotationConstants`, `gg/AnnotationConstants` (compat facade) | `AnnotationRasterTest.testStringInfinityBounds`, `GgCharmAdapterAnnotationTest.testAdapterDelegatesAnnotationRaster` | [x] |
| Phase Task 5.12.4: Add tests for each annotation type. | Cross-cutting | `CharmAnnotationRendererTest`, `GgCharmAdapterAnnotationTest` | `CharmAnnotationRendererTest` (4), `GgCharmAdapterAnnotationTest` (5) | [x] |
| Phase Task 5.12.5: Add tests for `annotation_map` parity. | Cross-cutting | `AnnotationEngine.renderMap`, `GgCharmAdapter.mapMapAnnotationLayer` | `CharmAnnotationRendererTest.testMapAnnotationDslRendersPaths`, `GgCharmAdapterAnnotationTest.testAdapterDelegatesAnnotationMapParity` | [x] |

## Recorded Test Commands

- [x] `./gradlew :matrix-charts:compileGroovy`
- [x] `./gradlew :matrix-charts:compileGroovy :matrix-charts:compileTestGroovy`
- [x] `./gradlew :matrix-charts:test -Pheadless=true --tests "charm.core.CharmModelExpansionTest" --tests "charm.render.geom.P1GeomRendererTest" --tests "charm.render.stat.P1StatTest" --tests "charm.render.position.PositionEngineTest" --tests "charm.render.coord.CoordEngineTest" --tests "charm.render.scale.ScaleEngineTest"`
- [x] `./gradlew :matrix-charts:test -Pheadless=true`
- [x] `./gradlew test -Pheadless=true`
- [x] `./gradlew :matrix-charts:test -Pheadless=true --tests "gg.adapter.GgCharmAdapterAnnotationTest" --tests "charm.render.CharmAnnotationRendererTest" --tests "gg.AnnotationCustomTest" --tests "gg.AnnotationLogticksTest" --tests "gg.AnnotationRasterTest" --tests "charm.render.CharmParityGovernanceTest"`
