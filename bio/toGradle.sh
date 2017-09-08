#!/bin/bash

echo "Converting $1 project to gradle"
cd $1
echo "Creating src dir"
mkdir -p src
echo "Moving main and test dirs under src ..."cd sr
git mv main/ src/
git mv test/ src/
echo "Done!"

cd src/main
git mv src java
rm .project
rm .classpath
rm .checkstyle

cd ../test
git mv src java
rm .project
rm .classpath
rm .checkstyle
echo "Renamed main/src into main/java and test/src into test/java"

cd ../..
cp ../skeleton-build.gradle build.gradle
echo "Created a build.gradle skeleton"  

 
