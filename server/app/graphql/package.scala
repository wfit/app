import sangria.execution.deferred.Deferred
import sangria.schema.{MappingDeferred, Named, OutputType}

package object graphql {
	/** Alias the Sangria Context as Env, 'cause why call something with a
	  * ctx property on it "Context" ?! */
	type Env[Val] = sangria.schema.Context[Context, Val]

	/** An output type with a name */
	type NamedOutputType[T] = OutputType[T] with Named

	/** Implicitly extracts Context from Env */
	implicit def ContextFromEnv(implicit env: Env[_]): Context = env.ctx

	/** Why no `map` on Deferred?! */
	implicit class DeferredOps[A] (private val deferred: Deferred[A]) extends AnyVal {
		def map[B](f: A => B): Deferred[B] = MappingDeferred(deferred, (a: A) => (f(a), Vector.empty))
		def mapWithErrors[B](f: A => (B, Vector[Throwable])): Deferred[B] = MappingDeferred(deferred, f)
	}
}
