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

import static uk.gov.dbt.ndtp.jena.abac.attributes.syntax.tokens.TokenType.STRING;

import java.util.Objects ;

import org.apache.jena.riot.RiotException ;

public final class Token
{
    private TokenType tokenType = null ;

    private String tokenImage = null ;
    private StringType stringType = null ;

    private long column ;
    private long line ;

    public final TokenType getType()        { return tokenType ; }
    public final String getImage()          { return tokenImage ; }

    public final StringType getStringType() { return stringType ; }

    public final Token setType(TokenType tokenType)     { this.tokenType = tokenType ; return this ; }
    public final Token setImage(String tokenImage)      { this.tokenImage = tokenImage ; return this ; }
    public final Token setImage(char tokenImage)        { this.tokenImage = String.valueOf(tokenImage) ; return this ; }
    public final Token setStringType(StringType st)     { this.stringType = st ; return this ; }

    static Token create(String s) {
        Tokenizer tt = TokenizerABAC.create().fromString(s).build();
        if ( ! tt.hasNext() ) {
            throw new RiotException("No token");
        }
        Token t = tt.next() ;
        if ( tt.hasNext() ) {
            throw new RiotException("Extraneous characters");
        }
        return t ;
    }

    public long getColumn() {
        return column;
    }

    public long getLine() {
        return line;
    }

    public Token(String string) { this(STRING, string) ; }

    public Token(TokenType type, String image1) {
        this() ;
        setType(type) ;
        setImage(image1) ;
    }

    private Token() { this(-1, -1) ; }

    public Token(long line, long column) { this.line = line ; this.column = column ; }

    // Convenience operations for accessing tokens.
    public String asString() {
        return switch (tokenType) {
            case STRING -> getImage();
            default -> null;
        };
    }

    public String asWord() {
        if ( !hasType(TokenType.WORD) && !hasType(TokenType.STRING) ) {
            return null;
        }
        return tokenImage;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    static final String DELIMITER = "" ;
    public String toString(boolean addLocation) {
        StringBuilder sb = new StringBuilder() ;
        if ( addLocation && getLine() >= 0 && getColumn() >= 0 ) {
            sb.append(String.format("[%d,%d]", getLine(), getColumn()));
        }
        sb.append("[") ;
        if ( getType() == null ) {
            sb.append("null");
        }
        else {
            sb.append(getType());
        }
        if ( getImage() != null ) {
            sb.append(":") ;
            sb.append(DELIMITER) ;
            sb.append(getImage()) ;
            sb.append(DELIMITER) ;
        }
        sb.append("]") ;
        return sb.toString() ;
    }
   // UNUSED
   // public boolean isEOF()      { return tokenType == TokenType.EOF ; }

    public boolean isWord()     { return tokenType == TokenType.WORD ; }

    public boolean isString()   { return tokenType == TokenType.STRING ; }

    public boolean isNumber() {
        switch (tokenType) {
            case DECIMAL :
            case DOUBLE :
            case INTEGER :
            case HEX :
                return true;
            default :
                return false;
        }
    }

    public boolean hasType(TokenType tokenType) {
        return getType() == tokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringType, tokenImage, tokenType);
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        Token other = (Token)obj;
        return stringType == other.stringType && Objects.equals(tokenImage, other.tokenImage) && tokenType == other.tokenType;
    }
}
