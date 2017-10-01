const {app, dialog} = require("electron");
const http = require("electron-is-dev") ? require("http") : require("https");
const vm = require("vm");

const base = global.base;

global.electronRoot = __dirname;
global.appBase = base;

function fetch(url, expectedContentType, callback) {
	try {
		http.get(url, (res) => {
			const {statusCode} = res;
			const contentType = res.headers["content-type"];

			let error;
			if (statusCode !== 200) {
				error = new Error(`Request Failed.\nStatus Code: ${statusCode}`);
			} else if (!expectedContentType.test(contentType)) {
				error = new Error(`Invalid content-type.\nExpected application/json but received ${contentType}`);
			}

			if (error) {
				res.resume();
				callback(error, null);
				return;
			}

			res.setEncoding("utf8");
			let rawData = "";
			res.on("data", (chunk) => rawData += chunk);
			res.on("end", () => callback(null, rawData));
		});
	} catch (e) {
		failure("Fetch failed.\n\n" + e.message);
	}
}

function failure(content) {
	global.closeSplash();
	dialog.showErrorBox("An error occurred", content);
	app.quit();
}

function bootstrap(manifest) {
	fetch(base + manifest.path, /^application\/javascript/, (err, data) => {
		if (err) return failure("Unable to fetch bootstrap script.\n\n" + err.message);
		try {
			const init = `
				const code = () => { ${data} };
				(function(r) {
					require = r;
					code();
				})
			`;
			vm.runInThisContext(init)(require);
		} catch (e) {
			failure(e.message + "\n\n" + e.stack);
		}
	});
}

fetch(base + "/electron/bootstrap", /^application\/json/, (err, data) => {
	if (err) return failure("Unable to fetch manifest.\n\n" + err.message);
	try {
		const manifest = JSON.parse(data);
		bootstrap(manifest);
	} catch (e) {
		failure("Unable to parse manifest.\n\n" + e.message);
	}
});
