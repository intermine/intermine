#!/bin/bash

### This script is used by travis to automatically deploy new
### master branch pushes to gh-pages. Thank http://www.steveklabnik.com/automatically_update_github_pages_with_travis_example/ for getting there.


set -o errexit -o nounset

rev=$(git rev-parse --short HEAD)

git init
git config user.name "Travis CI"
git config user.email "travis@fakemail.com"

git remote add upstream "https://$GH_TOKEN@github.com/$TRAVIS_REPO_SLUG.git"
git fetch --depth=1 upstream
git reset upstream/gh-pages

touch .
#stops gh-pages trying to build as jekyll
touch .nojekyll

#build db model for the javadoc
cd bio/test-all/dbmodel
ant clean-all
ant build-db

#build javadoc
cd ../../../imbuild/javadoc
ant clean
ant

#copy to the root
cp -r build/javadoc/* ../../

#commit
cd ../../
git add -A .
git commit -m "rebuild pages at ${rev}"
git push -q upstream HEAD:gh-pages
