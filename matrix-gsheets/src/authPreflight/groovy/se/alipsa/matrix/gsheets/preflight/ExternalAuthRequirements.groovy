package se.alipsa.matrix.gsheets.preflight

import se.alipsa.matrix.gsheets.GsAuthenticator

/** Shared credentials required by external Sheets operations, including cleanup. */
class ExternalAuthRequirements {

  /** Returns all scopes needed by enabled external Sheets tests. */
  static List<String> scopes() {
    (GsAuthenticator.SCOPES + [GsAuthenticator.SCOPE_SHEETS_READONLY, GsAuthenticator.SCOPE_DRIVE_FILE]).unique()
  }
}
