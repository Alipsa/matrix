package datasets

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.datasets.Rdatasets

class RdatasetsTest {

  @Test
  @Tag('external')
  void testOverview() {
    def overview = Rdatasets.overview()
    assertNotNull(overview, 'Overview should not be null')
    assertTrue(overview.rowCount() > 0, 'Overview should have rows')
    assertTrue(overview.columnCount() > 0, 'Overview should have columns')
  }

  @Test
  @Tag('external')
  void fetchInfo() {
    String info = Rdatasets.fetchInfo('AER', 'BankWages', true)
    assertTrue(info.contains('A data frame containing 474 observations on 4 variables.'))
  }

  @Test
  @Tag('external')
  void testFetchData() {
    def mtcars = Rdatasets.fetchData('datasets', 'mtcars')
    assertNotNull(mtcars, 'Mtcars dataset should not be null')
    mtcars.columnName(0, 'model')
    // println "mtcars content: ${mtcars.content()}"
    // println Dataset.mtcars().content()
    assertEquals(32, mtcars.rowCount(), 'Mtcars dataset should have 32 rows')
    assertEquals(12, mtcars.columnCount(), 'Mtcars dataset should have 12 columns')
    MatrixAssertions.assertContentMatches(Dataset.mtcars(), mtcars, Dataset.mtcars().diff(mtcars))
  }

  @Test
  void testFetchDataWithNullPackage() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData(null, 'mtcars')
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchDataWithNullItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasets', null)
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchDataWithEmptyItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasets', '')
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchDataWithEmptyPackage() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('', 'mtcars')
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  @Tag('external')
  void testFetchDataWithNonExistentDataset() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasets', 'nonexistent_dataset_xyz_123')
    }
    assertEquals('Dataset not found: datasets/nonexistent_dataset_xyz_123', exception.message)
  }

  @Test
  void testFetchInfoWithNullPackage() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchInfo(null, 'mtcars')
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchInfoWithNullItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchInfo('datasets', null)
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchInfoWithEmptyPackage() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchInfo('', 'mtcars')
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchInfoWithEmptyItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchInfo('datasets', '')
    }
    assertEquals('Package name and item name cannot be null or empty', exception.message)
  }

  @Test
  @Tag('external')
  void testFetchInfoWithNonExistentDataset() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchInfo('datasets', 'nonexistent_dataset_xyz_123')
    }
    assertEquals('Dataset not found: datasets/nonexistent_dataset_xyz_123', exception.message)
  }

  @Test
  @Tag('external')
  void testFetchDataSingleArgOverload() {
    def iris = Rdatasets.fetchData('datasets/iris')
    assertNotNull(iris, 'iris dataset should not be null')
    assertTrue(iris.rowCount() > 0, 'iris should have rows')
  }

  @Test
  void testFetchDataSingleArgWithNull() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData(null)
    }
    assertEquals('packageSlashItem cannot be null or blank', exception.message)
  }

  @Test
  void testFetchDataSingleArgWithBlank() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('   ')
    }
    assertEquals('packageSlashItem cannot be null or blank', exception.message)
  }

  @Test
  void testFetchDataSingleArgWithMissingSlash() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasetsiris')
    }
    assertTrue(exception.message.contains('must contain a slash'))
  }

  @Test
  void testFetchDataSingleArgWithMultipleSlashes() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasets/iris/extra')
    }
    assertTrue(exception.message.contains('exactly one slash'))
  }

  @Test
  void testFetchDataSingleArgWithBlankPackage() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('/iris')
    }
    assertTrue(exception.message.contains('cannot be blank'))
  }

  @Test
  void testFetchDataSingleArgWithBlankItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasets/')
    }
    assertTrue(exception.message.contains('cannot be blank'))
  }

  @Test
  @Tag('external')
  void testSearch() {
    def result = Rdatasets.search('iris')
    assertNotNull(result, 'search result should not be null')
    assertTrue(result.rowCount() > 0, 'search should return at least one row')
  }

  @Test
  @Tag('external')
  void testSearchWithNoResults() {
    def result = Rdatasets.search('zzz_no_match_xyz')
    assertNotNull(result, 'search result should not be null')
    assertEquals(0, result.rowCount(), 'search should return zero rows')
  }

  @Test
  @Tag('external')
  void testRefresh() {
    def overview1 = Rdatasets.overview()
    assertNotNull(overview1)
    Rdatasets.refresh()
    def overview2 = Rdatasets.overview()
    assertNotNull(overview2)
    assertEquals(overview1.rowCount(), overview2.rowCount())
  }

}
