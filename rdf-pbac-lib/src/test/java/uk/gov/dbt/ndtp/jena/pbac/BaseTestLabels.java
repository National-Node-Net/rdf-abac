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


package uk.gov.dbt.ndtp.jena.pbac;

import java.util.stream.Stream;

import uk.gov.dbt.ndtp.jena.pbac.labels.LabelsStore;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.params.provider.Arguments;

/**
 * File driven tests. See {@link BuildAIO}.
 * <p>
 * A test file has the data, the labelling and the expected results.
 */
public abstract class BaseTestLabels {
    static {
        JenaSystem.init();
        LogCtl.setLog4j2();
    }

    private final static String DIR = "src/test/files/labels/";

    protected void test(String filename, int count, LabelsStore testSubject) {
        PBACTestRunner.runTest(DIR+filename, "u1", count, testSubject);
    }

    // Choices of files.
    protected static Stream<Arguments> labels_files_concrete() {
        return Stream.of(
                         Arguments.of("t01-1triple-yes.trig", 1),
                         Arguments.of("t02-1triple-no.trig", 0),
                         Arguments.of("t03-1triple-all.trig", 1),
                         Arguments.of("t04-1triple-deny.trig", 0),
                         Arguments.of("t05-1triple-dft.trig", 1)
        );
    }

    protected static Stream<Arguments> labels_files_with_patterns() {
            return Stream.of(
                             Arguments.of("t01-1triple-yes.trig", 1),
                             Arguments.of("t02-1triple-no.trig", 0),

                             Arguments.of("t03-1triple-all.trig", 1),
                             Arguments.of("t04-1triple-deny.trig", 0),

                             Arguments.of("t05-1triple-dft.trig", 1),

                             // These have patterns
                             Arguments.of("t07-1triple-any-yes.trig", 1),
                             Arguments.of("t08-1triple-any-no.trig", 0),

                             Arguments.of("t50-triples.trig", 2),
                             Arguments.of("t51-triples.trig", 2),
                             Arguments.of("t55-triples.trig", 2)
            );
    }

}
