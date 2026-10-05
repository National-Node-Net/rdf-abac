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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import uk.gov.dbt.ndtp.jena.abac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.abac.Hierarchy;
import uk.gov.dbt.ndtp.jena.abac.attributes.Attribute;

import java.time.Duration;
import java.util.Set;

/**
 * Simple cached wrapper around attribute store for caching purposes.
 * Only makes sense for use with #AttributesStoreRemote but for testing purposes,
 * we will allow #AttributesStoreLocal.
 */
public class AttributeStoreCache implements AttributesStore {

    final Cache<String, AttributeValueSet> userAttributeCache;
    final Cache<Attribute, Hierarchy> hierarchyCache;
    final AttributesStore underlyingStore;

    final static long DEFAULT_USER_CACHE_SIZE = 50L;
    final static long DEFAULT_HIERARCHY_CACHE_SIZE = 5L;
    /**
     * Build the Cached store
     * @param underlyingStore the configured remote store (or for testing/dev local)
     * @param userCacheExpiryTime how long to hold user attributes
     * @param hierarchyCacheExpiryTime how long to hold hierarchies
     *
     * Note: We currently record cache statistics. We may wish to make this configurable in the future.
     */
    public AttributeStoreCache(AttributesStore underlyingStore,
                               Duration userCacheExpiryTime,
                               Duration hierarchyCacheExpiryTime) {
        this(underlyingStore, userCacheExpiryTime, hierarchyCacheExpiryTime, DEFAULT_USER_CACHE_SIZE, DEFAULT_HIERARCHY_CACHE_SIZE);
    }

    /**
     * Build the Cached store
     * @param underlyingStore the configured remote store (or for testing/dev local)
     * @param userCacheExpiryTime how long to hold user attributes
     * @param hierarchyCacheExpiryTime how long to hold hierarchies
     * @param userCacheMaxSize how big a cache to use
     * @param hierarchyCacheMaxSize how large a cache
     *
     * Note: We currently record cache statistics. We may wish to make this configurable in future.
     */
    public AttributeStoreCache(AttributesStore underlyingStore,
                               Duration userCacheExpiryTime,
                               Duration hierarchyCacheExpiryTime,
                               long userCacheMaxSize,
                               long hierarchyCacheMaxSize) {
        this.underlyingStore = underlyingStore;
        userAttributeCache = Caffeine.newBuilder().maximumSize(userCacheMaxSize).expireAfterWrite(userCacheExpiryTime).build();
        hierarchyCache = Caffeine.newBuilder().maximumSize(hierarchyCacheMaxSize).expireAfterWrite(hierarchyCacheExpiryTime).build();
    }

    @Override
    public AttributeValueSet attributes(String user) {
        return userAttributeCache.get(user, underlyingStore::attributes);
    }

    @Override
    public Set<String> users() {
        return underlyingStore.users();
    }

    @Override
    public boolean hasHierarchy(Attribute attribute) {
        Hierarchy hierarchy = getHierarchy(attribute);
        return hierarchy != null && ! hierarchy.values().isEmpty();
    }

    @Override
    public Hierarchy getHierarchy(Attribute attribute) {
        return hierarchyCache.get(attribute, underlyingStore::getHierarchy);
    }
}
