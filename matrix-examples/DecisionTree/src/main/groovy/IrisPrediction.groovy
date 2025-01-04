import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.Randomize

def iris = Dataset.iris()

iris = Randomize.randomOrder(iris)
