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

    Column s1 = new Column(['1', '2', '3'])
    assert s1 + ['px', 'em', 'rem'] == ['1px', '2em', '3rem']
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

    Column s1 = new Column(['100px', '2rem', 'fit-content'])
    assert s1 - ['px', 'rem', 'fit-'] == ['100', '2', 'content']
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
  void testLeftShiftCollectionChaining() {
    Column column = [] as Column

    Column result = column << [1, 2] << [3, 4]

    assert result.is(column)
    assert [1, 2, 3, 4] == column
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
    assertThrows(IllegalArgumentException) {
      strings.rolling(window: 2).mean()
    }
  }

  @SuppressWarnings('GStringAsMapKey')
  @Test
  void testRollingSupportsGStringOptionKeys() {
    Column strings = new Column('label', ['a', 'b', 'c'], String)
    Map options = ["${'window'}": 2, "${'minPeriods'}": 1]

    assert ['a', 'a', 'b'] == strings.rolling(options).min()
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

  @Test
  void testCumsumCumprodRejectStrayNonNumericValuesInNumericColumn() {
    Column mixed = new Column('value', [1, 'oops', 3], Integer)

    def sumError = assertThrows(IllegalArgumentException) { mixed.cumsum() }
    assert "cumsum requires numeric values within column 'value' but found String" == sumError.message

    def prodError = assertThrows(IllegalArgumentException) { mixed.cumprod() }
    assert "cumprod requires numeric values within column 'value' but found String" == prodError.message
  }

  @Test
  void testCumulativeNumericOpsRejectNaNAndInfinity() {
    Column nanValues = new Column('value', [1.0d, Double.NaN, 3.0d], Double)
    Column infinityValues = new Column('value', [1.0d, Double.POSITIVE_INFINITY, 3.0d], Double)

    assert "cumsum requires finite numeric values within column 'value' but found NaN" ==
        assertThrows(IllegalArgumentException) { nanValues.cumsum() }.message
    assert "cumprod requires finite numeric values within column 'value' but found Infinity" ==
        assertThrows(IllegalArgumentException) { infinityValues.cumprod() }.message
    assert "cummin requires finite numeric values within column 'value' but found Infinity" ==
        assertThrows(IllegalArgumentException) { infinityValues.cummin() }.message
    assert "cummax requires finite numeric values within column 'value' but found NaN" ==
        assertThrows(IllegalArgumentException) { nanValues.cummax() }.message
  }

  @Test
  void testShiftForward() {
    Column c = new Column('value', [1, 2, 3, 4, 5], Integer)
    assert [null, null, 1, 2, 3] == c.shift(2)
  }

  @Test
  void testShiftBackward() {
    Column c = new Column('value', [1, 2, 3, 4, 5], Integer)
    assert [3, 4, 5, null, null] == c.shift(-2)
  }

  @Test
  void testShiftZero() {
    Column c = new Column('value', [1, 2, 3], Integer)
    assert [1, 2, 3] == c.shift(0)
  }

  @Test
  void testShiftLargerThanSize() {
    Column c = new Column('value', [1, 2], Integer)
    assert [null, null] == c.shift(5)
    assert [null, null] == c.shift(-5)
  }

  @Test
  void testShiftWithNulls() {
    Column c = new Column('value', [1, null, 3, 4], Integer)
    assert [null, 1, null, 3] == c.shift(1)
  }

  @Test
  void testShiftEmptyColumn() {
    Column empty = new Column('value', [], Integer)
    assert [] == empty.shift(1)
  }

  @Test
  void testShiftPreservesNameAndType() {
    Column c = new Column('price', [10, 20, 30], Integer)
    Column result = c.shift(1)
    assert 'price' == result.name
    assert Integer == result.type
  }

  @Test
  void testShiftWithStrings() {
    Column c = new Column('label', ['a', 'b', 'c'], String)
    assert [null, 'a', 'b'] == c.shift(1)
    assert ['b', 'c', null] == c.shift(-1)
  }

  @Test
  void testLagBasic() {
    Column c = new Column('value', [1, 2, 3, 4], Integer)
    assert [null, 1, 2, 3] == c.lag(1)
    assert [null, null, 1, 2] == c.lag(2)
  }

  @Test
  void testLeadBasic() {
    Column c = new Column('value', [1, 2, 3, 4], Integer)
    assert [2, 3, 4, null] == c.lead(1)
    assert [3, 4, null, null] == c.lead(2)
  }

  @Test
  void testLagRejectsNegative() {
    Column c = new Column('value', [1, 2, 3], Integer)
    assertThrows(IllegalArgumentException) { c.lag(-1) }
  }

  @Test
  void testLeadRejectsNegative() {
    Column c = new Column('value', [1, 2, 3], Integer)
    assertThrows(IllegalArgumentException) { c.lead(-1) }
  }

  @Test
  void testShiftAndDiffRejectIntegerMinValue() {
    Column c = new Column('value', [1, 2, 3], Integer)

    assert "shift does not support periods == Integer.MIN_VALUE" ==
        assertThrows(IllegalArgumentException) { c.shift(Integer.MIN_VALUE) }.message
    assert "diff does not support periods == Integer.MIN_VALUE" ==
        assertThrows(IllegalArgumentException) { c.diff(Integer.MIN_VALUE) }.message
  }

  @Test
  void testLagEqualsShift() {
    Column c = new Column('value', [1, 2, 3, 4, 5], Integer)
    assert c.lag(2) == c.shift(2)
  }

  @Test
  void testLeadEqualsShiftNegative() {
    Column c = new Column('value', [1, 2, 3, 4, 5], Integer)
    assert c.lead(2) == c.shift(-2)
  }

  @Test
  void testDiffBasic() {
    Column c = new Column('value', [1, 3, 6, 10], Integer)
    Column result = c.diff()

    assert [null, 2, 3, 4] == result
    assert 'value' == result.name
    assert BigDecimal == result.type
  }

  @Test
  void testDiffPeriods2() {
    Column c = new Column('value', [1, 3, 6, 10], Integer)
    assert [null, null, 5, 7] == c.diff(2)
  }

  @Test
  void testDiffNegativePeriods() {
    Column c = new Column('value', [1, 3, 6, 10], Integer)
    assert [-2, -3, -4, null] == c.diff(-1)
  }

  @Test
  void testDiffWithNulls() {
    Column c = new Column('value', [1, null, 3, 4], Integer)
    assert [null, null, null, 1] == c.diff()
  }

  @Test
  void testDiffEmptyAndSingleElement() {
    Column empty = new Column('value', [], Integer)
    assert [] == empty.diff()

    Column single = new Column('value', [5], Integer)
    assert [null] == single.diff()
  }

  @Test
  void testDiffRejectsNonNumericColumn() {
    Column strings = new Column('label', ['a', 'b', 'c'], String)
    assertThrows(IllegalArgumentException) { strings.diff() }
  }

  @Test
  void testDiffRejectsNaNAndInfinity() {
    Column nanValues = new Column('value', [1.0d, Double.NaN, 3.0d], Double)
    Column infinityValues = new Column('value', [1.0d, Double.POSITIVE_INFINITY, 3.0d], Double)

    assert "diff requires finite numeric values within column 'value' but found NaN" ==
        assertThrows(IllegalArgumentException) { nanValues.diff() }.message
    assert "diff requires finite numeric values within column 'value' but found Infinity" ==
        assertThrows(IllegalArgumentException) { infinityValues.diff() }.message
  }

  // --- 1.4 Edge-case tests: Rolling ---

  @Test
  void testRollingOnEmptyColumn() {
    Column empty = new Column('value', [], Integer)
    assert [] == empty.rolling(window: 2, minPeriods: 1).mean()
    assert [] == empty.rolling(window: 2, minPeriods: 1).sum()
    assert [] == empty.rolling(window: 2, minPeriods: 1).min()
    assert [] == empty.rolling(window: 2, minPeriods: 1).max()
    assert [] == empty.rolling(window: 2, minPeriods: 1).sd()
    assert [] == empty.rolling(window: 2, minPeriods: 1).apply { it.size() }
  }

  @Test
  void testRollingOnSingleRowColumn() {
    Column single = new Column('value', [5], Integer)
    assert [5.0] == single.rolling(window: 3, minPeriods: 1).mean()
    assert [null] == single.rolling(window: 3, minPeriods: 2).mean()
  }

  @Test
  void testRollingOnAllNullColumn() {
    Column allNulls = new Column('value', [null, null, null], Integer)
    assert [null, null, null] == allNulls.rolling(window: 2, minPeriods: 1).mean()
    assert [null, null, null] == allNulls.rolling(window: 2, minPeriods: 1).sum()
    assert [null, null, null] == allNulls.rolling(window: 2, minPeriods: 1).min()
    assert [null, null, null] == allNulls.rolling(window: 2, minPeriods: 1).max()
    assert [null, null, null] == allNulls.rolling(window: 2, minPeriods: 1).sd()
  }

  @Test
  void testRollingWithWindowOfOne() {
    Column c = new Column('value', [1, 2, 3], Integer)
    assert [1.0, 2.0, 3.0] == c.rolling(window: 1).mean()
    assert [null, null, null] == c.rolling(window: 1).sd()
  }

  @Test
  void testRollingWithWindowLargerThanColumn() {
    Column c = new Column('value', [1, 2, 3], Integer)
    Column sum = c.rolling(window: 10, minPeriods: 1).sum()
    assert [1, 3, 6] == sum
  }

  @Test
  void testRollingApplyRejectsNullClosure() {
    Column c = new Column('value', [1, 2, 3], Integer)
    assertThrows(IllegalArgumentException) { c.rolling(2).apply(null) }
  }

  @Test
  void testRollingIntShorthand() {
    Column c = new Column('value', [1, 2, 3, 4, 5], Integer)
    assert c.rolling(3).mean() == c.rolling(window: 3).mean()
  }

  // --- 1.4 Edge-case tests: Cumulative ---

  @Test
  void testCumprodEmptyAndSingleElement() {
    Column empty = new Column('value', [], Integer)
    assert [] == empty.cumprod()

    Column single = new Column('value', [7], Integer)
    assert [7] == single.cumprod()
  }

  @Test
  void testCumminCummaxEmptyAndSingleElement() {
    Column empty = new Column('value', [], Integer)
    assert [] == empty.cummin()
    assert [] == empty.cummax()

    Column single = new Column('value', [5], Integer)
    assert [5] == single.cummin()
    assert [5] == single.cummax()
  }

  @Test
  void testCumsumBigDecimalPrecision() {
    Column c = new Column('value', [0.1, 0.2, 0.3], BigDecimal)
    assert [0.1, 0.3, 0.6] == c.cumsum()
  }

  @Test
  void testCumprodWithZero() {
    Column c = new Column('value', [2, 3, 0, 5], Integer)
    assert [2, 6, 0, 0] == c.cumprod()
  }

  @Test
  void testCumsumOnNormalDoubleColumn() {
    Column c = new Column('value', [1.0d, 2.0d, 3.0d], Double)
    Column result = c.cumsum()
    assert [1.0, 3.0, 6.0] == result
    assert BigDecimal == result.type
  }

  // --- 1.4 Edge-case tests: Shift/Lag/Lead/Diff ---

  @Test
  void testShiftDefaultParameter() {
    Column c = new Column('value', [1, 2, 3, 4], Integer)
    assert c.shift() == c.shift(1)
  }

  @Test
  void testLagLeadDefaultParameter() {
    Column c = new Column('value', [1, 2, 3, 4], Integer)
    assert c.lag() == c.lag(1)
    assert c.lead() == c.lead(1)
  }

  @Test
  void testDiffDefaultParameter() {
    Column c = new Column('value', [1, 3, 6, 10], Integer)
    assert c.diff() == c.diff(1)
  }

  @Test
  void testDiffAllNullColumn() {
    Column c = new Column('value', [null, null, null], Integer)
    assert [null, null, null] == c.diff()
  }

  @Test
  void testDiffPeriodsLargerThanColumnSize() {
    Column c = new Column('value', [1, 2, 3], Integer)
    assert [null, null, null] == c.diff(10)
  }

  @Test
  void testLagLeadZero() {
    Column c = new Column('value', [1, 2, 3], Integer)
    assert [1, 2, 3] == c.lag(0)
    assert [1, 2, 3] == c.lead(0)
  }

  @Test
  void testPipeBasic() {
    Column c = new Column('value', [1, 2, 3], Integer)
    def result = c.pipe { it.cumsum() }
    assert [1, 3, 6] == result
  }

  @Test
  void testPipeChained() {
    Column c = new Column('value', [1, null, 3], Integer)
    def result = c.pipe { it.removeNulls() }
                  .pipe { it.cumsum() }
    assert [1, 4] == result
  }

  @Test
  void testOrOperator() {
    Column c = new Column('value', [1, null, 3], Integer)
    def result = c | { it.removeNulls() } | { it.cumsum() }
    assert [1, 4] == result
  }

  @Test
  void testOrWithCollectionStillWorks() {
    Column c = new Column('value', [1, 2, 3], Integer)
    def result = c | [3, 4, 5]
    assert result instanceof Collection
    assert result as Set == [1, 2, 3, 4, 5] as Set
    assert result.size() == 5
  }
}
