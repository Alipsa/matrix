# matrix-jupyter — Design Spec

**Module:** `matrix-jupyter` (new), `se.alipsa.matrix:matrix-jupyter:0.1.0-SNAPSHOT`
**Date:** 2026-08-29 (rev. 11, after ten review rounds)
**Origin:** [groovy-jupyter assessment](https://github.com/paulk-asert/groovy-jupyter/blob/main/docs/assessment.md)
§6.1 — *"propose a small `matrix-jupyter` extension jar (renderer SPI: Matrix → HTML table,
chart → SVG) contributed to and maintained in the Alipsa org — the first worked example of the
Extension mechanism"* — and §10 phase 2, whose exit criterion is *"Matrix chart renders as SVG
with zero user glue"*.

Every API claim in this spec was verified against source or bytecode; see §10 for what was checked
and what remains unverified.

## Problem

Matrix's own `docs/python-comparison.md:266` lists "No Jupyter integration" as a known limitation.
The Apache Groovy team is building `groovy-jupyter`, a minimal Jupyter kernel for Groovy 6, whose
display layer is deliberately *pass-through only*: the kernel emits mime bundles and ships no
plotting API and no JavaScript. Libraries integrate themselves by contributing a jar that registers
renderers through the kernel's `Extension` `ServiceLoader` SPI.

Matrix is a natural fit — `Matrix.toHtml(...)` already produces escaped HTML tables and every chart
facade already renders to SVG — but nothing today connects a returned `Matrix` or `Chart` to a mime
bundle. Without that, a notebook cell shows `se.alipsa.matrix.core.Matrix@3f2a1b`.

## Goals

1. A returned `Matrix` renders as an HTML table, and a returned chart renders as inline SVG, with no
   user code beyond `@Grab`.
2. No *optional* module is forced on a notebook. matrix-core is a genuine runtime requirement and is a
   normal dependency (§1.1, D8); matrix-charts, matrix-ggplot and matrix-pict are not, and their
   renderers activate only when the module is present.
3. The kernel dependency (`org.dflib.jjava:jjava-jupyter`, alpha) is confined to exactly one class.
4. The rendering layer is usable outside Jupyter — Gade, gmd, HTML reports, tests — because it has no
   kernel dependency at all.
5. Matrix remains non-load-bearing for the kernel, per assessment §11 ("single-maintainer partners").

## Non-goals

Following the assessment's minimalism principle (§4):

- No `display()`/`update()` streaming handle — kernel core, not library scope.
- No magics.
- No JavaScript, no widgets, no interactive tables.
- No plotting API of our own.
- **No default-import injection.** Verified impossible through this SPI: `BaseKernel` exposes no
  import hook (§10.1). It was also outside the D1 charter — `se.alipsa.matrix.core.*` would put
  `Row`, `Column`, `Grid`, `Stat`, `Converter` into every cell namespace where they can shadow other
  grabbed libraries.
- No `matrix-xchart` renderer in v1 — AWT/Swing export path, headlessness unverified (assessment §11).
- No Vega-Lite emitter.
- No `Renderable` interface pushed into matrix-core or the chart modules.

## Decisions taken

| # | Decision | Rationale |
|---|---|---|
| D1 | Minimal renderer jar only | Assessment §6.1/§4; BeakerX died of accretion |
| D2 | Host-neutral core + one thin kernel adapter | Alpha churn touches one file; layer reusable by Gade/gmd |
| D3 | Renderers for matrix-core types + every SVG chart facade | Delivers phase-2 exit criterion; excludes the unverified xchart path |
| D4 | New module in this monorepo, in the BOM | Consistent with all other `matrix-*` modules |
| D5 | Unit tests + in-JVM adapter test; no testcontainers IT | Fast; the kernel repo owns real-Jupyter ITs |
| D6 | SVG id **and CSS** namespacing lives in gsvg + charm, not here | gsvg owns the dom4j tree and IDREF knowledge; see §5 |
| D7 | Tables truncate rows **and columns** by default | A 100k-row or 500-column Matrix must not serialize into notebook JSON |
| D8 | matrix-core is a normal (`api`) dependency; every other matrix module is `compileOnly` | The always-loaded registry uses `core.util.Logger` per AGENTS.md; matrix-jupyter is useless without matrix-core, so pretending it is optional buys nothing and risks `NoClassDefFoundError` from the registry itself |
| D9 | Service discovery unions this jar's own loader, the TCCL and an explicit `reload(ClassLoader)`; optional-class probes use the TCCL | Service providers need both deployment views, while application servers require context-loader class resolution (§2.3.1) |
| D10 | Registry supports `reload()`, **and the extension supports `refresh()`** | A registry-only reload never reaches the notebook: jjava must also be told about the newly renderable types (§2.6.1) |

## 1. Architecture

One jar, package root `se.alipsa.matrix.jupyter`:

```
se.alipsa.matrix.jupyter
├── MimeBundle          value type: mime → data, richest first, always incl. text/plain
├── MatrixRenderer      SPI interface
├── AbstractRenderer    shared classpath probing + plain-text fallback
├── ActiveRenderer      immutable record: an available renderer + normalized mime/type facts (§2.3.2)
├── SkippedRenderer     immutable record: an unavailable renderer + diagnostic facts (§2.3.2)
├── RenderOptions       immutable options + mutable global default
├── RendererRegistry    ServiceLoader discovery, type → renderer dispatch, active(), reload()
├── render/
│   ├── CoreRenderer    Matrix, Grid, Row, Column, Summary, Structure → text/html
│   ├── CharmRenderer   charm Chart, PlotGrid                          → image/svg+xml
│   ├── GgRenderer      GgChart                                        → image/svg+xml
│   └── PictRenderer    pict Chart                                     → image/svg+xml
└── kernel/
    └── MatrixJupyterExtension   the ONLY class referencing jjava-jupyter
```

The bottom two layers have no kernel dependency: `RendererRegistry.instance.render(value)` returns a
plain `Map<String, Object>` that Gade, gmd, a servlet or a unit test can consume directly.

### 1.1 Dependencies

```groovy
api project(':matrix-core')                       // D8: genuine runtime requirement

compileOnly libs.groovy
compileOnly project(':matrix-charts')
compileOnly project(':matrix-ggplot')
compileOnly project(':matrix-pict')
compileOnly libs.gsvg                             // SvgWriter; §5
compileOnly libs.jjava.jupyter                    // org.dflib.jjava:jjava-jupyter:1.0-a8, pinned

testImplementation ...                            // all of the above, plus junit/groovier-junit
```

matrix-stats is **not** a dependency: no renderer targets a stats type (`Summary` and `Structure` are
matrix-core), and with imports dropped there is nothing else it was there for.

Consequence: the published POM declares matrix-core and nothing else. Adding matrix-charts to a
notebook lights up chart rendering automatically; not adding it costs nothing.

## 2. Components

### 2.1 `MimeBundle`

A `LinkedHashMap<String, Object>` subclass whose insertion order is preference order. Static
factories: `MimeBundle.html(String, String plainFallback)`, `MimeBundle.svg(String, String
plainFallback)`, `MimeBundle.plain(String)`.

**Invariant:** every bundle carries `text/plain`. Console frontends, `nbconvert` to text and the
Zeppelin bridge (assessment §8) must never show an empty cell.

### 2.2 `MatrixRenderer` (the SPI)

```groovy
interface MatrixRenderer {
  /** Human-readable name, used by RendererRegistry.describe(). */
  String rendererName()

  /** Classpath probe. When false, the renderer is never registered and
      supportedTypes() is never called. */
  boolean available()

  /** Types this renderer handles. Only called when available() is true.
      Authoritative: there is no second-stage veto. */
  Set<Class<?>> supportedTypes()

  /** Diagnostic used only when available() is false.  Implementations must not
      require their optional target type to be loadable here. */
  default String unavailableReason() { 'available() returned false' }

  /** The richest mime type this renderer produces, e.g. 'text/html' or 'image/svg+xml'.
      Defaulted here so implementers of this interface — not just AbstractRenderer
      subclasses — get the common case for free; the three chart renderers override it. */
  default String preferredMime() { 'text/html' }

  /** Render the value. Never returns null for a type in supportedTypes(). */
  MimeBundle render(Object value, RenderOptions options)
}
```

**The default lives on the interface, not only on `AbstractRenderer`.** Groovy 5 supports interface
default methods, and third-party implementation of this SPI is an advertised feature — so putting the
default here means a third party implementing `MatrixRenderer` directly gets the common case free,
and, more usefully, establishes at 0.1.0 that additions to this SPI do not break existing
implementers. `AbstractRenderer` inherits it unchanged.

**`preferredMime()` is part of the SPI, not adapter knowledge.** The kernel adapter must tell jjava
which mime a registration prefers (§2.6), and it has no other honest way to learn it: `instanceof`
against the four renderers in this jar would register a *third-party* SVG renderer as HTML, breaking
the "third parties may register renderers the same way" promise below, and rendering a sample value
at install time to find out would defeat §2.6's laziness and requires a value the adapter does not
have. Declaring it also gives `describe()` something worth printing.

`handles(Object)` from rev. 1 is **removed**. It overlapped `supportedTypes()` with undefined
precedence, and jjava's own `Renderer` already dispatches by registered type (§10.1), so a
second-stage veto had no defined miss path in either consumer. `supportedTypes()` is the single
authority.

**Availability probing.** `AbstractRenderer.probe(String className)` uses
`Thread.currentThread().contextClassLoader.loadClass(className)` inside `try`/`catch (Throwable)`.
It treats a missing TCCL as unavailable. `Throwable`, rather than `Exception`, is required because a
partially present module raises `NoClassDefFoundError`. Context-loader resolution is required for
containers and application servers; it is the probing rule in D9.

`AbstractRenderer.unavailableReason()` reports the failed probe as
`<class name> not on classpath`. A third-party renderer that does not subclass it inherits the
interface's honest generic reason. This lets the registry expose skipped renderers as structured data
without trying to call `supportedTypes()` on a renderer that declared itself unavailable.

