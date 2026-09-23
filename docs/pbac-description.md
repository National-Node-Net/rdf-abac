# RDF PBAC - Security Labelling of RDF Data
**Repository:** `rdf-pbac`  
**Description:** `Description for security labelling of RDF data`  
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->
## Data Access

Data access is checked on every data item read from the data storage. 
For an RDF database, data items are the RDF triples.
The access requirements are checked by a filter at the point where the data is
read from the database storage for the user request being processed. The
data available to the request is a view of the database, with only
accessible data being available to the 
[SPARQL query](https://www.w3.org/TR/sparql11-query/) or any other operations
being performed on the data.

Each user has a set of attribute-value pairs granting them access rights.
Each triple has an attribute expression that is evaluated using the request
attribute-values. If the expression evaluates to "true" access is granted, 
otherwise the triple is not passed to the query engine.

Suppose the user has two attributes "employee" and "project-X". This will match a
label of "employee", and a label of "employee | contractor" where `|` means
"or". It will not match "manager & project-X", where "&" means "and", because
the user does not have the "manager" attribute. 

ABAC, "Attribute Based Access Control" (the attribute-expression labelling that
PBAC builds on), lets data owners control the attributes
required at the granularity required. This is in-contrast to RBAC "Role Based
Access Control" where roles are typically managed by the IT organisation.
By labelling the data, the security requirements for the data can be
transferred as the data moves about the system. 

As data security becomes more complex, with new data sources being integrated
each with its own access rules, managing roles becomes impractical. 
RBAC (role-based access control) is often used for API endpoint access control.

A feature of databases is that the access path through the data is
not meaningful. A query may ask for all people with a certain name. That request
does not start at a root point in the data and traverse the data graph to find
the matching items.

In a database, access control on the API is not sufficient because
different parts of the data have different access policies and it is not
possible to determine from a query request which data will accessed.
API access control is still used to protect the service; data access control
provides the fine-grained access policies.

A feature of this module is that the data does not have to be designed with
security in mind, nor does the security place requirements on the data model.
Existing data can have fine-grain security labels added when the data is used for
new purposes with new users.

## Transporting Data and Labels

Data can be labelled in two ways. An update message may have a common labelling policy applied to all the data it contains using a request header. If more control is required, there is an RDF format for the distribution of labels.

## By Header

The header `Security-Label` is used to indicate the label that is to apply to all of a request.

```
    Security-Label: "employee || classification=sensitive"
```

This can be used in HTTP or Kafka messages.

## By RDF

Data:

```
    :person4321 :phone "0400 111 222" .​
```

Label description:

```
    [ authz:pattern ':person4321 :phone "0400 111 222"' ; 
      authz:label "employee | status=contractor" ] .
```

The `authz:pattern` identifies a specific triple. It is also possible to apply a label to triples using a wildcard:

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
