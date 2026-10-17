"""Optional million-row layout benchmark inputs; never checked into Git."""
from pathlib import Path
import pyarrow as pa
import pyarrow.parquet as pq

assert pa.__version__ == '21.0.0'
out = Path(__file__).parents[3] / 'build/layout-benchmarks'
out.mkdir(parents=True, exist_ok=True)
rows = 1_000_000
table = pa.table({'id': pa.array(range(rows), type=pa.int64()), **{
    f'text{c}': [f'café-{c}-{i % 1000}-' + 'abcd' * 32 for i in range(rows)]
    for c in range(6)}})
for name, cdc, index in [('ordinary', False, False), ('indexed', False, True), ('cdc-indexed', True, True)]:
    target = out / f'{name}.parquet'
    pq.write_table(table, target, compression='snappy', row_group_size=250_000,
                   data_page_size=64 * 1024, write_page_index=index,
                   use_content_defined_chunking=cdc)
    assert pq.read_table(target).equals(table)
    print(f'{name}: rows={rows}, bytes={target.stat().st_size}')
