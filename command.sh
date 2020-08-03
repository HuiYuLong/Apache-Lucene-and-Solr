sudo docker cp ./lucene/demo/data/wiki-small/en/test.html Project1:/lucene-solr/lucene/demo/data/wiki-small/en/
sudo docker cp ./lucene/demo/build.xml Project1:/lucene-solr/lucene/demo/

sudo docker cp ./lucene/demo/src/java/org/apache/lucene/demo/HtmlIndexFiles.java Project1:/lucene-solr/lucene/demo/src/java/org/apache/lucene/demo
sudo docker cp ./lucene/demo/src/java/org/apache/lucene/demo/TFIDFHtmlIndexFiles.java Project1:/lucene-solr/lucene/demo/src/java/org/apache/lucene/demo
sudo docker cp ./lucene/demo/src/java/org/apache/lucene/demo/CMPT456Analyzer.java Project1:/lucene-solr/lucene/demo/src/java/org/apache/lucene/demo
sudo docker cp ./lucene/demo/src/java/org/apache/lucene/demo/TFIDFSearchFiles.java Project1:/lucene-solr/lucene/demo/src/java/org/apache/lucene/demo
sudo docker cp ./lucene/demo/src/java/org/apache/lucene/demo/SimpleMetrics.java Project1:/lucene-solr/lucene/demo/src/java/org/apache/lucene/demo
sudo docker cp ./lucene/demo/src/java/org/apache/lucene/demo/CMPT456Similarity.java Project1:/lucene-solr/lucene/demo/src/java/org/apache/lucene/demo

sudo docker start Project1
echo 'start the container'
sudo docker exec -it Project1 /bin/bash
