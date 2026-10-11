package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito

import se.alipsa.matrix.gsheets.GsAuthUtils
import se.alipsa.matrix.gsheets.GsAuthenticator
import se.alipsa.matrix.gsheets.SheetOperationException
import se.alipsa.matrix.gsheets.preflight.AuthPreflight
import se.alipsa.matrix.gsheets.preflight.ExternalAuthRequirements

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AuthPreflightTest {

  @Test
  void currentUnionIncludesCleanupAndAcceptsBroaderGrants() {
    GoogleCredentials credentials = GoogleCredentials.create(new AccessToken('preflight-test', new Date(System.currentTimeMillis() + 3600000)))
    Set<String> grants = [GsAuthenticator.SCOPE_SHEETS] as Set
    def backend = [
        existing: { List<String> scopes -> credentials },
        login: { List<String> scopes, String project -> fail('Preflight must never log in') },
        hasAllScopes: { GoogleCredentials creds, List<String> scopes ->
          GsAuthUtils.hasAllScopes(creds, scopes, { String token -> grants } as GsAuthUtils.ScopeResolver)
        },
        userEmail: { GoogleCredentials creds -> fail('Preflight must not request user info') }
    ] as GsAuthenticator.AuthBackend
    assertThrows(SheetOperationException) { GsAuthenticator.authenticate(ExternalAuthRequirements.scopes(), null, backend) }
    // Use a different token so the existing tokeninfo cache does not retain the first grant set.
    credentials = GoogleCredentials.create(new AccessToken('preflight-broader-test', new Date(System.currentTimeMillis() + 3600000)))
    grants = [GsAuthenticator.SCOPE_SHEETS, 'https://www.googleapis.com/auth/drive'] as Set
    assertSame(credentials, GsAuthenticator.authenticate(ExternalAuthRequirements.scopes(), null, backend))
  }

  @Test
  void entryPointUsesSharedScopesAndProbesSheetsAndDrive() {
    List<String> requested = []
    List<URI> endpoints = []
    def authentication = Mockito.mockStatic(GsAuthenticator)
    def http = Mockito.mockStatic(HttpClient)
    try {
      authentication.when({ GsAuthenticator.authenticate(Mockito.anyList()) } as MockedStatic.Verification).thenAnswer { call ->
        requested.addAll(call.getArgument(0) as List<String>)
        null
      }
      HttpClient.Builder builder = Mockito.mock(HttpClient.Builder, Mockito.RETURNS_SELF)
      HttpClient client = Mockito.mock(HttpClient)
      HttpResponse<String> response = Mockito.mock(HttpResponse)
      http.when({ HttpClient.newBuilder() } as MockedStatic.Verification).thenReturn(builder)
      Mockito.when(builder.build()).thenReturn(client)
      Mockito.when(response.statusCode()).thenReturn(401)
      Mockito.when(client.send(Mockito.any(HttpRequest), Mockito.any(HttpResponse.BodyHandler))).thenAnswer { call ->
        endpoints.add((call.getArgument(0) as HttpRequest).uri())
        response
      }
      AuthPreflight.main(new String[0])
      assertEquals(ExternalAuthRequirements.scopes(), requested)
      assertTrue(endpoints.any { it.host == 'sheets.googleapis.com' })
      assertTrue(endpoints.any { it.path.startsWith('/drive/v3') })
    } finally {
      http.close()
      authentication.close()
    }
  }

  @Test
  void entryPointSanitizesCredentialFailures() {
    def authentication = Mockito.mockStatic(GsAuthenticator)
    try {
      authentication.when({ GsAuthenticator.authenticate(Mockito.anyList()) } as MockedStatic.Verification).thenThrow(new IllegalStateException('sensitive-token'))
      IllegalStateException failure = assertThrows(IllegalStateException) { AuthPreflight.main(new String[0]) }
      assertFalse(failure.message.contains('sensitive-token'))
      assertNull(failure.cause)
    } finally {
      authentication.close()
    }
  }
}
