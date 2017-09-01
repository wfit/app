package services

import javax.inject.Singleton
import utils.UUID

@Singleton
class RuntimeService {
	val instanceUUID: UUID = UUID.random
}
