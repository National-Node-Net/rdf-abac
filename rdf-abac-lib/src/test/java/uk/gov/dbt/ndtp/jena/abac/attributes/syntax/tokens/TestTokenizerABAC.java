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

package uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens;

import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeSyntaxError;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens.Tokenizer;
import uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens.TokenizerABAC;

import static org.junit.jupiter.api.Assertions.*;

public class TestTokenizerABAC {

    @Test
    public void test_next_exception() {
        Tokenizer tokenizer = TokenizerABAC.fromString("");
        assertThrows(NoSuchElementException.class, tokenizer::next);
    }

    @Test
    public void test_has_next_broken_long_string() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\"\"");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 4] Broken long string", exception.getMessage());
    }

    @Test
    public void test_has_next_long_string_broken_token() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\"\"�");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 5] Broken long string", exception.getMessage());
    }

    @Test
    public void test_has_next_valid_long_string_type1() {
        Tokenizer tokenizer = TokenizerABAC.fromString("'''a'''");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_long_string_type2() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\"\"a\"\"\"");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_long_string_type_with_quote() {
        Tokenizer tokenizer = TokenizerABAC.fromString("'''a'b'''");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_long_string_type_with_two_quote() {
        Tokenizer tokenizer = TokenizerABAC.fromString("'''a''b'''");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_long_string() {
        Tokenizer tokenizer = TokenizerABAC.fromString("''a''");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_semicolon() {
        Tokenizer tokenizer = TokenizerABAC.fromString(";");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_left_bracket() {
        Tokenizer tokenizer = TokenizerABAC.fromString("[");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_right_bracket() {
        Tokenizer tokenizer = TokenizerABAC.fromString("]");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_valid_slash() {
        Tokenizer tokenizer = TokenizerABAC.fromString("/");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_qmark() {
        Tokenizer tokenizer = TokenizerABAC.fromString("?");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_bad_character() {
        Tokenizer tokenizer = TokenizerABAC.fromString("©");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 1] Bad character: ©", exception.getMessage());
    }

    @Test
    public void test_has_next_new_line() {
        Tokenizer tokenizer = TokenizerABAC.fromString("#a\n\n");
        assertFalse(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_broken_token() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"�");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 3] Broken token: �", exception.getMessage());
    }

    @Test
    public void test_has_next_broken_token_new_line() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\n");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertTrue(exception.getMessage().contains("[line: 2, col: 1 ] Broken token (newline): "));
    }

    @Test
    public void test_has_next_read_escape_cr() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\\r\"");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_read_escape_nl() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\\n\"");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_read_escape_f() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\\f\"");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_read_escape_b() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\\b\"");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_read_escape_backslash() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\\");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 3] Escape sequence not completed", exception.getMessage());
    }

    @Test
    public void test_close() {
        Tokenizer tokenizer = TokenizerABAC.fromString("");
        tokenizer.close();
        assertThrows(NullPointerException.class, tokenizer::hasNext);
    }

    @Test
    public void test_has_next_hash() {
        Tokenizer tokenizer = TokenizerABAC.fromString("#comment\n");
        assertFalse(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_hash_with_newlines() {
        Tokenizer tokenizer = TokenizerABAC.fromString("#comment\n\n");
        assertFalse(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_hex() {
        Tokenizer tokenizer = TokenizerABAC.fromString("0XABCD");
        assertTrue(tokenizer.hasNext());
    }

    @Test
    public void test_has_next_with_number() {
        Tokenizer tokenizer = TokenizerABAC.fromString("0XABCD");
        assertTrue(tokenizer.hasNext());
    }

}
