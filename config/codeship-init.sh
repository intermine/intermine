# Errors are fatal.
set -e

# Create databases.
psql -c 'create database notxmltest;'
psql -c 'create database truncunittest;'
psql -c 'create database flatmodetest;'
psql -c 'create database fulldatatest;'
psql -c 'create database userprofiletest;'
psql -c 'create database unittest;'
psql -c 'create database biotest;'
psql -c 'create database biofulldatatest;'

# Create property files for running tests.
mkdir $HOME/.intermine
cp config/ci.properties $HOME/.intermine/intermine-test.properties
sed -i "s/PG_USER/$PG_USER/" $HOME/.intermine/intermine-test.properties
sed -i "s/PG_PASS/$PG_PASSWORD/" $HOME/.intermine/intermine-test.properties
cp $HOME/.intermine/intermine-test.properties $HOME/.intermine/testmodel.properties
cp config/ci-bio.properties $HOME/.intermine/intermine-bio-test.properties
sed -i "s/PG_USER/$PG_USER/" $HOME/.intermine/intermine-bio-test.properties
sed -i "s/PG_PASS/$PG_PASSWORD/" $HOME/.intermine/intermine-bio-test.properties

# Install requirements for running selenium test.
pip install -r testmodel/webapp/selenium/requirements.txt

# Install and configure tomcat 7.0.53
TOMCAT_VERSION=7.0.53
wget http://mirror.ox.ac.uk/sites/rsync.apache.org/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.zip
unzip apache-tomcat-${TOMCAT_VERSION}.zip
cp config/tomcat-users.xml apache-tomcat-${TOMCAT_VERSION}/conf/tomcat-users.xml
echo 'JAVA_OPTS="$JAVA_OPTS -Dorg.apache.el.parser.SKIP_IDENTIFIER_CHECK=true"' >> prefixed
echo 'export JAVA_OPTS' >> prefixed
cat apache-tomcat-${TOMCAT_VERSION}/bin/startup.sh >> prefixed
cp prefixed apache-tomcat-${TOMCAT_VERSION}/bin/startup.sh
sed -i 's!<Context>!<Context sessionCookiePath="/" useHttpOnly="false">!' apache-tomcat-${TOMCAT_VERSION}/conf/context.xml
chmod +x apache-tomcat-${TOMCAT_VERSION}/bin/catalina.sh # startup.sh won't work unless catalina.sh is executable.

# Start tomcat on the default port (8080)
nohup bash -c "sh apache-tomcat-${TOMCAT_VERSION}/bin/startup.sh 2>&1 " && sleep 4; cat nohup.out

ant -f bio/test-all/dbmodel/build.xml build-db
