const {app, dialog} = require("electron");
const http = require("http");
const vm = require('vm');

const base = "http://localhost:9000";

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

function whenReady(thunk) {
	if (app.isReady()) thunk();
	else app.on("ready", thunk);
}

function failure(content) {
	whenReady(() => {
		dialog.showErrorBox("Bootstrap error", content);
		app.quit();
	});
}

function bootstrap(manifest) {
	fetch(base + manifest.path, /^application\/javascript/, (err, data) => {
		if (err) return failure("Unable to fetch bootstrap script.\n\n" + err.message);
		whenReady(() => {
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
