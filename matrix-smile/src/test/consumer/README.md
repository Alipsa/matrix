# Published dependency smoke test

This standalone Gradle consumer declares only Smile and Groovy. It verifies that
published metadata resolves core 3.10.0-SNAPSHOT transitively and that fillna and
both dropna overloads execute without missing core APIs. It also checks lossless
BigDecimal fills, legacy Smile replacement classes, mixed extrema rejection, and
raw duplicate-key diagnostics, and String/GString and JDBC Date/Timestamp
comparison compatibility. Java 21 is required.
From the repository root:

```sh
./gradlew :matrix-core:publishToMavenLocal :matrix-smile:publishToMavenLocal :matrix-groovy-ext:publishToMavenLocal
./gradlew -p matrix-smile/src/test/consumer verifyCore run
./gradlew -p matrix-smile/src/test/consumer verifyCore run -PpomOnly --rerun-tasks
```

The first consumer command uses Gradle module metadata; the second deliberately
uses only the Maven POM. `pomOnly` defaults to absent (Gradle metadata enabled).
Add `--offline` once dependencies are cached. Local publication changes only the
local Maven repository; these commands do not publish remotely. Directly forced
older core versions are unsupported. The fixture's SNAPSHOT pins track this plan's
development pair and must be updated when verifying a later release.
