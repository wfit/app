package gt.workers.updater

private sealed abstract class Status (val code: String, val color: String, val text: String)

private object Status {
	case object Initializing extends Status("initializing", "yellow", "Initialisation")
	case object Disabled extends Status("disabled", "#999", "Désactivé")
	case object Failure extends Status("failure", "red", "Échec")
	case object Enabled extends Status("enabled", "#0D0", "Activé")
	case object Updating extends Status("updating", "#0D0", "Synchronisation")
	case object Locked extends Status("locked", "red", "Bloqué")
}
