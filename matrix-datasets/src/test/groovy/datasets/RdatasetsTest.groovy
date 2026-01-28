package datasets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.datasets.Rdatasets

import static org.junit.jupiter.api.Assertions.*

class RdatasetsTest {

  @Test
  void testOverview() {
    def overview = Rdatasets.overview()
    assertNotNull(overview, 'Overview should not be null')
    assertTrue(overview.rowCount() > 0, 'Overview should have rows')
    assertTrue(overview.columnCount() > 0, 'Overview should have columns')
  }

  @Test
  void fetchInfo() {
    String info = Rdatasets.fetchInfo('AER', 'BankWages', true)
    assertTrue(info.contains('A data frame containing 474 observations on 4 variables.'))
  }

  @Test
  void testFetchData() {
    def mtcars = Rdatasets.fetchData('datasets', 'mtcars')
    assertNotNull(mtcars, 'Mtcars dataset should not be null')
    mtcars.columnName(0, 'model')
    //println "mtcars content: ${mtcars.content()}"
    //println Dataset.mtcars().content()
    assertEquals(32, mtcars.rowCount(), 'Mtcars dataset should have 32 rows')
    assertEquals(12, mtcars.columnCount(), 'Mtcars dataset should have 12 columns')
    MatrixAssertions.assertContentMatches(Dataset.mtcars(), mtcars, Dataset.mtcars().diff(mtcars))
  }

  @Test
  void testFetchDataWithNullPackage() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData(null, 'mtcars')
    }
    assertEquals('Dataset name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchDataWithNullItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasets', null)
    }
    assertEquals('Dataset name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchDataWithEmptyItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchData('datasets', '')
    }
    assertEquals('Dataset name cannot be null or empty', exception.message)
  }

  @Test
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
    assertEquals('Dataset name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchInfoWithNullItem() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchInfo('datasets', null)
    }
    assertEquals('Dataset name cannot be null or empty', exception.message)
  }

  @Test
  void testFetchInfoWithNonExistentDataset() {
    def exception = assertThrows(IllegalArgumentException) {
      Rdatasets.fetchInfo('datasets', 'nonexistent_dataset_xyz_123')
    }
    assertEquals('Dataset not found: datasets/nonexistent_dataset_xyz_123', exception.message)
  }
}
