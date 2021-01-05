# set up solr for testmine
# testmine's setup script populates these empty indexes

wget http://archive.apache.org/dist/lucene/solr/8.6.2/solr-8.6.2.tgz
tar xzf solr-8.6.2.tgz && ./solr-8.6.2/bin/solr start
./solr-8.6.2/bin/solr create -c intermine-search
./solr-8.6.2/bin/solr create -c intermine-autocomplete
