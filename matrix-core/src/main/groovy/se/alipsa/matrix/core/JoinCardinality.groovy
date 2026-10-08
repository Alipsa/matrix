package se.alipsa.matrix.core

/** Uniqueness requirements for matchable keys across the entire join inputs. */
enum JoinCardinality {
  ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
}
