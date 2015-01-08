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

echo "#--- removing old installation."
rm -rf ${TOMCAT}.zip
rm -rf ${TOMCAT}

$DOWNLOAD http://mirror.ox.ac.uk/sites/rsync.apache.org/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/${TOMCAT}.zip
unzip -q ${TOMCAT}.zip

cp ${DIR}/tomcat-users.xml ${TOMCAT}/conf/tomcat-users.xml
# Propagate user's choice of user and password if given
if [ ! -z $TOMCAT_USER ]; then
    sed -i -e 's/username="manager"/username="'$TOMCAT_USER'"/' ${TOMCAT}/conf/tomcat-users.xml
fi
if [ ! -z $TOMCAT_PWD ]; then
    sed -i -e 's/password="manager"/password="'$TOMCAT_PWD'"/' ${TOMCAT}/conf/tomcat-users.xml
fi
cp ${DIR}/tomcat_set_env.sh ${TOMCAT}/bin/setenv.sh # Standard way of defining environment
if [ ! -z $JMX ]; then
    # Allow the user to connect on localhost:9090 with intermine:changeme
    cat ${DIR}/tomcat_jmx_conf.sh >> ${TOMCAT}/bin/setenv.sh
    mkdir -p $HOME/.jmx
    cp ${DIR}/jmxremote.password $HOME/.jmx/remote.password
    cp ${DIR}/jmxremote.access $HOME/.jmx/remote.access
    chmod 0600 $HOME/.jmx/remote.*
fi
sed -i=bak -e 's!<Context>!<Context sessionCookiePath="/" useHttpOnly="false">!' ${TOMCAT}/conf/context.xml
chmod +x ${TOMCAT}/bin/*.sh # startup.sh won't work unless catalina.sh is executable.

# Start tomcat on the default port (8080)
./${TOMCAT}/bin/startup.sh 2>&1

echo "#--- Started tomcat application container"
