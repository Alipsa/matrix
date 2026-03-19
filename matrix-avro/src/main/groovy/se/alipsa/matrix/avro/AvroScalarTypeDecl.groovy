package se.alipsa.matrix.avro

/**
 * Scalar Avro schema declarations supported by explicit per-column overrides.
 */
enum AvroScalarTypeDecl {
  STRING,
  BOOLEAN,
  INT,
  LONG,
  FLOAT,
  DOUBLE,
  BYTES,
  DATE,
  TIME_MILLIS,
  TIMESTAMP_MILLIS,
  LOCAL_TIMESTAMP_MICROS,
  UUID
}
