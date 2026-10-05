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

import static org.apache.jena.atlas.lib.Chars.*;
import static org.apache.jena.riot.system.RiotChars.*;

import java.util.NoSuchElementException;
import java.util.Objects;

import uk.gov.dbt.ndtp.jena.abac.attributes.AttributeSyntaxError;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.io.PeekReader;
import org.apache.jena.riot.system.ErrorHandler;

/**
 * Tokenizer for the ABAC Attribute Labels.
 * Supports addition tokens.
 * Derived from TokenizerText in Jena.
 * Label tokenizing is a much simpler task.
 */
public final class TokenizerABAC implements Tokenizer
{
    // Drop through to final general symbol/keyword reader, including <=, !=
    // Care with <=

    private Token token = null;
    private final StringBuilder stringBuilder = new StringBuilder(200);
    private final PeekReader reader;
    // Whether whitespace between tokens includes newlines (in various forms).
    private final boolean lineMode;
    private boolean finished = false;

    // Not in jena 4.5.0, Replace at 4.6.0
    private static final char CH_EMARK        = '!' ;

    // The code assumes that errors throw exception and so stop parsing.
    private final ErrorHandler errorHandler;

    public static TokenizeLabelsBuilder create() { return new TokenizeLabelsBuilder() ; }

    public static Tokenizer fromString(String string) { return create().fromString(string).build(); }

    /*package*/ static TokenizerABAC internal(PeekReader reader, boolean lineMode, ErrorHandler errorHandler) {
        return new TokenizerABAC(reader, lineMode, errorHandler);
    }

    private TokenizerABAC(PeekReader reader, boolean lineMode, ErrorHandler errorHandler) {
        this.reader = Objects.requireNonNull(reader, "PeekReader");
        this.lineMode = lineMode;
        this.errorHandler = Objects.requireNonNull(errorHandler, "ErrorHandler");
    }

    @Override
    public final boolean hasNext() {
        if ( finished )
            return false;
        if ( token != null )
            return true;

        try {
            skip();
            if ( reader.eof() ) {
                // close();
                finished = true;
                return false;
            }
            token = parseToken();
            // token cannot be null as parseToken always creates one
//            if ( token == null ) {
//                // close();
//                finished = true;
//                return false;
//            }
            return true;
        } catch (AtlasException ex) {
            if ( ex.getCause() != null ) {
                if ( ex.getCause().getClass() == java.nio.charset.MalformedInputException.class ) {
                    error("Bad character encoding", reader.getLineNum(), reader.getColNum());
                }
                error("Bad input stream [" + ex.getCause() + "]", reader.getLineNum(), reader.getColNum());
            }
            error("Bad input stream", reader.getLineNum(), reader.getColNum());
            return false;
        }
    }

    @Override
    public final boolean eof() {
        return !hasNext();
    }

    @Override
    public final Token next() {
        if ( !hasNext() )
            throw new NoSuchElementException();
        Token t = token;
        token = null;
        return t;
    }

    @Override
    public final Token peek() {
        if ( !hasNext() )
            return null;
        return token;
    }

    @Override
    public void close() {
        IO.close(reader);
    }

    // ---- Machinery

    private void skip() {
        int ch;
        for (;;) {
            if ( reader.eof() ) {
                return;
            }
            ch = reader.peekChar();
            if ( ch == CH_HASH ) {
                ch = skipToNewLine();
            }

            // Including excess newline chars from comment.
            if ( lineMode ) {
                if ( !isHorizontalWhitespace(ch) ) {
                    break;
                }
            } else if ( !isWhitespace(ch) ) {
                break;
            }

            reader.readChar();
        }
    }

    private int skipToNewLine() {
        int ch;
        reader.readChar();
        for (;;) {
            ch = reader.peekChar();
            if ( ch == EOF || isNewlineChar(ch) ) {
                break;
            }
            reader.readChar();
        }
        return ch;
    }

