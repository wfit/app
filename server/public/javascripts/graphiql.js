function pathAbsolute(path) {
	return window.location.pathname.replace("/graphiql", path);
}

let socket = null;
let open = false;
let queue = [];
let queries = new Map();

function send(msg) {
	if (!open) {
		if (!socket) {
			socket = new WebSocket("ws://" + window.location.host + pathAbsolute("/graphql"));
			socket.onopen = () => {
				console.log("Socket open.");
				open = true;
				queue.forEach(send);
				queue = [];
			};
			socket.onmessage = (ev) => {
				try {
					const msg = JSON.parse(ev.data);
					switch (msg.type) {
						case "ks":
							break;

						case "connection_error":
							console.error("Connection error:", msg.payload);
							break;

						case "data":
						case "error":
						case "complete":
							const observer = queries.get(msg.id);
							if (!observer) {
								if (msg.type !== "complete") {
									unsubscribe(msg.id);
								}
							} else {
								switch (msg.type) {
									case "data":
										observer.next(msg.payload);
										break;
									case "error":
										if (msg.payload.error) {
											const str = `${msg.payload.error}: ${msg.payload.message}\n\n${msg.payload.details}`;
											observer.error(str);
										} else {
											observer.error(msg.payload.errors[0].message);
										}
										break;
									case "complete":
										observer.complete(msg.payload);
										break;
								}
							}
							break;

						default:
							console.warn("Unable to handle message:", msg);
					}
				} catch (err) {
					console.error("Parse error:", err);
				}
			};
			socket.onerror = (err) => {
				console.error("Socket error:", err);
				socket = null;
				open = false;
				queries.forEach(obs => obs.error("An error occurred while connecting to the server."));
				queries.clear();
			};
			socket.onclose = () => {
				console.log("Socket closed.");
				socket = null;
				open = false;
				queries.forEach(obs => obs.complete());
				queries.clear();
			};
		}
		queue.push(msg);
	} else {
		socket.send(msg);
	}
}

function uuidv4() {
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
		const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
		return v.toString(16);
	});
}

function query(doc, observer) {
	const id = uuidv4();
	send(JSON.stringify({type: "start", id: id, payload: doc}));
	queries.set(id, observer);
	return id;
}

function unsubscribe(id) {
	send(JSON.stringify({type: "stop", id: id}));
}

// Defines a GraphQL fetcher using the fetch API.
function fetcher(graphQLParams) {
	return {
		subscribe(a, b, c) {
			let observer;
			if (typeof a === "object") {
				observer = a;
			} else {
				observer = {
					next: a,
					error: b,
					complete: c,
				};
			}

			const id = query(graphQLParams, observer);

			return {
				unsubscribe() {
					if (queries.has(id)) {
						unsubscribe(id);
					}
				}
			}
		}
	};
}

// Render <GraphiQL /> into the body.
ReactDOM.render(
	React.createElement(GraphiQL, {fetcher}),
	document.getElementById("graphiql")
);
