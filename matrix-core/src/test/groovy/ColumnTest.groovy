import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Stat

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class ColumnTest {

  @Test
  void testPlusList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 + [1, 2, 3, 4] == [2, 4, 6, 8]
    // smaller
    assert c1 + [1, 2] == [2, 4, null, null]
    // larger
    assert c1 + [1, 2, 3, 4, 5, 6] == [2, 4, 6, 8]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 + [1, 2, 3, 4] == [2, null, 6, 8]
    // smaller
    assert c2 + [1, 2] == [2, null, null, null]
    // larger
    assert c2 + [1, 2, 3, 4, 5, 6] == [2, null, 6, 8]
  }

  @Test
  void testPlusObject() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1 + 1 == [2, 3, 4, 5]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 + 2 == [3, null, 5, 6]

    Column s1 = new Column(['1', '2', '3', '4'])
    assert s1 + 'px' == ['1px', '2px', '3px', '4px']

    Column s2 = new Column(['1', '2', null, '4'])
    assert s2 + 'px' == ['1px', '2px', null, '4px']
  }

  @Test
  void testMinusList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 - [1, 2, 3, 4] == [0, 0, 0, 0]
    // smaller
    assert c1 - [0, 1] == [1, 1, null, null]
    // larger
    assert c1 - [-1, 2, 2.1, 4.2, 5, 6] == [2, 0, 0.9, -0.2]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 - [1, 0, -1, -1.1] == [0, null, 4, 5.1]
    // smaller
    assert c2 - [2, 1] == [-1, null, null, null]
    // larger
    assert c2 - [1, 2, 3, 4, 5, 6] == [0, null, 0, 0]
  }

  @Test
  void testMinusObject() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1 - 1 == [0, 1, 2, 3]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 - 2.5 == [-1.5, null, 0.5, 1.5]

    Column s1 = new Column(['1', '2', '3', '4'])
    assert s1 - '2' == ['1', '', '3', '4']

    Column s2 = new Column(['1', '2', null, '4'])
    assert s2 - '1' == ['', '2', null, '4']
  }

  @Test
  void testMultiplyList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 * [1, 2, 3, 4] == [1, 4, 9, 16]
    // smaller
    assert c1 * [1, 2] == [1, 4, null, null]
    // larger
    assert c1 * [1, 2, 3, 4, 5, 6] == [1, 4, 9, 16]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 * [1, 2, 3, 4] == [1, null, 9, 16]
    // smaller
    assert c2 * [1, 2] == [1, null, null, null]
    // larger
    assert c2 * [1, 2, 3, 4, 5, 6] == [1, null, 9, 16]
  }

  @Test
  void testMultiplyNumber() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1 * 2 == [2, 4, 6, 8]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 * 2 == [2, null, 6, 8]
  }

  @Test
  void testDivList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 / [1, 1, 2, 0.5] == [1, 2, 1.5, 8]
    // smaller
    assert c1 / [2, 1] == [0.5, 2, null, null]
    // larger
    assert c1 / [1, 2, 0.5, 10, 11, 12] == [1, 1, 6, 0.4]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 / [2, 2, 1, 2] == [0.5, null, 3, 2]
    // smaller
    assert c2 / [1, 2] == [1, null, null, null]
    // larger
    assert c2 / [0.5, 2, 2, 2, 1, 60] == [2, null, 1.5, 2]
  }

  @Test
  void testDivNumber() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1 / 2 ==  [0.5, 1, 1.5, 2]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 / 2 == [0.5, null, 1.5, 2]
  }

  @Test
  void testPowerList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 ** [1, 2, 3, 4] == [1, 4, 27, 256]
    // smaller
    assert c1 ** [1, 2] == [1, 4, null, null]
    // larger
    assert c1 ** [1, 2, 3, 4, 5, 6] == [1, 4, 27, 256]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 ** [1, 2, 3, 4] == [1, null, 27, 256]
    // smaller
    assert c2 ** [1, 2] == [1, null, null, null]
    // larger
    assert c2 ** [1, 2, 3, 4, 5, 6] == [1, null, 27, 256]
  }

  @Test
  void testPowerNumber() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1 ** 2 == [1, 4, 9, 16]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 ** 2 == [1, null, 9, 16]
  }

  @Test
  void testLeftShift() {
    Column c1 = new Column([1, 2, 3, 4])
    c1 << 5
    assert [1,2,3,4,5] == c1
    c1 << [6,7]
    assert [1,2,3,4,5,6,7] == c1
  }

  @Test
  void testAsType() {
    Column c = [1,2,3] as Column
    assert [1,2,3] == c

    List<Number> l = []
    l.add(1)
    l.add(2)
    l.add(1)
    Column col = l as Column
    assert [2,4,4] == col + c
  }

  @Test
  void testSubList() {
    Column c = [1,2,3,4] as Column
    assert [1,2,3] == c.subList(0..2)
  }

  @Test
  void testUnique() {
    Column c = new Column([1, 2, 3, 4, 1, 2, 3])
    assert [1, 2, 3, 4] == c.unique() : "unique() should return a new Column with unique values"
    assert new Column([1, 2, 3, 4, 1, 2, 3]) == c : "unique() should not mutate the original Column"
  }

  @Test
  void testCollectionMaths() {
    Column c = new Column([1, 2, 3, 4, 1, 2, 3])
    assert 4 == c.max()
    assert 1 == c.min()
    assert 2 == c.median()
    assert 2.2857142857142857 == c.mean()
    assert 1.112697280528374 == c.sd()
    assert Stat.variance(c) == c.variance()
  }

  @Test
  void testRemoveNulls() {
    Column c = new Column([1, null, 3, null, 5])
    Column noNulls = c.removeNulls()
    assert [1,3,5] == noNulls
    assert [1, null, 3, null, 5] == c // original should be unchanged
  }

  @Test
  void testReplaceNulls() {
    Column c = new Column([1, null, 3, null, 5])
    Column result = c.replaceNulls(0)
    assert [1, 0, 3, 0, 5] == c : "replaceNulls() should mutate in place"
    assert result.is(c) : "replaceNulls() should return the same column instance"
  }

  @Test
  void testReplace() {
    Column c = new Column([1, 2, 3, 2, 5])
    Column result = c.replace(2, 99)
    assert [1, 99, 3, 99, 5] == c : "replace() should mutate in place"
    assert result.is(c) : "replace() should return the same column instance"
  }

  @Test
  void testNullScalarOperations() {
    Column c = new Column([1, 2, 3])

    assertThrows(IllegalArgumentException) { c + null }
    assertThrows(IllegalArgumentException) { c - null }
    assertThrows(IllegalArgumentException) { c * null }
    assertThrows(IllegalArgumentException) { c / null }
    assertThrows(IllegalArgumentException) { c ** null }
  }

  @Test
  void testRollingMeanUsesMinPeriodsAndSkipsNulls() {
    Column c = new Column('value', [1, null, 3, 4, 5], Integer)

    Column result = c.rolling(window: 3, minPeriods: 2).mean()

    assert [null, null, 2.0, 3.5, 4.0] == result
    assert 'value' == result.name
    assert BigDecimal == result.type
  }

  @Test
  void testRollingSumMaxAndSdHandleNullsAndThresholds() {
    Column c = new Column('value', [1, null, 3, 4, 5], Integer)

    Column sum = c.rolling(window: 3, minPeriods: 2).sum()
    Column max = c.rolling(window: 3, minPeriods: 2).max()
    Column sd = c.rolling(window: 3, minPeriods: 2).sd()

    assert [null, null, 4, 7, 12] == sum
    assert [null, null, 3, 4, 5] == max
    assert sd[0] == null
    assert sd[1] == null
    assertEquals(1.4142135623730951d, sd[2] as double, 1.0e-12d)
    assertEquals(0.7071067811865476d, sd[3] as double, 1.0e-12d)
    assertEquals(1.0d, sd[4] as double, 1.0e-12d)
    assert BigDecimal == sum.type
    assert Integer == max.type
    assert BigDecimal == sd.type
  }

  @Test
  void testRollingCenteredApplyReceivesWindowSlice() {
    Column c = new Column('value', [1, 2, 3, 4, 5], Integer)

    Column result = c.rolling(window: 3, minPeriods: 2, center: true).apply { Column window ->
      window.first() + window.last()
    }

    assert [3, 4, 6, 8, 9] == result
    assert Object == result.type
  }

  @Test
  void testRollingMinSupportsComparableColumns() {
    Column c = new Column('grade', ['c', null, 'a', 'b'], String)

    Column result = c.rolling(window: 2, minPeriods: 1).min()

    assert ['c', 'c', 'a', 'a'] == result
    assert String == result.type
  }

  @Test
  void testRollingRejectsInvalidConfigAndNonNumericMean() {
    Column strings = new Column('label', ['a', 'b', 'c'], String)

    assertThrows(IllegalArgumentException) {
      strings.rolling([:])
    }
    assertThrows(IllegalArgumentException) {
      strings.rolling(window: 2, minPeriods: 3)
    }
    assertThrows(IllegalArgumentException) {
      strings.rolling(window: 2.5)
    }
    assertThrows(IllegalArgumentException) {
      strings.rolling(window: 2, minPeriods: 1.5)
    }
    assertThrows(IllegalArgumentException) {
      strings.rolling([(1): 2])
    }
    Map options = ["${'window'}": 2, "${'minPeriods'}": 1]
    assert ['a', 'a', 'b'] == strings.rolling(options).min()
    assertThrows(IllegalArgumentException) {
      strings.rolling(window: 2).mean()
    }
  }

  @Test
  void testCumsumBasic() {
    Column c = new Column('value', [1, 2, 3, 4], Integer)
    Column result = c.cumsum()

    assert [1, 3, 6, 10] == result
    assert 'value' == result.name
    assert BigDecimal == result.type
  }

  @Test
  void testCumsumWithNulls() {
    Column c = new Column('value', [1, null, 3, 4], Integer)
    assert [1, null, 4, 8] == c.cumsum()

    Column allNulls = new Column('value', [null, null, null], Integer)
    assert [null, null, null] == allNulls.cumsum()

    Column leadingNulls = new Column('value', [null, null, 5], Integer)
    assert [null, null, 5] == leadingNulls.cumsum()
  }

  @Test
  void testCumsumEmptyAndSingleElement() {
    Column empty = new Column('value', [], Integer)
    assert [] == empty.cumsum()

    Column emptyStrings = new Column('label', [], String)
    assertThrows(IllegalArgumentException) { emptyStrings.cumsum() }
    assertThrows(IllegalArgumentException) { emptyStrings.cumprod() }

    Column single = new Column('value', [7], Integer)
    assert [7] == single.cumsum()
  }

  @Test
  void testCumprodBasic() {
    Column c = new Column('value', [1, 2, 3, 4], Integer)
    Column result = c.cumprod()

    assert [1, 2, 6, 24] == result
    assert 'value' == result.name
    assert BigDecimal == result.type
  }

  @Test
  void testCumprodWithNulls() {
    Column c = new Column('value', [2, null, 3, 4], Integer)
    assert [2, null, 6, 24] == c.cumprod()

    Column allNulls = new Column('value', [null, null, null], Integer)
    assert [null, null, null] == allNulls.cumprod()
  }

  @Test
  void testCumminBasic() {
    Column c = new Column('value', [3, 1, 4, 1, 5], Integer)
    Column result = c.cummin()

    assert [3, 1, 1, 1, 1] == result
    assert 'value' == result.name
    assert Integer == result.type
  }

  @Test
  void testCumminWithNullsAndStrings() {
    Column c = new Column('value', [3, null, 1, 4], Integer)
    assert [3, null, 1, 1] == c.cummin()

    Column strings = new Column('label', ['c', 'a', 'b'], String)
    assert ['c', 'a', 'a'] == strings.cummin()
  }

  @Test
  void testCummaxBasic() {
    Column c = new Column('value', [1, 3, 2, 5, 4], Integer)
    Column result = c.cummax()

    assert [1, 3, 3, 5, 5] == result
    assert 'value' == result.name
    assert Integer == result.type
  }

  @Test
  void testCummaxWithNullsAndStrings() {
    Column c = new Column('value', [1, null, 5, 3], Integer)
    assert [1, null, 5, 5] == c.cummax()

    Column strings = new Column('label', ['a', 'c', 'b'], String)
    assert ['a', 'c', 'c'] == strings.cummax()
  }

  @Test
  void testCumminCummaxHandleMixedNumericTypes() {
    Column mixedNumbers = new Column('value', [2, 1.5G, 3L, 0.5G], Object)

    assert [2, 1.5G, 1.5G, 0.5G] == mixedNumbers.cummin()
    assert [2, 2, 3L, 3L] == mixedNumbers.cummax()
  }

  @Test
  void testCumminCummaxRejectNonComparableMixedValues() {
    Column mixedValues = new Column('value', [1, 'a', 2], Object)

    def minError = assertThrows(IllegalArgumentException) { mixedValues.cummin() }
    assert "cummin requires mutually comparable values within column 'value'" == minError.message

    def maxError = assertThrows(IllegalArgumentException) { mixedValues.cummax() }
    assert "cummax requires mutually comparable values within column 'value'" == maxError.message
  }

  @Test
  void testCumsumCumprodRejectNonNumericColumn() {
    Column strings = new Column('label', ['a', 'b', 'c'], String)

    assertThrows(IllegalArgumentException) { strings.cumsum() }
    assertThrows(IllegalArgumentException) { strings.cumprod() }
  }
}
