package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

/**
 * Clipboard utilities for reading and writing plain text.
 */
@CompileStatic
final class ClipboardUtil {

  /**
   * Read text from the system clipboard.
   *
   * @return the clipboard contents as a string
   */
  static String readText() {
    try {
      return readText(Toolkit.defaultToolkit.systemClipboard)
    } catch (HeadlessException e) {
      throw new IllegalStateException('Clipboard is not available in a headless environment', e)
    }
  }

  @PackageScope
  static String readText(Clipboard clipboard) {
    try {
      def content = clipboard.getData(DataFlavor.stringFlavor)
      return content == null ? null : content.toString()
    } catch (UnsupportedFlavorException | IOException e) {
      throw new IllegalStateException("Failed to read clipboard text: ${e.message}", e)
    }
  }

  /**
   * Write text to the system clipboard.
   *
   * @param text the text to set on the clipboard (null becomes empty)
   */
  static void writeText(String text) {
    try {
      writeText(Toolkit.defaultToolkit.systemClipboard, text)
    } catch (HeadlessException e) {
      throw new IllegalStateException('Clipboard is not available in a headless environment', e)
    }
  }

  @PackageScope
  static void writeText(Clipboard clipboard, String text) {
    clipboard.setContents(new StringSelection(text ?: ''), null)
  }

  private ClipboardUtil() {
    // Utility class
  }
}
