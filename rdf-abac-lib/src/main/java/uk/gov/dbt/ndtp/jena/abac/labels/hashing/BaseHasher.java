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

package uk.gov.dbt.ndtp.jena.abac.labels.hashing;

import com.google.common.hash.HashFunction;

import java.nio.charset.StandardCharsets;

/**
 * Base class for hash functions implementing to the Google standard.
 */
public class BaseHasher implements Hasher {

    HashFunction hashFunction;

    protected BaseHasher(HashFunction function) {
        hashFunction = function;
    }

    /**
     * Takes a string and returns the byte[] hash
     */
    @Override
    public byte[] hash(String input) {
        return hashFunction.hashString(input, StandardCharsets.UTF_8).asBytes();
    }

    /**
     * To aid with testing - provide a name
     * @return a toString() of the underlying function
     */
    @Override
    public String toString() {
        String className = hashFunction.toString();
        int lastDollarIndex = className.lastIndexOf('(');
        if (lastDollarIndex >= 0) {
            className = className.substring(0, lastDollarIndex);
        }
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            className = className.substring(lastDotIndex + 1);
        }
        return className;
    }
}
