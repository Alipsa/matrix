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
    /** Density estimation for y values (violin/stat_ydensity) */
    YDENSITY,
    /** 2D kernel density estimation */
    DENSITY_2D,
    /** 2D binning */
    BIN2D,
    /** Hexagonal binning */
    BIN_HEX,
    /** Hexagonal summary statistics */
    SUMMARY_HEX,
    /** Contour computation */
    CONTOUR,
    /** Empirical cumulative distribution function */
    ECDF,
    /** Q-Q plot data */
    QQ,
    /** Q-Q plot reference line data */
    QQ_LINE,
    /** Confidence ellipse for bivariate normal data */
    ELLIPSE,
    /** Binned summary statistics */
    SUMMARY_BIN,
    /** Remove duplicate observations */
    UNIQUE,
    /** Compute y values from function of x */
    FUNCTION,
    /** Expand simple feature geometries to x/y coordinates */
    SF,
    /** Representative coordinates for simple feature labels */
    SF_COORDINATES
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
    /** Dodge with overlap-aware widths (ggplot2 dodge2 behavior) */
    DODGE2,
    /** Stacked positioning */
    STACK,
    /** Stacked to 100% */
    FILL,
    /** Random displacement */
    JITTER,
    /** Fixed displacement */
    NUDGE
}
