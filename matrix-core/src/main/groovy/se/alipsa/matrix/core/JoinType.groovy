package se.alipsa.matrix.core

/**
 * Specifies the type of join operation for {@link Joiner#merge}.
 */
enum JoinType {

  INNER,
  LEFT,
  RIGHT,
  FULL,
  CROSS,
  SEMI,
  ANTI

}
