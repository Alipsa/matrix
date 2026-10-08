import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.data.SmileFeatures

def data = Matrix.builder().data(a: [null, 2]).types(Integer).build()
assert SmileFeatures.fillna(data, 'a', 1)['a'] == [1, 2]
assert SmileFeatures.dropna(data)['a'] == [2]
assert SmileFeatures.dropna(data, ['a'])['a'] == [2]
println 'Published dependency fillna/dropna smoke test passed'
