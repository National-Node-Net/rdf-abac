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


package uk.gov.dbt.ndtp.jena.abac;

import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens.TestToken;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens.TestTokenizerABAC;
import uk.gov.dbt.ndtp.jena.abac.labels.TestStoreFmtByNodeId;
import uk.gov.dbt.ndtp.jena.abac.labels.TestStoreFmtByString;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE1;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE_Allow;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE_And;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE_AttrValue;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE_Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE_Bracketted;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE_RelAny;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAE_Var;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.TestAttrExprEvaluator;
import uk.gov.dbt.ndtp.jena.abac.lib.TestAttributeStoreCache;
import uk.gov.dbt.ndtp.jena.abac.lib.TestAttributeStoreLocal;
import uk.gov.dbt.ndtp.jena.abac.lib.TestAttributeStoreRemote;
import uk.gov.dbt.ndtp.jena.abac.lib.TestAttributes;
import uk.gov.dbt.ndtp.jena.abac.lib.TestCtxABAC;

@Suite
@SelectClasses({
    TestAuthMisc.class

    // Component testing.
    , TestAttributeParser.class
    , TestAttributeExprList.class
    , TestAttributeExprParse.class
    , TestHierarchy.class

    , TestAttributeValue.class
    , TestAttributeValueSet.class
    , TestAttributeValueList.class

    , TestAttributeExprEval.class
    , TestLabelsMem.class
    , TestLabelsMemPattern.class
    , TestLabelsMemNoPatterns.class
    , TestAssemblerABAC.class

    , TestLabelsStoreMem.class
    , TestLabelsVocabulary.class
    , TestAE.class
    , TestABAC.class
    , TestCtxABAC.class
    , TestAttributeStoreRemote.class
    , TestAttributeStoreLocal.class
    , TestAttributeStoreCache.class
    , TestAttributes.class
    , TestTokenizerABAC.class
    , TestAE_And.class
    , TestAE_AttrValue.class
    , TestAE_RelAny.class
    , TestAE_Bracketted.class
    , TestAttrExprEvaluator.class
    , TestAE1.class
    , TestAE_Var.class
    , TestAE_Attribute.class
    , TestAE_Allow.class
    , TestToken.class

    // RocksDB related.
    , TestStoreFmtByString.class
    , TestStoreFmtByNodeId.class

    , TestLabelStoreRocksDBGeneral.ByString.class
    , TestLabelStoreRocksDBGeneral.ByNodeId.class
    , TestLabelStoreRocksDBGeneral.ByNodeIdTrie.class

    /*
     * These tests are split because it seems RocksDB does not completely clear up fast enough within one suite.
     * Tests on their own we stable, but the suite is not if this test below is included.
     */
    , TestDatasetPersistentLabelsABAC.class
    , TestDatasetPersistentLabelsABAC2.class
})

public class TS_ABAC {}