    private Token parseToken() {
        token = new Token(getLine(), getColumn());

        int ch = reader.peekChar();

//        // ---- IRI
//        // Maybe switch to "longest wins"
//        if ( ch == CH_LT ) {
//            // Look ahead on char
//            reader.readChar();
//            int chPeek = reader.peekChar();
//            // not '=', or space
//            if ( chPeek != ' ' && chPeek != '=' ) {
//                token.setImage(readIRI());
//                token.setType(TokenType.IRI);
//                return token;
//            }
//            // Dropthrough.
//            reader.pushbackChar(CH_LT);
//            //fatal("Internal error - parsed '%c' after '<'", chPeek);
//        }

        // ---- Literal
        if ( ch == CH_QUOTE1 || ch == CH_QUOTE2 ) {
            return parseQuote(ch);
        }

        // Other single and start chars.
        switch(ch)
        {
            case CH_SEMICOLON:  return oneChar(TokenType.SEMICOLON, CH_SEMICOLON);
            case CH_COMMA:      return oneChar(TokenType.COMMA, CH_COMMA);

            case CH_LBRACE:     return oneChar(TokenType.LBRACE, CH_LBRACE);
            case CH_RBRACE:     return oneChar(TokenType.RBRACE, CH_RBRACE);

            case CH_LPAREN:     return oneChar(TokenType.LPAREN, CH_LPAREN);
            case CH_RPAREN:     return oneChar(TokenType.RPAREN, CH_RPAREN);
            case CH_LBRACKET:   return oneChar(TokenType.LBRACKET, CH_LBRACKET);
            case CH_RBRACKET:   return oneChar(TokenType.RBRACKET, CH_RBRACKET);

            case CH_SLASH:      return oneChar(TokenType.SLASH, CH_SLASH);
            case CH_RSLASH:     return oneChar(TokenType.RSLASH, CH_RSLASH);

            case CH_COLON:      return oneChar(TokenType.COLON, CH_COLON);
            case CH_STAR:       return oneChar(TokenType.STAR, CH_STAR);
            case CH_QMARK:      return oneChar(TokenType.QMARK, CH_QMARK);

            // Multi-character symbols
            // Two character tokens : !=, GE >= , LE <=, &&, ||
            case CH_EQUALS:     return maybeTwoChar(CH_EQUALS, CH_EQUALS, TokenType.EQ, TokenType.EQUIVALENT, "==");
            case CH_EMARK:      return maybeTwoChar(CH_EMARK, CH_EQUALS, TokenType.EMARK, TokenType.NE, "!=");
            case CH_LT:         return maybeTwoChar(CH_LT, CH_EQUALS, TokenType.LT, TokenType.LE, "<=");
            case CH_GT:         return maybeTwoChar(CH_GT, CH_EQUALS, TokenType.GT, TokenType.GE, ">=");

            case CH_VBAR:       return maybeTwoChar(CH_VBAR, CH_VBAR, TokenType.VBAR, TokenType.LOGICAL_OR, "||");
            case CH_AMPHERSAND: return maybeTwoChar(CH_AMPHERSAND, CH_AMPHERSAND, TokenType.AMPERSAND, TokenType.LOGICAL_AND, "&&");
            default:
                break;
        }

        if ( isNewlineChar(ch) ) {
            do {
                reader.readChar();
                // insertCodepointDirect(stringBuilder,ch2);
            } while (isNewlineChar(reader.peekChar()));
            token.setType(TokenType.NL);
            //** token.setImage(stringBuilder.toString());
            return token;
        }

        // Numbers - maybe.
        /*
         * Turtle syntax rules.
            integer         ::=     ('-' | '+') ? [0-9]+
            double          ::=     ('-' | '+') ? ( [0-9]+ '.' [0-9]* exponent | '.' ([0-9])+ exponent | ([0-9])+ exponent )
            decimal         ::=     ('-' | '+')? ( [0-9]+ '.' [0-9]* | '.' ([0-9])+ | ([0-9])+ )
                                        0.0 .0 0.
        [19]    exponent        ::=     [eE] ('-' | '+')? [0-9]+
        []      hex             ::=     0x0123456789ABCDEFG
        */
        if ( ch == CH_PLUS || ch == CH_MINUS || range(ch, '0', '9')) {
            return parseNumeric(ch);
        }

        // Plain words
        if ( Words.isWordStart(ch) ) {
            String str = readWord();
            token.setType(TokenType.WORD);
            token.setImage(str);
            return token;
        }
        throw fatal("Bad character: %c", (char)ch);
    }

    private Token parseNumeric(int ch) {
        if(ch == CH_PLUS || ch == CH_MINUS) {
            reader.readChar();
            // Peek for base plus and minus.
            int ch2 = reader.peekChar();

            if (!range(ch2, '0', '9')) {
                // ch was end of symbol.
                // reader.readChar();
                if (ch == CH_PLUS) {
                    token.setType(TokenType.PLUS);
                } else {
                    token.setType(TokenType.MINUS);
                }
                return token;
            }
        }
        readNumber();
        return token;
    }

