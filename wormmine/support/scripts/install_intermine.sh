#!/bin/bash

# Checks out an InterMine version that is suitable to be used in either
#
#   production, staging, or development
#
# environment.

cwd="`pwd`"
mode="$1"

if [ $mode == "production" ] ; then
  echo "Installing for a production environment."
elif [ $mode == "staging" ] ; then
  echo "Installing for a staging environment."
elif [ $mode == "development" ] ; then
  echo "Installing for a development environment."
  mode=""
else
  echo "One parameter needed:"
  echo ""
  echo "  production  : set-up a production environment"
  echo "  staging     : set-up a staging environment, that possibly becomes production later"
  echo "  development : set-up the latest development version of the code"
  exit 1
fi

if [ -d acedb-dev ] ; then
  cd acedb-dev
elif [ -d website-intermine ] ; then
  cd website-intermine/acedb-dev
else
  echo "Please execute this script with either:"
  echo ""
  echo "  1. the directory 'website-intermine' in the current working directory, or"
  echo "  2. within the 'website-intermine' directory."
  exit 2
fi

rmdir intermine
ln -s ../../intermine .

cd ../..
if [ $mode == "staging" ] ; then
  # Due to disk space constraints, set-up intermine on the large NFS drive under /nfs on staging.
  rm -rfi intermine
  ln -s /nfs/wormbase/data/intermine intermine
  rm -rfi intermine
  cd /nfs/wormbase/data
fi
git clone https://github.com/WormBase/intermine.git
cd intermine
if [ ! $mode == "" ] ; then
  git checkout $mode
fi

cd "$cwd"

if [ ! -d ~/.intermine ] ; then
  mkdir ~/.intermine
fi

