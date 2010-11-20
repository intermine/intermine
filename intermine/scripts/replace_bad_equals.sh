find . -name '*.java' -exec perl -np -i.orig -e 's/((?:\w|\.|_|\w\(.*?\))+)\.equals\((".*?")/$2.equals($1/' {} \;
