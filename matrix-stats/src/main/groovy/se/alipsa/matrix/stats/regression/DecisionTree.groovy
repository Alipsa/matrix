package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

/**
 * Decision Tree Regression (CART algorithm) models non-linear relationships by recursively partitioning
 * the feature space into regions and predicting the mean value within each region. It creates a tree-like
 * structure where each internal node represents a decision rule (threshold split) and each leaf node
 * represents a predicted value.
 *
 * <p><b>What is Decision Tree Regression?</b></p>
 * Decision tree regression is a non-parametric supervised learning algorithm that approximates a function
 * by recursively splitting the input space based on feature values. Unlike linear regression, which assumes
 * a linear relationship, decision trees can model complex, non-linear patterns including step functions,
 * piecewise constant functions, and interactions. This implementation uses the CART (Classification and
 * Regression Trees) algorithm with variance reduction as the splitting criterion.
 *
 * <p><b>When to use Decision Tree Regression:</b></p>
 * <ul>
 *   <li>When the relationship between predictor and response is non-linear or piecewise constant</li>
 *   <li>When you need an interpretable model that can be visualized as decision rules</li>
 *   <li>When the data contains natural thresholds or breakpoints (e.g., pricing tiers, age groups)</li>
 *   <li>When you want automatic feature interaction detection without manual engineering</li>
 *   <li>For exploratory analysis to identify important split points in the data</li>
 *   <li>When robustness to outliers is desired (predictions are based on local means)</li>
 *   <li>As a component in ensemble methods (Random Forests, Gradient Boosting)</li>
 * </ul>
 *
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Highly interpretable - can be visualized and explained as simple if-then rules</li>
 *   <li>Handles non-linear relationships without requiring feature transformations</li>
 *   <li>No assumptions about data distribution (non-parametric)</li>
 *   <li>Robust to outliers in X (splits based on ranks/ordering)</li>
 *   <li>Can capture interactions and thresholds automatically</li>
 *   <li>Fast prediction time - O(log n) for balanced trees</li>
 *   <li>No feature scaling required</li>
 *   <li>Can handle missing values with surrogate splits (advanced implementations)</li>
 * </ul>
 *
 * <p><b>Disadvantages:</b></p>
 * <ul>
 *   <li>Prone to overfitting, especially with deep trees (high variance)</li>
 *   <li>Unstable - small changes in data can lead to completely different trees</li>
 *   <li>Biased towards features with more unique values</li>
 *   <li>Cannot extrapolate beyond the range of training data</li>
 *   <li>Predictions are piecewise constant (cannot model smooth functions well)</li>
 *   <li>Single feature implementation (this class) - limited to univariate regression</li>
 *   <li>May create overly complex trees with noisy data</li>
 *   <li>Requires tuning of hyperparameters (maxDepth, minSamplesLeaf) to prevent overfitting</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Model a step function relationship
 * def x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
 * def y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]
 * def tree = new DecisionTree(x, y, 3, 2)  // maxDepth=3, minSamplesLeaf=2
 *
 * // Make predictions
 * println tree.predict(2.5)  // ≈ 1.0 (in first region)
 * println tree.predict(6.5)  // ≈ 5.0 (in second region)
 *
 * // Evaluate model quality
 * println "R² = ${tree.getRSquared(3)}"    // Coefficient of determination
 * println "MSE = ${tree.getMse(3)}"        // Mean squared error
 * println "Depth = ${tree.getDepth()}"     // Actual tree depth
 * println "Leaves = ${tree.getLeafCount()}" // Number of leaf nodes
 *
 * // View decision rules
 * println tree.summary()
 * // Output shows tree structure:
 * //   root:
 * //     X <= 4.5: predict 1.0
 * //     X > 4.5: predict 5.0
 *
 * // Use with Matrix
 * def data = Matrix.builder()
 *   .data(x: [1, 2, 3, 4, 5, 6, 7, 8], y: [1, 1, 1, 1, 5, 5, 5, 5])
 *   .build()
 * def tree2 = new DecisionTree(data, 'x', 'y', 3, 2)
 * println tree2
 * </pre>
 *
 * <p><b>Mathematical formulation:</b></p>
 * At each node, the algorithm selects the split threshold that maximizes variance reduction:
 * <pre>
 * Split criterion: maximize Gain = Var(parent) - (n_left/n)*Var(left) - (n_right/n)*Var(right)
 * Variance: Var(S) = (1/|S|) Σ(y - ȳ)² for samples S
 * Prediction: ŷ = mean(y) for samples reaching the leaf node
 * </pre>
 * where:
 * <ul>
 *   <li>Var(parent) is the variance of y values in the parent node</li>
 *   <li>Var(left), Var(right) are variances in left and right child nodes</li>
 *   <li>n_left, n_right are the number of samples in each child</li>
 *   <li>The split threshold is chosen from midpoints between consecutive unique X values</li>
 * </ul>
 *
 * <p>The tree-building process recursively partitions until:</p>
 * <ul>
 *   <li>Maximum depth (maxDepth) is reached</li>
 *   <li>Node has fewer than minSamplesLeaf samples</li>
 *   <li>All Y values in the node are identical (variance = 0)</li>
 *   <li>No valid split can be found that satisfies minSamplesLeaf constraint</li>
 * </ul>
 *
 * <p><b>Tuning parameters:</b></p>
 * <ul>
 *   <li><b>maxDepth</b> (default: 5): Maximum tree depth. Smaller values prevent overfitting but may underfit.
 *       Typical range: 2-10 for interpretability, 10-20 for complex patterns.</li>
 *   <li><b>minSamplesLeaf</b> (default: 2): Minimum samples required in a leaf. Larger values create
 *       smoother, more robust models. Typical range: 1-10 for small datasets, 5-50 for larger datasets.</li>
 * </ul>
 *
 * <p><b>Model evaluation metrics:</b></p>
 * <ul>
 *   <li><b>R² (coefficient of determination)</b>: Proportion of variance explained, ranges 0-1 (higher is better)</li>
 *   <li><b>MSE (mean squared error)</b>: Average squared prediction error (lower is better)</li>
 *   <li><b>RMSE (root mean squared error)</b>: Square root of MSE, in same units as Y (lower is better)</li>
 *   <li><b>MAE (mean absolute error)</b>: Average absolute prediction error (lower is better, more robust to outliers)</li>
 * </ul>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Breiman, L., Friedman, J., Stone, C. J., & Olshen, R. A. (1984). "Classification and Regression Trees". Wadsworth International Group.</li>
 *   <li>James, G., Witten, D., Hastie, T., & Tibshirani, R. (2013). "An Introduction to Statistical Learning". Springer, Chapter 8.</li>
 *   <li>Hastie, T., Tibshirani, R., & Friedman, J. (2009). "The Elements of Statistical Learning" (2nd ed.). Springer, Chapter 9.</li>
 * </ul>
 *
 * <p><b>Note:</b> This implementation supports single-feature (univariate) regression only. For multivariate
 * regression trees, consider using ensemble methods or dedicated machine learning libraries.</p>
 */
