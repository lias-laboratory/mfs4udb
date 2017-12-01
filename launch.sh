#!/usr/bin/env bash

mvn clean package -Dmaven.test.skip=true
cd target
java -Xmx6144m -cp mfs4udb-jar.jar fr.ensma.lias.mfs4udb.LauncherMain


