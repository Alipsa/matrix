package it

import se.alipsa.groovy.datautil.DataBaseProvider

class H2Test extends AbstractDbTest {


  H2Test() {
    super(DataBaseProvider.H2, 'h2Test', 'H2')
  }
}
