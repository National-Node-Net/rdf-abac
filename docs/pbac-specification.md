# RDF-PBAC : Specification
**Repository:** `rdf-pbac`  
**Description:** `Specifications for RDF-PBAC`    
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

* [Terminology](#terminology)
* [Attribute and Label Syntax](#syntax)
  * [Syntax of words](#syntax-of-words)
  * [Attribute Expressions](#attribute-expressions)
  * [Special Attribute Expressions](#special-attribute-expressions)
  * [Attribute Values](#attribute-values)
  * [Lists](#lists)
* [Evaluation](#evaluation)
  * [Attribute Expression Evaluation ](#attribute-expression-evaluation )
  * [Attribute Expression List Evaluation](#attribute-expression-list-evaluation)
* [Transport](#transport)
  * [Transport by header](#transport-by-header)
  * [Transport in RDF](#transport-in-rdf)

## Terminology

Concepts:
* <dfn>Attribute</dfn> - a name.
* <dfn>Attribute value</dfn> - a pair of an attribute and a value, written "attribute = value".
* <dfn>Attribute expression</dfn> - a boolean value expression where attributes
  are used to identify values in the current request context. An attribute value pair
  is an attribute expression.
* <dfn>Attribute expression list</dfn> - 
  a comma-separated list of attribute expressions.
* <dfn>Attribute value list</dfn> - 
  a comma-separated list of attribute-values.
* <dfn>label</dfn> - a string asscocuated with data items, in attribute expression list syntax.

These can be written using the syntax below.

Data items have labels.  
A user has a number of attribute values.  

Attribute expression evaluation uses the attribute values of an application
request to produce a value, which is either `true` or `false`.

## Syntax

An attribute is a sequence of Unicode characters.

It is written as either as a string value (in quotes) or as a word (no
quotes).

It does not matter how the attribute is written. `abc` and `"abc"` are the same attribute.
The string form is more general; a bare word has some syntax restrictions.

Strings can be written with single or double quotes; it does not matter which is
used except that a string must start and finish with the same kind of quote.

Strings may include escaped characters (same as an RDF Turtle): `\t`, `\n`, `\\\\` and unicode
escapes such as `\u1234` and `\U12345678`.

A string can be any sequence of characters, including an empty string, and
strings with spaces in them, which can not be written as word labels.

Examples:
```
label
"label"
'label'
"one attribute"
``` 

### Syntax of Words

* A word is one or more characters.
* It is composed of characters which are alphanumeric or one of `_`, `:`, `.`, `-`, `+`.
* It must start with a letter or with `_` (not `:`, `.`, `-`, `+`).
* It must end with a letter or with `_` (not `:`, `.`, `-`, `+`).
* Is not a keyword (`true` or `false`)

```
word ::= WordStartChar ( WordMiddleChar* WordLastChar )?
WordStartChar   ::= ( AZ  | '_' )
WordMiddleChar  ::= ( AZN | '_' | ':' | '.' | '-' | '+' )
WordLastChar    ::= ( AZ  | '_' )

AZ ::= (Unicode alphabetic)
AZN ::= (AZ|['0'-'9'])
```

## Attribute Expressions

Data is labelled with an attribute expressions.

Example expressions:
```
word
"string"
country=uk & employee
country=us & ( employee | contractor)
classification = secret
classification = "quite secret"
```

Grammar:
```
        Expr       = ExprOr | "*" | "!"
        ExprOr     = ExprAnd ("|" ExprOr)*
        ExprAnd    = ExprRel ("&" ExprAnd) *
        ExprRel    = Bracketted | Attr (Relation ValueTerm)?
        Attr       = (AZN|[_])(AZN|[_:.-+])*(AZN|[_])? | quotedString
        ValueTerm  = Attr or number
        Relation   = oneof "=", "==", "!="
```
`=` and `==` are the same, the equality relationship.

AND is written `&` or `&&`. 

OR is written `|` or `||`. 

Parentheses are allowed.

Precedence: 
`A&B|C&D` is equivalent to `(A & (B|C) & D)`.

(think of as: "&" is + (addition) and "|" is * (multiplication)).

### Special Attribute Expressions

An attribute expression of "!" means "deny" (i.e. evaluate to false)
and of "*" means "allow" (i.e. evaluates to true).
These elements can not be part of a larger expression.

### Attribute Values

Attribute values are written as "attribute = value".

An attribute on its own in a attribute value context is equivalent to "attribute = true"

Example attribute values: 
```
    classification = general
```

Grammar:
```
    AttributeValue = Attr ('=' ValueTerm)?
```

### Lists

An attribute expression list is a comma-separated list of attribute expressions. The empty string or a string of only white space is a list of zero elements. An attribute expression string canbe used as an attribute expression list with one element.

Example of an attribute expression list:
```
    classification = public , status != draft
```

which has two elements - `classification = public`  and `status != draft`.

Attribute expression list can not be nested.

Attribute value lists are used for the attributes that a user request is execute with.
The list is used as the "value set" in attribute expression evaluation.

In JSON, an attribute value list can also be a JSON array, with each member being a JSON string
which is an attribute value with the full range of characters.

## Evaluation

### Attribute Expression Evaluation 

Example:

Given a value set of "abc=true" , "def=published". This is the same as: "abc" , "def=published".

`abc` evaluates to true.

`xyz` evaluates to false - `xyz` is absent from the attribute value set

`abc || xyz` evaluates to true

`abc && xyz` evaluates to false

`*` evaluates to true.

`!` evaluates to false.

`def` evaluates to false because `def` has value "published", not `true`.

### Attribute Expression List Evaluation

The evaluation of an attribute expression list results in a list of values. In the context of making an access control decision, all the values must be `true` for the list to be consider to allow access.

## Transport

### Default Attribute Expressions

As a convenience, we allow attribute lists as the default labelling header for data
events as well as an attribute expression.

### Transport By Header

One way to apply labels is to place it in the header of the update request. Headers
are supported for Kafka events and for HTTP operations.

The header name is `Security-Label` and has an attribute expression list.

A list of labels is treated as an AND (that is, to access the data, the user making the request will need to have all the attributes).

A list of labels is treated as an AND (that is, to access the data, the user making
the request will need to have all the attributes).

```
Security-Label: attribute1 , attribute2="some value"
```

An expression can be used - it will need to be enclosed in `" "` (HTTP field rules)
and strings use `' '`.

```
Security-Label: "attribute1 && attribute2 = 'some value'"
```

### Transport In RDF

Data can be added to an RDF graph with an upload consisting of the triples data and
labels. The system uses [TriG format](https://www.w3.org/TR/trig) for a
collection of graphs for data upload. We use this format to separate the data to
be added to the dataset default graph from the labels that controls access to
the data. By using TriG we still have a single unit to load so data and labels
get added in a single transactional change to the data.

The labels go in the graph named `policy:labels` (full form
`http://ndtp.co.uk/security#labels`). Graphs in the names being
`http://ndtp.co.uk/security#` are reserved for PBAC information and can not be
inserted into the visible data.

Example data upload:

```
PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ​
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>​
PREFIX authz:   <http://ndtp.co.uk/security#>​
PREFIX :        <http://example/> ​
​
:person4321 rdf:type foaf:Person;​
    :phone "0400 111 222" ;​
    :phone "0400 111 333" ;​
    rdfs:label "Jones" ;​
    .​
​
GRAPH authz:labels {​
    [ authz:pattern ':person4321 :phone "0400 111 333"' ;  authz:label "*" ] .​
    [ authz:pattern ':person4321 :phone "0400 111 222"' ;  authz:label "employee" ] .​
    [ authz:pattern ':person4321 rdfs:label "Jones"' ;     authz:label "employee | contractor" ] .​
}​
```

Here, the triple `:person4321 :phone "0400 111 333` is visible to any user that
is permitted to use the query API.

The triple `:person4321 :phone "0400 111 222` is visible to any user with
attribute "employee".

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
