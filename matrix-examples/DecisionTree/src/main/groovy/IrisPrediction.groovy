import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.stats.*

def iris = Dataset.iris()

iris = Randomize.randomOrder(iris)
