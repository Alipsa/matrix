import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.core.JoinType
import se.alipsa.matrix.core.JoinCardinality
import se.alipsa.matrix.smile.data.SmileFeatures

def data = Matrix.builder().data(a: [null, 2]).types(Integer).build()
assert SmileFeatures.fillna(data, 'a', 1)['a'] == [1, 2]
assert SmileFeatures.dropna(data)['a'] == [2]
assert SmileFeatures.dropna(data, ['a'])['a'] == [2]
def amounts = Matrix.builder().data(amount: [null, 1.25]).types(BigDecimal).build()
assert amounts.fillNulls([amount: 0]).column('amount').get(0) instanceof BigDecimal
assert SmileFeatures.fillna(amounts, 'amount', 0).column('amount').get(0) instanceof Integer
try {
  Stat.min([[200], ['A']], [0])
  assert false
} catch (IllegalArgumentException expected) {
  assert expected.message.contains('String')
}
def duplicates = Matrix.builder().data(id: [10, 10]).types(Integer).build()
try {
  duplicates.merge(duplicates, 'id', JoinType.INNER, JoinCardinality.ONE_TO_ONE)
  assert false
} catch (IllegalArgumentException expected) {
  assert expected.message == 'Duplicate key [10] on left input violates ONE_TO_ONE'
}
println 'Published dependency fillna/dropna and review regression smoke tests passed'
