# RDF ABAC: Fuseki Server
**Repository:** `rdf-abac`  
**Description:** `Apache Jena Fuseki implementation`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The `rdf-abac-fuseki-server` module provides a ready-to-go implementation of an Apache Jena Fuseki server that can be run immediately from the generated jar.

This allows users to immediately test and experiment with the functionality on offer.


## Examples
### Running the server (Intellij)
I have included some Intellij runtime configuration - [LocalServer](../.idea/runConfigurations/LocalServer.xml). 

### Running the server (manually)
Assuming the version of the repo is 0.71.10-SNAPSHOT and the command is being run from the "docs" directory.
```bash
java -jar ../rdf-abac-fuseki-server/target/rdf-abac-fuseki-server-0.71.10-SNAPSHOT.jar --conf=../rdf-abac-fuseki/src/test/files/server/documentation-example-config.ttl
```
This will run a server accessible from port 3030 with two datasets called `/unsecuredDataset` and `/securedDataset`.
They can be interacted with via the following urls:
* http://localhost:3030/securedDataset
* http://localhost:3030/unsecuredDataset

As the names suggest, one is configured to make use of ABAC and the other is not.  

### Configuration
#### Service
If we review the configuration below, we can see that we have defined a Service ("secureService") and named it "securedDataset".
We then further define it to have 3 operations, more colloquially thought of as endpoints.

By also providing the endpoints with names, we can ensure that the path is as we would like it to be.
Note: query is called "query" by default.
* http://localhost:3030/securedDataset/query
* http://localhost:3030/securedDataset/read
* http://localhost:3030/securedDataset/update
```rdf
:secureService rdf:type fuseki:Service ;
    fuseki:name "securedDataset" ;
    fuseki:endpoint [ fuseki:operation fuseki:query ] ;
    fuseki:endpoint [ fuseki:operation fuseki:gsp-r ; fuseki:name "read"] ;
    fuseki:endpoint [ fuseki:operation fuseki:upload ; fuseki:name "upload" ] ;
    fuseki:dataset :dataset ;
    .
```

#### Dataset

```rdf
:dataset rdf:type authz:DatasetAuthz ;
    authz:dataset :inMemoryDatabase;
    authz:attributes <file:documentation-example-attribute-store.ttl> ;
    authz:tripleDefaultLabels "!";
    .
```


### Queries (Secured)
#### Upload
```bash
curl --location 'http://localhost:3030/securedDataset/upload' \
--header 'Security-Label: !' \
--header 'Content-Type: application/trig' \
--data-binary '@../rdf-abac-fuseki/src/test/files/server/documentation-example-data.trig'
```
*Note:* We are indicating that the default label be "!" such that any data without an explicit label; defined is not accesible

#### GSP-R
##### User Paul
This will return all the data that user Paul is able to see.
```bash
curl --location 'http://localhost:3030/securedDataset/read' \
--header 'Authorization: Bearer dXNlcjpQYXVs'
```
*Note:* We are using the base64 encryption of `user:Paul` to query.
##### User Jane
This will return all the data that user Jane is able to see.
```bash
curl --location 'http://localhost:3030/securedDataset/read' \
--header 'Authorization: Bearer dXNlcjpKYW5l'
```
*Note:* We are using the base64 encryption of `user:Jane` to query.

##### Other users
You can swap out the bearer values above for the remaining users (as defined in the user attribute config) with the following:

| User  | Base64           | Accessible item count (of 13) | 
|-------|------------------|-------------------------------|
| Laura | dXNlcjpMYXVyYQ== | 6                             |
| Frank | dXNlcjpGcmFuaw== | 3                             |
| Ryan  | dXNlcjpSeWFu     | 6                             |
| Jane  | dXNlcjpKYW5l     | 6                             |
| Paul  | dXNlcjpQYXVs     | 12                            |

##### Count
This query (for user Jane) will return 6 results as that is all that Jane has access to see.
```bash
curl -X POST --location 'http://localhost:3030/securedDataset/query' \
--header 'Accept: application/sparql-results+json' \
--header 'Content-Type: application/sparql-query' \
--header 'Authorization: Bearer dXNlcjpKYW5l' \
--data 'SELECT (COUNT(*) AS ?count)  WHERE { ?s ?p ?o }' 
```

### Queries (Unsecured)
#### Upload
```bash
curl --location 'http://localhost:3030/unsecuredDataset/upload' \
--header 'Content-Type: application/trig' \
--data-binary '@../rdf-abac-fuseki/src/test/files/server/documentation-example-data.trig'
```
*Note:* The lack of a securityLabel header. It will be ignored if included. 

#### GSP-R
A general query will return all the data in the dataset. 
```bash
curl --location 'http://localhost:3030/unsecuredDataset/read'
```

*Note*: The lack of authentication - it is unsecured after all. 

##### Count
This query (again for user Jane) will return 13 results, unlike the 6 above, as no access control is being applied.
```bash
curl -X POST --location 'http://localhost:3030/unsecuredDataset/query' \
--header 'Accept: application/sparql-results+json' \
--header 'Content-Type: application/sparql-query' \
--header 'Authorization: Bearer dXNlcjpKYW5l' \
--data 'SELECT (COUNT(*) AS ?count)  WHERE { ?s ?p ?o }' 
```
*Note:* Any user will return 13. It can also be run without any user authentication (or the previous Accept header - as it is the default)
```bash
curl -X POST --location 'http://localhost:3030/unsecuredDataset/query' \
--header 'Content-Type: application/sparql-query' \
--data 'SELECT (COUNT(*) AS ?count)  WHERE { ?s ?p ?o }' 
```

© Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally attributed to the UK's Department for Business, Innovation, Science and Trade (BIST) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
