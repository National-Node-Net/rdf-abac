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

package uk.gov.dbt.ndtp.jena.pbac.labels.hashing;

import net.openhft.hashing.LongTupleHashFunction;

import static com.google.common.hash.Hashing.*;
import static net.openhft.hashing.LongHashFunction.*;
import static net.openhft.hashing.LongTupleHashFunction.xx128;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for generate hashing functions.
 * Note: there is always a trade-off between speed of execution
 * and the risk of collision.
 */
public class HasherUtil {

    /** Private constructor as to be used as a static class **/
    private HasherUtil(){};

    /**
     * Mapping of string config parameters to Hash Functions
     */
    public static final Map<String, Supplier<Hasher>> hasherMap = new HashMap<>();
    static {
        hasherMap.put("city64", HasherUtil::createCity64Hasher);
        hasherMap.put("farm64", HasherUtil::createFarm64Hasher);
        hasherMap.put("farmna64", HasherUtil::createFarmNaHasher);
        hasherMap.put("farmuo64", HasherUtil::createFarmUoHasher);
        hasherMap.put("metro64", HasherUtil::createMetro64Hasher);
        hasherMap.put("murmur64", HasherUtil::createMurmur64Hasher);
        hasherMap.put("murmur128", HasherUtil::createMurmer128Hasher);
        hasherMap.put("sha256", HasherUtil::createSHA256Hasher);
        hasherMap.put("sha512", HasherUtil::createSHA512Hasher);
        hasherMap.put("sip24", HasherUtil::createSIP24Hasher);
        hasherMap.put("wy3", HasherUtil::createWY64Hasher);
        hasherMap.put("xx32", HasherUtil::createXX32Hasher);
        hasherMap.put("xx64", HasherUtil::createXX64Hasher);
        hasherMap.put("xx128", HasherUtil::createXX128Hasher);
    }

    /**
     * Obtain the appropriate Hasher based on the provided string key.
     *
     * @param string The string key representing the hashing algorithm.
     * @return The corresponding Hasher. Defaults to 64 Bit XX.
     */
    public static Hasher obtainHasherFromConfig(String string) {
        if (null == string || string.isEmpty()) {
            return createXX128Hasher();
        }
        return hasherMap.getOrDefault(string.toLowerCase(), HasherUtil::createXX128Hasher).get();
    }

    /**
     * Create a 64-bit XX Hash function.
     * - Very fast, often used in scenarios requiring speed over collision resistance.
     * - Not cryptographically secure.
     * - Lower collision resistance compared to 128-bit variant. Still rare, with about
     * 2^32 hashes (~4 billion) before a collision is expected.
     *
     * @return A Hasher using the 64-bit XX Hash algorithm.
     */
    public static Hasher createXX64Hasher() {
        return new BaseLongHasher(xx3());
    }

    /**
     * Create a 32-bit XX Hash function.
     * - Extremely fast for small inputs. Faster than 64, 128 variants
     * - Not cryptographically secure.
     * - Limited collision resistance due to the smaller 32-bit output space.
     * Around 2^16 (65,000) hashes before expecting a collision.
     *
     * @return A Hasher using the 32-bit XX Hash algorithm.
     */
    public static Hasher createXX32Hasher() {
        return new BaseLongHasher(xx());
    }

    /**
     * Create a 64-bit Farm Hash function.
     * - Highly optimized for speed on 64-bit architectures.
     * - Not cryptographically secure.
     * - Slightly slower than equivalent XX Hash in some scenarios.
     * - Collision rate probability grows significantly at 2^32 hashes.
     *
     * @return A Hasher using the 64-bit Farm Hash algorithm.
     */
    public static Hasher createFarm64Hasher() {
        return new BaseHasher(farmHashFingerprint64());
    }

    /**
     * Create a Sip Hash function.
     * - Cryptographically secure, designed for resistance against hash-flooding attacks.
     * - Slower than non-cryptographic hash functions like XX or Murmur.
     * - Collision rate is low, significantly increasing after 2^64 hashes.
     *
     * @return A Hasher using the Sip 24 algorithm.
     */
    public static Hasher createSIP24Hasher() {
        return new BaseHasher(sipHash24());
    }

