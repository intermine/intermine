#!/bin/sh
for dir in `ls -d */`
do
prj="${dir%%/}"

cd $prj
  echo "processing $prj"
  if [ -d main ]
  then
    cd $prj
    echo "Creating src dir"
    mkdir -p src
    echo "Moving main dir under src ..."
    git mv main/ src/
    cd src/main
    git mv src java
    echo "Moved /main/src directory to /src/main/java "
    cd ../..

    # if the resources directory is there 
    # AND the main directory, move resources into main
    if [ -d resources ]
    then
      git mv resources/* src/main/resources/
      rm -r resources/
    fi
  fi
  
  if [ -d test ]
  then
    echo "Moving test dir under src ..."
    git mv test/ src/
    cd src/test
    git mv src java
    echo "Moved /test/src directory to /src/test/java "
    cd ../..   
  fi

  # remove project properties files, they are pointless
  # keep main project properties file for now
  if [ -f src/main/project.properties ]
  then
    git rm src/main/project.properties
    echo "Removed main/project.properties "
  fi 
  if [ -f src/test/project.properties ]
  then
    git rm src/test/project.properties
    echo "Removed test/project.properties "
  fi 
  
  # if the build gradle file is not there, create
  if [ ! -f build.gradle ]
  then
    cp ../skeleton-build.gradle build.gradle
    echo "Created a build.gradle skeleton"
  fi

  # move additions file 
  if [ -f *_additions.xml ]
  then
    git mv *_additions.xml src/main/resources
    echo "Moved additions file to src/main/resources"
  fi

cd ..

done
