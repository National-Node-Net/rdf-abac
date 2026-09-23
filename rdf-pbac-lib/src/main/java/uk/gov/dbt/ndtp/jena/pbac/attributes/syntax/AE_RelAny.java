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


package uk.gov.dbt.ndtp.jena.pbac.attributes.syntax;

import uk.gov.dbt.ndtp.jena.pbac.attributes.Operator;
import uk.gov.dbt.ndtp.jena.pbac.attributes.VisitorAttrExpr;

/**
 * Syntax element for attribute-value relation.
 */
public class AE_RelAny extends AE2_Relation {

    private final String appearance;

    public AE_RelAny(Operator operator, AE_Attribute attribute, AE_AttrValue value) {
        this(null, operator, attribute, value);
    }

    public AE_RelAny(String image, Operator operator, AE_Attribute attribute, AE_AttrValue value) {
        super(operator, attribute, value);
        // Keep the "as seen" form. Helpful during development.
        this.appearance = image;
    }

    @Override
    protected String sym() {
        if ( appearance != null )
            return appearance;
        return super.sym();
    }

    @Override
    public void visitor(VisitorAttrExpr visitor) {visitor.visit(this); }
}
