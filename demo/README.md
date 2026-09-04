# PIP, label store demo

A Policy Information Point serving user attributes over HTTP, per-triple filtering by label, and the label vocabulary the PBAC work exposes.

Runs on `rdf-pbac-fuseki-server`, the standalone server built from this repo, not Secure
Agent Graph due to setup constraints. The filtering path is the same code either way (`FMod_ABAC`, `ABAC_Request`,
`SecurityFilterByLabel`, `AttributesStoreRemote`), which SAG consumes as a dependency.

No OPA yet, the decision is still the in-process evaluator. This shows the inputs a policy engine will need and the behaviour it must preserve.

## Setup

```sh
JAR=../rdf-abac-fuseki-server/target/rdf-pbac-fuseki-server-0.91.0-SNAPSHOT.jar
```

To rebuild, from the repo root:
`mvn --settings ../.maven/settings.xml install -Dgpg.skip=true -DskipTests`

## PIP

```sh
java -cp "$JAR" uk.gov.dbt.ndtp.jena.abac.services.SimpleAttributesStore attribute-store.ttl 8081
curl -s localhost:8081/users/lookup/alice
```

→ `{"attributes": ["clearance=secret", "employee"]}`

## Server

```sh
export USER_ATTRIBUTES_URL="http://localhost:8081/users/lookup/{user}"
export ABAC_HIERARCHIES_URL="http://localhost:8081/hierarchies/lookup/{name}"
java -jar "$JAR" --port=3030 --conf=config-demo.ttl
```

startup logs: `Label vocab : [clearance=ordinary, clearance=secret, employee]`

`employee` appears once although it is both a stored label and the dataset default — the vocabulary is
the union of the two.

## Query

```sh
Q='SELECT * WHERE { ?s ?p ?o }'
for u in alice bob carol; do
  echo -n "$u: "
  curl -s -G http://localhost:3030/ds/query --data-urlencode "query=$Q" \
    -H "Authorization: Bearer $(printf "user:$u" | base64)" \
    -H "Accept: application/sparql-results+json" \
  | python3 -c "import json,sys; print(len(json.load(sys.stdin)['results']['bindings']))"
done
```

## Expected

| Step | Result |
|---|---|
| Query all three users | alice **4**, bob **3**, carol **0** |
| Restart step 2 with `config-demo-no-hierarchies.ttl` | alice **3**, bob 3 |
| `pkill -f SimpleAttributesStore`, query again | **403** |

Alice drops 4 to 3 without hierarchies, because `clearance=secret` stops implying `clearance=ordinary` Related to SAG-04, which aims to preserve hierarchy.

Note: Permission denial will likely be 403 instead of 503 right now, subject to change.

## Files

| | |
|---|---|
| `attribute-store.ttl` | Users and the `clearance` hierarchy. Served by PIP. |
| `labels.ttl` | Triple labels, seeded at startup so the vocabulary is known immediately. |
| `data.ttl` | Triples data. |
| `config-demo.ttl` | File-seeded labels, HTTP PIP, hierarchies on. |
| `config-demo-no-hierarchies.ttl` | Same, but no hierarchies. |
