#!/bin/bash

####################################################################################################
## Variable declarations

INTERPRO_MYSQL_DUMP=./interpro_12.1.mysql.gz

TABLE_NAME_FILE=./table_list.txt

INSERT_FILE_SUFFIX=.SQL

## An alternative schema in which to move the data loaded into the 'public' schema from, which we
## can then create tables in the public schema from, which are then read in by the DBRetriever class
ALT_SCHEMA=interpro_src_data

# Note: we need to run the script on the database server (i.e. gollum) to get around postgresql's
# need for a password on connecting otherwise. (Or hack it to work some other way!)
DATABASE=interpro
DEFAULT_INTERPRO_USER=interpro

# Random Temp File
CREATE_TABLE_GREP_LIST=./create_table_grep_list.txt

# Where the raw MySQL format create statements end up...
RAW_CREATE_TABLE_FILE=raw_create_table_statments.sql
# Where we need to put the nicely edited postgres format create statements into...
CREATE_TABLES_CLEANED_FILE=./create_postgres_interpro_tables_cleaned.sql


####################################################################################################
## 1: Download the interpro mysql dump -- MANUAL INTERVENTION REQUIRED

#Check to see if there is the dump file...
if test -s "$INTERPRO_MYSQL_DUMP" ; then
    echo INTERPRO MYSQL DUMP FOUND - CONTINUING!
else
    echo DONWLOAD INTERPRO $INTERPRO_MYSQL_DUMP TO THE LOCAL DIRECTORY!
    exit
fi


####################################################################################################
## 2: Extract the create table statements -- MANUAL INTERVENTION REQUIRED
##
##    These will need modifying - maybe doable by code if the create statement is well formated...

if test -s $RAW_CREATE_TABLE_FILE; then
    echo FOUND EXISTING $RAW_CREATE_TABLE_FILE - CONTINUING!
else
    ## Renew files as required
    rm -f $RAW_CREATE_TABLE_FILE
    touch $RAW_CREATE_TABLE_FILE

    if test -s "$CREATE_TABLE_GREP_LIST" ; then
        rm -f $CREATE_TABLE_GREP_LIST
    fi
    touch $CREATE_TABLE_GREP_LIST

    ## Make a suitable file for grep to use to search with for the create_statements
    for i in `cat $TABLE_NAME_FILE` ; do
        echo "CREATE TABLE $i ">> $CREATE_TABLE_GREP_LIST
    done

    ## Extract the create statements && remove some Oracle crud, e.g. "SYS_NC00010"
    gzip -dc $INTERPRO_MYSQL_DUMP | grep -A 25 -F -f $CREATE_TABLE_GREP_LIST | grep -v 'INSERT INTO' | grep -v '\-\-' | grep -v 'SYS_' >> $RAW_CREATE_TABLE_FILE

    ## Some housekeeping
    rm -f $CREATE_TABLE_GREP_LIST
fi

if test -s "$CREATE_TABLES_CLEANED_FILE"; then
    echo AN EDITED CREATE_TABLES_CLEANED_FILE WAS FOUND - CONTINUING!
else
    echo YOU NEED TO CONVERT THE RAW CREATE TABLE STATEMENTS FILE INTO THE CLEANED ONE!!
    touch $CREATE_TABLES_CLEANED_FILE
    exit
fi


####################################################################################################
## 3: Extract the related insert statements...
##    Was ./build_insert_file.sh

#Remove any previous INSERT files...
rm -f *$INSERT_FILE_SUFFIX

for i in `cat $TABLE_NAME_FILE` ; do

    if test -s $i.SQL then
        echo REMOVING A PRE-EXISTING DATAFILE NAMED $i.SQL
    fi

    echo CREATING A DATAFILE NAMED $i.SQL
    gzip -d < $INTERPRO_MYSQL_DUMP | grep 'INSERT INTO $i ' > $i.SQL
done


####################################################################################################
## 4: Create the tables in the public/default schema using some modified create table statements
##    extracted from the Interpro data dump file..
##
psql -q -U $DEFAULT_INTERPRO_USER -d $DATABASE -f $CREATE_TABLES_CLEANED_FILE


####################################################################################################
## 5: Insert the raw table data into the public schema

for i in `cat $TABLE_NAME_FILE` ; do
    TARGET_SQL_FILE=`echo $i$INSERT_FILE_SUFFIX`
    echo LOADING $TARGET_SQL_FILE

    psql -q -U $DEFAULT_INTERPRO_USER -d $DATABASE -f $TARGET_SQL_FILE
done


####################################################################################################
## 6: (Re)Create the alt schema, migrate the raw public data, delete the raw public tables

## Drop any previous schema -- todo check in the pg_catalog for this schema...
psql -q -U $DEFAULT_INTERPRO_USER -d $DATABASE -c "drop schema $ALT_SCHEMA cascade;"

## Re-Create the schema
psql -q -U $DEFAULT_INTERPRO_USER -d $DATABASE -c "create schema $ALT_SCHEMA;"

## Move each table to schema other than the public one, then drop the table from the public schema
##  in order to make space for the tables we intend to derive from them.

for i in `cat $TABLE_NAME_FILE` ; do

    echo COPYING TABLE NAMED $i TO SCHEMA NAMED $ALT_SCHEMA
    MOVE_CMD="create table $ALT_SCHEMA.$i as select * from $i;"
    psql -q -U $DEFAULT_INTERPRO_USER -d $DATABASE -c "$MOVE_CMD"

    echo NOW DROPPING TABLE $i
    DROP_CMD="drop table $i;"
    psql -q -U $DEFAULT_INTERPRO_USER -d $DATABASE -c "$DROP_CMD"
done


####################################################################################################
##DONE IN THE ant build.xml FILE -- MAYBE BEST TO BE DONE HERE
## 7: Build the condensed public schema from the full interpro tables...
##psql -q -U $DEFAULT_INTERPRO_USER -d $DATABASE -f ./buildPostgresInterproDbFromInterproReleaseDb.sql
