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


package uk.gov.dbt.ndtp.jena.pbac.lib;

import java.util.Set;

import uk.gov.dbt.ndtp.jena.pbac.AttributeValueSet;
import uk.gov.dbt.ndtp.jena.pbac.Hierarchy;
import uk.gov.dbt.ndtp.jena.pbac.attributes.Attribute;

/**
 * API for getting the users attributes.
 * An Attribute store also has the attribute value hierarchies that apply.
 */
public interface AttributesStore extends HierarchyGetter {

    /*
     * Get attributes from this {@code AttributeStore} for a given user.
     * The user must not be null.
     * <ul>
     * <li>null {@literal ->} unknown user.
     * <li>empty AttributeSet {@literal ->} known user, no attributes.
     */
    AttributeValueSet attributes(String user);

    /** Return a set of the users with an attribute value set in this store. */
    Set<String> users();

    /** Does this attribute have an associated {@link Hierarchy hierarchy} of values? */
    boolean hasHierarchy(Attribute attribute);

    /** Return the hierarchy for an attribute. Returns null if there isn't one. */
    @Override
    Hierarchy getHierarchy(Attribute attribute);
}
