# Matrix-avro release history

## v0.2.0 2026-03-19

- clarified reader option semantics and naming precedence
  - `AvroReadOptions.matrixName(...)` wins first
  - otherwise the Avro record name is used
  - otherwise a source-derived fallback such as the file name or `AvroMatrix` is used
- aligned the supported read option surface with the implementation
  - `readerSchema(...)` remains the schema-evolution entry point
  - non-functional read options were removed from the public contract
- aligned writer defaults and validation
  - schema naming now prefers `schemaName(...)`, then `matrix.matrixName`, then `MatrixSchema`
  - invalid `compressionLevel` and `syncInterval` values now fail fast
- added explicit per-column schema control for writes
  - fixed decimal precision and scale with `AvroSchemaDecl.decimal(...)`
  - explicit array element, map value, scalar, and record declarations with `AvroSchemaDecl`
  - SPI support through `columnSchemas`
- refreshed the README, tutorial, and cookbook around the typed options-first APIs
- corrected Maven publication metadata for the Avro module
  - POM name, description, license URL, and SCM URLs now point at the Avro module on the `main` branch
  - the release script now uses the module-local Gradle wrapper and the final `0.2.0` module version

## v0.1.0 2026-01-30
initial release
Support for reading an avro file into a matrix and writing a matrix to an avro file
