import play.api.mvc.{PathBindable, QueryStringBindable}

package object models {
	implicit def UUIDPathBinder(implicit sb: PathBindable[String]): PathBindable[UUID] = new PathBindable[UUID] {
		def bind(key: String, value: String): Either[String, UUID] = sb.bind(key, value).map(UUID.apply)
		def unbind(key: String, value: UUID): String = sb.unbind(key, value.toString)
	}

	implicit def UUIDQueryBinder(implicit sb: QueryStringBindable[String]): QueryStringBindable[UUID] = new QueryStringBindable[UUID] {
		def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UUID]] = {
			sb.bind(key, params).map(res => res.map(UUID.apply))
		}
		def unbind(key: String, value: UUID): String = {
			sb.unbind(key, value.toString)
		}
	}
}
