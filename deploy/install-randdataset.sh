#!/usr/bin/env bash

wget http://pgfoundry.org/frs/download.php/1666/randdataset-1.1.0.tar.gz
tar xzf randdataset-1.1.0.tar.gz
cd randdataset-1.1.0
./configure
make
make install

randdataset -h