    private Token parseQuote(int ch) {
        // The token type is STRING.
        // We incorporate this into a token for LITERAL_LANG or LITERAL_DT.
        token.setType(TokenType.STRING);

        reader.readChar();
        int ch2 = reader.peekChar();
        if ( ch2 == ch ) {
            reader.readChar(); // Read potential second quote.
            int ch3 = reader.peekChar();
            if ( ch3 == ch ) {
                reader.readChar();     // Read potential third quote.
                token.setImage(readLongString(ch));
                StringType st = (ch == CH_QUOTE1) ? StringType.LONG_STRING1 : StringType.LONG_STRING2;
                token.setStringType(st);
            } else {
                // Two quotes then a non-quote.
                // Must be '' or ""
                // No need to pushback characters as we know the lexical
                // form is the empty string.
                // if ( ch2 != EOF ) reader.pushbackChar(ch2);
                // if ( ch1 != EOF ) reader.pushbackChar(ch1); // Must be
                // '' or ""
                token.setImage("");
                StringType st = (ch == CH_QUOTE1) ? StringType.STRING1 : StringType.STRING2;
                token.setStringType(st);
            }
        } else {
            // One quote character.
            token.setImage(readString(ch));
            // Record exactly what form of STRING was seen.
            StringType st = (ch == CH_QUOTE1) ? StringType.STRING1 : StringType.STRING2;
            token.setStringType(st);
        }
        return token;
    }

    private Token oneChar(TokenType tokenType, char character) {
        reader.readChar();
        token.setType(tokenType);
        token.setImage(character);
        return token;
    }

    private Token maybeTwoChar(char firstChar, char secondChar, TokenType oneCharToken, TokenType twoCharToken, String twoCharStr) {
        reader.readChar();
        int ch2 = reader.peekChar();
        if ( ch2 == secondChar ) {
            reader.readChar();
            token.setType(twoCharToken);
            token.setImage(twoCharStr);
            return token;
        }
        token.setType(oneCharToken);
        token.setImage(firstChar);
        return token;
    }

    // Get characters between two markers.
    // strEscapes may be processed
    private String readString(int endCh) {
        // Position at start of string.
        stringBuilder.setLength(0);
        // Assumes first delimiter char read already.
        // Reads terminating delimiter

        for (;;) {
            int ch = reader.readChar();

            // Raw replacement char in a string.
            if ( ch == REPLACEMENT ) {
                warning("Unicode replacement character U+FFFD in string");
            } else if ( ch == EOF ) {
                // if ( endNL ) return stringBuilder.toString();
                throw fatal("Broken token: %s", stringBuilder.toString());
            } else if ( ch == NL ) {
                throw fatal("Broken token (newline): %s", stringBuilder.toString());
            } else if ( ch == endCh ) {
                return stringBuilder.toString();
            }
            else if ( ch == CH_RSLASH ) {
                // Allow escaped replacement character.
                ch = readLiteralEscape();
            }
            insertCodepoint(stringBuilder, ch);
        }
    }

    private String readLongString(int quoteChar) {
        stringBuilder.setLength(0);
        for (;;) {
            int ch = reader.readChar();
            if ( ch == REPLACEMENT ) {
                warning("Input has Unicode replacement character U+FFFD in string");
            } else if ( ch == EOF ) {
                throw fatal("Broken long string");
            } else if ( ch == quoteChar ) {
                if ( threeQuotes(quoteChar) ) {
                    return stringBuilder.toString();
                }
            } else if ( ch == CH_RSLASH ) {
                ch = readLiteralEscape();
            }
            insertCodepoint(stringBuilder, ch);
        }
    }

    // Assume we have read the first quote char.
    // On return:
    //   If false, have moved over no more characters (due to pushbacks)
    //   If true, at end of 3 quotes
    private boolean threeQuotes(int ch) {
        // reader.readChar(); // Read first quote.
        int ch2 = reader.peekChar();
        if ( ch2 != ch ) {
            // reader.pushbackChar(ch2);
            return false;
        }

        reader.readChar(); // Read second quote.
        int ch3 = reader.peekChar();
        if ( ch3 != ch ) {
            // reader.pushbackChar(ch3);
            reader.pushbackChar(ch2);
            return false;
        }

        // Three quotes.
        reader.readChar(); // Read third quote.
        return true;
    }

