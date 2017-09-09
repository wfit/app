package models

import java.time.LocalDate
import utils.UUID

case class Profile (user: UUID,
                    name: Option[String], nameVisibility: Int,
                    birthday: Option[LocalDate], birthdayVisibility: Int,
                    location: Option[String], locationVisibility: Int,
                    btag: Option[String], btagVisibility: Int,
                    mail: Option[String], mailVisibility: Int,
                    phone: Option[String], phoneVisibility: Int) {

	def viewForRank(rank: Int): Profile = copy(
		name = if (rank >= nameVisibility) name else None,
		birthday = if (rank >= birthdayVisibility) birthday else None,
		location = if (rank >= locationVisibility) location else None,
		btag = if (rank >= btagVisibility) btag else None,
		mail = if (rank >= mailVisibility) mail else None,
		phone = if (rank >= phoneVisibility) phone else None,
	)
}

object Profile {
	object Visibility {
		final val Everyone = 0
		final val Friend = 1
		final val Apply = 2
		final val Member = 3
		final val Officer = 4
	}

	val empty = Profile(
		user = UUID.zero,
		name = None, nameVisibility = Visibility.Officer,
		birthday = None, birthdayVisibility = Visibility.Officer,
		location = None, locationVisibility = Visibility.Officer,
		btag = None, btagVisibility = Visibility.Officer,
		mail = None, mailVisibility = Visibility.Officer,
		phone = None, phoneVisibility = Visibility.Officer,
	)
}
