# Charm Parity Fixture Corpus

This corpus provides representative fixtures for P0 parity work in the gg-to-charm migration.

## Structure

- `p0-fixture-manifest.csv`: maps each P0 feature to a fixture file.
- `p0/geoms/*.csv`: representative data for each P0 geom.
- `p0/stats/*.csv`: representative data for each P0 stat.
- `p0/coords/*.csv`: representative data for each P0 coord.

## Notes

- CSV format is intentionally simple (comma-separated, no quoted escaping) so tests can parse fixtures with lightweight helpers.
- Fixtures are representative rather than exhaustive. Detailed behavior is validated by dedicated feature tests.
