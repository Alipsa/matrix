package se.alipsa.matrix.sql

import groovy.transform.CompileStatic
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.SqlUtil
import se.alipsa.groovy.resolver.MavenRepoLookup
import se.alipsa.matrix.core.util.Logger

@CompileStatic
class MatrixSqlFactory {

  private static final Logger log = Logger.getLogger(MatrixSqlFactory)
  static String REPO_URL = "https://repo1.maven.org/maven2/"
  static MatrixSql createH2(String url, String user, String password, String additionalUrlProperties = null, String version = null) {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDriver("org.h2.Driver")
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
        dependencyVersion = MavenRepoLookup
            .fetchLatestArtifact('com.h2database', 'h2', REPO_URL)
            .getVersion()
      } catch (Exception e) {
        dependencyVersion = '2.4.240'
        log.warn("Failed to fetch latest H2 artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
    } else {
      dependencyVersion = version
    }
    ci.setDependency("com.h2database:h2:$dependencyVersion")
    return new MatrixSql(ci)
  }

  static MatrixSql createH2(File dbFile, String user, String password, String additionalUrlProperties = null, String version = null) {
    //jdbc:h2:file:/Users/pernyf/project/analysis/Overdraft/overdraft.db
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
        dependencyVersion = MavenRepoLookup
            .fetchLatestArtifact('org.apache.derby', 'derby', REPO_URL)
            .getVersion()
      } catch (Exception e) {
        dependencyVersion = '10.17.1.0'
        log.warn("Failed to fetch latest Derby artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
    } else {
      dependencyVersion = version
    }
    ci.setDependency("org.apache.derby:derby:$dependencyVersion;org.apache.derby:derbytools:$dependencyVersion;org.apache.derby:derbyshared:$dependencyVersion")
    ci.setDriver("org.apache.derby.jdbc.EmbeddedDriver")
    ci.setUrl("jdbc:derby:$dbName;create=true")
    return new MatrixSql(ci)
  }

  /**
   * Gues the dependency based on the url.
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
   * Gues the dependency based on the url.
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
   * Gues the dependency based on the url.
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
    if (dependency == null) {
      throw new RuntimeException("Failed to find a suitable dependency for $ci.url")
    }
    String dependencyVersion = version
    if (dependencyVersion == null) {
      try {
        dependencyVersion = MavenRepoLookup
            .fetchLatestArtifact(dependency.groupId, dependency.artifactId, REPO_URL)
            .getVersion()
      } catch (Exception e) {
        throw new RuntimeException ("Failed to fetch latest artifact for $dependency.groupId:$dependency.artifactId", e)
      }
    }
    ci.setDependency("$dependency.groupId:$dependency.artifactId:$dependencyVersion")
    return new MatrixSql(ci)
  }

  static Map<String, String> getDependencyName(String url) {
    if (url == null) {
      return null
    }

    // Find the first DataBaseProvider whose urlStart matches the beginning of the URL.
    DataBaseProvider matchingProvider = DataBaseProvider.values().find { DataBaseProvider it ->
      url.startsWith(it.urlStart)
    } as DataBaseProvider

    return matchingProvider?.with { mapDependency(it) } ?: null
  }

  static Map<String, String> mapDependency(DataBaseProvider provider) {
    return [groupId: provider.dependencyGroupId, artifactId: provider.dependencyArtifactId]
  }
}