    /*
     * [146]  INTEGER  ::=  [0-9]+
     * [147]  DECIMAL  ::=  [0-9]* '.' [0-9]+
     * [148]  DOUBLE  ::=  [0-9]+ '.' [0-9]* EXPONENT | '.' ([0-9])+ EXPONENT | ([0-9])+ EXPONENT
     * []     hex             ::=     0x0123456789ABCDEFG
     */
    private void readNumber() {
        // One entry, definitely a number.
        // Already dealt with '.' as a (non) decimal.
        boolean isDouble = false;
        boolean isDecimal = false;
        stringBuilder.setLength(0);

        int x = 0; // Digits before a dot.
        int ch = reader.peekChar();
        if ( ch == '0' ) {
            x++;
            reader.readChar();
            insertCodepointDirect(stringBuilder, ch);
            ch = reader.peekChar();
            if ( ch == 'x' || ch == 'X' ) {
                reader.readChar();
                insertCodepointDirect(stringBuilder, ch);
                readHex(reader, stringBuilder);
                token.setImage(stringBuilder.toString());
                token.setType(TokenType.HEX);
                return;
            }
        }

        x += readDigits(stringBuilder);
        ch = reader.peekChar();
        if ( ch == CH_DOT ) {
            reader.readChar();
            stringBuilder.append(CH_DOT);
            isDecimal = true; // Includes things that will be doubles.
            readDigits(stringBuilder);
        }

        if ( x == 0 && !isDecimal ) {
            // Possible a tokenizer error - should not have entered readNumber
            // in the first place.
            throw fatal("Unrecognized as number");
        }
        if ( exponent(stringBuilder) ) {
            isDouble = true;
            isDecimal = false;
        }

        // Final part - "decimal" 123. is an integer 123 and a DOT.
        if ( isDecimal ) {
            int len = stringBuilder.length();
            if ( stringBuilder.charAt(len - 1) == CH_DOT ) {
                stringBuilder.setLength(len - 1);
                reader.pushbackChar(CH_DOT);
                isDecimal = false;
            }
        }

        token.setImage(stringBuilder.toString());
        if ( isDouble )
            token.setType(TokenType.DOUBLE);
        else if ( isDecimal )
            token.setType(TokenType.DECIMAL);
        else
            token.setType(TokenType.INTEGER);
    }

    private void readHex(PeekReader reader, StringBuilder sb) {
        // Just after the 0x, which are in sb
        int x = 0;
        for (;;) {
            int ch = reader.peekChar();
            if ( !isHexChar(ch) ) {
                break;
            }
            reader.readChar();
            insertCodepointDirect(sb, ch);
            x++;
        }
        if ( x == 0 ) {
            throw fatal("No hex characters after %s", sb.toString());
        }
    }

    private int readDigits(StringBuilder buffer) {
        int count = 0;
        for (;;) {
            int ch = reader.peekChar();
            if ( !range(ch, '0', '9') ) {
                break;
            }
            reader.readChar();
            insertCodepointDirect(buffer, ch);
            count++;
        }
        return count;
    }

    private void readPossibleSign(StringBuilder sb) {
        int ch = reader.peekChar();
        if ( ch == '-' || ch == '+' ) {
            reader.readChar();
            insertCodepointDirect(sb, ch);
        }
    }

    private boolean exponent(StringBuilder sb) {
        int ch = reader.peekChar();
        if ( ch != 'e' && ch != 'E' ) {
            return false;
        }
        reader.readChar();
        insertCodepointDirect(sb, ch);
        readPossibleSign(sb);
        int x = readDigits(sb);
        if ( x == 0 ) {
            throw fatal("Malformed double: %s", sb);
        }
        return true;
    }

