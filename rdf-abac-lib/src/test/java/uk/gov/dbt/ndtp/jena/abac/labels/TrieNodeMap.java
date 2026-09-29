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

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * Trie-based map which forms the core of {@link TrieNodeTable}
 */
public class TrieNodeMap {

    final Callable<Long> idGenerator;

    final Interior root = new Interior();

    public TrieNodeMap(final Callable<Long> idGenerator) {
        this.idGenerator = idGenerator;
    }

    public long add(final Node node) {
        String s;
        if (node.isURI()) {
            s = node.getURI();
        } else if (node.isLiteral()) {
            s = node.getLiteralLexicalForm();
        } else {
            throw new RuntimeException("TBD - TrieNodeMap can only handle URIs and Literals");
        }

        return root.add(s, node, idGenerator);
    }

    static class Vertex {
    };

    static class Interior extends Vertex {
        Map<Leaf, Long> leaves = null;

        List<Pair<String, Interior>> children = null;

        long add(final String key, final Node node, final Callable<Long> idGenerator) {

            if (key.isEmpty()) {
                return addLeaf(node, idGenerator);
            }

            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    var kv = children.get(i);
                    var k = kv.car();
                    var prefix = commonPrefix(k, key);
                    if (prefix == k.length()) {
                        //in other words, key.startsWith(k), so add residue to k subtree
                        return kv.cdr().add(key.substring(k.length()), node, idGenerator);
                    }

                    if (prefix > 0) {
                        // key and k share a substring, which may be key, but is a strict prefix of k
                        // add an interior node to split
                        var v = kv.cdr();
                        var intermediate = new Interior();

                        //split the path to v with intermediate
                        //should not require re-sorting
                        children.remove(i);
                        children.add(i, Pair.create(k.substring(0, prefix), intermediate));
                        intermediate.addChild(Pair.create(k.substring(prefix), v));

                        //and add the new node into intermediate
                        return intermediate.add(key.substring(prefix), node, idGenerator);
                    }
                }
            }

            // There is no common prefix for any existing key
            // This is also true of an empty "children"
            Interior interior = new Interior();
            this.addChild(Pair.create(key, interior));
            return interior.addLeaf(node, idGenerator);
        }

        void addChild(Pair<String, Interior> child) {
            if (children == null) {
                children = new ArrayList<>(4);
            }
            int i = 0;
            for (; i < children.size(); i++) {
                if (children.get(i).car().compareTo(child.car()) > 0) break;
            }
            children.add(i, child);
        }

        /**
         * Add a leaf to this interior, representing the supplied node.
         * @param node RDF Node
         */
        long addLeaf(Node node, final Callable<Long> idGenerator) {
            if (leaves == null) {
                // >1 is very unlikely (URI and literal with same string), so optimize for 1
                leaves = new HashMap<>(1);
            }

            Leaf newLeaf;
            if (node.isURI()) {
                newLeaf = URILeaf.singleton;
            } else if (node.isLiteral()) {
                newLeaf = new LiteralLeaf(node.getLiteralDatatype(), node.getLiteralLanguage());
            }else {
                throw new RuntimeException("TBD - TrieNodeMap can only handle URIs and Literals");
            }

            if (leaves.get(newLeaf) == null) {
                try {
                    leaves.put(newLeaf, idGenerator.call());
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected exception in id generator: ", e);
                }
            }

            return leaves.get(newLeaf);
        }
    };

    static class Leaf {};

    static class URILeaf extends Leaf {
        final static URILeaf singleton = new URILeaf();
        private URILeaf() {}
    };

    /**
     * Equality does not test value, as all leaves we compare must have the same value.
     * Because they are leaves of a trie.
     */
    static class LiteralLeaf extends Leaf {
        RDFDatatype datatype;
        String lang;

        public LiteralLeaf(final RDFDatatype rdfDatatype, final String lang) {
            this.datatype = rdfDatatype;
            this.lang = lang;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LiteralLeaf that = (LiteralLeaf) o;
            return Objects.equals(datatype, that.datatype) && Objects.equals(lang, that.lang);
        }

        @Override
        public int hashCode() {
            return Objects.hash(datatype, lang);
        }
    };

    private static int commonPrefix(final String s1, final String s2) {
        final var minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        return minLength;
    }

    public Iterator<Pair<Node, Long>> iterator() {
        return new EagerIterator();
    }

    private class EagerIterator implements Iterator<Pair<Node, Long>> {

        private List<Pair<Node, Long>> items() {

            List<Pair<Node, Long>> result = new ArrayList<>();
            acc(result, "", root);
            return result;
        }

        private void acc(final List<Pair<Node, Long>> result, final String path, final Interior interior) {
            if (interior.leaves != null) {
                for (var entry : interior.leaves.entrySet()) {
                    var leaf = entry.getKey();
                    var id = entry.getValue();
                    if (leaf instanceof URILeaf) {
                        result.add(Pair.create(NodeFactory.createURI(path), id));
                    } else if (leaf instanceof LiteralLeaf literal) {
                        result.add(Pair.create(NodeFactory.createLiteral(path, literal.lang, literal.datatype), id));
                    } else {
                        throw new RuntimeException("Unexpected leaf in TrieNodeMap " + leaf);
                    }
                }
            }

            if (interior.children != null) {
                for (var entry : interior.children) {
                    var suffix = entry.car();
                    acc(result, path + suffix, entry.cdr());
                }
            }
        }

        List<Pair<Node, Long>> list = items();

        @Override
        public boolean hasNext() {
            return !list.isEmpty();
        }

        @Override
        public Pair<Node, Long> next() {
            return list.remove(0);
        }
    }
}
