package spreadsheet

import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Builds throw-away XLSX files for regression tests. */
class XlsxTestUtil {

  private static final String APP_NAME = 'matrix-test'
  private static final String XLSX = '.xlsx'
  private static final String UTF8 = 'UTF-8'

  /** Create a temporary single-sheet workbook populated by the given closure. */
  static File createXlsx(@DelegatesTo(Worksheet) Closure<?> populate) {
    File file = File.createTempFile(APP_NAME, XLSX)
    file.delete()
    file.deleteOnExit()
    try (FileOutputStream fos = new FileOutputStream(file); Workbook wb = new Workbook(fos, APP_NAME, '1.0')) {
      Worksheet ws = wb.newWorksheet('Sheet1')
      populate.delegate = ws
      populate.resolveStrategy = Closure.DELEGATE_FIRST
      populate.call(ws)
      wb.finish()
    }
    file
  }

  /** Copy an XLSX while replacing or adding named text entries. */
  static File rewriteEntries(File source, Map<String, String> replacements) {
    File out = File.createTempFile('matrix-test-rewritten', XLSX)
    out.delete()
    out.deleteOnExit()
    Set<String> written = [] as Set
    new ZipFile(source).withCloseable { ZipFile zip ->
      new ZipOutputStream(new FileOutputStream(out)).withCloseable { ZipOutputStream zos ->
        zip.entries().each { ZipEntry entry ->
          zos.putNextEntry(new ZipEntry(entry.name))
          if (replacements.containsKey(entry.name)) {
            zos.write(replacements[entry.name].getBytes(UTF8))
          } else {
            zip.getInputStream(entry).withCloseable { InputStream input -> input.transferTo(zos) }
          }
          zos.closeEntry()
          written.add(entry.name)
        }
        (replacements.keySet() - written).each { String name ->
          zos.putNextEntry(new ZipEntry(name))
          zos.write(replacements[name].getBytes(UTF8))
          zos.closeEntry()
        }
      }
    }
    out
  }

}
