package se.alipsa.matrix.gg.layer

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.geom.Geom

/**
 * Represents a single layer in a ggplot chart.
 * Each layer combines data, aesthetics, a geometric representation,
 * statistical transformation, and position adjustment.
 */
@CompileStatic
class Layer {
    /** Optional per-layer data (overrides chart data if set) */
    Matrix data

    /** Per-layer aesthetic mappings */
    Aes aes

    /** Fixed aesthetics (e.g., size: 3, color: 'red') - not mapped to data */
    Map params = [:]

    /** Geometric representation */
    Geom geom

    /** Statistical transformation type */
    StatType stat = StatType.IDENTITY

    /** Position adjustment type */
    PositionType position = PositionType.IDENTITY

    /** Whether to inherit aesthetics from the parent chart */
    boolean inheritAes = true

    /** Parameters for the stat transformation (e.g., bins, binwidth, method) */
    Map statParams = [:]

    /** Parameters for position adjustment (e.g., width for dodge) */
    Map positionParams = [:]
}

/**
 * Statistical transformation types.
 * Used by the renderer to dispatch to GgStat methods.
 */
enum StatType {
    /** Pass-through, returns data unchanged */
    IDENTITY,
    /** Count occurrences (for bar charts) */
    COUNT,
    /** Binning (for histograms) */
    BIN,
    /** Compute quartiles, whiskers (for boxplots) */
    BOXPLOT,
    /** Smoothing/regression lines */
    SMOOTH,
    /** Summary statistics (mean, median, etc.) */
    SUMMARY,
    /** Kernel density estimation */
    DENSITY,
    /** 2D binning */
    BIN2D,
    /** Contour computation */
    CONTOUR
}

/**
 * Position adjustment types.
 * Used by the renderer to dispatch to GgPosition methods.
 */
enum PositionType {
    /** No adjustment (default) */
    IDENTITY,
    /** Side-by-side grouping */
    DODGE,
    /** Stacked positioning */
    STACK,
    /** Stacked to 100% */
    FILL,
    /** Random displacement */
    JITTER
}
