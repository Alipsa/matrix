package se.alipsa.matrix.sql

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.resolver.MavenRepoLookup

class MatrixSqlFactory {

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
            .fetchLatestArtifact('com.h2database', 'h2', "https://repo1.maven.org/maven2/")
            .getVersion()
      } catch (Exception e) {
        dependencyVersion = '2.3.232'
        System.err.println("Failed to fetch latest artifact, falling back to version $dependencyVersion, " + e)
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

}
