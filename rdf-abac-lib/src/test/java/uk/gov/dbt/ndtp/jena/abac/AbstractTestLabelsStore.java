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

import static uk.gov.dbt.ndtp.jena.abac.ABACTestRunner.assertEqualsUnordered;
import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Set;

import uk.gov.dbt.ndtp.jena.abac.labels.L;
import uk.gov.dbt.ndtp.jena.abac.labels.Labels;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsException;
import uk.gov.dbt.ndtp.jena.abac.labels.LabelsStore;
import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Test;

/**
 * General label store tests; no triple patterns for labelling.
 */
public abstract class AbstractTestLabelsStore {

    protected static final Triple triple1 = parseTriple("(:s :p 123)");
    protected static final Triple triple2 = parseTriple("(:s :p 'xyz')");
    final public static String HUGE_STRING = "Ukraine was the center of the first eastern Slavic state, Kyivan Rus, which during the 10th and 11th centuries was the largest and most powerful state in Europe. Weakened by internecine quarrels and Mongol invasions, Kyivan Rus was incorporated into the Grand Duchy of Lithuania and eventually into the Polish-Lithuanian Commonwealth. The cultural and religious legacy of Kyivan Rus laid the foundation for Ukrainian nationalism through subsequent centuries. A new Ukrainian state, the Cossack Hetmanate, was established during the mid-17th century after an uprising against the Poles. Despite continuous Muscovite pressure, the Hetmanate managed to remain autonomous for well over 100 years. During the latter part of the 18th century, most Ukrainian ethnographic territory was absorbed by the Russian Empire. Following the collapse of czarist Russia in 1917, Ukraine achieved a short-lived period of independence (1917-20) but was reconquered and endured a brutal Soviet rule that engineered two forced famines (1921-22 and 1932-33) in which over 8 million died. In World War II, German and Soviet armies were responsible for 7 to 8 million more deaths. Although Ukraine overwhelmingly voted for independence in 1991 around the time of the dissolution of the USSR, democracy and prosperity remained elusive as the legacy of state control, patronage politics, and endemic corruption stalled efforts at economic reform, privatization, and civil liberties. A peaceful mass protest referred to as the \"Orange Revolution\" in the closing months of 2004 and early 2005 forced the authorities to overturn a rigged presidential election and to allow a new internationally monitored vote that swept into power a reformist slate under Viktor YUSHCHENKO. Subsequent internal squabbles in the YUSHCHENKO camp allowed his rival Viktor YANUKOVYCH to stage a comeback in legislative (Rada) elections, become prime minister in August 2006, and be elected president in February 2010. In October 2012, Ukraine held Rada elections, widely criticized by Western observers as flawed due to use of government resources to favor ruling party candidates, interference with media access, and harassment of opposition candidates. President YANUKOVYCHs backtracking on a trade and cooperation agreement with the EU in November 2013 - in favor of closer economic ties with Russia - and subsequent use of force against students, civil society activists, and other civilians in favor of the agreement and fed up with blatant corruption led to a three-month protest occupation of Kyivs central square. The governments use of violence to break up the protest camp in February 2014 led to all out pitched battles, scores of deaths, international condemnation, a failed political deal, and the presidents abrupt departure for Russia. New elections in the spring allowed pro-West president Petro POROSHENKO to assume office in June 2014; he was succeeded by Volodymyr ZELENSKY in May 2019. Shortly after YANUKOVYCHs departure in late February 2014, Russian President PUTIN ordered the invasion of Ukraines Crimean Peninsula falsely claiming the action was to protect ethnic Russians living there. Two weeks later, a \"referendum\" was held regarding the integration of Crimea into the Russian Federation. The \"referendum\" was condemned as illegitimate by the Ukrainian Government, the EU, the US, and the UN General Assembly (UNGA). In response to Russias illegal annexation of Crimea, 100 members of the UN passed UNGA resolution 68/262, rejecting the \"referendum\" as baseless and invalid and confirming the sovereignty, political independence, unity, and territorial integrity of Ukraine. In mid-2014, Russia began supplying proxies in two of Ukraines eastern provinces with manpower, funding, and materiel beginning an armed conflict with the Ukrainian Government. Representatives from Ukraine, Russia, and the unrecognized Russian proxy republics signed the Minsk Protocol and Memorandum in September 2014 with the aim of ending the conflict. However, this agreement failed to stop the fighting or find a political solution. In a renewed attempt to alleviate ongoing clashes, leaders of Ukraine, Russia, France, and Germany negotiated a follow-on Package of Measures in February 2015 to implement the Minsk agreements, but this effort failed as well. By early 2022, more than 14,000 civilians were killed or wounded as a result of the Russian intervention in eastern Ukraine. On 24 February 2022, Russia escalated its conflict with Ukraine by launching a full-scale invasion of the country on several fronts in what has become the largest conventional military attack on a sovereign state in Europe since World War II. The invasion has received near universal international condemnation, and many countries have imposed sanctions on Russia and supplied humanitarian and military aid to Ukraine. Russia made substantial gains in the early weeks of the invasion but underestimated Ukrainian resolve and combat capabilities. By the end of 2022, Ukrainian forces had regained all territories in the north and northeast and made some advances in the east and south. Nonetheless, Russia in late September 2022 unilaterally declared its annexation of four Ukrainian oblasts - Donetsk, Kherson, Luhansk, and Zaporizhzhia - even though none was fully under Russian control. The annexations remain unrecognized by the international community.The invasion has also created Europes largest refugee crisis since World War II. As of 28 December 2023, there were 6.3 million Ukrainian refugees recorded globally, and 3.67 million people were internally displaced as of September 2023.  Nearly 28,500 civilian casualties had been reported, as of 21 November 2023. The invasion of Ukraine remains one of the two largest displacement crises worldwide (the other is the conflict in Syria).The Ukrainian people continue to fiercely resist Russia’s full-scale invasion, which has targeted civilian and critical infrastructure - including energy - to try to break the Ukrainian will. President ZELENSKYY has focused on the civic identity of Ukrainians, regardless of ethnic or linguistic background, to unite the country behind the goals of ending the war by regaining as much territory as possible and advancing Ukraine’s candidacy for membership in the European Union (EU). Support for joining the EU and NATO has grown significantly, overcoming the historical, and sometimes artificial, divide between eastern and western Ukraine.";
    protected LabelsStore labelsStore;
    /** Empty {@link LabelsStore} */
    protected abstract LabelsStore createLabelsStore();

