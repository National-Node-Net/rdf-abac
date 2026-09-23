// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.
/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */


package uk.gov.dbt.ndtp.jena.pbac.fuseki;

// Used by TestServerPBAC.

class ConstForTestServerPBAC {

    /** Fragment of a configuration file. Server. */
    static String configServer = """
        ## Configuration using authz: forms.

        PREFIX :        <#>
        PREFIX fuseki:  <http://jena.apache.org/fuseki#>
        PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX ja:      <http://jena.hpl.hp.com/2005/11/Assembler#>
        PREFIX tdb2:    <http://jena.apache.org/2016/tdb#>

        PREFIX authz:   <http://ndtp.co.uk/security#>

        :service1 rdf:type fuseki:Service ;
            fuseki:name "ds" ;
            ## Must be fuseki:{operation} for dynamic dispatch.
            fuseki:endpoint [ fuseki:operation fuseki:query ] ;
            fuseki:endpoint [ fuseki:operation fuseki:gsp-r ] ;

            ## Same but as named services
            fuseki:endpoint [ fuseki:operation authz:query ; fuseki:name "query" ] ;
            fuseki:endpoint [ fuseki:operation authz:gsp-r ; fuseki:name "gsp-r" ] ;

            fuseki:endpoint [ fuseki:operation authz:upload ; fuseki:name "upload" ] ;
            fuseki:endpoint [ fuseki:operation authz:labels ; fuseki:name "labels" ] ;

            fuseki:dataset :dataset ;
            .
        """;

    // ====
    // With authz:tripleDefaultLabels     "*" ;
    /** Fragment of a configuration file. Dataset. */
    static String configDatasetDftLabelStar = """
        ## PBAC Dataset:
        :dataset rdf:type authz:DatasetAuthz ;
            authz:attributes <file:src/test/files/server/all/attribute-store-all.ttl> ;

            ##authz:accessAttributes      "";
            authz:tripleDefaultLabels     "*" ;

            authz:dataset :datasetBase;
            .

        # Data: Transactional in-memory dataset.
        :datasetBase rdf:type ja:MemoryDataset .
        """;

    // ====
    // With no authz:tripleDefaultLabels
    /** Fragment of a configuration file. Dataset. */
    static String configDatasetNoDftLabel = """

        ## PBAC Dataset:
        :dataset rdf:type authz:DatasetAuthz ;
            authz:attributes <file:src/test/files/server/all/attribute-store-all.ttl> ;

            ##authz:accessAttributes      "";
            ##authz:tripleDefaultLabels     "*" ;

            authz:dataset :datasetBase;
            .

        # Data: Transactional in-memory dataset.
        :datasetBase rdf:type ja:MemoryDataset .
        """;

    // ====
    // With authz:tripleDefaultLabels status=confidential
    /** Fragment of a configuration file. Dataset. */
    static String configDatasetDfLabelConfidential = """

        ## PBAC Dataset:
        :dataset rdf:type authz:DatasetAuthz ;
            authz:attributes <file:src/test/files/server/all/attribute-store-all.ttl> ;

            ##authz:accessAttributes      "";
            authz:tripleDefaultLabels     "status=confidential" ;

            authz:dataset :datasetBase;
            .

        # Data: Transactional in-memory dataset.
        :datasetBase rdf:type ja:MemoryDataset .
        """;


    // ====
    /** The data, with labels using TriG. */
    static String dataWithLabelsTriG = """
        Content-type: application/trig

        PREFIX : <http://example/>

            :s :p1 123 .
            :s :p2 456 .
            :s :p2 789 .

            :s :q "No label" .

            :s1 :p1 1234 .
            :s1 :p2 2345 .

            :x :public "abc" .
            :x :public "def" .

            :x :confidential "C-abc" .
            :x :sensitive    "S-abc" .
            :x :private      "P-abc" .

            PREFIX authz: <http://ndtp.co.uk/security#>

            GRAPH authz:labels {

                [ authz:pattern ':s :p1 123'  ; authz:label "level-1" ] .
                [ authz:pattern ':s :p2 456'  ; authz:label "manager", "level-1" ] .
                [ authz:pattern ':s :p2 789'  ; authz:label "manager"  ] .

                [ authz:pattern ':s1 :p1 1234' ; authz:label "manager"  ] .
                [ authz:pattern ':s1 :p2 2345' ; authz:label "engineer" ] .

                [ authz:pattern ':x  :public        "abc"' ; authz:label "status=public" ] .
                [ authz:pattern ':x  :public        "def"' ; authz:label "status=public" ] .
                [ authz:pattern ':x  :confidential  "C-abc"' ; authz:label "status=confidential" ] .
                [ authz:pattern ':x  :sensitive     "S-abc"' ; authz:label "status=sensitive" ] .
                [ authz:pattern ':x  :private       "P-abc"' ; authz:label "status=private" ] .
            }
            """;

    /**
     * Data as triples with security header.
     * To set different labels, data is sent as several messages.
     */
    static String dataTriplesLabelOpen = """
        Content-type: text/turtle
        Security-Label: *

        PREFIX : <http://example/>
        :x :open "header-*" .
        """;

    static String dataTriplesLabelEmpty = """
        Content-type: text/turtle
        Security-Label: ""

        PREFIX : <http://example/>
        :x :empty "Empty header label" .
        """;

    static String dataTriplesLabelNone = """
        Content-type: text/turtle

        PREFIX : <http://example/>
        :x :none "No Label" .
        """;

    static String dataTriplesLabelDeny = """
        Content-type: text/turtle
        Security-Label: !

        PREFIX : <http://example/>
        :x :never "header-!" .
        """;

    static String payloadTriplesLabelPublic = """
        Content-type: text/turtle
        Security-Label: status=public

        PREFIX : <http://example/>
        :x :public "abc" .
        :x :public "def" .
    """;

    static String payloadTriplesLabelConfidential = """
        Content-type: text/turtle
        Security-Label: status=confidential

        PREFIX : <http://example/>
        :x :confidential "C-abc" .
        """;

    static String payloadTriplesLabelSensitive = """
        Content-type: text/turtle
        Security-Label: status=sensitive

        PREFIX : <http://example/>
        :x :sensitive "S-abc" .
        """;

    static String payloadTriplesLabelPrivate = """
        Content-type: text/turtle
        Security-Label: status=private

        PREFIX : <http://example/>
        :x :private "P-abc" .
        """;
}
