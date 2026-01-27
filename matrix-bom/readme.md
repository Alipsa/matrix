[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-bom/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-bom)
# Matrix-bom (Bill of Materials)
Since matrix modules are release separately, the version numbers of the matrix modules does not align with each other.
This requires som research (reading of readme.md files) to figure out which versions works well with each other.
so a way to handle this in a simpler way is to use the bom file which defines the versions that works best together
in a dependency management section.

The point is that, using the bom, you only need to define the version for the bom and not when declaring dependencies.

An example for matrix-core is as follows for Gradle
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:2.2.3'))
implementation('se.alipsa.matrix:matrix-core')
implementation('se.alipsa.matrix:matrix-spreadsheet')
```
...or the following for maven
```xml
<project>
   ...
   <dependencyManagement>
      <dependencies>
         <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-bom</artifactId>
            <version>2.2.3</version>
            <type>pom</type>
            <scope>import</scope>
         </dependency>
      </dependencies>
   </dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>se.alipsa.matrix</groupId>
         <artifactId>matrix-core</artifactId>
      </dependency>
       <dependency>
           <groupId>se.alipsa.matrix</groupId>
           <artifactId>matrix-spreadsheet</artifactId>
       </dependency>
       <!-- etc. etc. -->
   </dependencies>
   ...
</project>
```

## Matrix-all (convenience jar)

If you want a single dependency that pulls in all Matrix modules with their transitive dependencies, use
the `matrix-all` convenience jar. This is simpler than a BOM import, but less flexible.

Note: `matrix-all` does not include a Groovy runtime on purpose, so you can choose your Groovy
version (4.x or 5.x). Add the Groovy dependency explicitly in your build.

Gradle:
```groovy
implementation('org.apache.groovy:groovy-all:4.0.23') // or 5.x if you prefer
implementation('se.alipsa.matrix:matrix-all:2.2.3')
```

Maven:
```xml
<dependency>
  <groupId>org.apache.groovy</groupId>
  <artifactId>groovy-all</artifactId>
  <version>4.0.23</version>
</dependency>
<dependency>
  <groupId>se.alipsa.matrix</groupId>
  <artifactId>matrix-all</artifactId>
  <version>2.2.3</version>
</dependency>
```