    /**
     * A {@link LabelsStore}, initialized with graph recording labels.
     * The default is to create an empty {@link LabelsStore}
     * and call {@link  L#loadStoreFromGraph}.
     */
    protected LabelsStore createLabelsStore(Graph labelsGraph) {
        labelsStore = createLabelsStore();
        L.loadStoreFromGraph(labelsStore, labelsGraph);
        return labelsStore;
    }

    // ----

    private static String labelsGraphBadPattern = """
        PREFIX foo: <http://example/>
        PREFIX authz: <http://ndtp.co.uk/security#>
        ## No bar:
        [ authz:pattern 'bar:s bar:p1 123' ;  authz:label "allowed" ] .
    """;
    private static Graph BAD_PATTERN = RDFParser.fromString(labelsGraphBadPattern, Lang.TTL).toGraph();

    @Test public void labelsStore_noLabel() {
        labelsStore = createLabelsStore();
        List<String> x = labelsStore.labelsForTriples(triple1);
        assertEquals(List.of(), x);
    }

    @Test public void labelsStore_addLabel() {
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, "triplelabel");
        List<String> x = labelsStore.labelsForTriples(triple1);
        assertEquals(List.of("triplelabel"), x);
    }

    @Test public void labelsStore_addLabels_no_interference() {
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, "label1");
        labelsStore.add(triple2, "label2");
        List<String> x = labelsStore.labelsForTriples(triple1);
        assertEquals(List.of("label1"), x);
    }

    @Test public void labelsStore_addLabels_addDifferentLabel() {
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, "label1");
        labelsStore.add(triple2, "labelx");
        labelsStore.add(triple1, "label2");
        List<String> x1 = labelsStore.labelsForTriples(triple1);
        assertEquals(1, x1.size());
        assertEquals(x1, List.of("label2"));
        List<String> x2 = labelsStore.labelsForTriples(triple2);
        assertEquals(x2, List.of("labelx"));
    }

    @Test public void labelsStore_addLabel_is_empty() {
        labelsStore = createLabelsStore();
        assertTrue(labelsStore.isEmpty(), "Store is not empty on creation");
        labelsStore.add(triple1, "label1");
        assertFalse(labelsStore.isEmpty(), "Store is empty after adding a label");
    }

    @Test public void labels_bad_labels_graph() {
        assertThrows(LabelsException.class,
                     ()-> ABACTestRunner.loggerAtLevel(Labels.LOG, "FATAL",
                                                  ()->createLabelsStore(BAD_PATTERN))  // warning and error
                );
    }

    @Test public void labels_add_bad_label() {
        // Label is a parse error.
        String logLevel = "FATAL";
        labelsStore = createLabelsStore();
        ABACTestRunner.loggerAtLevel(ABAC.AttrLOG, logLevel, ()-> assertThrows(LabelsException.class, ()->labelsStore.add(triple1, "not .. good")));
    }

    @Test public void labels_add_bad_labels_graph() {
        labelsStore = createLabelsStore();
        String gs = """
                PREFIX : <http://example>
                PREFIX authz: <http://ndtp.co.uk/security#>
                [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
                """;
        Graph addition = RDFParser.fromString(gs, Lang.TTL).toGraph();

        ABACTestRunner.loggerAtLevel(Labels.LOG, "FATAL", ()-> assertThrows(LabelsException.class,
                 ()-> {
                     labelsStore.addGraph(addition);
                     labelsStore.labelsForTriples(triple1);
                 }));
    }

    @Test public void labels_add_same_triple_different_label() {
        labelsStore = createLabelsStore();
        List<String> x = labelsStore.labelsForTriples(triple1);
        labelsStore.add(triple1, "label-1");
        labelsStore.add(triple1, "label-2");

        List<String> labels = labelsStore.labelsForTriples(triple1);
        List<String> expected = List.of("label-2");
        // Order can not be assumed.
        assertTrue(ListUtils.equalsUnordered(expected, labels), "Expected: "+expected+"  Got: "+labels);
    }

    @Test public void labels_add_triple_multiple_label() {
        labelsStore = createLabelsStore();
        List<String> x = labelsStore.labelsForTriples(triple1);
        labelsStore.add(triple1, List.of("label-1", "label-2"));
        List<String> labels = labelsStore.labelsForTriples(triple1);
        List<String> expected = List.of("label-1", "label-2");
        // Order is not preserved
        assertEqualsUnordered(expected, labels);
    }

    @Test public void labels_add_same_triple_same_label() {
        labelsStore = createLabelsStore();
        labelsStore.labelsForTriples(triple1);
        labelsStore.add(triple1, "TheLabel");
        labelsStore.add(triple1, "TheLabel");
        List<String> labels = labelsStore.labelsForTriples(triple1);
        assertEquals(List.of("TheLabel"), labels);
    }

    @Test public void labels_add_triple_duplicate_label_in_list() {
        labelsStore = createLabelsStore();
        List<String> x = List.of("TheLabel", "TheLabel");
        assertThrows(LabelsException.class, ()->labelsStore.add(triple1, x));
    }

    @Test public void labelsStore_addHugeLabel() {
        // 6K string
        Triple hugeTriple = parseTriple("(:s :p '" + HUGE_STRING + "')");
        labelsStore = createLabelsStore();
        labelsStore.add(hugeTriple, "hugeLabel");
        List<String> x = labelsStore.labelsForTriples(hugeTriple);
        assertEquals(List.of("hugeLabel"), x);
    }

    protected boolean supportsEnumeration() { return true; }

    @Test public void distinctLabels_unsupported_throws() {
        assumeFalse(supportsEnumeration());
        labelsStore = createLabelsStore();
        assertThrows(UnsupportedOperationException.class, ()->labelsStore.distinctLabels());
    }

    @Test public void distinctLabels_empty() {
        assumeTrue(supportsEnumeration());
        labelsStore = createLabelsStore();
        assertEquals(Set.of(), labelsStore.distinctLabels());
    }

    @Test public void distinctLabels_oneLabel() {
        assumeTrue(supportsEnumeration());
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, "label1");
        assertEquals(Set.of("label1"), labelsStore.distinctLabels());
    }

    @Test public void distinctLabels_sharedLabel_deduplicated() {
        assumeTrue(supportsEnumeration());
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, "shared");
        labelsStore.add(triple2, "shared");
        assertEquals(Set.of("shared"), labelsStore.distinctLabels());
    }

    @Test public void distinctLabels_severalLabels() {
        assumeTrue(supportsEnumeration());
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, "label1");
        labelsStore.add(triple2, "label2");
        assertEquals(Set.of("label1", "label2"), labelsStore.distinctLabels());
    }

    @Test public void distinctLabels_multipleLabelsOnOneTriple() {
        assumeTrue(supportsEnumeration());
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, List.of("label1", "label2"));
        assertEquals(Set.of("label1", "label2"), labelsStore.distinctLabels());
    }

    @Test public void distinctLabels_afterRemove() {
        assumeTrue(supportsEnumeration());
        labelsStore = createLabelsStore();
        labelsStore.add(triple1, "label1");
        labelsStore.add(triple2, "label2");
        labelsStore.remove(triple1);
        assertEquals(Set.of("label2"), labelsStore.distinctLabels());
    }
}
