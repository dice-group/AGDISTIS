#AGDISTIS

AGDISTIS - Agnostic Named Entity Disambiguation. This projects aimes at delivering a framework for disambiguating a priori annotated named entities. More information about the project can be found here: http://aksw.org/projects/AGDISTIS

## Web Service

We deployed AGDISTIS as a RESTful service reachable via the following command:

```shell
curl --data-urlencode "text='The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.'&type='agdistis'" http://139.18.2.164:8080/AGDISTIS
```
or
```shell
curl --data-urlencode "text@test.txt" -d type=agdistis http://139.18.2.164:8080/AGDISTIS
```
AGDISTIS also provides also a Wrapper for DBpedia Spotlight. Just change the "type" to "spotlight" instead of "agdistis"
Please note that every entity you need disambiguated must be recognized beforehand.

## Knowledge Base

The important data for runnning AGDISTIS is stored in a Lucene 4.5.1 Index that can be found
<a href="http://139.18.2.164/rusbeck/index_dbpedia_39_en.7z">here</a>.

## Run your own webservice

For running AGDISTIS on your machine go to the root directory and of AGDISTIS and execute
```shell
mvn tomcat:run
```
Now a webservice is running on localhost:8080

## Running from source

The easiest way of running AGDISTIS from source is to have a look at the Java Class /src/test/java/AGDISTISTest.java


We hope you will enjoy using AGDISTIS!
