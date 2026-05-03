package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gsheets.GsAuthenticator.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.gsheets.GsAuthUtils

/**
 * Unit tests for GsAuthUtils focusing on utility methods that don't require
 * external dependencies (OAuth2 token validation methods are excluded as they
 * require network access).
 */
class GsAuthUtilsTest {

  @Test
  void testCanonScopeWithNull() {
    assertNull(GsAuthUtils.canonScope(null))
  }

  @Test
  void testCanonScopeWithEmail() {
    // 'email' should be canonicalized to the full userinfo.email scope
    assertEquals(SCOPE_USERINFO_EMAIL, GsAuthUtils.canonScope('email'))
    assertEquals(SCOPE_USERINFO_EMAIL, GsAuthUtils.canonScope(SCOPE_USERINFO_EMAIL))
  }

  @Test
  void testCanonScopeWithOtherScopes() {
    // Other scopes should pass through unchanged
    assertEquals(SCOPE_SHEETS, GsAuthUtils.canonScope(SCOPE_SHEETS))
    assertEquals(SCOPE_CLOUD_PLATFORM, GsAuthUtils.canonScope(SCOPE_CLOUD_PLATFORM))
    assertEquals(SCOPE_DRIVE_FILE, GsAuthUtils.canonScope(SCOPE_DRIVE_FILE))
    assertEquals('custom-scope', GsAuthUtils.canonScope('custom-scope'))
  }

  @Test
  void testHasAllScopesWithNullCredentials() {
    assertFalse(GsAuthUtils.hasAllScopes(null, [SCOPE_SHEETS]))
  }

  // Note: Testing hasAllScopes with real credentials requires network access
  // and valid OAuth2 tokens, so we only test the null case here.
  // Full integration tests for authentication should be tagged as @Tag('external')

}
