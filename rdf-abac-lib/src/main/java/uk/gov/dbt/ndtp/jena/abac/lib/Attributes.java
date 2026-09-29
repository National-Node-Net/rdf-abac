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

import uk.gov.dbt.ndtp.jena.abac.ABAC;
import uk.gov.dbt.ndtp.jena.abac.AE;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;
import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeValue;
import uk.gov.dbt.ndtp.jena.abac.attributes.ValueTerm;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.util.NodeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class Attributes {

    private Attributes(){}

    public static Attribute attribute(String name) { return new Attribute(name); }

    public static AttributesStore readAttributesStore(String filename, String shapesFilename) {
        Graph graph = RDFDataMgr.loadGraph(filename);
        Shapes shapesAttributesStore = getShapes(shapesFilename);
        if ( shapesAttributesStore != null ) {
            ValidationReport report = ShaclValidator.get().validate(shapesAttributesStore, graph);
            if ( ! report.conforms() ) {
                ShLib.printReport(System.err, report);
                throw new AuthzException("Bad attributes store file");
            }
        }
        AttributesStore attrStore = buildStore(graph);
        return attrStore;
    }

    private static Shapes getShapes(String shapesFilePath) {
        return ABAC.readSHACL(Objects.requireNonNullElse(shapesFilePath, "attribute-store-shapes.shc"));
    }

    private static final String PREFIXES = """
            PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX sh:      <http://www.w3.org/ns/shacl#>
            PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>
            PREFIX authz:   <http://ndtp.co.uk/security#>
            """;

    private static final String Q_USERS = PREFIXES+"\n"+"""
            SELECT ?user { [] authz:user ?user }
            """;
    private static final String Q_USER_ATTRIBUTES = PREFIXES+"\n"+"""
            SELECT ?attribute { [] authz:user ?user ;
                                   authz:userAttribute ?attribute
                              }
            """;
    private static final String Q_HIERARCHIES = PREFIXES+"\n"+"""
            SELECT ?attribute ?values { [] authz:hierarchy [ authz:attribute ?attribute ; authz:attributeValues ?values ] }
            """;

    public static AttributesStore buildStore(Graph attributesStoreGraph) {
        AttributesStoreModifiable store = new AttributesStoreLocal();
        populateStore(attributesStoreGraph, store);
        return store;
    }

    public static void populateStore(Graph attributesStoreGraph, AttributesStoreModifiable store) {
        users(attributesStoreGraph, store);
        hierarchies(attributesStoreGraph, store);
    }

    private static void users(Graph attributesStoreGraph, AttributesStoreModifiable store) {
        // Users
        RowSet rsUsers = QueryExec.graph(attributesStoreGraph).query(Q_USERS).select();
        Set<String> users = asStrings(rsUsers, "user");
        users.forEach(user->{
            Node u = NodeFactory.createLiteralString(user);
            RowSet rsAttributes = QueryExec.graph(attributesStoreGraph).query(Q_USER_ATTRIBUTES).substitution("user", u).select();
            AttributeValueSet attributeSet = attributeValueSet(rsAttributes, "attribute");
            store.put(user, attributeSet);
        });
    }

    /**
     *  Build attribute-values for a row set from {@link #Q_USER_ATTRIBUTES}.
     *  <p>
     *  Attribute-values can be multiple in the "attribute" variable or split across multiple declarations.
     */
    private static AttributeValueSet attributeValueSet(RowSet rowSet, String varName) {
        List<AttributeValue> acc = new ArrayList<>();
        rowSet.forEachRemaining(row -> {
            Node node = row.get(varName);
            if ( ! Util.isSimpleString(node) )
                throw new AuthzException("Bad value for ?" + varName + ": " + NodeFmtLib.displayStr(node));
            String string = node.getLiteralLexicalForm();
            List<AttributeValue> attrValues = AE.parseAttrValueList(string);
            acc.addAll(attrValues);
        });
        return AttributeValueSet.of(acc);
    }


    private static Set<String> asStrings(RowSet rowSet, String varName) {
        return rowSet.stream().map(row -> {
            Node node = row.get(varName);
            if ( ! Util.isSimpleString(node) )
                throw new AuthzException("Bad value for ?" + varName + ": " + NodeFmtLib.displayStr(node));
            String value = node.getLiteralLexicalForm();
            return value;
        }).collect(Collectors.toSet());
    }

    private static void hierarchies(Graph attributesStoreGraph, AttributesStoreModifiable store) {
        RowSet hierarchies = QueryExec.graph(attributesStoreGraph).query(Q_HIERARCHIES).select();
        hierarchies.forEachRemaining(row ->{
            String attrName = string(row.get("attribute"));
            String attrVals = string(row.get("values"));
            List<ValueTerm> values = AE.parseValueTermList(attrVals);
            Attribute a = Attribute.create(attrName);
            Hierarchy h = new Hierarchy(a, values);
            store.addHierarchy(h);
        });
    }

    static String string(Node n) {
        if ( n == null ) {
            throw new NullPointerException("Missing string for node");
        }
        if ( ! n.isLiteral() ) {
            throw new AuthzException("Not a literal string");
        }
        if ( ! NodeUtils.isSimpleString(n) ) {
            throw new AuthzException("Literal but not a plain string");
        }
        return n.getLiteralLexicalForm();
    }
}
