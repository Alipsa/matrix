# Matrix-Tablesaw

Note: this module is work in progress, no release version has been published yet.

Interoperability between Tablesaw and Matrix as well as
various extension to Tablesaw such as BigDecimalColumn,
GTable (which makes Tablesaw Groovier) and
complementary operations to deal with Tablesaw data, e.g. the ability to create frequency tables,
Normalize tablesaw columns etc.

See [test.alipsa.groovy.datautil.TableUtilTest](https://github.com/perNyfelt/data-utils/blob/master/src/test/groovy/test/alipsa/groovy/datautil/TableUtilTest.groovy)
for usage examples!

## Version history
- moved from data-utils 1.0.5-SNAPSHOT
- add putAt method in GTable allowing the shorthand syntax `table[0,1] = 12` and `table[0, 'columnName'] = 'foo'` to change data.
- add possibility to cast a GTable to a Grid