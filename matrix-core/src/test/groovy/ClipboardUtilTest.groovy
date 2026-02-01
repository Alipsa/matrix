package se.alipsa.matrix.core.util

import org.junit.jupiter.api.Test

import java.awt.GraphicsEnvironment
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

import static org.junit.jupiter.api.Assertions.*

class ClipboardUtilTest {

  private static Clipboard newClipboard() {
    return new Clipboard('test')
  }

  @Test
  void testReadWriteRoundTrip() {
    Clipboard clipboard = newClipboard()
    String value = "matrix-clipboard-test-${System.nanoTime()}"
    ClipboardUtil.writeText(clipboard, value)
    assertEquals(value, ClipboardUtil.readText(clipboard))
  }

  @Test
  void testReadTextReturnsNullWhenTransferableReturnsNull() {
    Clipboard clipboard = newClipboard()
    clipboard.setContents(new NullStringTransferable(), null)
    assertNull(ClipboardUtil.readText(clipboard))
  }

  @Test
  void testReadTextWrapsUnsupportedFlavor() {
    Clipboard clipboard = newClipboard()
    clipboard.setContents(new NoStringTransferable(), null)
    def ex = assertThrows(IllegalStateException) { ClipboardUtil.readText(clipboard) }
    assertTrue(ex.message.contains('Failed to read clipboard text'))
  }

  @Test
  void testReadTextWrapsIOException() {
    Clipboard clipboard = newClipboard()
    clipboard.setContents(new IOExceptionTransferable(), null)
    def ex = assertThrows(IllegalStateException) { ClipboardUtil.readText(clipboard) }
    assertTrue(ex.message.contains('Failed to read clipboard text'))
  }

  @Test
  void testWriteNullBecomesEmpty() {
    Clipboard clipboard = newClipboard()
    ClipboardUtil.writeText(clipboard, null)
    assertEquals('', ClipboardUtil.readText(clipboard))
  }

  @Test
  void testPublicMethodsThrowWhenHeadless() {
    if (!GraphicsEnvironment.isHeadless()) {
      return
    }

    assertThrows(IllegalStateException) { ClipboardUtil.readText() }
    assertThrows(IllegalStateException) { ClipboardUtil.writeText('test') }
  }

  private static final class NullStringTransferable implements Transferable {
    @Override
    DataFlavor[] getTransferDataFlavors() {
      return [DataFlavor.stringFlavor] as DataFlavor[]
    }

    @Override
    boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor == DataFlavor.stringFlavor
    }

    @Override
    Object getTransferData(DataFlavor flavor) {
      return null
    }
  }

  private static final class NoStringTransferable implements Transferable {
    @Override
    DataFlavor[] getTransferDataFlavors() {
      return [DataFlavor.imageFlavor] as DataFlavor[]
    }

    @Override
    boolean isDataFlavorSupported(DataFlavor flavor) {
      return false
    }

    @Override
    Object getTransferData(DataFlavor flavor) {
      throw new UnsupportedFlavorException(flavor)
    }
  }

  private static final class IOExceptionTransferable implements Transferable {
    @Override
    DataFlavor[] getTransferDataFlavors() {
      return [DataFlavor.stringFlavor] as DataFlavor[]
    }

    @Override
    boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor == DataFlavor.stringFlavor
    }

    @Override
    Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      throw new IOException('boom')
    }
  }
}
