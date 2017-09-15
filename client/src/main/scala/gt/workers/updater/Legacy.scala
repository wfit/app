package gt.workers.updater

object Legacy {
	val directories: Map[String, Seq[String]] = Map(
		"BigWigs" -> Seq(
			"/BigWigs_Core",
			"/BigWigs_Options",
			"/BigWigs_Plugins",
			"/BigWigs_Nighthold",
			"/BigWigs_Nightmare",
			"/BigWigs_BrokenIsles",
			"/BigWigs_TrialOfValor",
			"/BigWigs_TombOfSargeras",
			"/BigWigs_Antorus",
			"/BigWigs_ArgusInvasionPoints"
		),
		"WFI_Core" -> Seq("FS_Core"),
		"WFI_SmartColor" -> Seq("FS_SmartColor"),
		"WFI_UpdaterStatus" -> Seq("FS_UpdaterStatus"),
	)
}
