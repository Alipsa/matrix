"""Regenerate offline reference URL cases with huggingface_hub==2.2.0 (Python >=3.10)."""
from pathlib import Path
import huggingface_hub
from huggingface_hub import parse_hf_uri

assert huggingface_hub.__version__ == '2.2.0'
cases = [
    'hf://datasets/owner/repo/train.parquet',
    'hf://datasets/owner/repo@v1/train.parquet',
    'hf://datasets/owner/repo@feature%2Fbranch/train.parquet',
    'hf://datasets/owner/repo@refs/convert/parquet/train.parquet',
    'hf://datasets/owner/repo@refs%2Fconvert%2Fparquet/train.parquet',
    'hf://datasets/owner/repo@refs/pr/10/train.parquet',
    'hf://datasets/owner/repo@refs/convert/parquet-v2/train.parquet',
    'hf://datasets/owner/repo/sub/file@1.parquet',
    'hf://datasets/owner/repo@v%2B1/café x.parquet',
    'hf://datasets/owner/repo/file%20x.parquet',
    'hf://datasets/owner/repo@v*/file*.parquet',
    'hf://datasets/owner/repo@feature%252Fbranch/a%2Fb.parquet',
]
output = Path(__file__).parents[1] / 'resources/optimized/hub-urls.tsv'
output.write_text('# huggingface_hub=2.2.0\n' + ''.join(
    uri + '\t' + parse_hf_uri(uri).to_url().replace('/blob/', '/resolve/', 1) + '\n'
    for uri in cases), encoding='utf-8')
print(f'Generated {len(cases)} offline Hub client reference cases')
