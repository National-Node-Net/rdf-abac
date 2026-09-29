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
 *  © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the UK's Department for Business, Innovation, Science and Trade (BIST) as the governing entity.
 */


package uk.gov.dbt.ndtp.jena.abac;

import uk.gov.dbt.ndtp.jena.abac.assembler.Secured;
import uk.gov.dbt.ndtp.jena.abac.lib.DatasetGraphABAC;
import uk.gov.dbt.ndtp.jena.abac.lib.VocabAuthzDataset;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStoreRocksDB;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assembler testing.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestAssemblerABAC {
    static {
        JenaSystem.init();
    }

    private static String buildLogLevel = null;
    @BeforeAll public static void beforeAll() {
        LogCtl.getLevel(Secured.BUILD_LOG);
        LogCtl.set(Secured.BUILD_LOG, "error");
    }

    @AfterAll public static void afterAll() {
        if ( buildLogLevel != null )
            LogCtl.set(Secured.BUILD_LOG, buildLogLevel);
    }

    private static final String DIR = "src/test/files/dataset/";

    @Test public void assemble_1() {
        DatasetGraphABAC dsgz = assemble(DIR+"abac-assembler-1.ttl");
        assertNotNull(dsgz.labelsStore());
        assertNotNull(dsgz.attributesStore());
        dsgz.close();
    }

    @Test public void assemble_2() {
        DatasetGraphABAC dsgz = assemble(DIR+"abac-assembler-2.ttl");
        assertNotNull(dsgz.labelsStore());
        assertNotNull(dsgz.attributesStore());
        dsgz.close();
    }

    @Test public void assemble_3() {
        DatasetGraphABAC dsgz = assemble(DIR+"abac-assembler-3.ttl");
        assertNotNull(dsgz.labelsStore());
        assertNotNull(dsgz.attributesStore());
        dsgz.close();
    }

    private static void noLabelStoreDirectory(String dirName) {
        FileOps.clearDirectory(dirName);
        FileOps.delete(dirName);
    }

    @Test public void assemble_label_store_1() {
        // This name must agree with the assembler.
        String dirName = "target/LabelsStore.db";
        noLabelStoreDirectory(dirName);

        DatasetGraphABAC dsgz = assemble(DIR+"abac-assembler-label-store-1.ttl");
        assertTrue(FileOps.exists(dirName), "No label store directory");

        LabelsStore labelStore = dsgz.labelsStore();
        assertNotNull(labelStore);
        assertInstanceOf(LabelsStoreRocksDB.class, labelStore);
        dsgz.close();
    }

    @Test public void assemble_label_store_2() {
        // This name must agree with the assembler.
        String dirName = "target/LabelsStore.db";
        noLabelStoreDirectory(dirName);

        DatasetGraphABAC dsgz = assemble(DIR+"abac-assembler-label-store-2.ttl");
        assertTrue(FileOps.exists(dirName), "No label store directory");

        LabelsStore labelStore = dsgz.labelsStore();
        assertNotNull(labelStore);
        assertInstanceOf(LabelsStoreRocksDB.class, labelStore);
        dsgz.close();
    }

    @Test public void assemble_bad_1() {
        // Both auth:labelsStore and authz:labels on the dataset.
        assembleBad(DIR+"abac-assembler-bad-1.ttl");
    }

    @Test public void assemble_bad_2() {
        // Two auth:labelsStore
        assembleBad(DIR+"abac-assembler-bad-2.ttl");
    }

    @Test public void assemble_bad_3() {
        // Two authz:labels in the dataset.
        assembleBad(DIR+"abac-assembler-bad-3.ttl");
    }

    @Test public void assemble_bad_4() {
        // Empty linked object.
        assembleBad(DIR+"abac-assembler-bad-4.ttl");
    }

    /*package*/ static void assembleBad(String filename) {
        Assertions.assertThrows(AssemblerException.class, () ->assemble(filename));
    }

    /*package*/ static DatasetGraphABAC assemble(String filename) {
        Dataset ds = (Dataset)AssemblerUtils.build(filename, VocabAuthzDataset.tDatasetAuthz);
        assertNotNull(ds);
        DatasetGraphABAC dsgz = (DatasetGraphABAC)ds.asDatasetGraph();
        return dsgz;
    }
}
