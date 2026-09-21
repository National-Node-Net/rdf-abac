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


package uk.gov.dbt.ndtp.jena.abac.labels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.Match;

/**
 * Extremely simple patterns index.
 * This does not include S P O indexing
 */
public class PatternsIndex {
    // Indexing for triples (by pattern) to label.
    // The "triple" in the value slot of these maps is a pattern.
    //
    // Find order:
    // S P O
    // S P ANY
    // S ANY ANY
    // ANY P ANY
    // ANY ANY ANY

    // S P O is covered by LabelsIndexImpl
    // This class is for the "with wildcard" case
    // This class is not optimized.

    private static final List<String> NO_LABELS = List.of();

    // Simple structures. These are the various supported patterns for label matches.
    // Map description node from the labels graph to a triple pattern in the description.
    // At run time, do a hopefully short scan (needs optimization).

    private boolean hasPatterns = false;

    // Ultra-simple.
    static class Patterns {
        List<Pair<TriplePattern, List<String>>> triplePatterns = new ArrayList<>();

        void add(TriplePattern pattern, List<String> labels) {
            triplePatterns.add(Pair.create(pattern, labels));
        }

        List<String> find(Triple triple) {
            List<String> acc = new ArrayList<>();
            for ( Pair<TriplePattern,List<String>> p : triplePatterns ) {
                var pattern = p.getLeft();
                if ( ! patternMatch(triple, pattern) )
                    continue;
                var labels = p.getRight();
                acc.addAll(labels);
            }
            return acc;
        }

        /*package*/static boolean patternMatch(Triple triple, TriplePattern pattern) {
            Node s = pattern.subject();
            Node p = pattern.predicate();
            Node o = pattern.object();
            return Match.match(triple, s, p, o);
        }

        public void toGraph(Graph graph) {
            StreamRDF stream = StreamRDFLib.graph(graph);
            triplePatterns.forEach(p->{
                TriplePattern triplePattern = p.getLeft();
                List<String> labels = p.getRight();
                L.asRDF(triplePattern, labels, stream);
            });
        }

        public boolean isEmpty() {
            return triplePatterns.isEmpty();
        }

        void collectLabels(Set<String> acc) {
            triplePatterns.forEach(pair->acc.addAll(pair.getRight()));
        }
    }

    private Patterns S      = new Patterns();
    private Patterns SP     = new Patterns();
    private Patterns P      = new Patterns();
    private Patterns ANY    = new Patterns();

    /**
     * Match in order:
     * <ul>
     * <li>S P O
     * <li>S P <em>any</em>
     * <li>S <em>any</em> <em>any</em>
     * <li><em>any</em> P <em>any</em>
     * </ul>
     * The pattern "any any any" is the effect default for when no other match occurs.
     * Returns the empty list for "no labels".
     * Returns null for labels not configured.
     * @return List of labels.
     */
    public List<String> match(Triple triple) {
        if ( ! hasPatterns )
            return NO_LABELS;
        // ---- Pattern matching
        List<String> acc;
        // Patterns.
        acc = SP.find(triple);
        if ( ! acc.isEmpty() )
            return acc;
        acc = S.find(triple);
        if ( ! acc.isEmpty() )
            return acc;
        acc = P.find(triple);
        if ( ! acc.isEmpty() )
            return acc;
        acc = ANY.find(triple);
        if ( ! acc.isEmpty() )
            return acc;
        return List.of();
    }

    public void add(TriplePattern pattern, List<String> labels) {
        // Some simple categorization to make the search space smaller.
        Node s = pattern.subject();
        Node p = pattern.predicate();
        Node o = pattern.object();
        if ( s.isConcrete() && p.isConcrete() && o.isConcrete() )
            throw new LabelsException("Concrete triple pattern passed to pattern index");

        hasPatterns = true;
        if ( s.isConcrete() && p.isConcrete() && ! o.isConcrete() ) {
            SP.add(pattern, labels);
        } else if ( s.isConcrete() && ! p.isConcrete() && ! o.isConcrete() ) {
            S.add(pattern, labels);
        } else if ( ! s.isConcrete() && p.isConcrete() && ! o.isConcrete() ) {
            P.add(pattern, labels);
        } else if ( ! s.isConcrete() && ! p.isConcrete() && ! o.isConcrete() ) {
            ANY.add(pattern, labels);
        } else {
            throw new LabelsException("Pattern not supported: "+pattern);
        }
    }

    public void toGraph(Graph graph) {
        S.toGraph(graph);
        SP.toGraph(graph);
        P.toGraph(graph);
        ANY.toGraph(graph);
    }

    public void forEach(BiConsumer<Triple, List<String>> action) {
        S.triplePatterns.forEach(pair->{
            Triple t = pair.getLeft().asTriple();
            List<String> labels = pair.getRight();
            action.accept(t, labels);
        });
    }

    public boolean isEmpty() {
        return S.isEmpty() && SP.isEmpty() && P.isEmpty() && ANY.isEmpty();
    }

    /** Add every label in this index to {@code acc}, covering S, SP, P and ANY. */
    public void collectLabels(Set<String> acc) {
        S.collectLabels(acc);
        SP.collectLabels(acc);
        P.collectLabels(acc);
        ANY.collectLabels(acc);
    }
}
