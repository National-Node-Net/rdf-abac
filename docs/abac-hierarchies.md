# RDF-ABAC : Attribute Value Hierarchies
**Repository:** `rdf-abac`  
**Description:** `Attribute value permissions for users`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

Attribute Value Hierarchies provide a way to grant a user an attribute value permission
that has the idea that it also grants lesser permission.

Hierarchies are defined and managed by the [User Attribute Store](abac-user-attribute-store.md).

Suppose we have the hierarchy for the attribute `status`:

```
[] authz:hierarchy [ authz:attribute "status" ;
                     authz:attributeValues "public, confidential, sensitive, private" ];
```
The list is written in least-most restrictive order.
"public" is the least restrictive, "private" the most restrictive.


If the data triple has the label
```
    status=public
```

then a user with attribute value `status=confidental` can see that data triple.

© Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally attributed to the UK's Department for Business, Innovation, Science and Trade (BIST) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
