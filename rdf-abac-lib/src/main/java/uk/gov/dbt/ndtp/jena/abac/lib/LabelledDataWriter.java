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


package uk.gov.dbt.ndtp.jena.abac.lib;

import java.io.OutputStream;
import java.util.List;

import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFOps;
import org.apache.jena.riot.writer.WriterStreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;

public class LabelledDataWriter {

    public static void writeWithLabels(OutputStream output, DatasetGraphABAC dsgz) {
        Context cxt = RIOT.getContext().copy().set(RIOT.symTurtleDirectiveStyle, "sparql");
        StreamRDF stream = new LDW(output,cxt, dsgz.labelsStore());
        StreamRDFOps.datasetToStream(dsgz.getData(), stream);
    }

    static class LDW extends WriterStreamRDFBase {

        private final LabelsStore labelStore;


        private LDW(OutputStream output, Context context, LabelsStore labelStore) {
            super(output, context);
            this.labelStore = labelStore;
        }

        protected void printTripleNoNL(Triple triple)
        {
            Node s = triple.getSubject() ;
            Node p = triple.getPredicate() ;
            Node o = triple.getObject() ;

            outputNode(s) ;
            out.print(' ') ;
            printProperty(p);
            out.print(' ') ;
            outputNode(o) ;
            //out.println(" .") ;
            // No newline.
            out.print(" .") ;
        }

        @Override
        protected void print(Triple triple) {
            printTripleNoNL(triple);

            List<String> labels = labelStore.labelsForTriples(triple);
            if ( labels != null && ! labels.isEmpty() ) {
                super.out.pad(50);
                out.print(" //") ;
                labels.forEach(s-> {out.print(" ") ; out.print(s); });
            }
            out.println();
        }

        @Override
        protected void startData() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void endData() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void print(Quad quad) {
            throw new UnsupportedOperationException("Quads in DatasetGraphABAC");
        }

        @Override
        protected void reset() {
            throw new UnsupportedOperationException();
        }
    }
}
