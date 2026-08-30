package se.alipsa.matrix.jupyter.kernel

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import org.dflib.jjava.jupyter.kernel.BaseKernel
import org.dflib.jjava.jupyter.kernel.JupyterIO
import org.dflib.jjava.jupyter.kernel.LanguageInfo
import org.dflib.jjava.jupyter.kernel.ReplacementOptions
import org.dflib.jjava.jupyter.kernel.comm.CommManager
import org.dflib.jjava.jupyter.kernel.display.DisplayData
import org.dflib.jjava.jupyter.kernel.display.Renderer
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType
import org.dflib.jjava.jupyter.kernel.util.StringStyler
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

import java.nio.charset.StandardCharsets
import java.util.function.Function

/** Tests the jjava adapter against a real renderer without starting a Jupyter process. */
class MatrixJupyterExtensionTest {
  @Test
  void installsHtmlRenderingAndReportsKernelRegistration() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    Matrix matrix = Matrix.builder().columns(value: [1]).build()

    extension.install(kernel)
    DisplayData rendered = kernel.renderer.renderAs(matrix, 'text/html')

    assertTrue(rendered.getData(MIMEType.TEXT_HTML).contains('>1</td>'))
    assertTrue(MatrixJupyterExtension.describe().contains('kernel#1@'))
    assertTrue(MatrixJupyterExtension.describe().contains('CoreRenderer'))
    extension.uninstall(kernel)
    assertEquals(MatrixJupyterExtension.describe(), se.alipsa.matrix.jupyter.RendererRegistry.instance.describe())
  }

  private static class TestKernel extends BaseKernel {
    TestKernel() {
      super('test', '1', new LanguageInfo('Groovy', '5', 'text/x-groovy', '.groovy', 'groovy', null, 'script'), [],
          null, new JupyterIO(StandardCharsets.UTF_8), new CommManager(), new Renderer(), null, null, false,
          new StringStyler('', '', '', { Integer ignored -> '' } as Function<Integer, String>))
    }

    @Override
    protected Object doEval(String code) { null }

    @Override
    DisplayData inspect(String code, int cursorPosition, boolean detailLevel) { new DisplayData() }

    @Override
    ReplacementOptions complete(String code, int cursorPosition) { null }

    @Override
    String isComplete(String code) { IS_COMPLETE_YES }
  }
}
