"""Reproduce fixtures: python -m pip install pyarrow==21.0.0; python generate_optimized.py [output]."""
import hashlib
import json
import sys
from datetime import datetime, date
from decimal import Decimal
from pathlib import Path

import pyarrow as pa
import pyarrow.parquet as pq

assert pa.__version__ == '21.0.0', pa.__version__
out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parents[1] / 'resources/optimized'
out.mkdir(parents=True, exist_ok=True)
rows = 4096
table = pa.table({
    'id': pa.array(range(rows), type=pa.int64()),
    'text': [None if i % 17 == 0 else f'café-{i % 31}-' + 'abcd' * 32 for i in range(rows)],
    'payload': pa.array([bytes([i % 256, 0, 255]) for i in range(rows)], type=pa.binary()),
    'price': pa.array([Decimal(i).scaleb(-2) for i in range(rows)], type=pa.decimal128(12, 2)),
    'local': pa.array([datetime(2024, 3, 31, 1, 30)] * rows, type=pa.timestamp('us')),
    'date': pa.array([date(2024, 1, 1)] * rows),
    'nulls': pa.array([None] * rows, type=pa.string()),
    'media': pa.array([None if i % 11 == 0 else {'bytes': bytes([i % 256, 0, 255]), 'path': f'image-{i % 31}.jpg'} for i in range(rows)], type=pa.struct([('bytes', pa.binary()), ('path', pa.string())])),
    'tags': pa.array([None if i % 19 == 0 else ([] if i % 7 == 0 else ['α', None, 'β']) for i in range(rows)], type=pa.list_(pa.string())),
})
manifest = {'pyarrow': pa.__version__, 'rows': rows, 'expected': {'first_id': 0, 'last_id': rows - 1, 'price_123': '1.23', 'local': '2024-03-31T01:30:00', 'payload_0_hex': '0000ff', 'tags_1': ['α', None, 'β']}, 'files': {}}
# Eight-case binary orthogonal array: each pair of CDC/index/codec/dictionary/page factors
# covers all four combinations, rather than requiring the 32-case Cartesian product.
for cdc in (False, True):
    for index in (False, True):
        for alternate in (False, True):
            suffix = '-alternate' if alternate else ''
            name = f'cdc-{int(cdc)}-index-{int(index)}{suffix}.parquet'
            options = dict(compression='zstd' if cdc ^ alternate else 'snappy',
                           use_dictionary=['text', 'tags.list.element'] if index ^ alternate else False,
                           data_page_version='2.0' if cdc ^ index ^ alternate else '1.0',
                           write_page_index=index, row_group_size=2048,
                           data_page_size=4096, write_batch_size=128, dictionary_pagesize_limit=1024,
                           use_content_defined_chunking=dict(min_chunk_size=4096, max_chunk_size=16384, norm_level=0) if cdc else False)
            pq.write_table(table, out / name, **options)
            assert pq.read_table(out / name).equals(table)
            assert (out / name).stat().st_size <= 1024 * 1024
            manifest['files'][name] = {'options': options, 'sha256': hashlib.sha256((out / name).read_bytes()).hexdigest(), 'schema': str(table.schema)}
# Arrow emits an empty dictionary for an all-null string column.
for name, extra in {
    'arrow-all-null-string.parquet': pa.table({'s': pa.array([None] * 3, type=pa.string())}),
    'arrow-json.parquet': pa.table({'json': pa.array(['{"label":"α"}', None, '{}'], type=pa.json_())}),
}.items():
    pq.write_table(extra, out / name, use_dictionary=True, write_page_index=True)
    manifest['files'][name] = {'sha256': hashlib.sha256((out / name).read_bytes()).hexdigest(),
                             'rows': extra.num_rows, 'schema': str(extra.schema)}
# A synthetic legacy [B footer exercises contextual rejection; the original writer cannot
# produce a valid byte[] file in this checkout (InvalidSchemaException, captured separately).
legacy_binary = pa.table({'payload': pa.array([b'[B@legacy-text'], type=pa.binary())})
legacy_binary = legacy_binary.replace_schema_metadata({b'matrix.columnTypes': b'[B'})
pq.write_table(legacy_binary, out / 'legacy-binary-metadata.parquet')
manifest['files']['legacy-binary-metadata.parquet'] = {
    'provenance': 'synthetic legacy [B metadata, not claimed as original recoverable bytes',
    'sha256': hashlib.sha256((out / 'legacy-binary-metadata.parquet').read_bytes()).hexdigest(),
    'expected': 'unrecoverable-legacy-binary error'}
for name in ('matrix-0.7-strings.parquet', 'matrix-0.7-date-units.parquet', 'matrix-0.7-metadata.parquet'):
    if (out / name).is_file():
        manifest['files'][name] = {
            'provenance': 'original writer at b2095159664b34ce426685c387adf0bccdb33e01',
            'sha256': hashlib.sha256((out / name).read_bytes()).hexdigest(),
            'schema': str(pq.read_schema(out / name))}
if '--default-cdc' in sys.argv:
    pq.write_table(pa.concat_tables([table] * 4), out / 'default-cdc.parquet', use_content_defined_chunking=True, write_page_index=True, use_dictionary=False)
assert sum(p.stat().st_size for p in out.glob('*.parquet')) <= 4 * 1024 * 1024
(out / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
