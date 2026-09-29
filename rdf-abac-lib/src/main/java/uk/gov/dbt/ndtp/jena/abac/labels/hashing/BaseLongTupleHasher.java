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

import net.openhft.hashing.LongTupleHashFunction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for Hash Functions implemented to the LZ4 standard.
 * Each function is greater than 64-bit so individual longs cannot be the return value.
 */
public class BaseLongTupleHasher implements Hasher {
    final LongTupleHashFunction hashFunction;

    protected BaseLongTupleHasher(LongTupleHashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    @Override
    public byte[] hash(String input) {
        long[] hashValue = hashFunction.hashChars(input);
        return longArrayToByteArray(hashValue);
    }

    /**
     * To aid with testing - provide a name
     * @return a toString() of the underlying function
     */
    @Override
    public String toString() {
        String className = hashFunction.getClass().getName();
        int lastDollarIndex = className.lastIndexOf('$');
        if (lastDollarIndex >= 0) {
            className = className.substring(0, lastDollarIndex);
        }
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            className = className.substring(lastDotIndex + 1);
        }
        return className;
    }

    public static byte[] longArrayToByteArray(long[] longArray) {
        // Each long is 8 bytes, so the byte array should be longArray.length * 8 in size
        byte[] byteArray = new byte[longArray.length * Long.BYTES];

        // Use a ByteBuffer to convert the long array to bytes
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN); // You can set the byte order as needed

        // Put each long value into the ByteBuffer
        for (long value : longArray) {
            byteBuffer.putLong(value);
        }
        return byteArray;
    }

}
