#!/bin/bash
# Hackily scrape the currently available tomcat-7 version.

set -e

for dep in perl unzip; do
  if test -z $(which $dep); then
    echo "ERROR: $dep not found - please install $dep"
    exit 1
  fi
done

if test -z $(which wget); then
  if test -z $(which curl); then
    echo 'ERROR: neither wget or curl are available - cannot fetch tomcat'
  else # curl is available - use that
    alias download='curl -O'
    alias readurl='curl'
  fi
else # use wget
  alias download='wget'
  alias readurl='wget -O -'
fi

TOMCAT_VERSION=$(readurl http://mirror.ox.ac.uk/sites/rsync.apache.org/tomcat/tomcat-7/ | grep folder.gif | perl -ne 'm/v(7\.\d+\.\d+)/; print $1;')
echo "#--- Using tomcat $TOMCAT_VERSION"
download http://mirror.ox.ac.uk/sites/rsync.apache.org/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.zip
unzip apache-tomcat-${TOMCAT_VERSION}.zip
cp config/tomcat-users.xml apache-tomcat-${TOMCAT_VERSION}/conf/tomcat-users.xml
echo 'JAVA_OPTS="$JAVA_OPTS -Dorg.apache.el.parser.SKIP_IDENTIFIER_CHECK=true"' >> prefixed
echo 'export JAVA_OPTS' >> prefixed
cat apache-tomcat-${TOMCAT_VERSION}/bin/startup.sh >> prefixed
cp prefixed apache-tomcat-${TOMCAT_VERSION}/bin/startup.sh
sed -i=bak -e 's!<Context>!<Context sessionCookiePath="/" useHttpOnly="false">!' apache-tomcat-${TOMCAT_VERSION}/conf/context.xml
chmod +x apache-tomcat-${TOMCAT_VERSION}/bin/catalina.sh # startup.sh won't work unless catalina.sh is executable.
sh apache-tomcat-${TOMCAT_VERSION}/bin/startup.sh 2>&1 & sleep 3
echo "#--- Started tomcat application container"
