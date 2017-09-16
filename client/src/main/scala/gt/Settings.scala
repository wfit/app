package gt

import gt.util.SettingsRegistry

object Settings extends SettingsRegistry {
	val AppAutoLaunch = Setting("app.autolaunch", "1").typed(BooleanT).bind(GuildTools.setupAutoLaunch)

	val LoginIdentifier = Setting("login.identifier", "")

	val UpdaterPath = Setting("updater.path", null)
	val UpdaterNotify = Setting("updater.notify", "1").typed(BooleanT)
	val UpdaterNotifySound = Setting("updater.notify.sound", "0").typed(BooleanT)
}
