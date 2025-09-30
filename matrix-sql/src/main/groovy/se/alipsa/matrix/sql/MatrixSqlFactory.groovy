package se.alipsa.matrix.sql

import groovy.transform.CompileStatic
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.resolver.MavenRepoLookup

@CompileStatic
class MatrixSqlFactory {

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
        System.err.println("Failed to fetch latest H2 artifact, falling back to version $dependencyVersion, " + e)
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
        System.err.println("Failed to fetch latest Derby artifact, falling back to version $dependencyVersion, " + e)
      }
    } else {
      dependencyVersion = version
    }
    ci.setDependency("org.apache.derby:derby:$dependencyVersion;org.apache.derby:derbytools:$dependencyVersion;org.apache.derby:derbyshared:$dependencyVersion")
    ci.setDriver("org.apache.derby.jdbc.EmbeddedDriver")
    ci.setUrl("jdbc:derby:$dbName;create=true")
    return new MatrixSql(ci)
  }
}
