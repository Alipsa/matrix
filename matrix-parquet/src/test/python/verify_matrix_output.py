"""Run after :matrix-parquet:test, then rerun ParquetInteropTest to read CDC rewrites in Java."""
from decimal import Decimal
from pathlib import Path
import pyarrow as pa
import pyarrow.parquet as pq

assert pa.__version__ == '21.0.0'
root = Path(__file__).parents[3] / 'build/test-interop'
for codec in ('SNAPPY', 'ZSTD'):
    table = pq.read_table(root / f'{codec}.parquet')
    assert pa.types.is_string(table.schema.field('text').type)
    assert pa.types.is_binary(table.schema.field('payload').type)
    assert table['text'].to_pylist() == ['héllo', None]
    assert table['payload'].to_pylist() == [bytes([0, 255]), b'']
    assert table['price'].to_pylist() == [Decimal('1.25'), Decimal('-2.50')]
    assert table['tags'].to_pylist() == [['α', None], []]
    assert table.schema.metadata[b'matrix.parquet.version'] == b'2'
    output = root / f'{codec}.cdc.parquet'
    pq.write_table(table, output, compression=codec.lower(), use_content_defined_chunking=True, write_page_index=True)
    assert pq.read_table(output).equals(table, check_metadata=True)
    print(f'{codec}: annotated strings, binary and exact decimals verified; CDC rewrite preserves metadata')
