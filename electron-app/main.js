const {app, dialog, BrowserWindow, Menu, Tray} = require("electron");
const isReachable = require("is-reachable");
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

let watchdog = null;
global.startWatchdog = () => watchdog = setTimeout(() => app.quit(), 60 * 1000);
global.disableWatchdog = () => clearTimeout(watchdog);
global.closeSplash = () => {
	if (splash) splash.destroy();
	splash = null;
};

function errorDialog(err) {
	global.closeSplash();
	dialog.showErrorBox("An error occurred", err.stack);
	app.exit();
}

process.on("uncaughtException", errorDialog);

function updateDialog() {
	dialog.showMessageBox({
		title: "Mise à jour disponible",
		message: "Une mise à jour pour l'app Wait For It\nest disponible et va être installée.",
		icon: __dirname + "/build/icon.ico"
	});
}

function loadApp() {
	global.startWatchdog();
	if (require("electron-is-dev")) {
		load();
	} else {
		const {autoUpdater} = require("electron-updater");
		autoUpdater.once("update-not-available", load);
		autoUpdater.on("update-available", updateDialog)
		autoUpdater.on("update-downloaded", () => autoUpdater.quitAndInstall());
		autoUpdater.on("error", errorDialog);
		autoUpdater.checkForUpdates();
	}
}

let tray = null;
let reachTimeout = null;
let reachProgress = false;

function checkReachability() {
	if (reachProgress) return;
	if (reachTimeout) clearTimeout(reachTimeout);
	reachTimeout = null;
	reachProgress = true;
	console.log("checking reachability");
	isReachable(global.base).then(reachable => {
		reachProgress = false;
		console.log(reachable);
		if (reachable) {
			if (tray) {
				tray.destroy();
			}
			loadApp();
		} else {
			if (!tray) {
				global.closeSplash();
				tray = new Tray(__dirname + "/build/gray.ico");
				const contextMenu = Menu.buildFromTemplate([
					{label: "Quitter", role: "quit"}
				]);
				tray.setToolTip("Wait For It (Hors ligne)");
				tray.setContextMenu(contextMenu);
				tray.on("click", checkReachability);
			}
			reachTimeout = setTimeout(checkReachability, 15000);
		}
	});
}

app.on("window-all-closed", () => {});

app.on("ready", function () {
	splash = new BrowserWindow({width: 590, height: 150, frame: false, transparent: true,
		show: false, skipTaskbar: true, alwaysOnTop: true});
	splash.setIgnoreMouseEvents(true);
	splash.loadURL(`file://${__dirname}/splash.html`);
	splash.once("ready-to-show", () => splash.show());

	global.base = require("electron-is-dev") ? "http://localhost:9000" : "https://app.wfit.ovh";
	checkReachability();
});
