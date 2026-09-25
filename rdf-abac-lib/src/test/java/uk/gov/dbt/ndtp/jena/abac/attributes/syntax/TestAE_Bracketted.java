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

package uk.gov.dbt.ndtp.jena.abac.attributes.syntax;

import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestAE_Bracketted {

    @Test
    public void test_print() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        brackettedAllow.print(mockWriter);
        verify(mockWriter, times(2)).write(anyString());
    }

    @Test
    public void test_str() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        assertEquals("(*)", brackettedAllow.str());
    }

    @Test
    public void test_sym() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        assertEquals("[()]",brackettedAllow.sym());
    }

    @Test
    public void test_hash_code() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        assertEquals(brackettedAllow.hashCode(), brackettedAllow.hashCode());
    }

    @Test
    public void test_equals_same() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        assertTrue(brackettedAllow.equals(brackettedAllow)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        assertFalse(brackettedAllow.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        assertFalse(brackettedAllow.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different() {
        AE_Bracketed brackettedAllow = new AE_Bracketed(AE_Allow.value());
        AE_Bracketed brackettedDeny = new AE_Bracketed(AE_Deny.value());
        assertFalse(brackettedAllow.equals(brackettedDeny)); // we are specifically testing the equals method here
    }
}
