#!/usr/bin/env bash
sbt docker:stage | pv -t -N compile -i 0.1
cd server/target/docker
tar -zcvf stage.tar.gz stage | pv -pt -l -s $(find stage | wc -l) -i 0.1 -N compress > /dev/null
pv -N upload -i 0.1 -pter stage.tar.gz | ssh debian@app.wfit.ovh "
	tar -xzf -;
	docker build stage -t wfit-server:latest;
	rm -R stage;
	docker stop wfit-server && docker rm -f wfit-server;
	docker run -d --name wfit-server --restart unless-stopped -p 127.0.0.1:9000:9000 --env-file /home/debian/env wfit-server:latest;
	docker image prune -f;"
rm stage.tar.gz
