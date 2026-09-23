# RDF PBAC : Policy Engine (OPA)
**Repository:** `rdf-pbac`  
**Description:** `Policy-based label filtering using an external policy decision point`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

RDF PBAC decides which labelled triples a request may see by asking an external
policy decision point, [Open Policy Agent](https://www.openpolicyagent.org/) (OPA).
The PBAC engine acts as the policy enforcement point: it gathers the request
context, asks OPA for the set of permitted labels, and filters the dataset so only
triples carrying a permitted label are visible to the query.

When the policy engine is not enabled, labels are evaluated locally as
[attribute expressions](pbac-specification.md) instead.

* [Request flow](#request-flow)
* [Configuration](#configuration)
* [OPA request and response](#opa-request-and-response)
* [Failure behaviour](#failure-behaviour)
* [Extension point](#extension-point)

## Request flow

For each query or Graph Store Protocol read on a PBAC dataset:

1. The request user is identified (the `email` claim of the bearer JWT) and their
   attributes are looked up in the [user attribute store](pbac-user-attribute-store.md).
2. The dataset-level `authz:accessAttributes`, if set, are checked.
3. The label vocabulary of the dataset is computed: every distinct label held in
   the label store, plus the dataset default label.
4. OPA is called with the subject, action, organisation, dataset name, vocabulary
   and the subject's raw (not hierarchy-expanded) attributes.
5. OPA returns the permitted labels. Any label not in the submitted vocabulary is
   discarded, so a label OPA was not asked about is never trusted.
6. The request sees a filtered view of the dataset containing only triples whose
   label is in the permitted set.

The decision is cached for the duration of the request, so it is made once per
request rather than once per triple.

## Configuration

The Fuseki module reads the following environment variables at start-up:

| Variable | Default | Purpose |
|---|---|---|
| `POLICY_ENGINE_ENABLED` | `false` | Set to `true` to use OPA for label decisions. When `false`, datasets use local attribute label evaluation. |
| `OPA_BASE_URI` | `http://localhost:8181` | Base URI of the OPA server. |
| `OPA_POLICY_PATH` | `sag/test` | Path of the policy decision under the OPA Data API, i.e. requests go to `<OPA_BASE_URI>/v1/data/<OPA_POLICY_PATH>`. |

Connect and read timeouts are 2 seconds. After 5 consecutive failures the
circuit breaker opens for 30 seconds, during which requests fail fast without
calling OPA.

## OPA request and response

The request body uses the standard OPA Data API `input` envelope:

```json
{
  "input": {
    "subject_id": "user@example.org",
    "action": "query",
    "organisation_id": "org-1",
    "dataset_name": "/ds",
    "vocabulary": ["employee", "manager"],
    "subject_attributes": { "permitted_organisations": "org-1" }
  }
}
```

`organisation_id` is taken from the subject's `permitted_organisations` attribute
and is `null` if the subject has none.

The policy result must contain an explicit `allow` boolean and, when `allow` is
`true`, the list of permitted labels:

```json
{
  "result": {
    "allow": true,
    "permitted_labels": ["employee"]
  }
}
```

The field names and policy path are provisional and may change once the policy
contract is finalised.

## Failure behaviour

The policy engine fails closed:

| Situation | HTTP status |
|---|---|
| OPA denies access or permits no labels, or the dataset has no labels to ask about | `403 Forbidden` |
| OPA is unreachable, times out, returns an error or an invalid response, or the circuit breaker is open | `503 Service Unavailable` |

## Extension point

Label filtering is pluggable through `DatasetFilterProvider`
(`uk.gov.dbt.ndtp.jena.pbac.lib`):

* `DefaultDatasetFilterProvider` - local attribute label evaluation.
* `OpaDatasetFilterProvider` - policy decisions from a `DecisionServiceProvider`.

A provider can be registered per dataset or globally through `PBACRequest`. The
per-dataset provider takes priority over the global one.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
