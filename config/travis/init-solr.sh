# set up solr for testmine
# testmine's setup script populates these empty indexes

wget http://archive.apache.org/dist/lucene/solr/7.2.1/solr-7.2.1.tgz  
tar xzf solr-7.2.1.tgz && ./solr-7.2.1/bin/solr start
./solr-7.2.1/bin/solr create -c intermine-search
./solr-7.2.1/bin/solr create -c intermine-autocomplete