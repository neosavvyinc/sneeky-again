#!/bin/bash

DB_NAME="currant"

for SUF in "" "_test"
do
  DB="$DB_NAME$SUF"
  echo "suf: $SUF"
  echo "DB: $DB"
  echo "Cleanup ..."
  echo "DROP OWNED BY currant_user$SUF CASCADE;" | psql -h localhost -p 5432 $DB
  echo "DROP OWNED BY readwrite_currant$SUF CASCADE;" | psql -h localhost -p 5432 $DB
  echo "DROP OWNED BY integration_test_currant$SUF CASCADE;" | psql -h localhost -p 5432 $DB
  echo "DROP ROLE IF EXISTS currant_user$SUF;" | psql -h localhost -p 5432 $DB
  echo "DROP ROLE IF EXISTS readwrite_currant$SUF;" | psql -h localhost -p 5432 $DB
  echo "DROP ROLE IF EXISTS integration_test_currant$SUF;" | psql -h localhost -p 5432 $DB
  echo "DROP DATABASE IF EXISTS $DB;" | psql -h localhost -p 5432 postgres
 
  echo "Creating databases ... "
  echo "CREATE DATABASE $DB;" | psql -h localhost -p 5432 postgres

  echo ".....creating schema...."

  psql -h localhost -p 5432 $DB -f ddl.sql


  echo "Initializing system accounts ..."

  echo " ... RW service ..."

  currantPasswordEscaped="currant_user$SUF"

  echo "CREATE ROLE readwrite_currant$SUF; GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO readwrite_currant$SUF; GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO readwrite_currant$SUF;" | psql -h localhost -p 5432 $DB
  echo "CREATE ROLE currant_user$SUF with LOGIN PASSWORD '$currantPasswordEscaped' IN ROLE readwrite_currant$SUF;" | psql -h localhost -p 5432 $DB
  echo "CREATE ROLE integration_test_currant$SUF with LOGIN PASSWORD 'integration_test_currant$SUF' IN ROLE readwrite_currant$SUF;" | psql -h localhost -p 5432 $DB
  echo "ALTER ROLE integration_test_currant$SUF CREATEDB;" | psql -h localhost -p 5432 $DB

  echo "Done!"
done