@CompileStatic
class DecisionTree {

  /**
   * Internal node class representing a binary tree node
   */
  @CompileStatic
  static class Node {
    /** Split threshold (null for leaf nodes) */
    BigDecimal threshold

    /** Predicted value (for leaf nodes, this is the mean of training samples) */
    BigDecimal value

    /** Left child (samples where x <= threshold) */
    Node left

    /** Right child (samples where x > threshold) */
    Node right

    /**
     * Check if this node is a leaf node
     * @return true if this is a leaf node (no children)
     */
    boolean isLeaf() {
      return left == null && right == null
    }
  }

  /** Root node of the decision tree */
  private Node root

  /** Maximum depth of the tree (default: 5) */
  private int maxDepth

  /** Minimum number of samples required in a leaf node (default: 2) */
  private int minSamplesLeaf

  /** Name of the independent variable (for display purposes) */
  String x = 'X'

  /** Name of the dependent variable (for display purposes) */
  String y = 'Y'

  /** Training R² value */
  private BigDecimal trainingR2

  /** Training MSE value */
  private BigDecimal trainingMse

  /** Training RMSE value */
  private BigDecimal trainingRmse

  /** Training MAE value */
  private BigDecimal trainingMae

  /** Actual depth of the built tree */
  private int depth

  /** Number of leaf nodes in the tree */
  private int leafCount

  /** Mean of Y values (used for R² calculation) */
  private BigDecimal yMean

  /**
   * Construct a DecisionTree from a Matrix with default parameters
   *
   * @param table The data matrix
   * @param x Column name for independent variable
   * @param y Column name for dependent variable
   */
  DecisionTree(Matrix table, String x, String y) {
    this(table, x, y, 5, 2)
  }

