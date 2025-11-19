# Matrix-Tablesaw Version history

## v0.2.2, In progress
- com.github.miachm.sods:SODS [1.6.8 -> 1.7.0]
- org.apache.poi:poi [5.4.1 -> 5.5.0]

## v0.2.1, 2025-07-19
- Upgrade dependencies
  - com.github.miachm.sods:SODS [1.6.7 -> 1.6.8]
  - org.apache.poi:poi-ooxml [5.4.0 -> 5.4.1]
  - org.dom4j:dom4j [2.1.4 -> 2.2.0]
  
## v0.2.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-tablesaw/0.2.0/matrix-tablesaw-0.2.0.jar)

- Add BigDecimalAggregateFunctions to GTable
- Add column creation methods to Gtable to enable fluent interaction.
- Add from and toMatrix static factory methods to TableUtil

## v0.1
- moved from data-utils 1.0.5-SNAPSHOT
- add putAt method in GTable allowing the shorthand syntax `table[0,1] = 12` and `table[0, 'columnName'] = 'foo'` to change data.
- add possibility to cast a GTable to a Grid