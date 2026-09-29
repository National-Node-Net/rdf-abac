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


package uk.gov.dbt.ndtp.jena.abac.labels;

import static org.apache.jena.system.G.nullAsAny;
import static org.apache.jena.riot.out.NodeFmtLib.strTTL;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

record TriplePattern(Node subject, Node predicate, Node object) {
    // This record gives a type name to a triple used as a pattern.

    static TriplePattern create(Triple m) {
        Node s = nullAsAny(m.getSubject());
        Node p = nullAsAny(m.getPredicate());
        Node o = nullAsAny(m.getObject());
        return new TriplePattern(s, p, o);
    }

    static TriplePattern create(Node subject, Node predicate , Node object) {
        Node s = nullAsAny(subject);
        Node p = nullAsAny(predicate);
        Node o = nullAsAny(object);
        return new TriplePattern(s, p, o);
    }

    public boolean isConcrete() {
        return subject.isConcrete() && predicate.isConcrete() && object.isConcrete();
    }

    /** Convert to a triple if concrete else return null. */
    public Triple asTriple() {
        return Triple.create(subject, predicate, object);
    }

    public String str() {
        StringBuilder result = new StringBuilder();
        result.append(strTTL(subject));
        result.append( " " );
        result.append(strTTL(predicate));
        result.append( " " );
        result.append(strTTL(object));
        return result.toString();
    }
}
