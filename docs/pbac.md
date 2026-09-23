# Policy-Based, Triple-level security.
**Repository:** `rdf-pbac`  
**Description:** `Overview for policy-based security labelling of RDF data`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

* Overview (this document)
* [Description](pbac-description.md)
* [Policy Engine (OPA)](pbac-policy-engine.md)
* [Specification: RDF-PBAC attribute and label syntax, transport and evaluation](pbac-specification.md)
* [User Attribute Store](pbac-user-attribute-store.md)
* [Attribute Value Hierarchies](pbac-hierarchies.md)
* [Fuseki PBAC Module](pbac-fuseki.md)
* [Attribute Label Evaluation service (ALE)](pbac-label-eval-service.md)

This component module provides policy-based security labelling of RDF data.
Each data triple has an associated security label; the label
gives a condition for access to be granted and is tested
during every request.

Access requests have a set of attribute-values
that are the access rights of the user or software making the request.
We usually just refer to "the user".  This builds on strong user
authentication and a secure user attribute rights service.

By filtering for security at the storage level of the system, 
we ensure all data is tested for visibility,
whether accessed by query or retrieved for processing elsewhere.

The component consists of a PBAC security engine,
an extension module for [Apache Jena Fuseki](https://jena.apache.org/documentation/fuseki2/),
and a security evaluation service to provide security verification
to non-JVM components of the system.

### Policy-Based Access Control (PBAC)

Which labels a request may see is decided per request by a pluggable
`DatasetFilterProvider`:

* **Policy engine (OPA).** When enabled, the request's subject, action,
  organisation and dataset, together with the set of labels used in the dataset,
  are sent to an external policy decision point
  ([Open Policy Agent](https://www.openpolicyagent.org/)). The policy returns the
  labels the request is permitted to see, and only triples carrying those labels
  are visible. See [Policy Engine (OPA)](pbac-policy-engine.md).
* **Attribute label evaluation (default).** When no policy engine is configured,
  each label is evaluated locally as an attribute expression against the
  request's attributes, as described below.

### Attribute label expressions

Security requirements for access to an item data are expressed with an
attribute expression.

A simple example is:
```
    employee
```
which requires the request to have `employee` attribute.

More complex expressions are possible: 
```
    employee | ( contractor & nationality=UK )
```
which requires that the request
has the `employee` attribute or the attribute `contractor` together with 
the `nationality` attribute with value "UK".

Hierarchies of classifications are supported. Suppose a hierarchy of 
"public" (least restrictive), "confidential", "sensitive", and 
"private" (most restrictive) is defined for attribute `classification`.
If a request carries  "classification=sensitive" then the request has access to
data labelled "confidential" and "public" as well, and would match for data
condition of `classification = confidental`.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
