import java.sql.{Date, Timestamp}
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import models.UUID
import models.composer.Fragment
import models.wow.{Class, Role, Spec}
import org.apache.commons.codec.binary.Hex
import scala.concurrent.Future
import slick.jdbc.{MySQLProfile, PositionedParameters, SetParameter}

package object db {
	object api extends MySQLProfile.API {

		implicit final class DBIOActionOps[T] (private val dbio: DBIOAction[T, NoStream, Nothing]) extends AnyVal {
			@inline def run(implicit database: Database): Future[T] = database.run(dbio)
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
			uuid => scalaUuidToBin(uuid.toString),
			bin => UUID(scalaBinToUuid(bin))
		)

		implicit val uuidSetParameter: SetParameter[UUID] = new SetParameter[UUID] {
			def apply(v: UUID, pp: PositionedParameters): Unit = pp.setBytes(scalaUuidToBin(v.toString))
		}

		implicit val instantColumnType: BaseColumnType[Instant] = MappedColumnType.base[Instant, Timestamp](
			Timestamp.from,
			ts => ts.toInstant
		)

		implicit val localDateColumnType: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, Date](
			Date.valueOf,
			date => date.toLocalDate
		)

		implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
			ldt => Timestamp.from(ldt.toInstant(ZoneOffset.UTC)),
			ts => ts.toLocalDateTime
		)

		implicit val classColumnType: BaseColumnType[Class] = MappedColumnType.base[Class, Int](
			cls => cls.id,
			Class.fromId
		)

		implicit val specColumnType: BaseColumnType[Spec] = MappedColumnType.base[Spec, Int](
			spec => spec.id,
			Spec.fromId
		)

		implicit val roleColumnType: BaseColumnType[Role] = MappedColumnType.base[Role, String](
			role => role.key,
			Role.fromString
		)

		implicit val fragmentStyleColumnType: BaseColumnType[Fragment.Style] = MappedColumnType.base[Fragment.Style, String](
			style => style.value, {
				case "text" => Fragment.Text
				case "group" => Fragment.Group
				case "grid" => Fragment.Grid
			}
		)
	}
}
