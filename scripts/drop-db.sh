#!/bin/bash

DB_NAME="currant"

for SUF in "" "_test"
do
  DB="$DB_NAME$SUF"
  echo "suf: $SUF"
  echo "DB: $DB"
  echo "Cleanup ..."

  echo "Initializing system accounts ..."

  psql -h localhost -p 5432 $DB -f dropddl.sql

  echo " ... RW service ..."

  currantPasswordEscaped="currant_user$SUF"

  echo "Done!"
done
