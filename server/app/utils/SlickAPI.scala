package utils

import java.util.{UUID => JUUID}
import javax.inject.Inject
import models.wow.WClass
import org.apache.commons.codec.binary.Hex
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.{JdbcProfile, MySQLProfile}
import slick.lifted.AppliedCompiledFunction

object SlickAPI extends MySQLProfile.API {
	@Inject var dbc: DatabaseConfigProvider = _
	lazy val DB = dbc.get[JdbcProfile].db

	@Inject protected implicit var ec: ExecutionContext = _

	implicit class DBQueryExecutor[A](val q: Query[_, A, Seq]) extends AnyVal {
		@inline final def run: Future[Seq[A]] = DB.run(q.result)
		@inline final def head: Future[A] = DB.run(q.result.head)
		@inline final def headOption: Future[Option[A]] = DB.run(q.result.headOption)
	}

	implicit class DBCompiledExecutor[A, B](val q: AppliedCompiledFunction[_, Query[A, B, Seq], _]) extends AnyVal {
		@inline final def run: Future[Any] = DB.run(q.result)
		@inline final def head: Future[B] = DB.run(q.result.head)
		@inline final def headOption: Future[Option[B]] = DB.run(q.result.headOption)
	}

	implicit class DBRepExecutor[A](val q: Rep[A]) extends AnyVal {
		//@inline final def run: Future[A] = DB.run(q.result)
	}

	implicit class DBIOActionExecutor[R](val q: DBIOAction[R, NoStream, Nothing]) extends AnyVal {
		@inline final def run: Future[R] = DB.run(q)
	}

	val uuidToBin: (Rep[String]) => Rep[Array[Byte]] = SimpleFunction.unary[String, Array[Byte]]("uuid_to_bin")
	val binToUuid: (Rep[Array[Byte]]) => Rep[String] = SimpleFunction.unary[Array[Byte], String]("bin_to_uuid")

	private def scalaUuidToBin(uuid: String): Array[Byte] = {
		val hex = uuid.replace("-", "")
		val ord = hex.substring(12, 16) + hex.substring(8, 12) + hex.substring(0, 8) + hex.substring(16, 32)
		Hex.decodeHex(ord.toCharArray)
	}

	private def scalaBinToUuid(bin: Array[Byte]): String = {
		val hex = new String(Hex.encodeHex(bin, true))
		hex.substring(8, 16) + "-" +
		hex.substring(4, 8) + "-" +
		hex.substring(0, 4) + "-" +
		hex.substring(16, 20) + "-" +
		hex.substring(20, 32)
	}

	implicit val customUuidColumnType: BaseColumnType[UUID] = MappedColumnType.base[UUID, Array[Byte]](
		{ uuid => scalaUuidToBin(uuid.value) }, { bin => UUID(scalaBinToUuid(bin)) }
	)

	implicit val classColumnType: BaseColumnType[WClass] = MappedColumnType.base[WClass, Int](
		{ cls => cls.id }, { id => WClass.fromId(id) }
	)
}
