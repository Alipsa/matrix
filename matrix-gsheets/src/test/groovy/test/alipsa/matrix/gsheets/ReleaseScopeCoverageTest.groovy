package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.*
import se.alipsa.matrix.gsheets.preflight.ExternalAuthRequirements

class ReleaseScopeCoverageTest {

  @Test
  void productionOperationsRequestCoveredScopes() {
    Matrix matrix = Matrix.builder('Scope test').data(x: [1]).types([Integer]).build()
    List<Closure> operations = [
        { new GsUtilTest().testGetSheetNames() },
        { new GsUtilTest().testGetSheetNamesWithMultipleSheets() },
        { GsUtil.deleteSheet('id') },
        { GsUtil.getSheetNames('id') },
        { GsheetsReader.read('id', 'A1', true) },
        { GsheetsReader.readAsStrings('id', 'A1', true) },
        { GsheetsReader.readAsObject('id', 'A1', true) },
        { GsheetsWriter.write(matrix) },
        { GsheetsWriter.update('id', 'A1', matrix) },
        { GsImporter.importSheet('id', 'A1', true) },
        { GsImporter.importSheetAsStrings('id', 'A1', true) },
        { GsImporter.importSheetAsObject('id', 'A1', true) },
        { GsExporter.exportSheet(matrix) },
        { GsAuthenticator.authenticate(GsAuthenticator.SCOPE_SHEETS_READONLY) }
    ]
    operations.each { Closure operation ->
      List<String> recorded = []
      // Intercept List calls directly: Mockito.when with CALLS_REAL_METHODS would invoke ADC while stubbing.
      MockedStatic<GsAuthenticator> mock = Mockito.mockStatic(GsAuthenticator, { invocation ->
        if (invocation.method.name == 'authenticate' && invocation.method.parameterTypes.toList() == [List]) {
          recorded.addAll(invocation.getArgument(0) as List<String>)
          throw new AuthenticationReached()
        }
        Mockito.CALLS_REAL_METHODS.answer(invocation)
      } as org.mockito.stubbing.Answer)
      try {
        assertThrows(AuthenticationReached) { operation.call() }
        assertTrue(!recorded.empty, 'Production operation must reach List authentication interception')
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken('scope-test', new Date(System.currentTimeMillis() + 3600000)))
        assertTrue(GsAuthUtils.hasAllScopes(credentials, recorded, { String token -> ExternalAuthRequirements.scopes() as Set<String> } as GsAuthUtils.ScopeResolver))
      } finally {
        mock.close()
      }
    }
  }

  private static class AuthenticationReached extends RuntimeException {
  }
}
