perl -nlp -i.orig -e 's/((?:\w|\.|_|\w\(.*?\))+)\.equals\((".*?")/$2.equals($1/' $(find . -type f -name '*.java' -exec grep -l '.equals("' {} +)
