# Matrix-Tablesaw

Provides interoperability between Tablesaw and Matrix as well as
various extensions to Tablesaw such as BigDecimalColumn,
GTable (which makes Tablesaw Groovier) and
complementary operations to deal with Tablesaw data, e.g. the ability to create frequency tables,
Normalize tablesaw columns etc.

To use it add the following to your gradle build script (or equivalent for maven etc)
```groovy
implementation 'org.apache.groovy:groovy:4.0.27'
implementation 'se.alipsa.matrix:matrix-core:3.4.1'
implementation 'se.alipsa.matrix:matrix-tablesaw:0.2.1'
```

See The [tutorial section on Tablesaw](../docs/tutorial/14-matrix-tablesaw.md) and [test.alipsa.groovy.datautil.TableUtilTest](https://github.com/perNyfelt/data-utils/blob/master/src/test/groovy/test/alipsa/groovy/datautil/TableUtilTest.groovy)
for usage examples!


