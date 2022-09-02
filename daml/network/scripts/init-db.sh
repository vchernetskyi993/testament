#!/bin/bash
set -e

PGPASSWORD="$POSTGRES_PASSWORD" psql \
  -v ON_ERROR_STOP=1 \
  -U "$POSTGRES_USER" \
  <<-EOSQL
  CREATE USER domain WITH ENCRYPTED PASSWORD 'domain';
	CREATE DATABASE domain;
	GRANT ALL ON DATABASE domain TO domain;

  CREATE USER government WITH ENCRYPTED PASSWORD 'government';
	CREATE DATABASE government;
	GRANT ALL ON DATABASE government TO government;

	CREATE USER provider WITH ENCRYPTED PASSWORD 'provider';
	CREATE DATABASE provider;
	GRANT ALL ON DATABASE provider TO provider;
EOSQL
