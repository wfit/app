package utils

import java.sql.Timestamp
import java.time.Instant
import java.util.{UUID => JUUID}
import javax.inject.Inject
import models.wow.{Class, Spec}
import org.apache.commons.codec.binary.Hex
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.Result
import scala.concurrent.{ExecutionContext, Future}
import slick.dbio
import slick.jdbc.{JdbcProfile, MySQLProfile}
import slick.lifted.AppliedCompiledFunction

object SlickAPI extends MySQLProfile.API {
	@Inject var dbc: DatabaseConfigProvider = _
	lazy val DB = dbc.get[JdbcProfile].db

	@Inject protected implicit var ec: ExecutionContext = _

	implicit class DBIOActionExecutor[R](val q: DBIOAction[R, NoStream, Nothing]) extends AnyVal {
		@inline final def run: Future[R] = DB.run(q)
	}

	implicit def ResultFromDBIO[R](dbio: DBIOAction[R, NoStream, Nothing]): Future[R] = DB.run(dbio)
	implicit def ValueToDBIO[R](value: R): DBIOAction[R, NoStream, Effect] = DBIO.successful(value)

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
		uuid => scalaUuidToBin(uuid.value),
		bin => UUID(scalaBinToUuid(bin))
	)

	implicit val instantColumnType: BaseColumnType[Instant] = MappedColumnType.base[Instant, Timestamp](
		Timestamp.from,
		ts => ts.toInstant
	)

	implicit val classColumnType: BaseColumnType[Class] = MappedColumnType.base[Class, Int](
		cls => cls.id,
		Class.fromId
	)

	implicit val specColumnType: BaseColumnType[Spec] = MappedColumnType.base[Spec, Int](
		spec => spec.id,
		Spec.fromId
	)
}
