# Matrix-parquet Release History

## v0.4.0, In progress
- Upgrade dependencies 
  - org.apache.hadoop:hadoop-common [3.4.1 -> 3.4.2]
  - org.apache.hadoop:hadoop-mapreduce-client-core [3.4.1 -> 3.4.2]
  - org.apache.parquet:parquet-column [1.15.2 -> 1.16.0]
  - org.apache.parquet:parquet-hadoop [1.15.2 -> 1.16.0]
- remove parquet-carpet (MatrixCarpetIO).
- MatrixParquetWriter can now either take a file or a dir (and use the matrix name)
- Add write methods to MatrixParquetWriter that takes precision and scale for BigDecimal columns.
- Fixed bug when inferring schema that set scale to 2 as the smallest scale. 
- Add support for structs (pojos, maps) and repeated fields (arrays) when inferring schema and writing data.
- use matrixName as schema name if present

## v0.3.0, 2025-05-28
- Add a "native" parquet implementation in the form of MatrixParquetReader and MatrixParquetWriter.
Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-parquet/0.3.0/matrix-parquet-0.3.0.jar

## v0.2, 2025-03-12
- Require jdk21
Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-parquet/0.2/matrix-parquet-0.2.jar

## v0.1, 2025-02-16
- initial release