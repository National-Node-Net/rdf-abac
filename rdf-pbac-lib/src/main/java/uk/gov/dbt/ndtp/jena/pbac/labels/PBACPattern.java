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


package uk.gov.dbt.ndtp.jena.pbac.labels;

import uk.gov.dbt.ndtp.jena.pbac.lib.AuthzException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * Identify the different cases of triples which are stored in the RocksDB label store.
 *
 * The RocksDB implementation of the label store treats each of these different combinations
 * of wildcarded and non-wildcarded SPO triples differently, so it helps to keep the code
 * clean to abstract and extract the pattern once, in a single place.
 */
enum PBACPattern {
    PATTERN_SPO,
    PATTERN_SPX,
    PATTERN_SXX,
    PATTERN_XPX,
    PATTERN_XXX;

    static PBACPattern fromTriple(Triple triple) {
        return fromTriple(triple.getSubject(), triple.getPredicate(), triple.getObject());
    }

    static PBACPattern fromTriple(final Node subject, final Node property, final Node object) {
        if (subject != Node.ANY && property != Node.ANY && object != Node.ANY) {
            return PATTERN_SPO;
        }
        if (subject != Node.ANY && property != Node.ANY) {
            return PATTERN_SPX;
        }
        if (subject == Node.ANY && property != Node.ANY && object == Node.ANY) {
            return PATTERN_XPX;
        }
        if (subject != Node.ANY && object == Node.ANY) {
            return PATTERN_SXX;
        }
        if (subject == Node.ANY && property == Node.ANY && object == Node.ANY) {
            return PATTERN_XXX;
        }

        throw new AuthzException("Access control rule is not of the form SPO, SP_, S__, _P_ or ___");
    }
}