**The anchor class must come from the host-neutral layer.** `MatrixRenderer.classLoader`, never
`MatrixJupyterExtension.classLoader`: resolving a class resolves its declared superinterfaces, so
evaluating the latter's class literal in a JVM without jjava on the classpath throws
`NoClassDefFoundError: org/dflib/jjava/jupyter/Extension` from inside the layer Goal 4 exists to keep
kernel-free — the Gade/gmd/servlet case. `MatrixRenderer` ships in the same jar with the same defining
loader and has no jjava in its supertype closure. Because §6 puts `jjava-jupyter` on
`testImplementation`, no test in this suite would catch the mistake; §6 test 8 adds one that would.

**Registration.** `META-INF/services/se.alipsa.matrix.jupyter.MatrixRenderer`, listing the four
implementations in this jar. Third parties may register renderers for their own types the same way.

**Precedence.** When two renderers claim the same type, the first one loaded wins and the registry
logs a warning naming both — identical to `FormatRegistry`'s handling of a duplicate extension
(`FormatRegistry.groovy:232`). The loser stays **active** — it lost one type, not its registration,
and may still be the renderer for others — so `describe()` reports it as a `shadowed for <Type> by
<Other>` annotation on its `active:` row, not as a category of its own (§2.3.2).

`AbstractRenderer` also holds the plain-text fallback so the four renderers do not repeat it
(AGENTS.md DRY).

### 2.3 `RendererRegistry`

Singleton, modelled on `se.alipsa.matrix.core.spi.FormatRegistry`: `volatile boolean loaded`,
lock-guarded lazy load, providers filtered by `available()`.

#### 2.3.1 Classloader (D9)

`FormatRegistry` uses the single-argument `ServiceLoader.load(...)` form
(`FormatRegistry.groovy:230`), resolving against the thread context classloader. That is safe inside
matrix-core, where the question does not arise, but insufficient here — and a single fixed answer is
wrong too, because **two deployments exist and they need opposite loaders**
(both verified in §10.1):

| Deployment | How the extension is installed | Loader that sees `@Grab`-ed matrix modules |
|---|---|---|
| **Grabbed** — `@Grab('se.alipsa.matrix:matrix-jupyter')` in a cell | kernel calls `installExtensions(sessionLoader)`; Grape grabs into that same session `GroovyClassLoader` (assessment §3.4) | this jar's own defining loader — it *is* the session loader |
| **Static** — jar on the kernel's launch classpath | `installDefaultExtensions()` → `installExtensions(getClassLoader())` at startup | **not** this jar's loader (that is the app loader, which cannot see later grabs) — the session loader reaches it only as TCCL |

The two consumers of a loader need **different** rules:

- **Optional-class probing — TCCL.** Resolve optional renderer targets through
  `Thread.currentThread().contextClassLoader.loadClass(...)`. This is the loader a container exposes
  for the active application; using `Class.forName` or a library defining loader can select the wrong
  deployment and pin its classes.
- **`ServiceLoader` discovery — union, not fallback.** A fallback would never fire: the own-loader
  pass *always* succeeds, because `CoreRenderer` ships in this very jar. In the static deployment that
  would silently hide a third-party renderer jar grabbed into the session loader — the exact case the
  table above says the TCCL covers. So discovery runs over **both** loaders and unions the results,
  deduplicated by provider class name (name, not identity: the same provider class loaded by both
  loaders yields two distinct `Class` objects). Discovery reads that name once from
  `ServiceLoader.Provider.type().name`; it uses the same value for deduplication and carries it into
  each discovered record as `providerClassName` (§2.3.2).

```groovy
Set<String> seenProviderNames = new LinkedHashSet<>()
List<ClassLoader> loaders = []
[MatrixRenderer.classLoader, Thread.currentThread().contextClassLoader, extraLoader].each { ClassLoader loader ->
  if (loader != null && !loaders.any { ClassLoader known -> known.is(loader) }) {
    loaders.add(loader)
  }
}
loaders.each { ClassLoader loader ->
  ServiceLoader.load(MatrixRenderer, loader).stream().forEach {
      ServiceLoader.Provider<MatrixRenderer> provider ->
    String providerClassName = provider.type().name
    if (seenProviderNames.add(providerClassName)) {
      discover(provider, providerClassName) // catches Throwable from get(), available(), and record creation
    }
  }
}
```

The `stream()` form is intentional: `Provider.type()` yields the dedupe key before `Provider.get()`
constructs the renderer. A provider class visible through both loaders is consequently never
constructed twice; the first loader in the list wins, matching the registry's normal first-loaded
precedence. The loader list is identity-deduplicated and omits `null`: a grabbed deployment commonly
has the same own loader and TCCL, while a null TCCL must not silently turn into `ServiceLoader`'s
system-loader fallback.

