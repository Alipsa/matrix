package se.alipsa.matrix.smile.data

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Utility class for data splitting operations commonly used in machine learning workflows.
 * Provides train/test splitting and k-fold cross-validation.
 */
@CompileStatic
class SmileData {

  /**
   * Split a matrix into training and test sets.
   *
   * @param matrix the Matrix to split
   * @param testRatio the fraction of data to use for testing (0.0 to 1.0, default 0.2)
   * @param shuffle whether to shuffle before splitting (default true)
   * @return a list containing [trainMatrix, testMatrix]
   */
  static List<Matrix> trainTestSplit(Matrix matrix, double testRatio = 0.2, boolean shuffle = true) {
    return trainTestSplit(matrix, testRatio, shuffle, new Random())
  }

  /**
   * Split a matrix into training and test sets with a specific random seed.
   *
   * @param matrix the Matrix to split
   * @param testRatio the fraction of data to use for testing (0.0 to 1.0)
   * @param shuffle whether to shuffle before splitting
   * @param seed the random seed for reproducibility
   * @return a list containing [trainMatrix, testMatrix]
   */
  static List<Matrix> trainTestSplit(Matrix matrix, double testRatio, boolean shuffle, long seed) {
    return trainTestSplit(matrix, testRatio, shuffle, new Random(seed))
  }

  /**
   * Split a matrix into training and test sets with a specific Random instance.
   *
   * @param matrix the Matrix to split
   * @param testRatio the fraction of data to use for testing (0.0 to 1.0)
   * @param shuffle whether to shuffle before splitting
   * @param random the Random instance to use
   * @return a list containing [trainMatrix, testMatrix]
   */
  static List<Matrix> trainTestSplit(Matrix matrix, double testRatio, boolean shuffle, Random random) {
    if (testRatio <= 0.0 || testRatio >= 1.0) {
      throw new IllegalArgumentException("testRatio must be between 0 and 1 (exclusive): was $testRatio")
    }

    int rowCount = matrix.rowCount()
    if (rowCount < 2) {
      throw new IllegalArgumentException("Matrix must have at least 2 rows for splitting")
    }

    List<Integer> indices = (0..<rowCount).toList()
    if (shuffle) {
      Collections.shuffle(indices, random)
    }

    int testSize = Math.max(1, (int) Math.round(rowCount * testRatio))
    int trainSize = rowCount - testSize

    List<Integer> trainIndices = indices.subList(0, trainSize)
    List<Integer> testIndices = indices.subList(trainSize, rowCount)

    Matrix trainMatrix = createSubMatrix(matrix, trainIndices)
    Matrix testMatrix = createSubMatrix(matrix, testIndices)

    return [trainMatrix, testMatrix]
  }

  /**
   * Split a matrix into training and test sets with named parameters.
   *
   * @param params a map containing optional parameters: testRatio (default 0.2), shuffle (default true), seed (optional)
   * @param matrix the Matrix to split
   * @return a list containing [trainMatrix, testMatrix]
   */
  static List<Matrix> trainTestSplit(Map<String, Object> params, Matrix matrix) {
    double testRatio = params.containsKey('testRatio') ? ((Number) params.testRatio).doubleValue() : 0.2d
    boolean shuffle = params.containsKey('shuffle') ? (params.shuffle as boolean) : true

    if (params.containsKey('seed')) {
      return trainTestSplit(matrix, testRatio, shuffle, params.seed as long)
    }
    return trainTestSplit(matrix, testRatio, shuffle)
  }

  /**
   * Perform k-fold cross-validation split.
   *
   * @param matrix the Matrix to split
   * @param k the number of folds
   * @return a list of k Fold objects, each containing training and validation sets
   */
  static List<Fold> kFold(Matrix matrix, int k) {
    return kFold(matrix, k, true, new Random())
  }

  /**
   * Perform k-fold cross-validation split with optional shuffling.
   *
   * @param matrix the Matrix to split
   * @param k the number of folds
   * @param shuffle whether to shuffle before splitting
   * @return a list of k Fold objects, each containing training and validation sets
   */
  static List<Fold> kFold(Matrix matrix, int k, boolean shuffle) {
    return kFold(matrix, k, shuffle, new Random())
  }

