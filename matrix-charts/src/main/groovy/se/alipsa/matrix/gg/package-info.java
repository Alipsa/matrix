/**
 * The se.alips.matrix.gg package is an implementation of grammar of graphics interpreted in the
 * same way that the R package ggplot2 does. It is close to 100% compatible with ggplot2 syntax
 * with the only exception being that named method calls in groovy uses : instead of = and that
 * String such as column names must be quoted.
 * Ggplot creates a GgChart. Ggchart.render() is then used to create a Svg model. This Svg model
 * can be written to a file or converted to png, swing panel of javafx node using the appropriate
 * class in the se.alipsa.matrix.chartexport package.
 */
package se.alipsa.matrix.gg;