**Discovery isolates every provider.** `discover(provider, providerClassName)` wraps `provider.get()`,
`rendererName()`, `available()`, mime normalization, and record creation in `try`/`catch (Throwable)`.
The same breadth as `AbstractRenderer.probe` (§2.2) is required: `Provider.get()` can throw
`ServiceConfigurationError` before `available()` is reached, and a direct third-party
`MatrixRenderer` can throw from `available()` without using `AbstractRenderer.probe`. On failure,
discovery logs a warning and adds a `SkippedRenderer` with the provider class name, the best
renderer-name value already obtained (or the provider class's simple name), **unknown** mime facts,
and `throwable.message` as its reason (falling back to the throwable class name when the message is
blank). One broken provider therefore cannot prevent `CoreRenderer` or any later provider from being
discovered.

`reload(ClassLoader)` remains available for a host that knows better. This is the difference between
chart rendering working and silently not working in the static deployment.

**`reload()`.** Required, not optional. Notebook cells run one at a time; if any `render(...)` or
`describe()` happens before `@Grab('…matrix-charts…')`, `CharmRenderer.available()` is false and — with
a one-shot `loaded` flag — the renderer would be skipped for the life of the kernel with no recovery.
`reload()` re-runs discovery under the lock, exactly as `FormatRegistry.reload()` does
(`FormatRegistry.groovy:173`). Overload `reload(ClassLoader)` per §2.3.1.

**`reload()` alone is not the notebook cure** — see §2.6.1 — so `describe()` names both remedies as
static text. It does *not* detect which applies: that would require knowing whether a kernel is
attached, which this layer cannot know (§2.3.2, §2.6.2).

`RendererRegistry.describe()` — everything below is derivable host-neutrally:

```
matrix-jupyter renderers
  active:  CoreRenderer  → text/html      (Matrix, Grid, Row, Column, Summary, Structure)
           AcmeRenderer [com.acme.WidgetRenderer] → "chart"  (com.acme.Widget)
             unsupported-mime — "chart" is not a type/subtype string
           AcmeTables    → text/html      (com.acme.Report, Matrix)
             shadowed for Matrix by CoreRenderer
  skipped: CharmRenderer → image/svg+xml  — se.alipsa.matrix.charm.Chart not on classpath
           AcmeRenderer [com.acme.BrokenRenderer] → ? — com.acme.OptionalWidget missing
Grabbed a module after first render?
  in a notebook:  MatrixJupyterExtension.refresh()      // reloads AND re-registers with the kernel
  otherwise:      RendererRegistry.instance.reload()
```

#### 2.3.2 `preferredMime()` is normalized and shape-checked at load time

Discovery does not hand renderers around bare. Each active renderer is wrapped in a small record that
carries the *evaluated* mime alongside it:

```groovy
/** One discovered, available renderer plus the mime facts derived from it at load time. */
class ActiveRenderer {
  final MatrixRenderer renderer
  /** Stable provider identity from ServiceLoader.Provider.type().name, never a display label. */
  final String providerClassName
  final String preferredMime      // normalized; never null or blank
  final boolean mimeUsable        // false ⇒ declared mime is not a type/subtype string
  final Set<Class<?>> supportedTypes
  /** Types this renderer lost, mapped to the winning provider class name. */
  final Map<Class<?>, String> shadowedBy

  ActiveRenderer(MatrixRenderer renderer, String providerClassName, String preferredMime, boolean mimeUsable,
                 Set<Class<?>> supportedTypes, Map<Class<?>, String> shadowedBy) { … }
}

/** One unavailable renderer, or a provider that could not be safely discovered. */
class SkippedRenderer {
  /** Captured display label, or the provider class's simple name if construction failed. */
  final String rendererName
  /** Stable provider identity from ServiceLoader.Provider.type().name, never a display label. */
  final String providerClassName
  /** Normalized declared mime, or null when discovery failed before the provider declared one. */
  final String preferredMime
  /** Null when preferredMime is unknown; otherwise its shape-validation result. */
  final Boolean mimeUsable
  final String reason             // e.g. "se.alipsa.matrix.charm.Chart not on classpath"

  SkippedRenderer(String rendererName, String providerClassName, String preferredMime, Boolean mimeUsable,
                  String reason) { … }
}

/** Unmodifiable list of the records the registry holds. */
List<ActiveRenderer> RendererRegistry.active()

/** Unmodifiable list of the unavailable records from the same discovery pass. */
List<SkippedRenderer> RendererRegistry.skipped()
```

The fields, collections and returned lists are unmodifiable because the registry publishes the *same*
records it holds: mutable state would let a consumer rewrite its mime, type or precedence facts and
undo the very cross-layer agreement these records exist to establish. Same discipline as
`RenderOptions`' defensive copy (§2.4). `@Immutable` is not used because `MatrixRenderer` is not a
known-immutable type.

Validation at discovery applies the same mime normalization to both record kinds when a renderer was
constructed successfully (it never calls `supportedTypes()` for a skipped renderer). A provider that
fails before those facts can be read carries `preferredMime = null` and `mimeUsable = null`; the
formatter prints its mime column as `?`, rather than fabricating a declaration:

- `null` or blank `preferredMime()` → `preferredMime = 'text/html'`, `mimeUsable = true`. A third
  party can return either despite the interface default, which only covers implementers that do not
  override the method — and `MIMEType.parse(null)` throws `NullPointerException`, not
  `MIMETypeParseException`, so leaving this to the kernel layer would kill the install one step before
  the guard meant to prevent it.
- otherwise a `type/subtype` shape check → `mimeUsable` accordingly, `preferredMime` unchanged.

**The record exists because normalization has to travel.** A registry cannot rewrite a method on
someone else's object, so `preferredMime()` still returns `null` no matter what the registry decided.
Any consumer that re-calls the SPI method — as an earlier draft of §2.6 did — sees the raw value and
silently diverges from what `describe()` reports. Publishing the normalized string in `active()` is
what keeps the two layers describing the same renderer, and it removes the adapter's double
`preferredMime()` call as a side effect.

**A bad mime does not disable host-neutral rendering.** `mimeUsable = false` renderers stay **active**.
`MimeBundle`'s keys come from the renderer's own `render()` output, not from `preferredMime()`, so such
a renderer still produces a perfectly usable bundle for Gade, gmd or a unit test — none of which have
any notion of `MIMEType` — and Goal 4 says that layer works without a kernel. The declared-mime
constraint binds in exactly one place, the kernel adapter, which declines to register it (§2.6). It
would be wrong for the host-neutral layer to lose a working renderer over a kernel-layer rule.

**Why the shape check lives here at all.** The parse failure in §2.6's `toMimeType` happens in the
kernel-dependent layer, but `describe()` lives on `RendererRegistry` in the host-neutral layer and has
no channel back to it. A shape check needs no jjava, keeps the fact where `describe()` can report it,
and gives host-neutral consumers the same diagnostic. `MIMEType.parse` remains the kernel-side
authority for the narrower case of a shape-valid string jjava still rejects.

**`describe()` is a thin formatter over the published records.** It iterates `active()` and
`skipped()`; it does not retain a second skipped set or reconstruct availability, type, mime, or
precedence facts from its human-readable output. `ActiveRenderer.supportedTypes` and `shadowedBy`
provide the renderer/type and shadow annotations, while `SkippedRenderer.reason` provides the skipped
diagnostic. The records are populated once per discovery pass, before either formatter runs.

**Provider identity and display labels are deliberately separate.** Both record kinds carry
`providerClassName`, and each `shadowedBy` value is also a fully qualified provider class name; these
are stable identity values, never the `rendererName()` labels shown in a report. A formatter builds a
lookup from the current `active()` records (`providerClassName → renderer.rendererName()`) and prints
that display label for an owner when present. If the provider is no longer active — possible for a
historical kernel ownership entry after `reload()` — it prints the stored fully qualified class name
instead. To make rows distinguishable too, if two active or skipped records share `rendererName()`,
both formatters append ` [<providerClassName>]` to those rows' display labels; for skipped records
the label is the safely captured `SkippedRenderer.rendererName`, not a call into the unavailable
provider. Unique labels keep the compact form shown in the samples. Thus the sample's `CoreRenderer`
is a current display-label lookup, while a dropped owner, failed provider, or colliding skipped
provider is still diagnosed accurately rather than silently disappearing.

Its top-level categories mirror the accessors — `active:` and `skipped:`, and nothing else.
Everything narrower is an **annotation on a row**, never a peer category:

| Annotation | Produced by | Derived from | Renderer is still |
|---|---|---|---|
| `unsupported-mime` | `RendererRegistry` | `mimeUsable == false` | active, renders host-neutrally |
| `shadowed for <Type> by <Other>` | `RendererRegistry` | its own precedence decision | active, still owns its other types |
| `kernel#…@…: registered` / `partially registered` / `NOT registered` | `MatrixJupyterExtension` (§2.6.2) | the `attached` map | — |

Making any of these a peer of `active:` would read as exclusion and contradict both `active()` and the
host-neutral guarantee above. A shadowed renderer is the clearest case: it was discovered, is
available, and merely lost *one* type — it may well still be the renderer for others, as `AcmeTables`
is for `com.acme.Report` in the sample. The annotations still have to stay distinct from `skipped`,
because a renderer buried in the missing-module bucket is indistinguishable from one whose module
simply is not installed.

**Only the first two are the registry's to print.** Whether a renderer was registered with a kernel
lives in `MatrixJupyterExtension.attached` (§2.6.1) and is published nowhere — by the same argument
that moved the shape check into the registry, `describe()` has no channel to it. The kernel status is
therefore printed by a second, kernel-layer diagnostic (§2.6.2) that formats the registry's structured
records with its own annotations, rather than by a callback registered into the host-neutral layer,
which would need to hold the extension weakly to avoid reintroducing exactly the kernel-retention
problem §2.6.1 solved.

**The registry logs the flag at discovery** — `log.warn` naming the renderer and the offending string,
through `core.util.Logger` — so a shape-invalid renderer is never unregistered without a trace. Each
layer owns exactly one message: the registry's for a shape-invalid string, the adapter's for a
shape-valid string jjava rejects. The adapter's `!mimeUsable` early return (§2.6) is therefore silent
because the registry already owns that message, not because it avoids repetition — `refresh()` calls
`reload()`, `reload()` re-runs discovery, and discovery is where the registry logs, so a misconfigured
renderer warns once per `refresh()` regardless. That is acceptable and deliberately not deduplicated:
`refresh()` is a rare, user-initiated action, and a warning that recurs while a real misconfiguration
persists is more useful than a "already warned" set whose invalidation rule (clear on loader change,
but not on reload) would be one more thing to get subtly wrong.

**Dispatch.** `ConcurrentHashMap<Class<?>, MatrixRenderer>` cache; a miss walks the value's superclass
and interface chain, so `GgChart` subclasses and `pict` `Chart<T>` subtypes resolve without being
enumerated. This mirrors jjava, which walks superclasses *and* all interfaces via
`InheritanceIterator` (§10.1) — so subclass rendering behaves identically in the notebook and in a
host-neutral consumer, and no per-subtype registration is needed. `MimeBundle render(Object value, RenderOptions options = RenderOptions.defaults)` returns
**`null`** for an unhandled type — not an empty bundle — so a host falls through to its own default.

### 2.4 `RenderOptions`

| Property | Type | Default | Meaning |
|---|---|---|---|
| `maxRows` | `Integer` | `50` | Max table rows; `null` means no limit |
| `maxColumns` | `Integer` | `50` | Max table columns; `null` means no limit |
| `fromHead` | `boolean` | `true` | Truncate rows from the head (`true`) or tail (`false`) |
| `attr` | `Map<String,String>` | `[:]` | Passed to `Matrix.toHtml(attr, …)` (`id`, `class`, `align`, `caption`) |
| `width` | `int` | `800` | Chart width in px, where applied (see below) |
| `height` | `int` | `600` | Chart height in px, where applied (see below) |

**Thread semantics.** `RenderOptions` instances are immutable: the `attr` map is defensively copied
into an unmodifiable map on construction. The global default field `RenderOptions.defaults` is
`volatile`, because it is read on the kernel execution thread and may be assigned from another.

**Truncation is never silent.** A truncated table renders
`<caption>showing 50 of 12,483 rows, 50 of 312 columns</caption>` (only the clauses that apply).
Column truncation matters as much as row truncation: `maxRows` alone still lets a 500-column matrix
serialize 500 `<th>` and 25,000 `<td>` into the notebook file.

**Mechanism.** Rows truncate through `toHtml(attr, numRows, fromHead)` (`Matrix.groovy:3652`), which
takes no column subset. Columns therefore truncate *first*, by building a sub-Matrix with
`selectColumns(List<String>)` (`Matrix.groovy:3065`) over the **first `maxColumns` column names in
declaration order** — `fromHead` is a row-direction flag only and does not apply to columns. The
resulting Matrix is then rendered with the row limit.

`<caption>` counts report pre-truncation totals, so `rowCount()` and `columnCount()` must be captured
**before** `selectColumns` — the sub-Matrix no longer knows the original column count.

**Caption precedence.** `caption` is both matrix-jupyter's truncation channel and a user-settable
`attr` key (§7.9). When the user sets `attr.caption` on a table that also truncates, the two are
**concatenated**, user text first: `My table — showing 50 of 12,483 rows`. Neither dropping the user's
caption nor dropping the truncation notice is acceptable; the latter would reintroduce the silent
truncation this section exists to prevent.

**Attribute values are not escaped.** `Matrix.toHtml` interpolates `attr` values raw into the
`<table>` tag (`Matrix.groovy:3730`); only cell and column-name content is escaped
(`escapeHtml`, `Matrix.groovy:4474`). The user supplies `attr` themselves so exploitability is low,
but the README must not claim attributes are sanitised. **`caption` is the exception:** §7.9's
matrix-core change escapes it with `escapeHtml` unconditionally, inside `toHtml`. That must not be
read as matrix-jupyter's job — under the concatenation rule above the caption is partly user text,
and the escaping belongs where the element is written.

**Chart size.** `width`/`height` are applied to charm (`Chart.render(int, int)`,
`Chart.groovy:375`) and pict (`CharmBridge.renderSvg(chart, w, h)`, `CharmBridge.groovy:54`).
They are **not** applied to `GgChart`. `GgChart` does have public mutable `width`/`height`
(`GgChart.groovy:55,58`) — this is a deliberate choice, not a facade limitation: setting them would
mutate the user's chart object, which §5's non-mutation rule forbids. Their defaults are 800×600,
identical to `RenderOptions`, so the visible behaviour matches unless the user has overridden
`RenderOptions` globally. Documented in the README.

### 2.5 Renderers

| Renderer | Types | Output |
|---|---|---|
| `CoreRenderer` | `Matrix`, `Grid`, `Row`, `Column`, `Summary`, `Structure` | `text/html` via `Matrix.toHtml` after the conversion below; `text/plain` as below |
| `CharmRenderer` | `charm.Chart`, `PlotGrid` | `render(width, height)` → `Svg` → `image/svg+xml` |
| `GgRenderer` | `gg.GgChart` | `render()` → `Svg` → `image/svg+xml` |
| `PictRenderer` | `pict.Chart` | `CharmBridge.renderSvg(chart, width, height)` → `image/svg+xml` |

No type gets its own HTML writer — each is converted to a `Matrix` and rendered through
`Matrix.toHtml`, so this repo does not acquire a second table serializer and escaping stays in one
place:

| Type | Conversion to `Matrix` | `text/plain` |
|---|---|---|
| `Matrix` | none | `content(...)` |
| `Row` | one-row `Matrix` using the row's own `columnNames`, copied from its parent (`Row.groovy:27,35`) | `content(...)` |
| `Column` | one-column `Matrix` whose single column name is the column's `name` property (`Column.groovy:25`) | `content(...)` |
| `Grid` | `Matrix.builder().data(grid).build()`; **`Grid` carries no headers** (`Grid.groovy:23`), so columns get `MatrixBuilder`'s default names `c1…cN` (`MatrixBuilder.groovy:764`) | `content(...)` |
| `Summary` | `getData()` → `Map<String, Map<String,?>>`; first column `variable`, then one column per stat key in encounter order across all variables, blank where absent | its own `toString()` |
| `Structure` | `getData()` → ragged `Map<String, List<String>>`; two columns, `variable` and the comma-joined descriptor list, insertion order preserved | its own `toString()` |

`Summary` and `Structure` are classes holding a map, not maps
(`Summary.groovy:11`, `Structure.groovy:9`), and `Summary` overrides `getProperty(String)` to return
`data[key]` (`Summary.groovy:25`) — so `summary.data` yields `null` under dynamic Groovy. Both the
conversions and the §6 test 2 cases (dynamically compiled by default per AGENTS.md) must call
`getData()`.

Every SVG passes through the namespacing step of §5 before serialization.

### 2.6 `MatrixJupyterExtension`

Implements `org.dflib.jjava.jupyter.Extension` (§10.1 for the verified signatures). On
`install(BaseKernel kernel)`, for each active `MatrixRenderer` and each of its `supportedTypes()`:

```groovy
/** The one place the two key spaces meet: MimeBundle's String keys ↔ jjava's MIMEType constants. */
private static final Map<String, MIMEType> MIME_TYPES = [
    'text/html'    : MIMEType.TEXT_HTML,
    'image/svg+xml': MIMEType.IMAGE_SVG,
    'text/plain'   : MIMEType.TEXT_PLAIN,
].asImmutable()

/** Known constant, else parsed; null (with a warning) if jjava cannot use what was declared. */
private static MIMEType toMimeType(ActiveRenderer source) {
  // already flagged AND logged by the registry at discovery (§2.3.2); this layer owns the
  // jjava-rejected message only, so nothing is logged here
  if (!source.mimeUsable) { return null }
  MIMEType known = MIME_TYPES[source.preferredMime]
  if (known != null) { return known }
  try {
    MIMEType.parse(source.preferredMime)
  } catch (Exception e) {                                // parse failure, and anything else
    log.warn("Renderer ${source.renderer.rendererName()} declared unusable preferredMime " +
        "'${source.preferredMime}'; not registering with the kernel", e)
    null
  }
}

// called by the ownership-guarded install/refresh loop in §2.6.1.
/** True only when this call registered a render function for type. */
private static <T> boolean registerType(Renderer renderer, Class<T> type, ActiveRenderer source) {
  MIMEType preferred = toMimeType(source)
  if (preferred == null) { return false }  // skip this renderer, never the whole install
  String preferredMime = source.preferredMime      // normalized at discovery, not re-derived here
  renderer.createRegistration(type)
      .preferring(preferred)
      .supporting(MIMEType.TEXT_PLAIN)
      .register { T value, RenderContext ctx ->
        MimeBundle bundle = null
        Closure<MimeBundle> once = {
          bundle != null ? bundle : (bundle = RendererRegistry.instance.render(value))
        }
        // rich mime: emit nothing rather than a non-SVG string into an image/svg+xml slot
        ctx.renderIfRequested(preferred, { MIMEType mime, DisplayData out ->
          Object data = once()?.get(preferredMime)
          if (data != null) { out.putData(mime, data) } else { logMissing(value) }
        } as BiConsumer<MIMEType, DisplayData>)
        ctx.renderIfRequested(MIMEType.TEXT_PLAIN, {
          Object plain = once()?.get('text/plain')
          plain != null ? plain : note(value)
        } as Supplier<Object>)
      }
  true
}
```

**The helper's own type parameter is required.** `createRegistration` is
`<T> RenderRegistration<T> createRegistration(Class<T>)` and its `register` takes
`RenderFunction<T>` (§10.1), while `supportedTypes()` yields `Class<?>`. Iterating the set inline
binds `T` to a wildcard capture that cannot be named, so no `RenderFunction` can be supplied — this
fails in Java too, not only under `@CompileStatic`. A generic helper called from the loop captures
the wildcard into a real type variable. Same class of statically-compiled-generics snag as the
explicit `ConcurrentHashMap` generic arguments in §2.6.1, and for the same reason worth showing:
this sketch will be copied.

**`bundle != null`, not `bundle ?:`.** `MimeBundle` extends `LinkedHashMap` (§2.1), so Groovy truth
on it is `!isEmpty()`, not a null test. An elvis would work today only because §2.1's always-carries-
`text/plain` invariant makes an empty bundle unreachable — resting the memo's correctness on an
invariant declared in another section for another purpose. If a bundle ever becomes legitimately
empty, the elvis silently re-renders on every mime. This is precisely the Groovy-truth trap AGENTS.md
calls out.

**The preferred mime drives both the request and the lookup.** Hardcoding `TEXT_HTML` here would
break every chart: the registration would advertise `image/svg+xml`, the frontend would request it,
no branch would answer, and `'text/html'` would be absent from a chart bundle — so every chart cell
would fall through to `toString()`, the precise failure this module exists to remove. `preferred` and
`preferredMime` are therefore the same fact in two type systems, and `MIME_TYPES` is the single place
they are related, so the `MimeBundle` string keys and jjava's `MIMEType` constants cannot drift apart.
§6 test 7 asserts the **SVG payload** of a charm chart's `DisplayData`, not merely that a bundle is
non-empty — a weaker assertion would pass on exactly this bug.

**Both closure literals need an explicit SAM coercion.** `renderIfRequested` has three overloads
(§10.1) and a Groovy closure literal's static type is `groovy.lang.Closure`, coercible to all of them
— so under `@CompileStatic` neither call site has anything to discriminate on: the two-parameter rich
branch is ambiguous, and the plain branch is ambiguous between `Supplier` and `Function` irrespective
of arity. Hence `as BiConsumer<MIMEType, DisplayData>` and `as Supplier<Object>`. This is the third
"redundant-looking but required" `@CompileStatic` construct in this spec, alongside the explicit
`ConcurrentHashMap` generic arguments and the generic `registerType` helper — all three exist because
the sketches are written to be copied verbatim.

**An unknown `preferredMime()` costs one renderer, never the install.** `MIME_TYPES` holds only the
three mimes this jar produces, so a third-party renderer — §2.2 invites them, and §9 anticipates a
Vega-Lite emitter — could declare `application/vnd.vegalite.v5+json` and hand `null` to
`.preferring(...)`, taking down every other registration in the same loop, `CoreRenderer` included.
`toMimeType` therefore falls back to `MIMEType.parse` (§10.1) so arbitrary mimes work, and on failure
logs and returns `null` so only that renderer goes unregistered. The catch is `Exception`, not
`MIMETypeParseException`, as a backstop — but the null/blank case is handled a layer earlier (§2.3.2),
because `parse(null)` throws `NullPointerException` before any of this runs.

**`registerType` takes the registry's `ActiveRenderer` record, never the raw SPI method.** It reads
`source.preferredMime` — the value the registry normalized and that `describe()` reports — instead of
re-calling `preferredMime()`, which would still return the raw `null` and put the two layers into
contradiction: `describe()` announcing the renderer as active on `text/html` while the notebook
rendered nothing for it. Skipping registration here leaves the renderer working for every
host-neutral consumer (§2.3.2).

**Null checks are explicit, on both the bundle and the payload.** `once()?.get(mime) ?: fallback`
would repeat one paragraph up the very trap it warns about: `?:` on a `String` tests
`!isEmpty()`, so an empty payload — plausible for `content(...)` on a zero-row Matrix, which §6
test 2 covers — would silently emit `Matrix@3f2a1b` in place of the empty rendering.

**The rich-mime branch emits nothing rather than a fallback string.** A `text/plain` note dropped into
an `image/svg+xml` slot is not degraded output, it is malformed SVG the frontend will try to render.
So the rich branch uses `renderIfRequested`'s `BiConsumer<MIMEType, DisplayData>` overload (§10.1) and
simply declines to `putData`; the annotated fallback goes into `text/plain`, where a string is always
valid.

**Why any of this can fire.** `RendererRegistry.render` returns `null` for an unhandled type (§2.3),
and §2.6.1 opens a window where that happens for an *already registered* type: `refresh()` re-runs
discovery, so a renderer can be shadowed by a duplicate (§2.2 precedence) or disappear when the
loader set changes, while jjava still holds the earlier registration. Unguarded, the lookup throws
inside the `Supplier` — outside `RendererRegistry.render`'s own try/catch — and kills the cell render
§4 promises can never be killed.

**And it fires visibly.** `logMissing`/`note` log through `se.alipsa.matrix.core.util.Logger` and
produce the same annotated text §4 uses for a failing renderer — `value.toString()` plus a one-line
note, here naming the type whose registration outlived its renderer and pointing at
`MatrixJupyterExtension.refresh()`. A silent degradation would leave the user with no way to see a
diagnosable condition; both degradation paths must look the same.

There is exactly **one** path to the plain-text fallback: `AbstractRenderer`'s, reached through the
bundle (§2.2). The adapter never computes plain text itself — an independent `plainFor(value)` would
be a second implementation of a rule that already has an owner (AGENTS.md DRY) and could drift from
what `MimeBundle.svg(svgXml, plainFallback)` already carries.

**Memoization is a local, not a map.** `RenderContext`'s verified API is `renderIfRequested` /
`wantsDataRenderedAs` / `getParams` (§10.1) — there is no slot to attach a cached bundle to, so
"memoize per `RenderContext`" would in practice mean an external `Map<RenderContext, MimeBundle>`,
which would then need weak keys and eviction for exactly the reason `attached` does, or it would
retain one bundle per rendered cell for the kernel's life. None of that is necessary:
`RenderFunction.render(T, RenderContext)` is invoked once per value, so a local captured by both
closures gives per-invocation memoization with no lifetime question at all.

Laziness still holds — `renderIfRequested` runs neither closure unless that mime is wanted, so an SVG
is never serialized for a `text/plain`-only request — and the bundle is computed at most once when
both mimes are requested, which is the common case. The adapter keeps its "stays dumb" property.

`uninstall(BaseKernel)` **is** overridden (§2.6.1). No import injection (see Non-goals).

Declared in `META-INF/services/org.dflib.jjava.jupyter.Extension`. Target size 80–100 lines: this is
the file alpha churn will break, so it stays dumb.

#### 2.6.1 `refresh()` — registration must be re-run, not just discovery

`install(kernel)` registers the renderers active *at that instant*. A registry-only `reload()`
therefore does not fix the notebook: the registry gains `CharmRenderer`, but jjava was never told
`charm.Chart` is renderable, so the cell still prints `toString()`. The extension must close that
loop:

State must be **static** (a cell calls `refresh()` with no instance in hand, and jjava creates a fresh
`Extension` instance per `installExtensions` call — `ServiceLoader.stream().map(Provider::get)`,
§10.1) but **keyed per kernel**, never global. The inner map records the owning renderer's provider
class name for each jjava registration, not merely that a type was registered. A name is the ownership
identity §2.6.2 reports, and does not retain renderer instances or their classloaders across reloads:

```groovy
class MatrixJupyterExtension implements Extension {
  /** Attached kernels → types registered with each and their owning provider class names. Weak keys: see below. */
  private static final Map<BaseKernel, Map<Class<?>, String>> attached =
      Collections.synchronizedMap(new WeakHashMap<>())

  void install(BaseKernel kernel) {
    Map<Class<?>, String> registered =
        attached.computeIfAbsent(kernel) { new ConcurrentHashMap<Class<?>, String>() }
    registerNewTypes(kernel, registered)
  }

  @Override
  void uninstall(BaseKernel kernel) { attached.remove(kernel) }

  /** Reload discovery and register newly active types with every attached kernel. */
  static void refresh() {
    RendererRegistry.instance.reload()
    Map<BaseKernel, Map<Class<?>, String>> snapshot
    synchronized (attached) { snapshot = new LinkedHashMap<>(attached) }
    snapshot.each { kernel, registered -> registerNewTypes(kernel, registered) }
  }

  // registerNewTypes iterates active renderers and their captured supportedTypes(), calling this helper.
  private static <T> void registerTypeIfAbsent(Renderer renderer, Class<T> type,
      ActiveRenderer source, Map<Class<?>, String> registered) {
    String providerClassName = source.providerClassName
    registered.computeIfAbsent(type, { Class<?> ignored ->
      registerType(renderer, type, source) ? providerClassName : null
    } as Function<Class<?>, String>)
  }
}
```

**Three details in that sketch are load-bearing, not incidental.**

*Iterate a snapshot taken under the lock, never `attached` itself.* `Collections.synchronizedMap`
synchronizes individual operations but explicitly **not** iteration of its views — the javadoc puts
that on the caller — and §2.6.1's own premise is that `refresh()` runs on the cell execution thread
while `install` runs on the kernel startup path. Independently of threading, a `WeakHashMap` can
throw `ConcurrentModificationException` from a *single* thread, because GC clearing an entry is a
structural modification that may land mid-iteration. Copying into a plain `LinkedHashMap` also
promotes the keys to strong references for the duration of the pass, which iterating an `entrySet()`
view would not.

*`computeIfAbsent` needs no extra locking:* `Collections.synchronizedMap` overrides it, so the
lookup-and-insert is atomic under the same monitor the snapshot uses.

*The explicit `ConcurrentHashMap<Class<?>, String>` generic arguments are required, not
decoration.* Groovy's static type checker infers generic constructor arguments far less aggressively
than Java's — particularly through the closure-to-SAM coercion `computeIfAbsent` needs here — so a
bare constructor can land on `Object` and be rejected by the `Map<Class<?>, String>`
assignment. This is exactly the class of "redundant-looking but required" `@CompileStatic` construct
AGENTS.md calls out, and production code in this repo is statically compiled project-wide.

A single global type-to-provider map would be a correctness bug, not just untidiness: the second
`install(BaseKernel)` in a JVM — a second kernel, or simply the next test method in §6 test 7 — would find
every type already recorded and register **nothing**, so that kernel would render no `Matrix` at all.
For the same reason a single `installedKernel` field is wrong: it silently orphans the first kernel
and `refresh()` would only ever reach the last one. Keying by kernel and iterating all of them fixes
both. `uninstall` is therefore **overridden** (§2.6 says so; an earlier revision said it was not) to drop the
entry; without that, `attached` retains dead kernels for the JVM's life.

`registerNewTypes` uses `ConcurrentHashMap.computeIfAbsent` as the sole ownership guard: its mapping
closure invokes `registerType` once for an unowned type and returns the provider class name only on
success. A `null` result creates no mapping, so an unusable mime does not create a false ownership
entry. This makes check, registration and ownership recording one atomic per-type operation without
an external monitor whose discipline a future call site could accidentally bypass. The guard is
**required**, not defensive: `Renderer.register` stores into a `Map<Class,
List<RenderFunctionProps>>` via `Map.compute`, appending to the list (§10.1). Re-registering an
already-registered type therefore *appends a duplicate* rather than replacing it, so naive
re-registration on every `refresh()` accumulates render functions for the life of the kernel.
Recording the provider also distinguishes a genuine re-registration from a later renderer's losing
shadow claim without pinning a renderer instance or its defining classloader. Post-install
registration is otherwise safe — `renderFunctions` is a plain mutable map with no init-time freeze.

**Why weak keys as well as `uninstall`.** `Extension.uninstall` is confirmed to have a caller —
`BaseKernel.onShutdown(boolean)` → `uninstallExtension()` → `uninstall(this)` per extension, errors
logged and ignored (§10.1) — so the override does fire. But it fires *only at kernel shutdown*, and
`attached` otherwise holds a strong reference to every kernel it has seen. In §6 test 7, which
installs two kernels in one JVM, a kernel dropped without `onShutdown` would be retained for the
test JVM's life. Weak keys make that a non-question; the override remains for prompt cleanup. The
value map does not reference its key, so there is no cycle to defeat the weakness. `WeakHashMap`
already uses identity-ish semantics here because `BaseKernel` overrides neither `equals` nor
`hashCode`.

#### 2.6.2 `MatrixJupyterExtension.describe()` — the notebook-facing diagnostic

The kernel status the §2.3.2 table's third row describes is known only here, in `attached`
(§2.6.1). So the kernel layer owns a second diagnostic that composes on top of the registry's:

```groovy
/** The same structured registry report, annotated with per-kernel registration status. */
static String describe() { … }
```

This is **not** `RendererRegistry.instance.describe()` followed by insertion of `kernel:` lines into
that formatted string. `RendererRegistry.describe()` is a thin host-neutral formatter over
`active()` and `skipped()`. `MatrixJupyterExtension.describe()` independently formats those same
records and supplies one additional annotation source: a snapshot of `attached`. Thus changes to
column spacing, arrows, indentation, or the remedy block in either formatter cannot change the
other formatter's interpretation of registry state.

The extension takes one `attached` snapshot under its monitor, sorts the entries by unsigned
`System.identityHashCode(kernel)`, and only then assigns each live key the diagnostic label
`kernel#<ordinal>@<identity-hash-hex>`. The ordinal makes labels distinct even in the unlikely event
of an identity-hash collision; sorting makes a label stable across calls while the set of live kernels
does not change. It then formats every active renderer from that snapshot. A renderer is `registered`
for a kernel only when **all** of that `ActiveRenderer`'s `supportedTypes` are owned by that renderer
in the kernel's map. It is `partially registered` when it owns at least one but not all types, and
`NOT registered` when it owns none. A partial line names the owned types and the other owner for each
unowned type. Ownership is a direct comparison between the recorded provider class name and
`source.providerClassName`, so `reload()` may construct new renderer instances without changing
status or retaining their predecessors. Owner names in those partial lines use §2.3.2's current
display-label lookup and fall back to the stored fully qualified class name when the owner is no
longer active. Skipped renderers have no kernel annotation. This
definition makes a renderer that cannot be registered due to an unusable mime honestly `NOT
registered`, and makes a shadowed renderer's missing ownership visible rather than attributing
another renderer's registration to it.

```
matrix-jupyter renderers
  active:  CoreRenderer  → text/html      (Matrix, Grid, Row, Column, Summary, Structure)
             kernel#1@4a7d3c: registered
             kernel#2@12f40c: registered
           AcmeRenderer [com.acme.WidgetRenderer] → "chart"  (com.acme.Widget)
             unsupported-mime — "chart" is not a type/subtype string
             kernel#1@4a7d3c: NOT registered
             kernel#2@12f40c: NOT registered
           AcmeTables    → text/html      (com.acme.Report, Matrix)
             shadowed for Matrix by CoreRenderer
             kernel#1@4a7d3c: partially registered — owns com.acme.Report; Matrix owned by CoreRenderer
             kernel#2@12f40c: partially registered — owns com.acme.Report; Matrix owned by CoreRenderer
  skipped: CharmRenderer → image/svg+xml  — se.alipsa.matrix.charm.Chart not on classpath
           AcmeRenderer [com.acme.BrokenRenderer] → ? — com.acme.OptionalWidget missing
Grabbed a module after first render?
  in a notebook:  MatrixJupyterExtension.refresh()      // reloads AND re-registers with the kernel
  otherwise:      RendererRegistry.instance.reload()
```

There is deliberately one labeled annotation line **per attached kernel**. Two kernels can have
different type-to-provider ownership maps after a late `@Grab` and registry reload but before extension
`refresh()` reaches every attached kernel, so collapsing them to a single unlabeled
`kernel: registered` line would be false or ambiguous. These labels are diagnostic-only
and distinguish the live kernels consistently while that set is unchanged; tests assert stability and
presence by matching the labels, not a particular ordinal or hash value.

**This is the one a notebook user should call**, and the README leads with it; `RendererRegistry`'s is
for Gade, gmd, tests and anything else with no kernel. The alternative — a status callback registered
into the registry so one `describe()` serves both — was rejected: it puts a kernel-shaped hook into
the layer Goal 4 keeps kernel-free, and the hook would have to hold the extension weakly or
reintroduce the kernel-retention problem `attached`'s `WeakHashMap` exists to prevent. Composing
outward costs one extra method and no new host-neutral API.

With no kernel attached, `attached` is empty and the extension formatter produces the registry
formatter's exact output, with no `kernel#…@…:` lines — the honest rendering of "this fact does not
exist here".

### 2.7 Integration requirement on `groovy-jupyter`

Extension discovery is entirely the kernel's choice, and both jjava entry points are `protected`
(§10.1). `installDefaultExtensions()` scans `getClassLoader()` — the kernel's own loader — **at
startup only**. A `@Grab`-ed matrix-jupyter is therefore installed *only if* the Groovy kernel calls
`installExtensions(sessionLoader)` after a grab.

This is a one-line requirement to raise with the `groovy-jupyter` authors, and Goal 1 depends on it:

> After a `@Grab`/`%maven` that adds jars to the session `GroovyClassLoader`, call
> `installExtensions(sessionClassLoader)` so newly grabbed Extension jars are installed.

Until that is confirmed, the **static** deployment (jar on the kernel launch classpath, §2.3.1) is
the only guaranteed path, and the README documents it first. This is recorded as a risk in §8 rather
than assumed away.

## 3. Data flow

```
cell value
  → jjava Renderer (type dispatch + mime negotiation)
  → MatrixJupyterExtension's RenderFunction
  → RendererRegistry.render(value, RenderOptions.defaults)
  → MatrixRenderer.render(...)
  → [charts only] SvgWriter.toXml(svg, "mjx<n>-")
  → DisplayData → display_data
```

## 4. Error handling

- `RendererRegistry.render` wraps each renderer call in `try`/`catch`. A failure is logged through
  `se.alipsa.matrix.core.util.Logger` (available unconditionally per D8) and degraded to a
  `text/plain` bundle containing `value.toString()` plus a one-line note naming the failing renderer.
  A broken renderer must never blank a cell or kill the kernel.
- `available()` probes catch `Throwable` (§2.2).
- Unhandled types return `null`.
- `maxRows`/`maxColumns` guard against pathological table sizes.

## 5. Prerequisite: SVG namespacing in gsvg and charm

### 5.1 The bug, in two parts

JupyterLab inlines a trusted `image/svg+xml` output into the notebook's single DOM. Two charts in one
notebook then collide in **two** distinct ways:

**(a) Element ids.** Charm emits fixed ids — `axes`, `grid`, `legend`, `panel-clip-0-0`
(`CharmRenderer.groovy:320-321`, `AxisRenderer.groovy`, `LegendRenderer.groovy:111`). Chart B's
`clip-path="url(#panel-clip-0-0)"` resolves against chart A's clip rect, so chart B renders clipped to
the wrong geometry.

**(b) CSS.** `AnimationSpec.toCss()` emits `@keyframes charm-fade-in` and the selector
`.charm-data-layer *` (`AnimationSpec.groovy:37-52`), injected as `<style>` CDATA by
`CharmRenderer.injectAnimationStyle` / `injectStylesheet` (`CharmRenderer.groovy:362-381`). Chart B's
`@keyframes charm-fade-in` overrides chart A's, and either chart's `.charm-*` rules apply to both.
**Prefixing ids does not touch keyframe names or class selectors**, so an id-only fix leaves this
collision class entirely unsolved.

### 5.2 Where the fix belongs

`PlotGridRenderer` already solves (a) when composing subplots into one grid document — four private
methods, `rewriteDomIds` (`PlotGridRenderer.groovy:207`), `collectDomDescendants` (`:262`),
`rewriteUrlRefs` (`:277`), `rewriteHrefRef` (`:300`), covering `id`, `url(#…)`, `href="#…"`,
`xlink:href="#…"`. They are `private`, so matrix-jupyter, gmd and Gade cannot reach them.
Re-implementing them here would be a third copy and violates the DRY rule in AGENTS.md. It does not
solve (b) either — grid composition has the same keyframe collision today.

**Chosen approach: charm anchors, gsvg namespaces.**

*matrix-charts (0.5.1-SNAPSHOT):*

1. `CharmRenderer` gives the root `<svg>` an id (`charm-root`); it carries none today, so there is
   nothing for a scope selector to anchor to. **The id is added only when a `<style>` block is
   actually injected** (`chart.animation?.active` or a non-blank `chart.stylesheet`). Adding it
   unconditionally would change the output of *every* charm SVG — and, through the facades, every
   matrix-ggplot and matrix-pict SVG — for charts that have no CSS and therefore nothing to scope.
   Conditioning it keeps the diff to exactly the charts that need it.
2. **Only charm-generated rules are scoped.** `AnimationSpec.toCss()` has a grammar charm owns, so
   emitting `#charm-root .charm-data-layer * { … }` is trivial and exact. `injectStylesheet` injects
   `chart.stylesheet` **verbatim** (`CharmRenderer.groovy:372-379`) — arbitrary user CSS. Scoping that
   correctly means splitting selector lists, handling `@media` blocks, comments, and commas inside
   `:is()`/`:not()` — the general CSS parsing this approach exists to avoid. **Decision:** charm
   scopes only its own generated rules; user stylesheets pass through verbatim.
   **This is not the same as leaving the user without a remedy.** Step 1 adds `#charm-root` whenever a
   stylesheet is present precisely so the user has an anchor: `#charm-root` is a *declared id*, so
   gsvg rewrites its occurrences inside the user's `<style>` text along with every other id
   reference. A user who writes `#charm-root .my-rule { … }` therefore gets correct per-chart
   isolation for free, in the notebook and in any other multi-chart DOM. Rules the user writes
   unanchored stay global and collide — documented, with the one-line remedy, in both charm's and
   matrix-jupyter's README. gsvg's `@keyframes` renaming (below) applies to user keyframes regardless,
   since that grammar *is* bounded.
3. `PlotGridRenderer`'s four private methods are deleted; it calls `SvgIdRewriter.prefixIds` instead.
4. **No new `ChartToSvg` overloads.** `ChartToSvg` exposes twelve `export` methods — File/OutputStream/
   Writer across `Svg`, `CharmChart`, `PlotGrid` and `Object` (`ChartToSvg.groovy:34-254`). Adding a
   single `prefix` variant would leave `export(chart, File)` — the likeliest path for the report
   consumers this change affects — without namespacing and break an otherwise uniform family;
   threading it through means twelve more overloads for a two-line call. `SvgWriter.toXml(svg, prefix)`
   *is* the capability. Consumers needing namespacing render then serialize:
   `SvgWriter.toXml(chart.render(), 'r1-')`. Documented as a recipe in matrix-charts' README.
5. **Regression scope:** the full matrix-charts suite plus matrix-ggplot and matrix-pict (both render
   through Charm), not just the grid tests — any test asserting root-element attributes on a
   CSS-carrying chart changes. The output change is recorded in matrix-charts' `release.md`.

*gsvg (1.2.0):*

```groovy
// non-mutating: clones the dom4j document, rewrites the copy, serializes
static String SvgWriter.toXml(Svg svg, String prefix)
static String SvgWriter.toXmlPretty(Svg svg, String prefix)

// in-place: for callers that own the tree (PlotGridRenderer)
class SvgIdRewriter {
  static <T extends SvgElement> T prefixIds(T element, String prefix)
}
```

`prefixIds` = the promoted `PlotGridRenderer` logic, plus two additions:
- **`#id` references inside `<style>` text/CDATA.** Restricted to the exact set of ids the document
  declares, so `#fff` colour literals are untouched by construction. Matching must be
  **longest-id-first or boundary-aware**, or prefixing `#legend` corrupts `#legend-title`.
- **`@keyframes <name>` blocks and matching `animation-name:` / `animation:` shorthand references**,
  renamed with the same prefix. This is a closed, bounded grammar — not a general CSS parser.

Because the scope selector is itself an id reference, step 2 above means the scoping fixes itself:
`#charm-root .charm-data-layer *` becomes `#mjx1-charm-root .charm-data-layer *` for free.

The two gsvg entry points differ only in mutation: the `SvgWriter` overloads copy first (a notebook
must never mutate the user's chart object), `SvgIdRewriter` mutates in place (a grid renderer owns its
subplots).

### 5.3 Consequences

- matrix-charts bumps `v_gsvg` to `1.2.0` in `libs.versions.toml`.
- Grid output gains a fix for collision class (b), which it has today.
- `release.md` entries in matrix-charts (root id on CSS-carrying charts; the
  `SvgWriter.toXml(chart.render(), 'r1-')` namespacing recipe — **no new `ChartToSvg` overload**, see
  §5.2 step 4) and matrix-core (§7.9's `attr` contract change).
- matrix-jupyter owns no rewriter. Chart renderers call
  `SvgWriter.toXml(svg, "mjx${counter.incrementAndGet()}-")`, where `counter` is a package-private
  `AtomicLong` tests can reset for determinism.
- **Release order:** **§6.1 spike** → gsvg 1.2.0 → matrix-charts (gsvg bump, root id, scoped rules,
  delegation) → matrix-jupyter 0.1.0.

  The spike comes first because its priority-1 question decides whether collision class (b) exists at
  all. If `<style>` does not survive inside inlined trusted SVG, then the `@keyframes` renaming in
  gsvg, the charm root id and the scoping in step 2 are all unnecessary, and this prerequisite shrinks
  to the id half. Collision class (a) is real either way, so the id work is not blocked by the spike
  and can start immediately if convenient — but the CSS half must not.

## 6. Testing

Fast tests run in the normal suite (`./gradlew :matrix-jupyter:test`).

1. `MimeBundle` — preference ordering; `text/plain` always present.
2. `CoreRenderer` — `<script>` in a cell value is escaped; row-truncation and column-truncation
   caption wording and counts; empty Matrix; `maxRows`/`maxColumns` boundaries and `null`;
   `Grid` wrapping produces `c1…cN` headers; `Row`/`Column` wrapping; `Summary` whose variables do not
   share stat keys (blank cells, stable column order); `Structure` with ragged descriptor lists. All
   `Summary`/`Structure` access through `getData()`.
3. Chart renderers — two consecutive renders produce disjoint id sets *and* disjoint keyframe names;
   every `url(#…)` in a bundle resolves within that bundle; `width`/`height` reach charm and pict; the
   source `Svg`/`GgChart` is not mutated.
4. `RendererRegistry` — superclass/interface dispatch; unknown type returns `null`; a throwing stub
   renderer degrades to `text/plain`; duplicate-type registration warns and keeps the first;
   **`RendererRegistry.describe()`** lists it under `active:` with a `shadowed for <Type> by <Other>`
   annotation — and, when it owns other types, still renders those (§2.3.2's
   annotation-not-category rule). Assert that `active()` exposes its immutable supported-type and
   shadow facts — including fully qualified `providerClassName` values in `shadowedBy` — and that
   `skipped()` exposes immutable unavailable-renderer records, provider class names and reasons.
   Two skipped providers with the same `rendererName()` must be disambiguated in `describe()` with
   their provider class names; `describe()` is asserted as their formatter. No `kernel#…@…:` line
   appears in this layer's output at all.
5. `available()` gating and `reload()` — a renderer whose probe class is absent is never registered
   (restricted classloader); after the class becomes visible, `reload()` activates it. Plus §2.3.2's
   validation, asserted on the `ActiveRenderer` record rather than on the SPI method: `null` and `''`
   normalize to `preferredMime == 'text/html'` with `mimeUsable == true`; a shape-invalid string keeps
   its `preferredMime` with `mimeUsable == false`, stays in `active()`, **still renders
   host-neutrally**, is logged at discovery, and appears in **`RendererRegistry.describe()`** under
   `active:` with an `unsupported-mime` annotation — not under `skipped`, and with no `kernel#…@…:` line,
   which this layer cannot produce (§2.3.2). Also: `active()` and its records are unmodifiable. A
   provider whose constructor/`Provider.get()` throws, and a direct SPI implementation whose
   `available()` throws, each log and appear under `skipped:` with the failure reason while
   `CoreRenderer` remains active; construction failure uses the provider class's simple name as its
   captured display label. Assert the construction-failure record has its provider class name and
   throwable message, `preferredMime == null`, `mimeUsable == null`, and a `→ ?` report column; every
   later provider remains discovered.
6. Loader resolution (§2.3.1), one case per rule, because the two rules differ:
   - **context-classloader probe** — a probe class visible only via the TCCL resolves, while a null
     TCCL fails cleanly with the missing-class diagnostic;
   - **union discovery** — a provider jar visible only via the TCCL *is* discovered, which a fallback
     would have missed because the own-loader pass always succeeds;
   - **dedupe** — a provider visible through *both* loaders is registered once. Without this,
     §2.2's duplicate-type precedence fires spuriously and `describe()` reports a renderer as
     shadowing itself.
   - **loader-list normalization** — a TCCL equal to the defining loader triggers one scan, and a
     null TCCL contributes no system-loader scan.
7. `MatrixJupyterExtension` — with `jjava-jupyter:1.0-a8` on `testImplementation`, install into a real
   `BaseKernel`'s `Renderer` and assert:
   - the `DisplayData` produced for a `Matrix` (HTML payload) and for a charm `Chart` — asserting the
     **`image/svg+xml` payload itself**, since a non-empty-bundle assertion passes even when the
     adapter registers every renderer as HTML (§2.6);
   - `renderIfRequested` skips SVG serialization when only `text/plain` is requested;
   - a subclass of a registered type dispatches (pins jjava's `InheritanceIterator` behaviour);
   - **`refresh()` after a late class becoming visible makes the notebook path render it** — the §2.6.1
     loop, which a registry-only `reload()` does not close;
   - **`refresh()` called twice registers each type once**, pinning the `Map.compute` append behaviour
     that would otherwise accumulate duplicate render functions;
   - **a renderer declaring an unusable `preferredMime()` goes unregistered alone**, leaving
     `CoreRenderer` and every other registration intact — one case for a shape-invalid string (flagged
     in the registry, §2.3.2) and one for a shape-valid string jjava still rejects (caught in the
     adapter);
   - **a renderer returning `null` from `preferredMime()` registers on `text/html`** and renders in
     the notebook, matching what `describe()` reports — the cross-layer agreement §2.3.2's
     `ActiveRenderer` record exists to guarantee;
   - **a renderer declaring a valid non-constant mime** (e.g. a Vega-Lite emitter) registers via
     `MIMEType.parse`;
   - **a registered type whose renderer has since disappeared** emits the annotated `text/plain` note
     and *no* rich-mime entry, rather than throwing inside the `Supplier` or writing a plain string
     into an SVG slot — the §2.6 guard path, reachable via `refresh()`, with the log line asserted;
   - **`MatrixJupyterExtension.describe()`** (§2.6.2) formats the same `active()`/`skipped()` records
     as the registry formatter and adds a labeled `kernel#…@…: registered`, `partially registered`,
     or `NOT registered` line for every attached kernel and active renderer; with no kernel attached it equals
     `RendererRegistry.describe()` exactly, emitting no `kernel#…@…:` lines. A renderer shadowed for
     one type is asserted `partially registered` when it owns another type, naming the winning owner
     for the shadowed type, even when another renderer has registered every one of its types: status
     is renderer ownership, not mere type presence. After a reload drops a historical owner, assert
     the partial line falls back from a display label to its stored fully qualified provider class
     name;
   - **two kernels installed in one JVM both render a `Matrix`**, and `uninstall` on the first leaves
     the second working. Install one before a late renderer becomes visible and the other after it;
     reload the registry before installing the second (but do not call extension `refresh()` yet),
     then assert `describe()` contains two distinct kernel labels and the late renderer is
     `NOT registered` for the first but registered for the second. Call `refresh()` and assert it
     brings the first kernel up to date too. Also assert two `describe()` calls with unchanged live
     kernels retain the same labels. This pins the intentionally per-kernel ownership maps and the
     multi-kernel diagnostic. A `refresh()`-twice test passes even with a global ownership map, so it
     cannot stand in for this one (§2.6.1).
8. **Host-neutral layer without jjava** — run `RendererRegistry.instance.render(matrix)` in a JVM
   where `org.dflib.jjava` is absent, asserting no `NoClassDefFoundError`. Goal 4's guarantee, and the
   only test that would catch a probe anchored on a jjava-implementing class (§2.2).
   **The loader must be child-first for `se.alipsa.matrix.jupyter.*` as well as hiding
   `org.dflib.jjava.*`** — it has to *define* `RendererRegistry` and `AbstractRenderer` itself. If it
   delegates them to the app loader, it reuses classes already linked in a JVM where jjava is present
   and the test passes vacuously, proving nothing. This is the single test standing between Goal 4 and
   the `NoClassDefFoundError` §2.2 guards against, so the loader setup is part of the requirement, not
   an implementation detail.
9. **Groovy 6 smoke check** (`./gradlew :matrix-jupyter:groovy6Smoke`, opt-in, not in `build`) — runs
   one `CoreRenderer` and one `CharmRenderer` render with an SDKMAN Groovy 6 runtime. It resolves the
   executable from `-Pgroovy6Executable`, then `GROOVY_6_HOME/bin/groovy`, then SDKMAN's
   `~/.sdkman/candidates/groovy/6.0.0-beta-3/bin/groovy`. §6 test 7 runs on Groovy 5 and cannot catch
   Groovy-5-compiled call-site/metaclass bytecode misbehaving on a Groovy 6 runtime, which is the
   actual compatibility risk (§8).

Rewriter semantics (nested defs, CSS `#id` selectors, longest-id-first, `@keyframes` renaming,
unknown-id references left alone, idempotence, source `Svg` not mutated) belong to **gsvg's** suite.

Per AGENTS.md, tests use JUnit Jupiter with `se.alipsa.groovy:groovier-junit`; SVG assertions use
direct object access or `SvgWriter.toXml()` — never `svg.toString()`.

### 6.1 Prerequisite spike: notebook trust modes

Before the phase-2 exit criterion may be claimed, run two charts and one wide Matrix in a single
notebook in JupyterLab, VS Code and Notebook 7, in both trusted and untrusted state, and record what
survives. Known from JupyterLab's `sanitizer.ts` (§10.2): `caption`, `table`, and the `class`, `id`
and `style` *attributes* are allowed, but `style` is **not** an allowed tag — so charm's injected
`<style>` block is stripped in untrusted HTML output.

Three things the spike must answer, in priority order:

1. **Does a `<style>` block survive inside an inlined *trusted* `image/svg+xml` output?** §5 is built
   entirely on collision class (b), which presupposes that it does. If trusted SVG is sanitized the
   same way HTML is, charm's CSS never reaches the DOM, the animation never runs anywhere, and §5.2's
   scoping work is unnecessary — a different and much cheaper conclusion.
2. **Is untrusted `image/svg+xml` inlined, or isolated in an `<img src="data:…">`?** If isolated,
   collisions cannot occur in that mode at all and §5 matters only for trusted output — still the
   normal just-ran case.
3. Do `<caption>` and the inline `text-align` style survive in both modes, as `sanitizer.ts` implies?

## 7. Deliverables

In this repo:

1. `matrix-jupyter/build.gradle` — modelled on `matrix-xchart/build.gradle` (same publish, signing,
   nexus blocks), `version = '0.1.0-SNAPSHOT'`, `options.release = 21`.
2. `matrix-jupyter/LICENSE` (MIT), `matrix-jupyter/release.md`, `matrix-jupyter/release.sh` — all 21
   published modules carry the latter two.
3. `matrix-jupyter/src/main/resources/META-INF/services/se.alipsa.matrix.jupyter.MatrixRenderer` and
   `…/org.dflib.jjava.jupyter.Extension`.
4. `settings.gradle` — `include 'matrix-jupyter'`.
5. `matrix-bom/bom.xml` and `matrix-bom/pom.xml` — new entry.
6. `gradle/libs.versions.toml` — `v_jjava = "1.0-a8"` and a `jjava-jupyter` library entry.
7. `matrix-jupyter/README.md` — complete runnable walkthrough per AGENTS.md: install, `@Grab` lines, a
   Matrix table cell, a charm chart cell, a `RenderOptions.defaults` cell, and
   `MatrixJupyterExtension.describe()` / `refresh()` for diagnosing a late `@Grab` — the notebook
   diagnostic is the extension's (§2.6.2); `RendererRegistry.describe()` is the kernel-free one. Both deployments (§2.3.1) documented, static first
   until §2.7 is confirmed. Every option with its default, and three honest caveats: `attr` values are
   not escaped; `RenderOptions` size is not applied to `GgChart`; a user-supplied `chart.stylesheet`
   is not auto-scoped, with the `#charm-root .my-rule` remedy shown (§5.2 step 2).
8. `readme.md` root module list (alphabetical, between matrix-json and matrix-logging),
   `docs/agents/architecture.md` module table, and `docs/python-comparison.md:266` — the limitation
   this module removes.
9. **matrix-core (3.9.0-SNAPSHOT) — `<caption>` support in `toHtml`.** `toHtml` emits
   `<table>…<thead>…<tbody>…</table>` with no caption and no caption API anywhere in the class, so the
   §2.4 truncation notice is otherwise impossible without either string surgery on the returned HTML
   or a second table writer, both of which this spec forbids. Add a `caption` key to the `attr` map,
   handled inside `toHtml` exactly as `align` already is (`Matrix.groovy:3726-3735`): consumed rather
   than emitted as an attribute, escaped with `escapeHtml`, and written as the first child of
   `<table>`. Independently useful, and `caption` is in JupyterLab's allowed-tag list (§10.2).
   **This changes a released contract:** today every key except `align` becomes a literal table
   attribute (`Matrix.groovy:3726-3735`), so a caller passing `caption: 'x'` currently gets
   `<table caption="x">` and will now get a `<caption>` element. Needs an entry in matrix-core's
   `release.md`.
10. `matrix-charts/release.md` and README — the §5.2 root-id output change, the
    `SvgWriter.toXml(chart.render(), 'r1-')` namespacing recipe for multi-chart pages, and the
    `#charm-root .my-rule` remedy for scoping a user stylesheet. No `ChartToSvg` overload (§5.2 step 4).

Out of repo, prerequisites: gsvg 1.2.0 (§5.2) and the matrix-charts changes (§5.2), in that order.

## 8. Risks

| Risk | Mitigation |
|---|---|
| `jjava-jupyter` alpha API churn | Confined to `MatrixJupyterExtension`; pinned to 1.0-a8; adapter kept dumb; §6 test 7 fails loudly on a bump |
| Groovy 5-compiled classes on a Groovy 6 kernel runtime | The real risk is call-site/metaclass bytecode, not the JDK. §6 test 9 adds an opt-in Groovy 6 smoke check; until it runs, compatibility is **claimed only for Groovy 5** |
| JDK: module built at `release = 21`, kernel targets JDK 25 | JDK 21 bytecode runs on 25; unproblematic and separate from the Groovy question above |
| gsvg 1.2.0 and the charm changes not yet released | Hard prerequisite with the release order in §5.3; matrix-jupyter is not started before they land |
| Untrusted-notebook sanitization removing styling | §6.1 spike, sequenced **first** in §5.3 — it can invalidate the CSS half of the gsvg and charm work before either is built |
| Multiple kernels or repeated installs in one JVM | Per-kernel registration state and an overridden `uninstall` (§2.6.1), pinned by §6 test 7 |
| Host-neutral layer accidentally coupled to jjava | `MatrixRenderer` as the probe anchor (§2.2), pinned by §6 test 8 |
| **`groovy-jupyter` may never install a grabbed Extension** | Discovery timing is the kernel's choice and `installExtensions` is `protected` (§2.7). Goal 1's `@Grab`-only story depends on a kernel-side call this spec cannot provide. Raise it with the kernel authors early; until confirmed, the static-classpath deployment is the documented path |
| jjava subtype dispatch | **Resolved:** `Renderer.render` walks superclasses and all interfaces via `InheritanceIterator` (§10.1); pinned by §6 test 7 |
| Duplicate render functions on repeated `refresh()` | `Renderer.register` appends via `Map.compute` (§10.1); guarded by the per-kernel `attached` map (§2.6.1) and pinned by §6 test 7 |
| Large matrices bloating notebook files | `maxRows` and `maxColumns`, both defaulting to 50, with an explicit caption |
| xchart headlessness unverified (assessment §11) | Excluded from v1 |

## 9. Open items for a follow-up release

- `matrix-xchart` renderer, once headlessness is verified.
- Vega-Lite output, if a matrix chart facade ever emits a Vega-Lite spec.
- Scoping user-supplied `chart.stylesheet` (§5.2 step 2), if demand appears and someone is willing to
  own selector-level CSS parsing in gsvg.
- Default imports, if `groovy-jupyter`'s own kernel class (not `BaseKernel`) grows a public hook.

## 10. Verification record

### 10.1 jjava-jupyter 1.0-a8

Downloaded from Maven Central (`maven-metadata.xml`: latest `1.0-a8`, published 2026-08-18) and
inspected with `javap`:

```
public interface org.dflib.jjava.jupyter.Extension {
  void install(BaseKernel);
  default void uninstall(BaseKernel);
}
public class BaseKernel {
  public Renderer getRenderer();              // public — registration is reachable
  protected ClassLoader getClassLoader();     // protected — NOT reachable from an Extension (D9)
  protected void installDefaultExtensions();  // = installExtensions(getClassLoader())  — startup only
  protected void installExtensions(ClassLoader);  // = ServiceLoader.load(Extension, loader)
                                                  //     .stream().map(get).forEach(this::installExtension)
  protected void installExtensions(String paths); // builds URLClassLoader(paths, getClassLoader())
  // no import-injection API of any kind
}
public class Renderer {
  public <T> RenderRegistration<T> createRegistration(Class<T>);
  public <T> void register(Set<MIMEType>, Set<MIMEType>, Set<Class<? extends T>>, RenderFunction<T>);
}
public class Renderer.RenderRegistration<T> {
  RenderRegistration<T> supporting(MIMEType...); RenderRegistration<T> preferring(MIMEType...);
  RenderRegistration<T> onType(Class<? extends T>); void register(RenderFunction<T>);
}
public interface RenderFunction<T> { void render(T, RenderContext); }
public class RenderContext {
  public boolean renderIfRequested(MIMEType, Supplier<Object>);                    // lazy
  public boolean renderIfRequested(MIMEType, Function<MIMEType, Object>);          // lazy
  public boolean renderIfRequested(MIMEType, BiConsumer<MIMEType, DisplayData>);   // lazy, may decline
  public boolean wantsDataRenderedAs(MIMEType);
  public MIMEType resolveRequestedType(MIMEType);
  public DisplayData getOutputContainer();
  public Map<String, Object> getParams();
}
public class DisplayData {
  public void putData(String, Object);
  public void putData(MIMEType, Object);
  public void putData(MIMEType, Object, Object);
  public Object getData(MIMEType);
  public boolean hasDataForType(MIMEType);
}
public class MIMEType {
  public static final MIMEType TEXT_HTML, TEXT_PLAIN, IMAGE_SVG, TEXT_MARKDOWN, IMAGE_PNG, …;
  public static MIMEType parse(String) throws MIMETypeParseException;   // arbitrary mime types
}
```

The `BiConsumer` overload is what lets §2.6's rich-mime branch **decline to emit** — a `Supplier`
must return something, so with only that form the "never put a text/plain note into an
`image/svg+xml` slot" rule would need a different mechanism. `MIMEType.parse` is what lets §2.6
accept a `preferredMime()` outside the three constants.

Decompiled (`javap -c`), because three design decisions turn on the implementation, not the signature:

- `Renderer.renderFunctions` is `Map<Class, List<RenderFunctionProps>>`, and `register` stores via
  `Map.compute` **appending to the list**. Post-install registration is safe (no init-time freeze),
  but re-registering a type duplicates it → §2.6.1's per-kernel registered-type guard.
- `BaseKernel.onShutdown(boolean)` (public) calls `uninstallExtension()`, which copies the extension
  map, clears it, and calls `Extension.uninstall(this)` on each, logging and ignoring failures. So a
  cleanup callback does exist and §2.6.1's `uninstall` override will fire → but only at kernel
  shutdown, hence the weak keys.
- `Renderer.render` builds `new InheritanceIterator(value.getClass())` and walks it;
  `InheritanceIterator` traverses superclasses **and** all interfaces (`getInterfaces` /
  `getAllInterfaces`, deduped through `observedInterfaces`). Subtype dispatch works → no per-subtype
  registration needed.
- `installDefaultExtensions()` delegates to `installExtensions(getClassLoader())`, so startup
  discovery is over the kernel's own loader; `installExtensions(ClassLoader)` exists for post-startup
  installation over an arbitrary loader → §2.3.1's two deployments and §2.7's kernel-side requirement.

Not verified: whether `groovy-jupyter` will call `installExtensions(sessionLoader)` after a `@Grab`
(§2.7) — that kernel does not exist yet.

### 10.2 JupyterLab sanitizer

`packages/apputils/src/sanitizer.ts` (main branch, fetched 2026-08-29): `allowedTags` includes
`table` and `caption` but **not** `style`; `allowedAttributes['*']` includes `class`, `id` and
`style`. Not verified: untrusted `image/svg+xml` rendering path (§6.1).

### 10.3 In-repo claims

`AnimationSpec.groovy:37-52`; `CharmRenderer.groovy:320-321,362-381`; `PlotGridRenderer.groovy:207,262,277,300`;
`FormatRegistry.groovy:173,230,232`; `Matrix.groovy:3722-3781` (no caption), `:3730` (raw attr
interpolation), `:4474` (`escapeHtml`); `Summary.groovy:11,25`; `Structure.groovy:9`;
`Grid.groovy:23`; `MatrixBuilder.groovy:478,764`; `GgChart.groovy:55,58`; `Chart.groovy:375`;
`CharmBridge.groovy:54`; `Matrix.groovy:3065` (`selectColumns`), `:3652` (`toHtml` row overload);
`Row.groovy:27,35`; `Column.groovy:25`; `CharmRenderer.groovy:372-379` (verbatim stylesheet
injection); `ChartToSvg.groovy:34-254` (twelve public `export` overloads: File/OutputStream/Writer ×
`Svg`/`CharmChart`/`PlotGrid`/`Object`); `python-comparison.md:266`. All checked 2026-08-29.
