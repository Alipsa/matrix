# Interoperability fixture provenance

The manifest records Arrow 21.0.0 schemas, write options, expected values and SHA-256 hashes.
Eight cases cover CDC/index/codec/dictionary/page-version pairs, with two row groups, raw media
bytes, local timestamps, exact decimals, nulls and nested values. CDC uses 4–16 KiB chunks to
keep checked-in files below 1 MiB each and 4 MiB in total. Java tests inspect actual page indexes.

Recreate the Arrow cases from the repository root:

```bash
python3 -m venv /tmp/matrix-parquet-fixtures
/tmp/matrix-parquet-fixtures/bin/python -m pip install pyarrow==21.0.0
/tmp/matrix-parquet-fixtures/bin/python matrix-parquet/src/test/python/generate_optimized.py
PARQUET_PYTHON=/tmp/matrix-parquet-fixtures/bin/python \
  ./gradlew :matrix-parquet:test -PrunSlowTests=true
```

The slow command also generates a default-size CDC file under build/default-cdc. Python is not
required for ordinary tests, which consume the checked-in fixtures. CI installs pinned Python
dependencies and regenerates both kinds.

`matrix-0.7-strings.parquet`, `matrix-0.7-metadata.parquet` and
`matrix-0.7-date-units.parquet` were captured using the original writer at
b2095159664b34ce426685c387adf0bccdb33e01. The opt-in `ParquetLegacyCaptureTest` compiles that
writer and its value helper together in an isolated loader. It records the mixed Date/Timestamp
1000-fold unit corruption. The original writer cannot produce a valid byte[] file here: it fails
with InvalidSchemaException. `legacy-binary-metadata.parquet` is explicitly synthetic old `[B`
metadata with a textual payload; it tests rejection, not recovery of original bytes.

`hub-urls.tsv` comes from offline parsing and URL generation by huggingface_hub **2.2.0**:

```bash
python3 -m venv /tmp/matrix-hf-fixtures
/tmp/matrix-hf-fixtures/bin/python -m pip install huggingface_hub==2.2.0
/tmp/matrix-hf-fixtures/bin/python matrix-parquet/src/test/python/generate_hub_urls.py
```

The reviewed v1.0.0 parser uses resolve_path; 2.2.0 uses parse_hf_uri. The later convert-ref
regex accepts dots/dashes (`[\w.-]+`), and revision separation occurs immediately after the
namespaced repository, so deep-path `@` is literal. Special convert/pr refs remain unescaped;
other revision slashes are encoded. Revision text is unquoted once; paths are literal and
quoted, including existing percent signs. The Java parser follows the tested 2.2.0 behavior.