  /**
   * Perform k-fold cross-validation split with specific random seed.
   *
   * @param matrix the Matrix to split
   * @param k the number of folds
   * @param shuffle whether to shuffle before splitting
   * @param seed the random seed for reproducibility
   * @return a list of k Fold objects, each containing training and validation sets
   */
  static List<Fold> kFold(Matrix matrix, int k, boolean shuffle, long seed) {
    return kFold(matrix, k, shuffle, new Random(seed))
  }

  /**
   * Perform k-fold cross-validation split with specific Random instance.
   *
   * @param matrix the Matrix to split
   * @param k the number of folds
   * @param shuffle whether to shuffle before splitting
   * @param random the Random instance to use
   * @return a list of k Fold objects, each containing training and validation sets
   */
  static List<Fold> kFold(Matrix matrix, int k, boolean shuffle, Random random) {
    if (k < 2) {
      throw new IllegalArgumentException("k must be at least 2: was $k")
    }

    int rowCount = matrix.rowCount()
    if (rowCount < k) {
      throw new IllegalArgumentException("Matrix must have at least $k rows for $k-fold cross-validation, but has $rowCount rows")
    }

    List<Integer> indices = (0..<rowCount).toList()
    if (shuffle) {
      Collections.shuffle(indices, random)
    }

    // Calculate fold sizes
    int foldSize = rowCount.intdiv(k) as int
    int remainder = rowCount % k

    List<Fold> folds = []
    int start = 0

    for (int i = 0; i < k; i++) {
      // Distribute remainder across first folds
      int currentFoldSize = foldSize + (i < remainder ? 1 : 0)
      int end = start + currentFoldSize

      List<Integer> validationIndices = indices.subList(start, end)
      List<Integer> trainIndices = []
      trainIndices.addAll(indices.subList(0, start))
      trainIndices.addAll(indices.subList(end, rowCount))

      Matrix trainMatrix = createSubMatrix(matrix, trainIndices)
      Matrix validationMatrix = createSubMatrix(matrix, validationIndices)

      folds << new Fold(i, trainMatrix, validationMatrix)
      start = end
    }

    return folds
  }

  /**
   * Perform k-fold cross-validation split with named parameters.
   *
   * @param params a map containing optional parameters: k (required), shuffle (default true), seed (optional)
   * @param matrix the Matrix to split
   * @return a list of k Fold objects
   */
  static List<Fold> kFold(Map<String, Object> params, Matrix matrix) {
    if (!params.containsKey('k')) {
      throw new IllegalArgumentException("Parameter 'k' is required")
    }
    int k = params.k as int
    boolean shuffle = params.containsKey('shuffle') ? (params.shuffle as boolean) : true

    if (params.containsKey('seed')) {
      return kFold(matrix, k, shuffle, params.seed as long)
    }
    return kFold(matrix, k, shuffle)
  }

  /**
   * Perform stratified train/test split, ensuring proportional representation of each class.
   *
   * @param matrix the Matrix to split
   * @param targetColumn the name of the target/class column for stratification
   * @param testRatio the fraction of data to use for testing (0.0 to 1.0, default 0.2)
   * @return a list containing [trainMatrix, testMatrix]
   */
  static List<Matrix> stratifiedSplit(Matrix matrix, String targetColumn, double testRatio = 0.2) {
    return stratifiedSplit(matrix, targetColumn, testRatio, new Random())
  }

  /**
   * Perform stratified train/test split with a specific random seed.
   *
   * @param matrix the Matrix to split
   * @param targetColumn the name of the target/class column for stratification
   * @param testRatio the fraction of data to use for testing (0.0 to 1.0)
   * @param seed the random seed for reproducibility
   * @return a list containing [trainMatrix, testMatrix]
   */
  static List<Matrix> stratifiedSplit(Matrix matrix, String targetColumn, double testRatio, long seed) {
    return stratifiedSplit(matrix, targetColumn, testRatio, new Random(seed))
  }

