const {app, dialog, BrowserWindow} = require("electron");
const load = () => require("./loader");

app.setAppUserModelId("fr.waitforit.app");

const isSecondInstance = app.makeSingleInstance(() => {});
if (isSecondInstance) {
	app.on("ready", function () {
		dialog.showMessageBox({
			title: "Un instance de l'application est déjà en cours d'exécution",
			message: "Le lancement de l'application Wait for It a été interrompu\npuisqu'une autre instance de cette application est déjà\nen cours d'exécution.\n\nVérifiez les applications dans votre barre des tâches et\nvos processus en arrière-plan.",
			icon: __dirname + "/build/icon.ico"
		});
		app.quit();
	});
	return;
}

let splash;

let watchdog = setTimeout(() => app.quit(), 60 * 1000);
global.disableWatchdog = () => clearTimeout(watchdog);
global.closeSplash = () => {
	if (splash) splash.destroy();
	splash = null;
};

process.on("uncaughtException", (err) => {
	global.closeSplash();
	dialog.showErrorBox("An error occurred", err.stack);
	app.exit();
});

function doUpdate(autoUpdater) {
	return () => {
		global.closeSplash();
		dialog.showMessageBox({
			title: "Mise à jour disponible",
			message: "Une mise à jour pour l'app Wait For It\nest disponible et va être installée.",
			icon: __dirname + "/build/icon.ico"
		});
		autoUpdater.quitAndInstall();
	};
}

app.on("window-all-closed", () => {});

app.on("ready", function () {
	splash = new BrowserWindow({width: 590, height: 150, frame: false, transparent: true,
		show: false, skipTaskbar: true, alwaysOnTop: true});
	splash.setIgnoreMouseEvents(true);
	splash.loadURL(`file://${__dirname}/splash.html`);
	splash.once("ready-to-show", () => splash.show());

	if (require("electron-is-dev")) {
		load();
	} else {
		const {autoUpdater} = require("electron-updater");
		autoUpdater.on("update-not-available", load);
		autoUpdater.on("update-downloaded", doUpdate(autoUpdater));
		autoUpdater.checkForUpdates();
	}
});
