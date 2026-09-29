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


package uk.gov.dbt.ndtp.jena.abac.services;

/**
 * Constants for the attribute store service.
 */
public class AttributeService {
    // User lookup
    public static final String LOOKUP_USER_ATTRIBUTE_TEMPLATE = "/users/lookup/{user}";
    public static final String LOOKUP_USER_ATTRIBUTE_PATH = LibAuthService.templateToPathName(LOOKUP_USER_ATTRIBUTE_TEMPLATE);
    public static final String LOOKUP_USER_ATTRIBUTE_SERVLET_PATH_SPEC = LOOKUP_USER_ATTRIBUTE_PATH +"*";

    // Hierarchy lookup
    public static final String LOOKUP_HIERARCHY_TEMPLATE = "/hierarchies/lookup/{name}";
    public static final String LOOKUP_HIERARCHY_PATH = LibAuthService.templateToPathName(LOOKUP_HIERARCHY_TEMPLATE);
    public static final String LOOKUP_HIERARCHY_SERVLET_PATH_SPEC = LOOKUP_HIERARCHY_PATH +"*";
}
