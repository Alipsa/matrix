# matrix-jupyter

`matrix-jupyter` makes returned Matrix values render as HTML tables and supported Matrix charts render as inline SVG in a jjava-based Groovy Jupyter kernel. SVG rendering requires gsvg 1.2.0, which namespaces every inline SVG serialization to prevent collisions between chart IDs and CSS animations.

Install `matrix-jupyter` on the kernel launch classpath (the guaranteed deployment path), together with whichever optional chart module you use. Then, in a notebook cell:

```groovy
@Grab('se.alipsa.matrix:matrix-jupyter:0.1.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.5.1-SNAPSHOT')

import se.alipsa.matrix.core.Matrix

Matrix.builder().columns(city: ['Stockholm', 'Uppsala'], population: [984748, 245329]).build()
```

`RenderOptions.defaults` controls tables and applicable chart sizes: `maxRows` and `maxColumns` default to `50` (use `null` for no limit), `fromHead` is `true`, `attr` is an empty map, and `width`/`height` are `800`/`600`. For example:

```groovy
import se.alipsa.matrix.jupyter.RenderOptions
RenderOptions.defaults = new RenderOptions(20, 10, true, [class: 'matrix-table'], 1000, 700)
```

Tables report truncation in a caption. Attribute values other than `caption` are passed through to `Matrix.toHtml` and are not escaped. `GgChart` dimensions are not changed because doing so would mutate the chart object.

If a chart module is added after a first render, call:

```groovy
import se.alipsa.matrix.jupyter.kernel.MatrixJupyterExtension
MatrixJupyterExtension.describe()
MatrixJupyterExtension.refresh()
```

`@Grab`-only extension discovery requires the Groovy kernel to rescan its session loader after grabbing; until that kernel behavior is confirmed, static installation is the supported approach. For standalone hosts, use `RendererRegistry.instance.render(value)` and `RendererRegistry.instance.describe()`.
