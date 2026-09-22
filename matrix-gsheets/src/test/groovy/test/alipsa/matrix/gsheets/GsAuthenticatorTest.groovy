package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gsheets.GsAuthenticator.*

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import org.junit.jupiter.api.Test

import se.alipsa.matrix.gsheets.GsAuthenticator
import se.alipsa.matrix.gsheets.SheetOperationException

import java.time.Instant

/**
 * Unit tests for GsAuthenticator focusing on scope normalization and utility methods.
 *
 * Note: Full authentication flow testing requires external dependencies (gcloud, OAuth2, file system)
 * and should be done as integration tests tagged with @Tag('external').
 */
class GsAuthenticatorTest {

  @Test
  void testNormalizeScopesForGcloudAddsCloudPlatform() {
    def scopes = [SCOPE_SHEETS]
    def normalized = GsAuthenticator.normalizeScopesForGcloud(scopes)

    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM),
      'Normalized scopes should always include cloud-platform')
    assertTrue(normalized.contains(SCOPE_SHEETS))
  }

  @Test
  void testNormalizeScopesForGcloudPreservesReadonly() {
    def scopes = [SCOPE_SHEETS_READONLY]
    def normalized = GsAuthenticator.normalizeScopesForGcloud(scopes)

    assertTrue(normalized.contains(SCOPE_SHEETS_READONLY),
      'Should retain the requested readonly scope')
    assertFalse(normalized.contains(SCOPE_SHEETS),
      'Should not upgrade readonly access to read/write')
  }

  @Test
  void testNormalizeScopesForGcloudRemovesShortEmail() {
    def scopes = [SCOPE_SHEETS, 'email']
    def normalized = GsAuthenticator.normalizeScopesForGcloud(scopes)

    assertFalse(normalized.contains('email'),
      "Should remove short 'email' scope in favor of userinfo.email")
  }

  @Test
  void testNormalizeScopesForGcloudHandlesNull() {
    def normalized = GsAuthenticator.normalizeScopesForGcloud(null)

    assertNotNull(normalized)
    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM))
  }

  @Test
  void testNormalizeScopesForGcloudHandlesEmpty() {
    def normalized = GsAuthenticator.normalizeScopesForGcloud([])

    assertNotNull(normalized)
    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM))
  }

  @Test
  void testNormalizeScopesForGcloudPreservesOrder() {
    def scopes = [SCOPE_DRIVE_FILE, SCOPE_OPENID, SCOPE_USERINFO_EMAIL]
    def normalized = GsAuthenticator.normalizeScopesForGcloud(scopes)

    // Should preserve input scopes and add cloud-platform
    assertTrue(normalized.containsAll(scopes))
    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM))
  }

  @Test
  void testNormalizeScopesForGcloudRemovesDuplicates() {
    def scopes = [SCOPE_SHEETS, SCOPE_SHEETS, SCOPE_DRIVE_FILE, SCOPE_DRIVE_FILE]
    def normalized = GsAuthenticator.normalizeScopesForGcloud(scopes)

    // Should have no duplicates (LinkedHashSet removes them)
    assertEquals(3, normalized.size(),
      'Should have 3 unique scopes: cloud-platform, sheets, drive.file')
  }

  @Test
  void testScopeConstants() {
    // Verify scope constants are correctly defined
    assertNotNull(SCOPE_CLOUD_PLATFORM)
    assertNotNull(SCOPE_SHEETS)
    assertNotNull(SCOPE_SHEETS_READONLY)
    assertNotNull(SCOPE_DRIVE_FILE)
    assertNotNull(SCOPE_OPENID)
    assertNotNull(SCOPE_USERINFO_EMAIL)

    // Verify they contain expected patterns
    assertTrue(SCOPE_CLOUD_PLATFORM.contains('googleapis.com'))
    assertTrue(SCOPE_SHEETS.contains('spreadsheets'))
    assertTrue(SCOPE_DRIVE_FILE.contains('drive'))
  }

  @Test
  void testDefaultScopes() {
    // Verify SCOPES contains only the default write scope
    assertNotNull(SCOPES)
    assertTrue(SCOPES.contains(SCOPE_SHEETS))
    assertEquals([SCOPE_SHEETS], SCOPES)
  }

  @Test
  void testAdcFilePath() {
    // Verify ADC_FILE_PATH is set to the correct location
    assertNotNull(ADC_FILE_PATH)
    assertTrue(ADC_FILE_PATH.absolutePath.contains('.config/gcloud'))
    assertTrue(ADC_FILE_PATH.absolutePath.contains('application_default_credentials.json'))
  }

  @Test
  void testAuthenticateDoesNotStartInteractiveLoginWhenAdcIsUnavailable() {
    int logins = 0
    def backend = [
        existing    : { List<String> scopes -> null },
        login       : { List<String> scopes, String quotaProject -> logins++; null },
        hasAllScopes: { GoogleCredentials creds, List<String> scopes -> false },
        userEmail   : { GoogleCredentials creds -> 'stub@example.test' }
    ] as GsAuthenticator.AuthBackend

    SheetOperationException exception = assertThrows(SheetOperationException,
        () -> GsAuthenticator.authenticate([SCOPE_SHEETS], null, backend))
    assertEquals('authenticate', exception.operation)
    assertTrue(exception.message.contains('gcloud auth application-default login --scopes='))
    assertEquals(0, logins)
  }

  @Test
  void testAuthenticateUsesExistingScopedCredentialsWithoutLogin() {
    GoogleCredentials credentials = credentials()
    int logins = 0
    def backend = [
        existing    : { List<String> scopes -> credentials },
        login       : { List<String> scopes, String quotaProject -> logins++; credentials },
        hasAllScopes: { GoogleCredentials creds, List<String> scopes -> true },
        userEmail   : { GoogleCredentials creds -> 'stub@example.test' }
    ] as GsAuthenticator.AuthBackend

    assertSame(credentials, GsAuthenticator.authenticate([SCOPE_SHEETS], null, backend))
    assertEquals(0, logins)
  }

  @Test
  void testAuthenticateDoesNotMergeUnrequestedScopes() {
    GoogleCredentials credentials = credentials()
    List<String> requested = []
    def backend = [
        existing    : { List<String> scopes -> requested = scopes; credentials },
        login       : { List<String> scopes, String quotaProject -> fail('Login must not be called'); null },
        hasAllScopes: { GoogleCredentials creds, List<String> scopes -> true },
        userEmail   : { GoogleCredentials creds -> 'stub@example.test' }
    ] as GsAuthenticator.AuthBackend

    assertSame(credentials, GsAuthenticator.authenticate([SCOPE_SHEETS_READONLY], null, backend))
    assertEquals([SCOPE_SHEETS_READONLY], requested)
  }

  @Test
  void testAuthenticateInteractivelyUsesLoginPathAndRejectsMissingScopes() {
    GoogleCredentials credentials = credentials()
    int logins = 0
    def backend = [
        existing    : { List<String> scopes -> credentials },
        login       : { List<String> scopes, String quotaProject -> logins++; credentials },
        hasAllScopes: { GoogleCredentials creds, List<String> scopes -> false },
        userEmail   : { GoogleCredentials creds -> 'stub@example.test' }
    ] as GsAuthenticator.AuthBackend

    SheetOperationException exception = assertThrows(SheetOperationException,
        () -> GsAuthenticator.authenticateInteractively([SCOPE_SHEETS], null, backend))
    assertTrue(exception.message.contains('missing required scopes'))
    assertEquals(1, logins)
  }

  private static GoogleCredentials credentials() {
    GoogleCredentials.create(new AccessToken('tok', Date.from(Instant.now().plusSeconds(3600))))
  }

  // Note: Testing actual authentication flows (runGcloudLogin, getCredentials, authenticate)
  // requires external dependencies and should be done as integration tests

}
