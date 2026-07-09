# Bundled OAuth client

matrix-gsheets bundles its own "Desktop app" OAuth 2.0 client into the jar
(`se/alipsa/matrix/gsheets/oauth/client_secret.json` on the classpath) so
that no caller has to register their own OAuth client just to authenticate
interactively — they get the standard Google consent screen out of the box
(`GsAuthUtils.loginAndWriteAdc`).

**The secret is not committed to git.** Although Google's OAuth docs say
installed-app client secrets aren't confidential
(https://developers.google.com/identity/protocols/oauth2/native-app),
GitHub's push protection still (correctly) blocks committing a recognizable
Google OAuth client ID/secret pair, and a public repo isn't a great place
for it regardless — anyone could scrape it and burn your API quota, or spin
up a client impersonating "matrix-gsheets" in a phishing-style consent
screen if the app is ever moved out of Testing mode. Instead, it's injected
at **build time** from `~/client_secret_desktop.json` on whichever machine
runs the build (see `build.gradle`'s `processResources` configuration).

This is the same path `GsAuthenticator.CLIENT_SECRET_FILE` checks at
*runtime* as a caller override, so the one file serves two purposes:
- On the machine that **builds/publishes** the library, its presence there
  gets baked into the jar as the default bundled client.
- On any machine that **runs** code using the library, its presence there
  overrides the bundled client with the caller's own registered client.

Building without that file present simply ships a jar with no bundled
client — `GsAuthUtils` falls back to its existing clear `IllegalStateException`
at login time, pointing at the steps below.

## Setting up the client (once, on the build/release machine)

1. Google Cloud Console → APIs & Services → Credentials, for whichever
   project should own the client.
2. Configure the OAuth consent screen if not already done (External,
   Testing mode is fine for personal/small-team use; add test users by
   email, or set to Internal if the project is under a Workspace org).
3. Create Credentials → OAuth client ID → Application type: **Desktop app**.
4. Download the JSON and save it as `~/client_secret_desktop.json`
   (`chmod 600` recommended).
5. Rebuild — `processResources` picks it up automatically.

For CI-driven releases, store the client JSON as a CI secret and have the
release job write it to `~/client_secret_desktop.json` before building.
