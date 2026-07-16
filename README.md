# README  

**Repository:** `rdf-abac`  
**Description:** `This repository provides an Apache Jena plugin to enable fine-grained access control (ABAC). Used by secure-agent-graph.`

<!-- SPDX-License-Identifier: Apache-2.0 AND OGL-UK-3.0 -->

## Overview  
This repository contributes to the development of **secure, scalable, and interoperable data-sharing infrastructure**. It supports NDTP’s mission to enable **trusted, federated, and decentralised** data-sharing across organisations.  

This repository is one of several open-source components that underpin NDTP’s **Integration Architecture (IA)**—a framework designed to allow organisations to manage and exchange data securely while maintaining control over their own information. The IA is actively deployed and tested across multiple sectors, ensuring its adaptability and alignment with real-world needs. 

For a complete overview of the Integration Architecture (IA) project, please see the [Integration Architecture Documentation](https://github.com/National-Node-Net/integration-architecture-documentation).

## Prerequisites  
Before using this repository, ensure you have the following dependencies installed:  
- **Required Tooling:** 
    - Java 17
- **Pipeline Requirements:** 
    - Cloud platform credentials
- **Supported Kubernetes Versions:** N/A
- **System Requirements:** 
    - Java 17

## Quick Start  
Follow these steps to get started quickly with this repository. For detailed installation, configuration, and deployment, refer to the relevant MD files.  

### 1. Download and Build  
```sh  
git clone https://github.com/National-Node-Net/rdf-abac.git
cd rdf-abac
```
### 2. Run Build  
```sh  
mvn clean install
```

which creates the `rdf-abac-fmod` module for Fuseki.
### 3. Usage

To use the library directly in your project:

```xml

    <dependency>
      <groupId>uk.gov.dbt.ndtp.jena</groupId>
      <artifactId>rdf-abac-lib</artifactId>
      <version>VERSION</version>
    </dependency>

```
#### Use with Apache Jena Fuseki
This project uses the Apache Jena Fuseki Main server and is configured with a
Fuseki configuration file.

See [documentation](docs/abac-fuseki-server.md) for details on how to run the library within a local Fuseki Server.

See "[Configuring Fuseki](https://jena.apache.org/documentation/fuseki2/fuseki-configuration.html)"
for authentication.

## Features
- **Key Functionality**  
   - RDF ABAC consists of a ABAC security engine, an extension module for [Apache Jena Fuseki](https://jena.apache.org/documentation/fuseki2/),
and a security evaluation service to provide security verification
to non-JVM components of the system.
- **Key Integrations**  
    - Enhances access control for the [secure-agent-graph](https://github.com/National-Node-Net/secure-agent-graph) project.  
- **Scalability & Performance**  
    - Optimised for high-throughput environments.
- **Modularity**  
    - Designed as a modular component of NDTP’s Integration Architecture (IA).  
    - Includes the `rdf-abac-fmod` module for Fuseki.  
    - Usable as a library in other projects via Maven dependency management.  

## ABAC Documentation  
Documentation providing further details on the individual modules is provided [here](docs/abac.md ). 

## Testing Guide

### Running Unit Tests
Navigate to the root of the project and run `mvn test` to run the tests for the repository.

## Public Funding Acknowledgment  
This repository has been developed with public funding as part of the National Digital Twin Programme (NDTP), a UK Government initiative. NDTP, alongside its partners, has invested in this work to advance open, secure, and reusable digital twin technologies for any organisation, whether from the public or private sector, irrespective of size.  

## License  
This repository contains both source code and documentation, which are covered by different licenses:  
- **Code:** Originally developed by Telicent UK Ltd, now maintained by National Digital Twin Programme. Licensed under the [Apache License 2.0](LICENSE.md).  
- **Documentation:** Licensed under the [Open Government Licence (OGL) v3.0](OGL_LICENSE.md).  

By contributing to this repository, you agree that your contributions will be licensed under these terms.

See [LICENSE.md](LICENSE.md), [OGL_LICENSE.md](OGL_LICENSE.md), and [NOTICE.md](NOTICE.md) for details.  

## Security and Responsible Disclosure  
We take security seriously. If you believe you have found a security vulnerability in this repository, please follow our responsible disclosure process outlined in [SECURITY.md](SECURITY.md).  

## Contributing  
We welcome contributions that align with the Programme’s objectives. Please read our [Contributing](CONTRIBUTING.md) guidelines before submitting pull requests.  

## Acknowledgements  
This repository has benefited from collaboration with various organisations. For a list of acknowledgments, see [ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md).  

## Support and Contact  
For questions or support, check our Issues or contact the NDTP team on ndtp@businessandtrade.gov.uk.

**Maintained by the National Digital Twin Programme (NDTP).**  

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
