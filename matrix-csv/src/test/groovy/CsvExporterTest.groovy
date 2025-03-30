import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.csv.CsvExporter
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
    CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.MYSQL, file)
    def content = file.text.split("\n")
    assertEquals('model\tmpg\tcyl\tdisp\thp\tdrat\twt\tqsec\tvs\tam\tgear\tcarb', content[0])
    assertEquals('Volvo 142E\t21.4\t4\t121\t109\t4.11\t2.78\t18.6\t1\t1\t4\t2', content[content.length -1])
    file.delete()

    file = File.createTempFile('mtcars', '.csv')
    CsvExporter.exportToCsv(Dataset.mtcars(), file)
    content = file.text.split("\r\n")
    assertEquals('model,mpg,cyl,disp,hp,drat,wt,qsec,vs,am,gear,carb', content[0])
    assertEquals('Volvo 142E,21.4,4,121,109,4.11,2.78,18.6,1,1,4,2', content[content.length -1])
    file.delete()
  }
}
