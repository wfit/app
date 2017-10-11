#!/usr/bin/env bash
ssh_path=${GIT_SSH:-ssh}
sbt --no-colors docker:stage
cd server/target/docker
tar -zcvf - stage | "${ssh_path}" debian@app.wfit.ovh "
	tar -xzf -;
	docker build stage -t wfit-server:latest;
	rm -R stage;
	docker stop wfit-server && docker rm -f wfit-server;
	docker run -d --name wfit-server --restart unless-stopped -p 127.0.0.1:9000:9000 --env-file /home/debian/env wfit-server:latest;
	docker image prune -f;"
