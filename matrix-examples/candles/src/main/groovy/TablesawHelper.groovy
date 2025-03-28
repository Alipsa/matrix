import tech.tablesaw.plotly.Plot
import tech.tablesaw.plotly.components.Figure

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
}