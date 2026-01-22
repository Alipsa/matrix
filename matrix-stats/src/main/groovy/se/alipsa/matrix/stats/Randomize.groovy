package se.alipsa.matrix.stats


import groovyjarjarantlr4.v4.runtime.misc.NotNull
import se.alipsa.matrix.core.Matrix

/**
 * Utility class for randomly shuffling the rows of a Matrix.
 *
 * <p>This class provides methods to create a new Matrix with rows in random order, which is useful for:
 * <ul>
 *   <li>Breaking temporal or sequential patterns in data</li>
 *   <li>Randomization before k-fold cross-validation</li>
 *   <li>Preparing data for bootstrapping or resampling techniques</li>
 *   <li>Ensuring unbiased train/test splits in machine learning</li>
 *   <li>Simulating random permutations for permutation tests</li>
 * </ul>
 *
 * <h3>What is Random Row Shuffling?</h3>
 * <p>Random row shuffling creates a new Matrix where all rows from the original Matrix are
 * present but in a randomized order. This is particularly important in statistical analysis and
 * machine learning to prevent biases that could arise from ordered data (e.g., time series,
 * sorted datasets).</p>
 *
 * <h3>When to Use</h3>
 * <ul>
 *   <li><b>Before train/test splitting</b> - Ensure random distribution of samples across sets</li>
 *   <li><b>Cross-validation setup</b> - Randomize before creating k-folds</li>
 *   <li><b>Breaking temporal order</b> - Remove time-based patterns for non-temporal analysis</li>
 *   <li><b>Permutation tests</b> - Generate random permutations for statistical testing</li>
 *   <li><b>Bootstrap resampling</b> - Create randomized datasets for bootstrap methods</li>
 * </ul>
 *
 * <h3>Reproducibility</h3>
 * <p>The class provides three overloaded methods:</p>
 * <dl>
 *   <dt><b>randomOrder(Matrix)</b></dt>
 *   <dd>Uses the default random number generator - results will vary on each call</dd>
 *
 *   <dt><b>randomOrder(Matrix, Random)</b></dt>
 *   <dd>Uses a provided Random instance - allows control over the RNG</dd>
 *
 *   <dt><b>randomOrder(Matrix, long seed)</b></dt>
 *   <dd>Uses a seeded Random instance - ensures reproducible results</dd>
 * </dl>
 *
 * <h3>Usage Examples</h3>
 *
 * <b>Basic random shuffling (non-reproducible):</b>
 * <pre>
 * def data = Matrix.builder()
 *   .data(id: [1, 2, 3, 4, 5],
 *         value: [10, 20, 30, 40, 50])
 *   .build()
 *
 * def shuffled = Randomize.randomOrder(data)
 * // Rows now in random order, different each time
 * </pre>
 *
 * <b>Reproducible shuffling with seed:</b>
 * <pre>
 * def data = Matrix.builder()
 *   .data(x: [1, 2, 3, 4, 5],
 *         y: [10, 20, 30, 40, 50])
 *   .build()
 *
 * def shuffled1 = Randomize.randomOrder(data, 42L)
 * def shuffled2 = Randomize.randomOrder(data, 42L)
 * // shuffled1 and shuffled2 have identical row orders
 * </pre>
 *
 * <b>Using with cross-validation:</b>
 * <pre>
 * // Randomize before splitting into k-folds
 * def randomized = Randomize.randomOrder(dataset, 12345L)
 * def folds = randomized.splitInto(5)  // 5-fold CV
 * </pre>
 *
 * <b>Using with train/test split:</b>
 * <pre>
 * // Randomize before splitting
 * def randomized = Randomize.randomOrder(dataset)
 * def splits = Sampler.split(randomized, 0.8)
 * def train = splits[0]
 * def test = splits[1]
 * </pre>
 *
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><b>Immutability</b> - Creates a new Matrix, does not modify the original</li>
 *   <li><b>Row integrity</b> - Each row is shuffled as a unit; column relationships are preserved within rows</li>
 *   <li><b>Metadata preserved</b> - Matrix name, column names, and types are maintained</li>
 *   <li><b>No sampling</b> - All original rows are present exactly once (permutation, not sampling with replacement)</li>
 * </ul>
 *
 * <h3>Performance Considerations</h3>
 * <ul>
 *   <li>Time complexity: O(n) where n is the number of rows</li>
 *   <li>Space complexity: O(n) - creates a new Matrix of the same size</li>
 *   <li>Fisher-Yates shuffle algorithm used internally (via Groovy's shuffle())</li>
 * </ul>
 *
 * @see Sampler for train/test splitting after randomization
 * @see <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Fisher-Yates Shuffle Algorithm</a>
 */
class Randomize {

  /**
   * Creates a new Matrix with the rows randomly distributed
   *
   * @param data the Matrix to reorder randomly
   * @return a new Matrix with the rows randomly distributed
   */
  static Matrix randomOrder(@NotNull Matrix data) {
    def copy = data.clone()
    def rows = copy.rows()
    rows.shuffle()
    return Matrix.builder()
        .matrixName(copy.matrixName)
        .columnNames(copy.columnNames())
        .rows(rows)
        .types(copy.types())
        .build()
  }

  /**
   * Creates a new Matrix with the rows randomly distributed
   *
   * @param data the Matrix to reorder randomly
   * @param random the object used to generate a stream of pseudorandom numbers
   * @return a new Matrix with the rows randomly distributed
   */
  static Matrix randomOrder(Matrix data, Random random) {
    def copy = data.clone()
    def rows = copy.rows()
    rows.shuffle(random)
    return Matrix.builder()
        .matrixName(copy.matrixName)
        .columnNames(copy.columnNames())
        .rows(rows)
        .types(copy.types())
        .build()
  }

  /**
   * Creates a new Matrix with the rows randomly distributed
   *
   * @param data the Matrix to reorder randomly
   * @param seed the initial value of the internal state of the pseudorandom number generator
   * @return a new Matrix with the rows randomly distributed
   */
  static Matrix randomOrder(Matrix data, long seed) {
    Random random = new Random(seed)
    return randomOrder(data, random)
  }
}