  /**
   * Construct a DecisionTree from a Matrix with custom parameters
   *
   * @param table The data matrix
   * @param x Column name for independent variable
   * @param y Column name for dependent variable
   * @param maxDepth Maximum tree depth
   * @param minSamplesLeaf Minimum samples required in a leaf node
   */
  DecisionTree(Matrix table, String x, String y, int maxDepth, int minSamplesLeaf) {
    this(table[x] as List<? extends Number>, table[y] as List<? extends Number>, maxDepth, minSamplesLeaf)
    this.x = x
    this.y = y
  }

  /**
   * Construct a DecisionTree from lists with default parameters
   *
   * @param xValues Independent variable values
   * @param yValues Dependent variable values
   */
  DecisionTree(List<? extends Number> xValues, List<? extends Number> yValues) {
    this(xValues, yValues, 5, 2)
  }

  /**
   * Construct a DecisionTree from lists with custom parameters
   *
   * @param xValues Independent variable values
   * @param yValues Dependent variable values
   * @param maxDepth Maximum tree depth
   * @param minSamplesLeaf Minimum samples required in a leaf node
   */
  DecisionTree(List<? extends Number> xValues, List<? extends Number> yValues, int maxDepth, int minSamplesLeaf) {
    validateInputs(xValues, yValues, maxDepth, minSamplesLeaf)

    this.maxDepth = maxDepth
    this.minSamplesLeaf = minSamplesLeaf

    // Convert to BigDecimal lists
    List<BigDecimal> xData = xValues.collect { it as BigDecimal }
    List<BigDecimal> yData = yValues.collect { it as BigDecimal }

    // Store y mean for R² calculation
    this.yMean = Stat.mean(yData)

    // Build the tree
    this.root = buildTree(xData, yData, 0)

    // Calculate metrics
    calculateMetrics(xData, yData)
  }

  /**
   * Validate constructor inputs
   */
  private static void validateInputs(List<? extends Number> xValues, List<? extends Number> yValues,
                                     int maxDepth, int minSamplesLeaf) {
    if (xValues == null || yValues == null) {
      throw new IllegalArgumentException("Input data cannot be null")
    }

    if (xValues.size() != yValues.size()) {
      throw new IllegalArgumentException(
        "Must have equal number of X and Y data points. " +
        "Got ${xValues.size()} X values and ${yValues.size()} Y values."
      )
    }

    if (xValues.isEmpty()) {
      throw new IllegalArgumentException("Input data cannot be empty")
    }

    if (xValues.size() < 2) {
      throw new IllegalArgumentException(
        "Need at least 2 data points. Got ${xValues.size()}."
      )
    }

    if (maxDepth < 1) {
      throw new IllegalArgumentException("maxDepth must be at least 1. Got ${maxDepth}.")
    }

    if (minSamplesLeaf < 1) {
      throw new IllegalArgumentException("minSamplesLeaf must be at least 1. Got ${minSamplesLeaf}.")
    }
  }

  /**
   * Build the decision tree recursively
   *
   * @param xData X values for this node
   * @param yData Y values for this node
   * @param currentDepth Current depth in the tree
   * @return The built node
   */
  private Node buildTree(List<BigDecimal> xData, List<BigDecimal> yData, int currentDepth) {
    // Track maximum depth reached
    if (currentDepth > this.depth) {
      this.depth = currentDepth
    }

    // Create leaf node if stopping criteria met
    if (currentDepth >= maxDepth || xData.size() <= minSamplesLeaf) {
      return createLeafNode(yData)
    }

    // Check if all Y values are the same (no variance to reduce)
    BigDecimal yVariance = calculateVariance(yData)
    if (yVariance == 0) {
      return createLeafNode(yData)
    }

    // Find best split
    BigDecimal bestThreshold = findBestSplit(xData, yData)

    // If no valid split found, create leaf node
    if (bestThreshold == null) {
      return createLeafNode(yData)
    }

    // Split data
    List<BigDecimal> leftX = []
    List<BigDecimal> leftY = []
    List<BigDecimal> rightX = []
    List<BigDecimal> rightY = []

    for (int i = 0; i < xData.size(); i++) {
      if (xData[i] <= bestThreshold) {
        leftX << xData[i]
        leftY << yData[i]
      } else {
        rightX << xData[i]
        rightY << yData[i]
      }
    }

    // Check if split is valid
    if (leftX.size() < minSamplesLeaf || rightX.size() < minSamplesLeaf) {
      return createLeafNode(yData)
    }

    // Create internal node
    Node node = new Node()
    node.threshold = bestThreshold
    node.value = Stat.mean(yData)  // Store mean for potential pruning/display
    node.left = buildTree(leftX, leftY, currentDepth + 1)
    node.right = buildTree(rightX, rightY, currentDepth + 1)

    return node
  }

