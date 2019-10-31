#!/usr/bin/env bash

if [ $# -ne 3 ]; then
	echo "Parameters are missing."
	echo "First = number of vectors (i.e. 100), Second = Postgresql user (i.e. liasidd), Third = password (i.e. psql)."
	echo "Example: ./mfs4udb-generate.sh 100 3 liasidd psql"
   	exit 1
fi

export PGPASSWORD=$3
echo "********* 8 *********"
randdataset -c -d 16 -n $1 -C -R -T lasttabc > datac.sql
randdataset -i -d 16 -n $1 -C -R -T lasttabi > datai.sql
randdataset -a -d 16 -n $1 -C -R -T lasttaba > dataa.sql
echo "********* create data *********"
psql -d postgres -U $2 -f datac.sql
psql -d postgres -U $2 -f datai.sql
psql -d postgres -U $2 -f dataa.sql
echo "********* add column *********"
psql -d postgres -U $2 -f sql/appendc.sql
psql -d postgres -U $2 -f sql/appendi.sql
psql -d postgres -U $2 -f sql/appenda.sql
echo "********* index *********"
psql -d postgres -U $2 -f sql/indexc.sql
psql -d postgres -U $2 -f sql/indexi.sql
psql -d postgres -U $2 -f sql/indexa.sql
