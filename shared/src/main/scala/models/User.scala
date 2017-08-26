package models

import utils.UUID

case class User (uuid: UUID, name: String, group: Int,
                 mail: Option[String], tel: Option[String], btag: Option[String])