  /**
   * Create a leaf node with the mean of Y values
   */
  private Node createLeafNode(List<BigDecimal> yData) {
    this.leafCount++
    Node leaf = new Node()
    leaf.value = Stat.mean(yData)
    return leaf
  }

  /**
   * Find the best split threshold using variance reduction
   *
   * @param xData X values
   * @param yData Y values
   * @return Best threshold, or null if no valid split found
   */
  private BigDecimal findBestSplit(List<BigDecimal> xData, List<BigDecimal> yData) {
    BigDecimal parentVariance = calculateVariance(yData)

    // Get unique sorted X values
    List<BigDecimal> uniqueX = xData.toSet().sort()

    if (uniqueX.size() < 2) {
      return null
    }

    BigDecimal bestGain = 0
    BigDecimal bestThreshold = null
    int n = yData.size()

    // Try each midpoint between consecutive unique values
    for (int i = 0; i < uniqueX.size() - 1; i++) {
      BigDecimal threshold = (uniqueX[i] + uniqueX[i + 1]) / 2

      // Split Y values by threshold
      List<BigDecimal> leftY = []
      List<BigDecimal> rightY = []

      for (int j = 0; j < xData.size(); j++) {
        if (xData[j] <= threshold) {
          leftY << yData[j]
        } else {
          rightY << yData[j]
        }
      }

      // Check minimum samples constraint
      if (leftY.size() < minSamplesLeaf || rightY.size() < minSamplesLeaf) {
        continue
      }

      // Calculate weighted child variance
      BigDecimal leftVariance = calculateVariance(leftY)
      BigDecimal rightVariance = calculateVariance(rightY)
      BigDecimal weightedChildVariance =
        (leftY.size() * leftVariance + rightY.size() * rightVariance) / n

      // Calculate variance reduction (information gain)
      BigDecimal gain = parentVariance - weightedChildVariance

      if (gain > bestGain) {
        bestGain = gain
        bestThreshold = threshold
      }
    }

    return bestGain > 0 ? bestThreshold : null
  }

  /**
   * Calculate variance of a list of values
   */
  private static BigDecimal calculateVariance(List<BigDecimal> values) {
    if (values == null || values.size() < 2) {
      return 0
    }
    BigDecimal mean = Stat.mean(values)
    BigDecimal sumSquaredDiff = values.collect { (it - mean) ** 2 }.sum() as BigDecimal
    return sumSquaredDiff / values.size()
  }

  /**
   * Calculate training metrics (R², MSE, RMSE, MAE)
   */
  private void calculateMetrics(List<BigDecimal> xData, List<BigDecimal> yData) {
    List<BigDecimal> predictions = xData.collect { predictInternal(it) }

    // Calculate MSE
    BigDecimal sumSquaredError = 0
    BigDecimal sumAbsoluteError = 0
    BigDecimal totalSumSquares = 0

    for (int i = 0; i < yData.size(); i++) {
      BigDecimal error = predictions[i] - yData[i]
      sumSquaredError += error ** 2
      sumAbsoluteError += error.abs()
      totalSumSquares += (yData[i] - yMean) ** 2
    }

    int n = yData.size()
    this.trainingMse = sumSquaredError / n
    this.trainingRmse = trainingMse.sqrt(java.math.MathContext.DECIMAL128)
    this.trainingMae = sumAbsoluteError / n

    // Calculate R²
    if (totalSumSquares == 0) {
      // All Y values are the same
      this.trainingR2 = 1.0
    } else {
      this.trainingR2 = 1 - (sumSquaredError / totalSumSquares)
    }
  }

  /**
   * Internal prediction method using BigDecimal
   */
  private BigDecimal predictInternal(BigDecimal x) {
    Node current = root
    while (!current.isLeaf()) {
      if (x <= current.threshold) {
        current = current.left
      } else {
        current = current.right
      }
    }
    return current.value
  }

  /**
   * Predict the value for a single input
   *
   * @param x Value of the independent variable
   * @return Predicted value
   */
  BigDecimal predict(Number x) {
    return predictInternal(x as BigDecimal)
  }