    /**
     * Create a SHA-256 Hash function.
     * - Cryptographically secure, widely used in security applications.
     * - Extremely low chance of collisions (256-bit hash). Almost impossible,
     * with 2^128 hashes (340 undecillion) before collisions become probable.
     * - Significantly slower than non-cryptographic hash functions.
     *
     * @return A Hasher using the SHA-256 algorithm.
     */
    public static Hasher createSHA256Hasher() {
        return new BaseHasher(sha256());
    }

    /**
     * Create a SHA-512 Hash Function
     * - Cryptographically secure with an even larger 512-bit hash size.
     * - Almost zero chance of collisions in practical applications. With
     * 2^512 hashes (115 quattuorvigintillion) before the possibility of
     * collision becomes significant.
     * - Even slower than SHA-256, especially on 32-bit architectures.
     *
     * @return A Hasher using the SHA-512 algorithm.
     */
    public static Hasher createSHA512Hasher() {
        return new BaseHasher(sha512());
    }

    /**
     * Create a 64-bit City Hash function.
     * - Optimized for small, short strings.
     * - Performs well in low-latency environments.
     * - Not cryptographically secure.
     * - Slightly slower than XX equivalent.
     * - Collision probability similar to other 64 bit functions.
     *
     * @return A Hasher using the City Hash 64-bit algorithm.
     */
    public static Hasher createCity64Hasher() {
        return new BaseLongHasher(city_1_1());
    }

    /**
     * Create a 64-bt Farm Hash NA function.
     * - Fast on modern architectures and optimized for various input sizes.
     * - Not cryptographically secure.
     * - Collision probability similar to other 64 bit functions.
     *
     * @return A Hasher using the Farm Hash NA 64-bit algorithm.
     */
    public static Hasher createFarmNaHasher() {
        return new BaseLongHasher(farmNa());
    }

    /**
     * Create a 64-bit Farm Hash UO function.
     * - Similar to FarmHash NA, optimized for various architectures.
     * - Not cryptographically secure.
     * - Collision probability similar to other 64 bit functions.
     *
     * @return A Hasher using the FarmHash UO algorithm.
     */
    public static Hasher createFarmUoHasher() {
        return new BaseLongHasher(farmUo());
    }

    /**
     * Create a 64-bit Metro Hash function.
     * - High-speed hash function for use in hash tables and checksums.
     * - Not cryptographically secure.
     * - Collision probability similar to other 64 bit functions.
     *
     * @return A Hasher using the Metro Hash 64-bit algorithm.
     */
    public static Hasher createMetro64Hasher() {
        return new BaseLongHasher(metro());
    }

    /**
     * Create a 64-bit Murmur Hash Function.
     * - Very fast on both 32-bit and 64-bit architectures.
     * - Good distribution for general-purpose use cases.
     * - Not cryptographically secure.
     * - Collision probability similar to other 64 bit functions.
     *
     * @return A Hasher using the Murmur Hash 64-bit algorithm.
     */
    public static Hasher createMurmur64Hasher() {
        return new BaseLongHasher(murmur_3());
    }


    /**
     * Create a 64-bit WY Hash function.
     * - Extremely fast, suitable for performance-sensitive environments.
     * - Not cryptographically secure.
     * - Collision probability similar to other 64 bit functions.
     *
     * @return A Hasher using the WY3 algorithm.
     */
    public static Hasher createWY64Hasher() {
        return new BaseLongHasher(wy_3());
    }


    /**
     * Create a 128-bit XX Hash function.
     * - Extremely fast, often the fastest non-cryptographic hash function.
     * - Not cryptographically secure.
     * - 128-bit size offers good collision resistance. The number of hashes
     * required before expecting a collision is in the region of ~2^64 (a quintillion) so very rare.
     * - Slower than 64 and 32 bit variants.
     *
     * @return A Hasher using the 128-bit XX Hash algorithm.
     */
    public static Hasher createXX128Hasher() {
        return new BaseLongTupleHasher(xx128());
    }

    /**
     * Create a 128-bit Murmur Hash Function.
     * - High-performance with better collision resistance than MurmurHash 64-bit.
     * - Not cryptographically secure.
     * @return A Hasher using the Murmur Hash 128-bit algorithm.
     */
    public static Hasher createMurmer128Hasher() {
        return new BaseLongTupleHasher(LongTupleHashFunction.murmur_3());
    }
}