  /**
   * Perform stratified train/test split with a specific Random instance.
   *
   * @param matrix the Matrix to split
   * @param targetColumn the name of the target/class column for stratification
   * @param testRatio the fraction of data to use for testing (0.0 to 1.0)
   * @param random the Random instance to use
   * @return a list containing [trainMatrix, testMatrix]
   */
  static List<Matrix> stratifiedSplit(Matrix matrix, String targetColumn, double testRatio, Random random) {
    if (testRatio <= 0.0 || testRatio >= 1.0) {
      throw new IllegalArgumentException("testRatio must be between 0 and 1 (exclusive): was $testRatio")
    }

    List<?> targetValues = matrix.column(targetColumn)

    // Group indices by class
    Map<Object, List<Integer>> classSamples = new LinkedHashMap<>()
    for (int i = 0; i < targetValues.size(); i++) {
      Object classValue = targetValues[i]
      classSamples.computeIfAbsent(classValue) { [] as List<Integer> }.add(i)
    }

    List<Integer> trainIndices = []
    List<Integer> testIndices = []

    // Split each class proportionally
    for (Map.Entry<Object, List<Integer>> entry : classSamples.entrySet()) {
      List<Integer> classIndices = new ArrayList<>(entry.value)
      Collections.shuffle(classIndices, random)

      int classTestSize = Math.max(1, (int) Math.round(classIndices.size() * testRatio))
      if (classTestSize >= classIndices.size()) {
        classTestSize = classIndices.size() - 1
      }

      int trainSize = classIndices.size() - classTestSize
      trainIndices.addAll(classIndices.subList(0, trainSize))
      testIndices.addAll(classIndices.subList(trainSize, classIndices.size()))
    }

    Matrix trainMatrix = createSubMatrix(matrix, trainIndices)
    Matrix testMatrix = createSubMatrix(matrix, testIndices)

    return [trainMatrix, testMatrix]
  }

  /**
   * Create bootstrap samples from the matrix.
   *
   * @param matrix the Matrix to sample from
   * @param n the number of bootstrap samples to create
   * @param sampleSize the size of each bootstrap sample (default: same as original)
   * @return a list of n bootstrap sample matrices
   */
  static List<Matrix> bootstrap(Matrix matrix, int n, int sampleSize = -1) {
    return bootstrap(matrix, n, sampleSize, new Random())
  }

  /**
   * Create bootstrap samples with a specific random seed.
   *
   * @param matrix the Matrix to sample from
   * @param n the number of bootstrap samples to create
   * @param sampleSize the size of each bootstrap sample (default: same as original)
   * @param seed the random seed for reproducibility
   * @return a list of n bootstrap sample matrices
   */
  static List<Matrix> bootstrap(Matrix matrix, int n, int sampleSize, long seed) {
    return bootstrap(matrix, n, sampleSize, new Random(seed))
  }

  /**
   * Create bootstrap samples with a specific Random instance.
   *
   * @param matrix the Matrix to sample from
   * @param n the number of bootstrap samples to create
   * @param sampleSize the size of each bootstrap sample (default: same as original)
   * @param random the Random instance to use
   * @return a list of n bootstrap sample matrices
   */
  static List<Matrix> bootstrap(Matrix matrix, int n, int sampleSize, Random random) {
    if (n < 1) {
      throw new IllegalArgumentException("n must be at least 1: was $n")
    }

    int actualSampleSize = sampleSize > 0 ? sampleSize : matrix.rowCount()
    int rowCount = matrix.rowCount()

    List<Matrix> samples = []
    for (int i = 0; i < n; i++) {
      List<Integer> indices = []
      for (int j = 0; j < actualSampleSize; j++) {
        indices << random.nextInt(rowCount)
      }
      samples << createSubMatrix(matrix, indices)
    }

    return samples
  }

  // Helper method to create a sub-matrix from row indices
  private static Matrix createSubMatrix(Matrix matrix, List<Integer> indices) {
    if (indices.isEmpty()) {
      return Matrix.builder()
          .columnNames(matrix.columnNames() as List<String>)
          .types(matrix.types())
          .matrixName(matrix.matrixName)
          .build()
    }

    List<List<?>> rows = matrix.rows(indices) as List<List<?>>
    return Matrix.builder()
        .rows(rows as List<List>)
        .columnNames(matrix.columnNames() as List<String>)
        .types(matrix.types())
        .matrixName(matrix.matrixName)
        .build()
  }

  /**
   * Represents a single fold in k-fold cross-validation.
   */
  @CompileStatic
  static class Fold {
    final int index
    final Matrix train
    final Matrix validation

    Fold(int index, Matrix train, Matrix validation) {
      this.index = index
      this.train = train
      this.validation = validation
    }

    @Override
    String toString() {
      return "Fold[index=$index, trainSize=${train.rowCount()}, validationSize=${validation.rowCount()}]"
    }
  }
}
