#!/bin/bash

### This script is used by travis to automatically deploy new
### master branch pushes to gh-pages


set -o errexit -o nounset

rev=$(git rev-parse --short HEAD)

git init
git config user.name "Travis CI"
git config user.email "travis@fakemail.com"

git remote add upstream "https://$GH_TOKEN@github.com/$TRAVIS_REPO_SLUG.git"
git fetch upstream
git reset upstream/gh-pages

touch .
touch .nojekyll

cd bio/test-all/dbmodel
ant build-db
ant javadoc

git add -A .

git commit -m "rebuild pages at ${rev}"
git push -q upstream HEAD:gh-pages
