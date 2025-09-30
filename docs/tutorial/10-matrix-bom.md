# Matrix BOM Module (Bill of Materials)

The Matrix BOM (Bill of Materials) module provides a convenient way to manage dependencies across the various Matrix library modules. Since Matrix modules are released separately, their version numbers don't always align with each other. This can make it challenging to determine which versions work well together. The BOM module solves this problem by defining compatible versions in a dependency management section.

## What is a Bill of Materials (BOM)?

A Bill of Materials (BOM) is a special kind of Maven POM (Project Object Model) file that provides version management for a set of related libraries. It allows you to:

1. Specify a single version for the BOM
2. Omit version numbers when declaring individual dependencies
3. Ensure all dependencies use compatible versions

This approach simplifies dependency management and helps avoid version conflicts.

## Installation

### Gradle Configuration

To use the Matrix BOM in a Gradle project, add the following to your build script:

```groovy
implementation(platform('se.alipsa.matrix:matrix-bom:2.2.0'))
implementation('se.alipsa.matrix:matrix-core')
implementation('se.alipsa.matrix:matrix-spreadsheet')
// Add other matrix modules as needed without specifying versions
```

The `platform` keyword tells Gradle to use the BOM for dependency management. After that, you can declare 
Matrix dependencies without specifying their versions.

### Maven Configuration

To use the Matrix BOM in a Maven project, add the following to your `pom.xml` file:

```xml
<project>
   ...
   <dependencyManagement>
      <dependencies>
         <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-bom</artifactId>
            <version>2.2.0</version>
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
      <!-- Add other matrix modules as needed without specifying versions -->
   </dependencies>
   ...
</project>
```

The `dependencyManagement` section imports the BOM, and then you can declare Matrix dependencies in the `dependencies` section without specifying their versions.

## Benefits of Using the Matrix BOM

Using the Matrix BOM offers several advantages:

1. **Simplified Dependency Management**: You only need to specify the BOM version once, rather than managing versions for each Matrix module.

2. **Compatibility Assurance**: The BOM ensures that you're using compatible versions of Matrix modules that have been tested together.

3. **Easier Upgrades**: When upgrading, you only need to change the BOM version, and all dependencies will be updated to compatible versions.

4. **Reduced Risk of Version Conflicts**: The BOM helps prevent version conflicts that can occur when different modules require different versions of shared dependencies.

## Example: Using Multiple Matrix Modules

Here's an example of how to use the Matrix BOM to manage dependencies for a project that uses multiple Matrix modules:

### Gradle Example

```groovy
plugins {
    id 'groovy'
}

repositories {
    mavenCentral()
}

dependencies {
    // Import the BOM
    implementation(platform('se.alipsa.matrix:matrix-bom:2.2.0'))
    
    // Add Groovy
    implementation 'org.apache.groovy:groovy:5.0.1'
    
    // Add Matrix modules without specifying versions
    implementation 'se.alipsa.matrix:matrix-core'
    implementation 'se.alipsa.matrix:matrix-stats'
    implementation 'se.alipsa.matrix:matrix-csv'
    implementation 'se.alipsa.matrix:matrix-spreadsheet'
    implementation 'se.alipsa.matrix:matrix-json'
    implementation 'se.alipsa.matrix:matrix-xchart'
    implementation 'se.alipsa.matrix:matrix-sql'
}
```

### Maven Example

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>matrix-example</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <groovy.version>5.0.1</groovy.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>se.alipsa.matrix</groupId>
                <artifactId>matrix-bom</artifactId>
                <version>2.2.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <dependencies>
        <!-- Groovy -->
        <dependency>
            <groupId>org.apache.groovy</groupId>
            <artifactId>groovy</artifactId>
            <version>${groovy.version}</version>
        </dependency>
        
        <!-- Matrix modules without versions -->
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-core</artifactId>
        </dependency>
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-stats</artifactId>
        </dependency>
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-csv</artifactId>
        </dependency>
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-spreadsheet</artifactId>
        </dependency>
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-json</artifactId>
        </dependency>
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-xchart</artifactId>
        </dependency>
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-sql</artifactId>
        </dependency>
    </dependencies>
</project>
```

## Available Matrix Modules in the BOM

The Matrix BOM includes version management for the following modules:

- `matrix-core`: The core Matrix functionality
- `matrix-stats`: Statistical functions and utilities
- `matrix-datasets`: Common datasets for data science
- `matrix-spreadsheet`: Excel/OpenOffice integration
- `matrix-csv`: CSV file handling
- `matrix-json`: JSON serialization and deserialization
- `matrix-xchart`: Chart creation using XChart
- `matrix-sql`: Database interaction
- `matrix-parquet`: Parquet file format support (experimental)
- `matrix-bigquery`: Google BigQuery integration (experimental)
- `matrix-charts`: Chart creation in various formats (experimental)
- `matrix-tablesaw`: Interoperability with Tablesaw (experimental)

## Choosing the Right BOM Version

When selecting a BOM version, consider the following:

1. **Latest Stable Version**: Generally, you should use the latest stable version of the BOM for new projects.

2. **Compatibility Requirements**: If your project has specific compatibility requirements, check the BOM's documentation to find a version that meets your needs.

3. **Experimental Modules**: If you're using experimental modules (i.e. the version number is < 1.0.0), be aware that they might have more frequent updates or breaking changes.

## Best Practices

Here are some best practices for using the Matrix BOM:

1. **Always Use the BOM**: Even if you're only using one Matrix module, it's a good practice to use the BOM for future-proofing your project.

2. **Keep the BOM Updated**: Regularly check for updates to the BOM to benefit from bug fixes and new features.

3. **Test After Upgrading**: After upgrading the BOM version, thoroughly test your application to ensure compatibility.

4. **Specify Explicit Versions When Needed**: If you need a specific version of a module that differs from what the BOM provides, you can override it by explicitly specifying the version in your dependency declaration.

## Conclusion

The Matrix BOM module simplifies dependency management for projects that use multiple Matrix modules. By using the BOM, you can ensure that all modules are compatible with each other and reduce the complexity of managing version numbers.

In the next section, we'll explore the matrix-parquet module, which provides support for the Apache Parquet file format.

Go to [previous section](9-matrix-sql.md) | Go to [next section](11-matrix-parquet.md) | Back to [outline](outline.md)