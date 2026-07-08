# Bundled OAuth client

`src/main/resources/se/alipsa/matrix/gsheets/oauth/client_secret.json` is the
"Desktop app" OAuth 2.0 client that matrix-gsheets uses by default for
interactive login (`GsAuthUtils.loginAndWriteAdc`). It ships inside the jar so
that no caller has to register their own OAuth client just to authenticate
interactively — they get the standard Google consent screen out of the box.

Per Google's OAuth docs, installed-app client secrets are not confidential
(https://developers.google.com/identity/protocols/oauth2/native-app), so this
file is committed rather than gitignored.

`GsAuthenticator.CLIENT_SECRET_FILE` (`~/client_secret_desktop.json`) still
lets any caller override this with their own registered client if they need
to (e.g. a different consent screen branding, or their own quota/project).

## Rotating or regenerating the client

1. Google Cloud Console → APIs & Services → Credentials, for whichever
   project should own the client.
2. Configure the OAuth consent screen if not already done (External,
   Testing mode is fine for personal/small-team use; add test users by
   email, or set to Internal if the project is under a Workspace org).
3. Create Credentials → OAuth client ID → Application type: **Desktop app**.
4. Download the JSON and overwrite
   `src/main/resources/se/alipsa/matrix/gsheets/oauth/client_secret.json`.
5. Rebuild — Gradle picks it up automatically via the normal resources
   source set.
