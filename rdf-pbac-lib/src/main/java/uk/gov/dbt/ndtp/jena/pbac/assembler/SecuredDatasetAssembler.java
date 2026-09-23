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


package uk.gov.dbt.ndtp.jena.pbac.assembler;

import uk.gov.dbt.ndtp.jena.pbac.lib.DatasetGraphPBAC;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.Mode;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.assembler.DatasetAssembler;
import uk.gov.dbt.ndtp.jena.pbac.lib.VocabAuthzDataset;

/**
 * A secured dataset.
 */
public class SecuredDatasetAssembler extends DatasetAssembler implements Assembler {

    @Override
    public Dataset open(Assembler a, Resource root, Mode mode) {
        DatasetGraph dsg = createDataset(a, root);
        return DatasetFactory.wrap(dsg);
    }

    @Override
    public DatasetGraph createDataset(Assembler a, Resource root) {
        /*
         *   [] rdf:type authz:DatasetAuthz ;
         *
         *       ## API access control based on PBAC labelling.
         *       authz:accessAttributes "" ;          -- default: none
         *
         *       ## For RocksDB:
         *       ## ** ## authz:labelStore [ authz:labelsStorePath <file:directory> ] ;
         *
         *       ## Label store, external storage. If not present, use an in-memory store.
         *       authz:labels  <store of labels>      -- default: in-memory store
         *
         *       ## Dataset default setting. There is also a system-wide default of "deny".
         *       authz:tripleDefaultLabels "" ;       -- default: null (system default applies)
         *
         *       ## Logically::
         *       ## ** ## authz:attributesStore [ authz:attributes <store> ; authz:attributesURL "" ];
         *       ## User/request attribute store
         *       authz:attributes "filename" ;        -- User attributes and hierarchies
         *       ## OR remote store.
         *       ## authz:attributesURL "URL" ;
         *
         *       ## The underlying dataset.
         *       authz:dataset <base data> ;          -- Data
         *       .
         */
        DatasetGraph dsgBase = createBaseDataset(root, VocabAuthzDataset.pDataset);
        DatasetGraphPBAC dsgAuthz = Secured.buildDatasetGraphAuthz(dsgBase, root);
        if ( dsgAuthz == null )
            throw new AssemblerException(root, "Failed to find configuration for a DatasetGraphAuthz");
        return dsgAuthz;
    }
}

