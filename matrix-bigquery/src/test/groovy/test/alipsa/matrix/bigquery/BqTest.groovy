package test.alipsa.matrix.bigquery

import org.junit.jupiter.api.Test
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix

class BqTest {

  @Test
  void testExample() {
    Bq bq = new Bq()
    Matrix m = bq.query("""SELECT CONCAT('https://stackoverflow.com/questions/', 
        CAST(id as STRING)) as url, view_count 
        FROM `bigquery-public-data.stackoverflow.posts_questions` 
        WHERE tags like '%google-bigquery%' 
        ORDER BY view_count DESC
        LIMIT 10
        """.stripIndent())
    println m.content()
  }
}
