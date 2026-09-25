# RDF ABAC : User Attribute Store
**Repository:** `rdf-abac`  
**Description:** `Attribute Store and user permissions`    
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The attributes (permissions) for each user are held in a user attribute store.
This can be a local store, for when running RDF ABAC as a standalone service or
for a development system,
or a separate service that may be shared, and the ABAC engine needs a URL to
fetch the user attributes.

The attribute store also stores the attribute value hierarchies.

### Local Attribute Store

For testing and development, and for a standalone deployment, there is a simple,
file-based local attribute store. It is specified in the dataset description with
`authz:attributes`. It is an RDF file, with `authz:user` and `authz:userAttribute`
properties. Users have specific attributes, not attribute expressions.

```
    :dataset rdf:type authz:DatasetAuthz ;
        ...
        authz:attributes   <file:attirbute-store.ttl>
```

A local attribute store is loaded when the Fuseki server starts up and 
does not change while the server is running.

```
PREFIX authz: <http://ndtp.co.uk/security#>

[] authz:user "u1" ;
    authz:userAttribute "manager";
    authz:userAttribute "project-Y";
    .
    
[] authz:user "u2" ;
    authz:userAttribute "engineer";
    .

[] authz:user "u3" ;
    authz:userAttribute "unused";
    .
```

### Remote User Attributes Store

An attribute store may be remote. Attributes needed for the request are fetched.  
The [configuration for the dataset](abac-fuseki.md#dataset-configuration) needs to include the URL template.

```
    :dataset rdf:type authz:DatasetAuthz ;
        ...
        authz:attributesURL "https://someHost/user/{user}"
```

or the value may be the special form to read the URL template from an environment variable:

```
    :dataset rdf:type authz:DatasetAuthz ;
        ...
        authz:attributesURL <env:ENV_VAR>
```

The URL is used by replacing "{user}" with the username from the request.

For example: for user _Smith_ "https://someHost/user/{user}" becomes "https://someHost/users/Smith"

```
:dataset rdf:type authz:DatasetAuthz ;
    ## Attribute store
    authz:attributesURL <env:USER_ATTRIBUTE_STORE>;
```
where environment variable `USER_ATTRIBUTE_STORE` has the value
`https://host/user/{user}` is equivalent to:
```
:dataset rdf:type authz:DatasetAuthz ;
    ## Attribute store
    authz:attributesURL <https://host/user/{user}>;
    ...
```
except the URL is not hardcoded into the configuration file.


### Cached User Attributes Store
As the User & Hierarchy data are unlikely to change very often, we can improve the performance of the Remote Store calls, we can configure the 
attribute store by having some caching in place. Technically, you can apply caching to local attribute stores too but there would be little need outside of testing.

As you might expect we can configure how much data to cache and also for how long we hold on to that data.

For example, we might have very few Hierarchical entries that never change thus a small but long-lived cache would 
be appropriate. By contrast, we could have a huge numbers users whose details change in a regular manner leading to 
a larger but briefer cache.

```
:dataset rdf:type authz:DatasetAuthz ;
    ## Attribute store
    authz:attributesURL <env:USER_ATTRIBUTE_STORE>;
    ## Enable Caching
    authz:cache true ;
    ## User Attribute Cache Details
    authz:attributeCacheSize 10 ;
    authz:attributeCacheExpiryTime "PT1S"^^xsd:duration ;
    ## Hierarchy Cache Details
    authz:hierarchyCacheSize 1 ;
    authz:hierarchyCacheExpiryTime "PT1S"^^xsd:duration ;
    
```

© Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally attributed to the UK's Department for Business, Innovation, Science and Trade (BIST) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
