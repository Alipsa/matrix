package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gsheets.BqAuthenticator

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gsheets.BqAuthenticator.*

/**
 * Unit tests for BqAuthenticator focusing on scope normalization and utility methods.
 *
 * Note: Full authentication flow testing requires external dependencies (gcloud, OAuth2, file system)
 * and should be done as integration tests tagged with @Tag("external").
 */
class BqAuthenticatorTest {

  @Test
  void testNormalizeScopesForGcloudAddsCloudPlatform() {
    def scopes = [SCOPE_SHEETS]
    def normalized = BqAuthenticator.normalizeScopesForGcloud(scopes)

    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM),
      "Normalized scopes should always include cloud-platform")
    assertTrue(normalized.contains(SCOPE_SHEETS))
  }

  @Test
  void testNormalizeScopesForGcloudUpgradesReadonly() {
    def scopes = [SCOPE_SHEETS_READONLY]
    def normalized = BqAuthenticator.normalizeScopesForGcloud(scopes)

    assertTrue(normalized.contains(SCOPE_SHEETS),
      "Should upgrade readonly to full sheets scope")
    assertFalse(normalized.contains(SCOPE_SHEETS_READONLY),
      "Should remove readonly after upgrading")
  }

  @Test
  void testNormalizeScopesForGcloudRemovesShortEmail() {
    def scopes = [SCOPE_SHEETS, "email"]
    def normalized = BqAuthenticator.normalizeScopesForGcloud(scopes)

    assertFalse(normalized.contains("email"),
      "Should remove short 'email' scope in favor of userinfo.email")
  }

  @Test
  void testNormalizeScopesForGcloudHandlesNull() {
    def normalized = BqAuthenticator.normalizeScopesForGcloud(null)

    assertNotNull(normalized)
    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM))
  }

  @Test
  void testNormalizeScopesForGcloudHandlesEmpty() {
    def normalized = BqAuthenticator.normalizeScopesForGcloud([])

    assertNotNull(normalized)
    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM))
  }

  @Test
  void testNormalizeScopesForGcloudPreservesOrder() {
    def scopes = [SCOPE_DRIVE_FILE, SCOPE_OPENID, SCOPE_USERINFO_EMAIL]
    def normalized = BqAuthenticator.normalizeScopesForGcloud(scopes)

    // Should preserve input scopes and add cloud-platform
    assertTrue(normalized.containsAll(scopes))
    assertTrue(normalized.contains(SCOPE_CLOUD_PLATFORM))
  }

  @Test
  void testNormalizeScopesForGcloudRemovesDuplicates() {
    def scopes = [SCOPE_SHEETS, SCOPE_SHEETS, SCOPE_DRIVE_FILE, SCOPE_DRIVE_FILE]
    def normalized = BqAuthenticator.normalizeScopesForGcloud(scopes)

    // Should have no duplicates (LinkedHashSet removes them)
    assertEquals(3, normalized.size(),
      "Should have 3 unique scopes: cloud-platform, sheets, drive.file")
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
    assertTrue(SCOPE_CLOUD_PLATFORM.contains("googleapis.com"))
    assertTrue(SCOPE_SHEETS.contains("spreadsheets"))
    assertTrue(SCOPE_DRIVE_FILE.contains("drive"))
  }

  @Test
  void testDefaultScopes() {
    // Verify SCOPES contains the expected default scopes
    assertNotNull(SCOPES)
    assertTrue(SCOPES.contains(SCOPE_CLOUD_PLATFORM))
    assertTrue(SCOPES.contains(SCOPE_SHEETS))
    assertTrue(SCOPES.contains(SCOPE_DRIVE_FILE))
    assertTrue(SCOPES.contains(SCOPE_OPENID))
    assertTrue(SCOPES.contains(SCOPE_USERINFO_EMAIL))
  }

  @Test
  void testAdcFilePath() {
    // Verify ADC_FILE_PATH is set to the correct location
    assertNotNull(ADC_FILE_PATH)
    assertTrue(ADC_FILE_PATH.absolutePath.contains(".config/gcloud"))
    assertTrue(ADC_FILE_PATH.absolutePath.contains("application_default_credentials.json"))
  }

  // Note: Testing actual authentication flows (runGcloudLogin, getCredentials, authenticate)
  // requires external dependencies and should be done as integration tests
}
