# matrix-jupyter release history

## v0.1.0, 2026-09-22

- Add Jupyter MIME renderers for Matrix tables and optional Charm, ggplot, and pict SVG charts.
- `Row` and `Column` values render as typed single-row / single-column tables so numeric columns right-align and carry their type class.
- `RenderOptions` validates its arguments: negative `maxRows`/`maxColumns` and non-positive `width`/`height` throw `IllegalArgumentException`.
- Renderer failures are reported in the `text/plain` payload as `Rendering failed in <renderer>: <message>`; plain-only bundles are passed through unchanged.
- Renderer providers whose `supportedTypes()` is null or contains null are skipped and listed in `describe()` instead of aborting kernel installation.
