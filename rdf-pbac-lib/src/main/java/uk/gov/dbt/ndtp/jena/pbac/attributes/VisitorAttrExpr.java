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


package uk.gov.dbt.ndtp.jena.pbac.attributes;

import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Allow;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_And;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Attribute;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Bracketed;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Deny;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Or;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_RelAny;
import uk.gov.dbt.ndtp.jena.pbac.attributes.syntax.AE_Var;

public interface VisitorAttrExpr {
//    public void visit(AE_Allow element);
//    public void visit(AE_Deny element);
//
//    public void visit(AE_Attribute element);
//    public void visit(AE_Var element);
//
//    //public void visit(AE1 element);
//
//    public void visit(AE_Bracketted element);
//
//    public void visit(AE_And element);
//    public void visit(AE_Or element);
//    public void visit(AE_RelAny element);

    public default void visit(AE_Allow element) {}
    public default void visit(AE_Deny element) {}

    public default void visit(AE_Attribute element) {}
    public default void visit(AE_Var element) {}

    //public void visit(AE1 element) {}

    public default void visit(AE_Bracketed element) {}

    public default void visit(AE_And element) {}
    public default void visit(AE_Or element) {}
    public default void visit(AE_RelAny element) {}

}
