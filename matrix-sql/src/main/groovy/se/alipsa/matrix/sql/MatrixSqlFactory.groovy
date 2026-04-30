package se.alipsa.matrix.sql

import groovy.transform.CompileStatic

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.SqlUtil
import se.alipsa.matrix.core.util.Logger
import se.alipsa.mavenutils.ArtifactLookup

/**
 * Factory for creating {@link MatrixSql} instances from JDBC URLs or connection information,
 * automatically resolving driver dependencies from Maven Central.
 */
@CompileStatic
class MatrixSqlFactory {

  private static final Logger log = Logger.getLogger(MatrixSqlFactory)
  static final String REPO_URL = 'https://repo1.maven.org/maven2/'
  static ArtifactLookup artifactLookup = new ArtifactLookup(REPO_URL) // visible for testing
  static final Map<DataBaseProvider, String> FALLBACK_VERSIONS = [
      (DataBaseProvider.H2)   : '2.4.240',
      (DataBaseProvider.DERBY): '10.17.1.0'
  ].asImmutable()

  static MatrixSql createH2(String url, String user, String password, String additionalUrlProperties = null, String version = null) {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDriver('org.h2.Driver')
    if (additionalUrlProperties == null) {
      ci.setUrl(url)
    } else {
      ci.setUrl("$url;$additionalUrlProperties")
    }
    ci.setUser(user)
    ci.setPassword(password)
    String dependencyVersion
    if (version == null) {
      try {
        dependencyVersion = artifactLookup
            .fetchLatestVersion('com.h2database', 'h2')
      } catch (Exception e) {
        dependencyVersion = FALLBACK_VERSIONS[DataBaseProvider.H2]
        log.warn("Failed to fetch latest H2 artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
    } else {
      dependencyVersion = version
    }
    ci.setDependency("com.h2database:h2:$dependencyVersion")
    return new MatrixSql(ci)
  }

  static MatrixSql createH2(File dbFile, String user, String password, String additionalUrlProperties = null, String version = null) {
    // jdbc:h2:file:/Users/pernyf/project/analysis/Overdraft/overdraft.db
    createH2("jdbc:h2:file:$dbFile.absolutePath", user, password, additionalUrlProperties, version)
  }

  static MatrixSql createDerby(File dbFile, String version = null) {
    createDerby(dbFile.absolutePath, version)
  }

  static MatrixSql createDerby(String dbName, String version = null) {
    ConnectionInfo ci = new ConnectionInfo()
    String dependencyVersion
    if (version == null) {
      try {
        dependencyVersion = artifactLookup
            .fetchLatestVersion('org.apache.derby', 'derby')
      } catch (Exception e) {
        dependencyVersion = FALLBACK_VERSIONS[DataBaseProvider.DERBY]
        log.warn("Failed to fetch latest Derby artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
    } else {
      dependencyVersion = version
    }
    ci.setDependency("org.apache.derby:derby:$dependencyVersion;org.apache.derby:derbytools:$dependencyVersion;org.apache.derby:derbyshared:$dependencyVersion")
    ci.setDriver('org.apache.derby.jdbc.EmbeddedDriver')
    ci.setUrl("jdbc:derby:$dbName;create=true")
    return new MatrixSql(ci)
  }

  /**
   * Guess the dependency based on the url.
   *
   * @param url the jdbc url to connect to
   * @param user the username
   * @param password the password
   * @param version the version of the dependency to use, if null, the latest version will be used
   * @return a MatrixSql instance
   */
  static MatrixSql create(String url, String user, String password, String version = null) {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setUrl(url)
    ci.setUser(user)
    ci.setPassword(password)
    create(ci, version)
  }

  /**
   * Guess the dependency based on the url.
   *
   * @param url the jdbc url to connect to
   * @param version the version of the dependency to use, if null, the latest version will be used
   * @return a MatrixSql instance
   */
  static MatrixSql create(String url, String version = null) {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setUrl(url)
    create(ci, version)
  }

  /**
   * Guess the dependency based on the url.
   *
   * @param url the jdbc url to connect to
   * @param version the version of the dependency to use, if null, the latest version will be used
   * @return a MatrixSql instance
   */
  static MatrixSql create(ConnectionInfo ci, String version = null) {
    if (ci.driver == null || ci.driver.isBlank()) {
      String driverClassName = SqlUtil.getDriverClassName(ci.url)
      if (driverClassName != null && !driverClassName.isBlank()) {
        ci.setDriver(driverClassName)
      }
    }
    Map<String, String> dependency = getDependencyName(ci.url)
    if (dependency == null || dependency.isEmpty()) {
      throw new IllegalStateException("Failed to find a suitable dependency for $ci.url")
    }
    String dependencyVersion = version
    if (dependencyVersion == null) {
      try {
        dependencyVersion = artifactLookup
            .fetchLatestVersion(dependency.groupId, dependency.artifactId)
      } catch (Exception e) {
        DataBaseProvider provider = DataBaseProvider.fromUrl(ci.url)
        String fallback = FALLBACK_VERSIONS[provider]
        if (fallback != null) {
          dependencyVersion = fallback
          log.warn("Failed to fetch latest artifact for $dependency.groupId:$dependency.artifactId, " +
              "falling back to version $dependencyVersion: ${e.message}", e)
        } else {
          throw new IllegalStateException(
              "Failed to fetch latest artifact for $dependency.groupId:$dependency.artifactId" +
              ' and no fallback version is configured for this provider', e)
        }
      }
    }
    ci.setDependency("$dependency.groupId:$dependency.artifactId:$dependencyVersion")
    return new MatrixSql(ci)
  }

  static Map<String, String> getDependencyName(String url) {
    if (url == null) {
      return [:]
    }

    // Find the first DataBaseProvider whose urlStart matches the beginning of the URL.
    DataBaseProvider matchingProvider = DataBaseProvider.values().find { DataBaseProvider it ->
      url.startsWith(it.urlStart)
    } as DataBaseProvider

    matchingProvider != null ? mapDependency(matchingProvider) : [:]
  }

  static Map<String, String> mapDependency(DataBaseProvider provider) {
    [groupId: provider.dependencyGroupId, artifactId: provider.dependencyArtifactId]
  }

}
