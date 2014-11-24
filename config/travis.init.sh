# Create all databases we might need.
psql -c 'create database notxmltest;' -U postgres
psql -c 'create database truncunittest;' -U postgres
psql -c 'create database flatmodetest;' -U postgres
psql -c 'create database fulldatatest;' -U postgres
psql -c 'create database userprofiletest;' -U postgres
psql -c 'create database unittest;' -U postgres
psql -c 'create database biotest;' -U postgres
psql -c 'create database biofulldatatest;' -U postgres

touch failures.list # Allowing us to cat it later.

mkdir ~/.intermine
cp config/ci.properties ~/.intermine/intermine-test.properties
sed -i 's/PG_USER/postgres/' ~/.intermine/intermine-test.properties
cp ~/.intermine/intermine-test.properties ~/.intermine/testmodel.properties
cp config/ci-bio.properties ~/.intermine/intermine-bio-test.properties
sed -i 's/PG_USER/postgres/' ~/.intermine/intermine-bio-test.properties
sudo pip install -r testmodel/webapp/selenium/requirements.txt
gem install travis-artifacts
./config/download_and_configure_tomcat.sh

# Build any models we might require.
ant -f intermine/objectstore/model/testmodel/build.xml
ant -f intermine/integrate/model/fulldata/build.xml
ant -f intermine/api/model/userprofile/build.xml
ant -f bio/test-all/dbmodel/build.xml build-db

