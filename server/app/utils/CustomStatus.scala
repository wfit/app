package utils

import base.UserRequest

object CustomStatus {
	def FullRedirect(implicit request: UserRequest[_]): Int = if (request.isFetch) 392 else 303
}
