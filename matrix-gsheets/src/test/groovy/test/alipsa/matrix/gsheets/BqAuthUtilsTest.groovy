package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gsheets.BqAuthUtils

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gsheets.BqAuthenticator.*

/**
 * Unit tests for BqAuthUtils focusing on utility methods that don't require
 * external dependencies (OAuth2 token validation methods are excluded as they
 * require network access).
 */
class BqAuthUtilsTest {

  @Test
  void testCanonScopeWithNull() {
    assertNull(BqAuthUtils.canonScope(null))
  }

  @Test
  void testCanonScopeWithEmail() {
    // "email" should be canonicalized to the full userinfo.email scope
    assertEquals(SCOPE_USERINFO_EMAIL, BqAuthUtils.canonScope("email"))
    assertEquals(SCOPE_USERINFO_EMAIL, BqAuthUtils.canonScope(SCOPE_USERINFO_EMAIL))
  }

  @Test
  void testCanonScopeWithOtherScopes() {
    // Other scopes should pass through unchanged
    assertEquals(SCOPE_SHEETS, BqAuthUtils.canonScope(SCOPE_SHEETS))
    assertEquals(SCOPE_CLOUD_PLATFORM, BqAuthUtils.canonScope(SCOPE_CLOUD_PLATFORM))
    assertEquals(SCOPE_DRIVE_FILE, BqAuthUtils.canonScope(SCOPE_DRIVE_FILE))
    assertEquals("custom-scope", BqAuthUtils.canonScope("custom-scope"))
  }

  @Test
  void testHasAllScopesWithNullCredentials() {
    assertFalse(BqAuthUtils.hasAllScopes(null, [SCOPE_SHEETS]))
  }

  // Note: Testing hasAllScopes with real credentials requires network access
  // and valid OAuth2 tokens, so we only test the null case here.
  // Full integration tests for authentication should be tagged as @Tag("external")
}
