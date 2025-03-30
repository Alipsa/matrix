import tech.tablesaw.plotly.Plot
import tech.tablesaw.plotly.components.Figure
import tech.tablesaw.plotly.components.Page
import tech.tablesaw.plotly.display.Browser

import java.nio.charset.StandardCharsets

class TablesawHelper {
  private File parent

  /**
   * Creates the plot files in a suitable temporary location
   * determined from the parent of the passed file - typically
   * a build folder or IDE temporary folder.
   *
   * @param filename Of a file in a suitable temporary directory
   */
  TablesawHelper(String filename) {
    parent = new File(filename).parentFile
  }

  def show(Figure figure, String filename) {
    def file = new File(parent, filename + '.html')
    try {
      Plot.show(figure, file)
    } catch(ex) {
      println "Unable to show file '$file' due to '$ex.message'"
    }
  }

  def save(Figure figure, String filename) {
    if (!filename.endsWith('.html')) {
      filename += '.html'
    }
    def outputFile = new File(parent, filename)
    Page page = Page.pageBuilder(figure, "target").build();
    String output = page.asJavascript();

    try( Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
      writer.write(output)
      println("Saved html to $outputFile")
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}