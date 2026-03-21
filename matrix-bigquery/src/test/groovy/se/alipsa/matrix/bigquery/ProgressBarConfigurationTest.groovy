package se.alipsa.matrix.bigquery

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * These tests live in `se.alipsa.matrix.bigquery` so they can exercise
 * package-scope progress-bar configuration helpers directly.
 */
@CompileStatic
class ProgressBarConfigurationTest {

  @Test
  void progressBarDefaultsToDisabledWithoutInteractiveTerminal() {
    withProgressBarProperty(null) {
      assertFalse(Bq.progressBarEnabled(false))
    }
  }

  @Test
  void progressBarDefaultsToEnabledWithInteractiveTerminal() {
    withProgressBarProperty(null) {
      assertTrue(Bq.progressBarEnabled(true))
    }
  }

  @Test
  void progressBarPropertyFalseDisablesProgressEvenWithInteractiveTerminal() {
    withProgressBarProperty('false') {
      assertFalse(Bq.progressBarEnabled(true))
    }
  }

  @Test
  void progressBarPropertyTrueForcesProgressWithoutInteractiveTerminal() {
    withProgressBarProperty('true') {
      assertTrue(Bq.progressBarEnabled(false))
    }
  }

  @Test
  void blankProgressBarPropertyFallsBackToTerminalDetection() {
    withProgressBarProperty('   ') {
      assertFalse(Bq.progressBarEnabled(false))
      assertTrue(Bq.progressBarEnabled(true))
    }
  }

  private static void withProgressBarProperty(String value, Closure<Void> action) {
    String previous = System.getProperty(Bq.ENABLE_PROGRESS_BAR_PROPERTY)
    try {
      if (value == null) {
        System.clearProperty(Bq.ENABLE_PROGRESS_BAR_PROPERTY)
      } else {
        System.setProperty(Bq.ENABLE_PROGRESS_BAR_PROPERTY, value)
      }
      action.call()
    } finally {
      if (previous == null) {
        System.clearProperty(Bq.ENABLE_PROGRESS_BAR_PROPERTY)
      } else {
        System.setProperty(Bq.ENABLE_PROGRESS_BAR_PROPERTY, previous)
      }
    }
  }
}
