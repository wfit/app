package utils

@FunctionalInterface
trait PrimaryKey[T, K] {
	def get(obj: T): K
}
