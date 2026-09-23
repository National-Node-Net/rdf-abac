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


package uk.gov.dbt.ndtp.jena.pbac.rocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import uk.gov.dbt.ndtp.jena.pbac.SysPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import uk.gov.dbt.ndtp.jena.pbac.lib.VocabAuthzDataset;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.tdb2.sys.TDBInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestLabelsRocksDBNormalization {

    static String DIR = "target/NormalizationStore";

    @BeforeEach public void beforeEach() {
        FileOps.ensureDir(DIR);
        FileOps.clearAll(DIR);
    }


    @AfterEach  public void afterEach() {
        FileOps.clearAll(DIR);
    }

    @Test public void testDouble() {
        DatasetGraphPBAC dsgz = assembleFromString(authzDatasetAssemblerPersistent1);
        Triple triple = SSE.parseTriple("(:s :p '0.1'^^xsd:double)");
        roundTripWithLabel(dsgz, triple);
        TDBInternal.expel(dsgz.getBase());
    }

    @Test public void testInt() {
        DatasetGraphPBAC dsgz = assembleFromString(authzDatasetAssemblerPersistent2);
        Triple triple = SSE.parseTriple("(:s :p '00001'^^xsd:int)");
        roundTripWithLabel(dsgz, triple);
        TDBInternal.expel(dsgz.getBase());
    }

    private static void roundTripWithLabel(DatasetGraphPBAC dsgz, Triple triple) {
        dsgz.executeRead(()->{
            // Check empty.
            boolean b2 = dsgz.getDefaultGraph().isEmpty();
            boolean b1 = dsgz.labelsStore().isEmpty();
            assertTrue(b1);
            assertTrue(b2);
        });


        List<String> labels = List.of("XYZ");

        // Store in the database and in the labels store.
        dsgz.executeWrite(()->{
            dsgz.getDefaultGraph().add(triple);
            dsgz.labelsStore().add(triple, labels);
        });

        // Only needed if there has been a label lookup before now.
        //((LabelsStoreRocksDB)dsgz.labelsStore()).clearTripleLookupCache();

        dsgz.executeRead(()->{
            Triple tFind= dsgz.getDefaultGraph().find().next();
            List<String> x2 = dsgz.labelsStore().labelsForTriples(tFind);
            assertEquals(labels, x2);
        });
    }

    public static String authzDatasetAssemblerPersistent1 = """
            ## PBAC Dataset assembler.
            PREFIX :        <#>
            PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX ja:      <http://jena.hpl.hp.com/2005/11/Assembler#>
            PREFIX tdb2:    <http://jena.apache.org/2016/tdb#>
            PREFIX authz:   <http://ndtp.co.uk/security#>

            ## PBAC Dataset: Rocks labels store. TDB2
            :dataset rdf:type authz:DatasetAuthz ;
                authz:labelsStore           [ authz:labelsStorePath "target/NormalizationStore/LabelsStore-1.db" ] ;
                authz:attributes            <file:src/test/files/dataset/attribute-store.ttl> ;
                authz:tripleDefaultLabels   "!";
                authz:dataset               :datasetBase;
                .

            :datasetBase rdf:type tdb2:DatasetTDB2 ;
                tdb2:location "target/NormalizationStore/DB-1-Data" ;
                .
            """;

    public static String authzDatasetAssemblerPersistent2 = """
            ## PBAC Dataset assembler.
            PREFIX :        <#>
            PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX ja:      <http://jena.hpl.hp.com/2005/11/Assembler#>
            PREFIX tdb2:    <http://jena.apache.org/2016/tdb#>
            PREFIX authz:   <http://ndtp.co.uk/security#>

            ## PBAC Dataset: Rocks labels store. TDB2
            :dataset rdf:type authz:DatasetAuthz ;
                authz:labelsStore           [ authz:labelsStorePath "target/NormalizationStore/LabelsStore-2.db" ] ;
                authz:attributes            <file:src/test/files/dataset/attribute-store.ttl> ;
                authz:tripleDefaultLabels   "!";
                authz:dataset               :datasetBase;
                .

            :datasetBase rdf:type tdb2:DatasetTDB2 ;
                tdb2:location "target/NormalizationStore/DB-2-Data" ;
                .
            """;



    public static DatasetGraphPBAC assembleFromString(String content) {
        Model assemblerModel = RDFParser.fromString(content, Lang.TURTLE).toModel();
        AtomicReference<Dataset> result = new AtomicReference<>(null);
        LogCtl.withLevel(SysPBAC.SYSTEM_LOG, "ERROR",
                         ()->{
                             Dataset ds = (Dataset)AssemblerUtils.build(assemblerModel, VocabAuthzDataset.tDatasetAuthz);
                             result.set(ds);
                         });

        Dataset ds = result.get();
        DatasetGraphPBAC dsgz = (DatasetGraphPBAC)ds.asDatasetGraph();
        return dsgz;
    }
}
