#!/bin/bash
# Hackily scrape the currently available tomcat-7 version.

set -e

SCRIPT=${BASH_SOURCE[0]}
DIR=$(dirname $SCRIPT)

echo "Working relative to ''$DIR''"

for dep in perl unzip; do
  if test -z $(which $dep); then
    echo "ERROR: $dep not found - please install $dep"
    exit 1
  fi
done

if test -z $(which wget); then
  if test -z $(which curl); then
    echo 'ERROR: neither wget or curl are available - cannot fetch tomcat'
    exit 1
  else # curl is available - use that
    DOWNLOAD='curl -O'
    READURL='curl'
  fi
else # use wget
  DOWNLOAD='wget'
  READURL='wget -O -'
fi

TOMCAT_VERSION=$($READURL http://mirror.ox.ac.uk/sites/rsync.apache.org/tomcat/tomcat-7/ | grep folder.gif | perl -ne 'm/v(7\.\d+\.\d+)/; print $1;')

if test -z $TOMCAT_VERSION; then
  echo '#--- Error reading tomcat version'
  exit 1
fi

echo "#--- Using tomcat $TOMCAT_VERSION"
TOMCAT=apache-tomcat-${TOMCAT_VERSION}
$DOWNLOAD http://mirror.ox.ac.uk/sites/rsync.apache.org/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/${TOMCAT}.zip
unzip -q ${TOMCAT}.zip
cp ${DIR}/tomcat-users.xml ${TOMCAT}/conf/tomcat-users.xml
echo 'JAVA_OPTS="$JAVA_OPTS -Dorg.apache.el.parser.SKIP_IDENTIFIER_CHECK=true"' >> prefixed
echo 'export JAVA_OPTS' >> prefixed
cat ${TOMCAT}/bin/startup.sh >> prefixed
cp prefixed ${TOMCAT}/bin/startup.sh
sed -i=bak -e 's!<Context>!<Context sessionCookiePath="/" useHttpOnly="false">!' ${TOMCAT}/conf/context.xml
chmod +x ${TOMCAT}/bin/*.sh # startup.sh won't work unless catalina.sh is executable.

# Start tomcat on the default port (8080)
./${TOMCAT}/bin/startup.sh 2>&1

echo "#--- Started tomcat application container"
