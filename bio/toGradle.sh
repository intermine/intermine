#!/bin/bash

echo "Converting $1 project to gradle"
cd $1
echo "Creating src dir"
mkdir -p src
echo "Moving main and test dirs under src ..."
git mv main/ src/
git mv test/src/
echo "Done!"

cd src/main
mv src/ java/
cd ../test
mv src/ java/
echo "Renamed main/src into main/java and test/src into test/java"

cp ../skeleton-build.gradle .
mv skeleton-build.gradle build.gradle
echo "Created a build.gradle skeleton"  

 
