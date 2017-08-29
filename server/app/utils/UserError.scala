package utils

import play.api.mvc.Results.{InternalServerError, Status}

case class UserError (msg: String, status: Status = InternalServerError) extends Error(msg)
