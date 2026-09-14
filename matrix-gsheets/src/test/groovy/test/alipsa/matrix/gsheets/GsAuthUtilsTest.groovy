package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gsheets.GsAuthenticator.*

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.gsheets.GsAuthUtils

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Unit tests for GsAuthUtils focusing on utility methods that don't require
 * external dependencies (OAuth2 token validation methods are excluded as they
 * require network access).
 */
class GsAuthUtilsTest {

  @TempDir
  Path tempDir

  @AfterEach
  void clearScopeCache() {
    GsAuthUtils.clearScopeCache()
  }

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

  @Test
  void testHasAllScopesCachesResolverResultsAndHandlesFailures() {
    GoogleCredentials creds = credentials('tok-1')
    int calls = 0
    GsAuthUtils.ScopeResolver resolver = { String token ->
      calls++
      [SCOPE_SHEETS] as Set<String>
    } as GsAuthUtils.ScopeResolver

    assertTrue(GsAuthUtils.hasAllScopes(creds, [SCOPE_SHEETS_READONLY], resolver))
    assertFalse(GsAuthUtils.hasAllScopes(creds, [SCOPE_DRIVE_FILE], resolver))
    assertEquals(1, calls)
    GsAuthUtils.clearScopeCache()
    assertTrue(GsAuthUtils.hasAllScopes(creds, [SCOPE_SHEETS], resolver))
    assertEquals(2, calls)

    GsAuthUtils.ScopeResolver failing = { String token -> throw new IOException('offline') } as GsAuthUtils.ScopeResolver
    assertFalse(GsAuthUtils.hasAllScopes(credentials('tok-2'), [SCOPE_SHEETS], failing))
  }

  @Test
  void testHasAllScopesRetriesFailedResolutionsInsteadOfCachingThem() {
    AtomicInteger calls = new AtomicInteger()
    GsAuthUtils.ScopeResolver failing = { String token ->
      calls.incrementAndGet()
      throw new IOException('offline')
    } as GsAuthUtils.ScopeResolver

    assertFalse(GsAuthUtils.hasAllScopes(credentials('flaky-token'), [SCOPE_SHEETS], failing))
    assertFalse(GsAuthUtils.hasAllScopes(credentials('flaky-token'), [SCOPE_SHEETS], failing))
    assertEquals(2, calls.get())
  }

  @Test
  void testHasAllScopesTreatsNullResolverResultAsNoScopesGranted() {
    GsAuthUtils.ScopeResolver nullResolver = { String token -> null } as GsAuthUtils.ScopeResolver

    assertFalse(GsAuthUtils.hasAllScopes(credentials('null-token'), [SCOPE_SHEETS], nullResolver))
    // an empty granted set satisfies an empty requirement
    assertTrue(GsAuthUtils.hasAllScopes(credentials('null-token'), [], nullResolver))
  }

  @Test
  void testHasAllScopesPropagatesUnexpectedResolverFailuresUnwrapped() {
    GsAuthUtils.ScopeResolver boom = { String token -> throw new IllegalStateException('boom') } as GsAuthUtils.ScopeResolver

    IllegalStateException exception = assertThrows(IllegalStateException,
        () -> GsAuthUtils.hasAllScopes(credentials('boom-token'), [SCOPE_SHEETS], boom))
    assertEquals('boom', exception.message)
  }

  @Test
  void testHasAllScopesRestoresInterruptStatus() {
    CountDownLatch resolverStarted = new CountDownLatch(1)
    CountDownLatch allowResolution = new CountDownLatch(1)
    CountDownLatch waiterStarted = new CountDownLatch(1)
    AtomicReference<Thread> waiterThread = new AtomicReference<>()
    GsAuthUtils.ScopeResolver blocking = { String token ->
      resolverStarted.countDown()
      allowResolution.await()
      [SCOPE_SHEETS] as Set<String>
    } as GsAuthUtils.ScopeResolver

    def executor = Executors.newFixedThreadPool(2)
    try {
      def resolver = executor.submit {
        GsAuthUtils.hasAllScopes(credentials('interrupted-token'), [SCOPE_SHEETS], blocking)
      }
      assertTrue(resolverStarted.await(5, TimeUnit.SECONDS))
      def waiter = executor.submit {
        waiterThread.set(Thread.currentThread())
        waiterStarted.countDown()
        try {
          GsAuthUtils.hasAllScopes(credentials('interrupted-token'), [SCOPE_SHEETS], blocking)
          false
        } catch (InterruptedException ignored) {
          Thread.currentThread().isInterrupted()
        }
      }
      assertTrue(waiterStarted.await(5, TimeUnit.SECONDS))
      waiterThread.get().interrupt()
      assertTrue(waiter.get(5, TimeUnit.SECONDS))
      allowResolution.countDown()
      assertTrue(resolver.get(5, TimeUnit.SECONDS))
    } finally {
      allowResolution.countDown()
      executor.shutdownNow()
    }
  }

  @Test
  void testHasAllScopesResolvesOneTokenOnlyOnceAcrossConcurrentCallers() {
    GoogleCredentials creds = credentials('concurrent-token')
    AtomicInteger calls = new AtomicInteger()
    CountDownLatch start = new CountDownLatch(1)
    GsAuthUtils.ScopeResolver resolver = { String token ->
      calls.incrementAndGet()
      Thread.sleep(50)
      [SCOPE_SHEETS] as Set<String>
    } as GsAuthUtils.ScopeResolver
    def executor = Executors.newFixedThreadPool(8)
    try {
      def futures = (1..8).collect {
        executor.submit {
          start.await()
          GsAuthUtils.hasAllScopes(creds, [SCOPE_SHEETS], resolver)
        }
      }
      start.countDown()
      futures.each { assertTrue(it.get(5, TimeUnit.SECONDS)) }
      assertEquals(1, calls.get())
    } finally {
      executor.shutdownNow()
    }
  }

  @Test
  void testWriteAdcFileUsesOwnerOnlyPermissionsAndOverwrites() {
    File adc = tempDir.resolve('application_default_credentials.json').toFile()
    GsAuthUtils.writeAdcFile(adc, '{}')
    assertEquals('{}', adc.text)
    if (FileSystems.default.supportedFileAttributeViews().contains('posix')) {
      assertEquals(PosixFilePermissions.fromString('rw-------'), Files.getPosixFilePermissions(adc.toPath()))
    }
    GsAuthUtils.writeAdcFile(adc, '{"a":1}')
    assertEquals('{"a":1}', adc.text)
    assertEquals(['application_default_credentials.json'], tempDir.toFile().list().toList().sort())
  }

  @Test
  void testRestrictToOwnerRefusesUnsupportedFileSystemsBeforeWriting() {
    Path target = tempDir.resolve('empty.json')
    Files.createFile(target)
    IOException exception = assertThrows(IOException, () -> GsAuthUtils.restrictToOwner(target, [] as Set<String>))
    assertTrue(exception.message.contains('Cannot restrict ADC file permissions'))
    assertEquals(0L, Files.size(target))
  }

  private static GoogleCredentials credentials(String token) {
    GoogleCredentials.create(new AccessToken(token, Date.from(Instant.now().plusSeconds(3600))))
  }

  // Note: Testing hasAllScopes with real credentials requires network access
  // and valid OAuth2 tokens, so we only test the null case here.
  // Full integration tests for authentication should be tagged as @Tag('external')

}
