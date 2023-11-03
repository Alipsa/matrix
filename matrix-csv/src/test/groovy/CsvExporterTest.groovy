import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.matrixcsv.CsvExporter
import org.apache.commons.csv.CSVFormat
import static org.junit.jupiter.api.Assertions.*

class CsvExporterTest {

  @Test
  void exportCsv() {
    StringWriter writer = new StringWriter()
    CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.DEFAULT, writer)
    def content = writer.toString().split("\r\n")
    assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
    assertEquals('Volvo 142E,21.4,4,121,109,4.11,2.78,18.6,1,1,4,2', content[content.length -1])
  }

  @Test
  void exportToFile() {
    File file = File.createTempFile('mtcars', '.csv')
    CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.DEFAULT, file)
    def content = file.text.split("\r\n")
    assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
    assertEquals('Volvo 142E,21.4,4,121,109,4.11,2.78,18.6,1,1,4,2', content[content.length -1])
    file.delete()
  }
}