  /**
   * Predict the value for a single input, rounded to specified decimals
   *
   * @param x Value of the independent variable
   * @param numberOfDecimals Number of decimal places to round to
   * @return Predicted value rounded to specified decimals
   */
  BigDecimal predict(Number x, int numberOfDecimals) {
    return predict(x).setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Predict values for a list of inputs
   *
   * @param xValues List of independent variable values
   * @return List of predicted values
   */
  List<BigDecimal> predict(List<? extends Number> xValues) {
    return xValues.collect { predict(it) }
  }

  /**
   * Predict values for a list of inputs, rounded to specified decimals
   *
   * @param xValues List of independent variable values
   * @param numberOfDecimals Number of decimal places to round to
   * @return List of predicted values rounded to specified decimals
   */
  List<BigDecimal> predict(List<? extends Number> xValues, int numberOfDecimals) {
    return xValues.collect { predict(it, numberOfDecimals) }
  }

  /**
   * Get the R² (coefficient of determination) value
   *
   * @return R² value (between 0 and 1)
   */
  BigDecimal getRSquared() {
    return trainingR2
  }

  /**
   * Get the R² value rounded to specified decimals
   *
   * @param numberOfDecimals Number of decimal places to round to
   * @return R² value rounded to specified decimals
   */
  BigDecimal getRSquared(int numberOfDecimals) {
    return getRSquared().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Get the Mean Squared Error (MSE)
   *
   * @return MSE value
   */
  BigDecimal getMse() {
    return trainingMse
  }

  /**
   * Get the MSE value rounded to specified decimals
   *
   * @param numberOfDecimals Number of decimal places to round to
   * @return MSE value rounded to specified decimals
   */
  BigDecimal getMse(int numberOfDecimals) {
    return getMse().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Get the Root Mean Squared Error (RMSE)
   *
   * @return RMSE value
   */
  BigDecimal getRmse() {
    return trainingRmse
  }

  /**
   * Get the RMSE value rounded to specified decimals
   *
   * @param numberOfDecimals Number of decimal places to round to
   * @return RMSE value rounded to specified decimals
   */
  BigDecimal getRmse(int numberOfDecimals) {
    return getRmse().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Get the Mean Absolute Error (MAE)
   *
   * @return MAE value
   */
  BigDecimal getMae() {
    return trainingMae
  }

  /**
   * Get the MAE value rounded to specified decimals
   *
   * @param numberOfDecimals Number of decimal places to round to
   * @return MAE value rounded to specified decimals
   */
  BigDecimal getMae(int numberOfDecimals) {
    return getMae().setScale(numberOfDecimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Get the actual depth of the tree
   *
   * @return Tree depth
   */
  int getDepth() {
    return depth
  }

  /**
   * Get the number of leaf nodes
   *
   * @return Number of leaf nodes
   */
  int getLeafCount() {
    return leafCount
  }

  /**
   * Get the maximum depth parameter
   *
   * @return Maximum depth setting
   */
  int getMaxDepth() {
    return maxDepth
  }

  /**
   * Get the minimum samples per leaf parameter
   *
   * @return Minimum samples per leaf setting
   */
  int getMinSamplesLeaf() {
    return minSamplesLeaf
  }

  /**
   * String representation of the decision tree
   *
   * @return String showing tree summary
   */
  @Override
  String toString() {
    return "DecisionTree[depth=${depth}, leaves=${leafCount}, R²=${getRSquared(3)}]"
  }

  /**
   * Get a detailed summary of the decision tree model
   *
   * @return Multi-line summary string
   */
  String summary() {
    StringBuilder sb = new StringBuilder()
    sb.append("Decision Tree Regression\n")
    sb.append("========================\n\n")
    sb.append("Parameters:\n")
    sb.append("  maxDepth: ${maxDepth}\n")
    sb.append("  minSamplesLeaf: ${minSamplesLeaf}\n\n")
    sb.append("Tree Structure:\n")
    sb.append("  Actual depth: ${depth}\n")
    sb.append("  Number of leaves: ${leafCount}\n\n")
    sb.append("Training Metrics:\n")
    sb.append("  R²:   ${getRSquared(4)}\n")
    sb.append("  MSE:  ${getMse(4)}\n")
    sb.append("  RMSE: ${getRmse(4)}\n")
    sb.append("  MAE:  ${getMae(4)}\n\n")
    sb.append("Tree Rules:\n")
    appendTreeRules(sb, root, "  ", "root")
    return sb.toString()
  }

  /**
   * Recursively append tree rules to StringBuilder
   */
  private void appendTreeRules(StringBuilder sb, Node node, String indent, String condition) {
    if (node.isLeaf()) {
      sb.append("${indent}${condition}: predict ${node.value.setScale(4, RoundingMode.HALF_EVEN)}\n")
    } else {
      sb.append("${indent}${condition}:\n")
      appendTreeRules(sb, node.left, indent + "  ", "${x} <= ${node.threshold}")
      appendTreeRules(sb, node.right, indent + "  ", "${x} > ${node.threshold}")
    }
  }
}
