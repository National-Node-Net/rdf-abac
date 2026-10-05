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

import net.openhft.hashing.LongHashFunction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for Hash Functions implemented to the LZ4 standard.
 * Each function is 64-bit due to Long being the return value.
 */
public class BaseLongHasher implements Hasher {
    final LongHashFunction hashFunction;

    protected BaseLongHasher(LongHashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    @Override
    public byte[] hash(String input) {
        long hashValue = hashFunction.hashChars(input);
        return formatLongVariable(hashValue);
//        return convertLong(hashValue);
    }

    /**
     * To aid with testing - provide a name
     *
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

    /**
     * Alternate method for converting long to byte[]
     * @param hashValue value to convert
     * @return byte[]
     */
    static byte[] convertLong(long hashValue) {
        byte[] hashBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            hashBytes[i] = (byte) ((hashValue >>> (8 * i)) & 0xFF);
        }
        return hashBytes;
    }

    /**
     * Convert long to byte[] but use less space if we can.
     * @param value to convert
     * @return byte[] of smallest size possible.
     */
    static byte[] formatLongVariable(long value) {
        ByteBuffer byteBuffer;
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            // Value fits in 8 bytes (long)
            byteBuffer = ByteBuffer.allocate(Long.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putLong(value);
        } else if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            // Value fits in 4 bytes (int)
            byteBuffer = ByteBuffer.allocate(Integer.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt((int) value);
        } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            // Value fits in 2 bytes (short)
            byteBuffer = ByteBuffer.allocate(Short.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putShort((short) value);
        } else {
            // Value fits in 1 byte
            byteBuffer = ByteBuffer.allocate(Byte.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.put((byte) value);
        }

        return byteBuffer.array();
    }
}