    // -- Words, processed as a string of characters.
    // First character has been verified but not read yet.
    private String readWord() {
        // First char
        int firstCh = reader.readChar();
        //if ( ! Words.isWordStart(firstCh) ) {}

        stringBuilder.setLength(0);
        insertCodepoint(stringBuilder, firstCh);
        // First char is valid last char.
        int idx = 0;
        // Remember the index of the last seen character that is legal as a last character.
        int lastValidLastChar = 0;
        // Loop:middle and last character.
        int chRead = EOF;
        for (;;) {
            int ch = reader.peekChar();
            if ( ch == EOF ) {
                break;
            }
            if ( ! Words.isWordMiddle(ch) ) {
                break;
            }

            chRead = reader.readChar();
            idx++;
            insertCodepointDirect(stringBuilder, chRead);
            if ( Words.isWordEnd(ch) ) {
                lastValidLastChar = idx;
            }
            // Loop
        }

        // Delete: note first character was valid
        if ( chRead != EOF && lastValidLastChar != idx ) {
            // Move backwards.
            for ( int i = idx ; i > lastValidLastChar ; i-- ) {
                stringBuilder.deleteCharAt(i);
                reader.pushbackChar(chRead);
            }
        }
        return stringBuilder.toString();
    }

    // --

    private void insertCodepoint(StringBuilder buffer, int ch) {
        if ( Character.charCount(ch) == 1 )
            insertCodepointDirect(buffer, ch);
        else {
            // Convert to UTF-16. Note that the rest of any system this is used
            // in must also respect codepoints and surrogate pairs.
            if ( !Character.isDefined(ch) && !Character.isSupplementaryCodePoint(ch) ) {
                throw fatal("Illegal codepoint: 0x%04X", ch);
            }
            char[] chars = Character.toChars(ch);
            buffer.append(chars);
        }
    }

    // Insert code point, knowing that 'ch' is 16 bit (basic plane)
    private static void insertCodepointDirect(StringBuilder buffer, int ch) {
        buffer.append((char)ch);
    }

    @Override
    public long getColumn() {
        return reader.getColNum();
    }

    @Override
    public long getLine() {
        return reader.getLineNum();
    }

    // ---- Escape sequences

    private final int readLiteralEscape() {
        int c = reader.readChar();
        if ( c == EOF ) {
            throw fatal("Escape sequence not completed");
        }
        return switch (c) {
            case 'n' -> NL;
            case 'r' -> CR;
            case 't' -> TAB;
            case 'f' -> '\f';
            case 'b' -> BSPACE;
            case '"' -> '"';
            case '\'' -> '\'';
            case '\\' -> '\\';
            case 'u' -> readUnicode4Escape();
            case 'U' -> readUnicode8Escape();
            default -> throw fatal("Illegal escape sequence value: %c (0x%02X)", c, c);
        };
    }

    private final
    int readUnicode4Escape() { return readHexSequence(4); }

    private final int readUnicode8Escape() {
        int ch8 = readHexSequence(8);
        if ( ch8 > Character.MAX_CODE_POINT ) {
            throw fatal("Illegal code point in \\U sequence value: 0x%08X", ch8);
        }
        return ch8;
    }

    private final int readHexSequence(int N) {
        int x = 0;
        for (int i = 0; i < N; i++) {
            int d = readHexChar();
            if ( d < 0 ) {
                return -1;
            }
            x = (x << 4) + d;
        }
        return x;
    }

    private final int readHexChar() {
        int ch = reader.readChar();
        if ( ch == EOF ) {
            throw fatal("Not a hexadecimal character (end of file)");
        }
        int x = valHexChar(ch);
        if ( x != -1 ) {
            return x;
        }
        throw fatal("Not a hexadecimal character: '%c'", (char)ch);
    }

    /** Warning - can continue. */
    private void warning(String message, Object... args) {
        String msg = String.format(message, args);
        errorHandler.warning(msg, reader.getLineNum(), reader.getColNum());
    }

    /**
     * Error - at the tokenizer level, it can continue (with some junk) but it is a serious error and the
     * caller probably should treat as an error and stop.
     * @param message description of error
     * @param args details to populate description
     */
    private void error(String message, Object... args) {
        String msg = String.format(message, args);
        errorHandler.error(msg, reader.getLineNum(), reader.getColNum());
        throw new AttributeSyntaxError(msg);
    }

    /** Structural error - unrecoverable - but reported as ERROR (FATAL can imply system fault) */
    private AttributeSyntaxError fatal(String message, Object... args) {
        String msg = String.format(message, args);
        long line = reader.getLineNum();
        if ( line == 1 ) {
            // Line is normally 1: don't print.
            line = -1;
        }
        errorHandler.fatal(msg, line, reader.getColNum());
        // We require that errors to cause the tokenizer to stop so in case the
        // provided error handler does not, we throw an exception.
        return new AttributeSyntaxError(message);
    }
}
