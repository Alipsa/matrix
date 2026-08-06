package test.alipsa.matrix.api

import groovy.xml.XmlSlurper
import org.junit.jupiter.api.Test
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvExporter
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.ext.NumberExtension
import se.alipsa.matrix.gg.GgPlot
import se.alipsa.matrix.gsheets.GsUtil
import se.alipsa.matrix.json.JsonReader
import se.alipsa.matrix.logging.MatrixLogging
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.pict.Plot
import se.alipsa.matrix.smile.SmileUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.stats.Sampler
import se.alipsa.matrix.tablesaw.TableUtil
import se.alipsa.matrix.xchart.PieChart

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

/** Verifies that the isolated Maven resolution matches every active BOM version. */
class BomResolutionIT implements ApiItSupport {

  private static final Map<String, Class> REPRESENTATIVES = [
      'matrixArffVersion'       : MatrixArffReader,
      'matrixAvroVersion'       : MatrixAvroReader,
      'matrixBigQueryVersion'   : Bq,
      'matrixChartsVersion'     : Charts,
      'matrixCoreVersion'       : Matrix,
      'matrixCsvVersion'        : CsvExporter,
      'matrixDatasetsVersion'   : Dataset,
      'matrixGgplotVersion'     : GgPlot,
      'matrixGroovyExtVersion'  : NumberExtension,
      'matrixGsheetsVersion'    : GsUtil,
      'matrixJsonVersion'       : JsonReader,
      'matrixLoggingVersion'    : MatrixLogging,
      'matrixParquetVersion'    : MatrixParquetReader,
      'matrixPictVersion'       : Plot,
      'matrixSmileVersion'      : SmileUtil,
      'matrixSpreadsheetVersion': SpreadsheetWriter,
      'matrixSqlVersion'        : MatrixSql,
      'matrixStatsVersion'      : Sampler,
      'matrixTablesawVersion'   : TableUtil,
      'matrixXChartVersion'     : PieChart,
  ]

  @Test
  void resolvesTheBomVersionForEveryModule() {
    String bomFileName = System.getProperty('bom.file')
    assertTrue(bomFileName as boolean, 'failsafe must provide the absolute bom.file system property')
    def bom = new XmlSlurper().parse(new File(bomFileName))
    Map<String, String> versions = [:]
    bom.properties.children().each { property ->
      if (property.name().toString().startsWith('matrix') && property.name().toString().endsWith('Version')) {
        versions[property.name().toString()] = property.text().trim()
      }
    }

    assertEquals(REPRESENTATIVES.keySet(), versions.keySet().findAll { REPRESENTATIVES.containsKey(it) } as Set,
        'representative map must cover every active BOM module')
    REPRESENTATIVES.each { property, type ->
      URL location = type.protectionDomain.codeSource.location
      String artifact = new File(location.toURI()).name
      assertTrue(artifact.contains(versions[property]),
          "$type must resolve from the BOM version ${versions[property]}, got $location")
      if (!versions[property].endsWith('-SNAPSHOT')) {
        assertFalse(artifact.contains('-SNAPSHOT'), "$property resolved a SNAPSHOT artifact: $location")
      }
    }
    assertEquals('5.0.8', GroovySystem.version, 'the integration runtime must match the groovy-all pin')
  }
}
