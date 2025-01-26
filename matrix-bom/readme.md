# Matrix-bom (Bill of Materials)

Since matrix modules are release separately, the version numbers of the matrix modules does not align with each other.
This requires som research (reading of readme.md files) to figure out which versions works well with each other.
so a way to handle this in a simpler way is to use the bom file which defines the versions that works best together
in a dependency management section.

The point is that, using the bom, you only need to define the version for the bom and not when declaring dependencies.

An example for matrix-core is as follows for Gradle
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:1.0.0'))
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
            <version>1.0.0</version>
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