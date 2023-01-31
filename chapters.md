1. # Chapters

   1. ## Introduction

      1. Context
      2. Problem
      3. Objectives
      4. Contributions
      5. Document Organisation

   Minor changes to conform with contributions alteration (encrypted triplestore and two dynamic SSE protocols, with support for SPARQL & SPARQL Update queries and reasoning capabilities).

   1. ## Related Work

      1. Semantic Web: A layered architecture
         1. RDF: Resource Description Framework
            1. Overview
            2. Syntax
         2. RDFS: RDF Schema
            1. Overview
            2. RDFS primitives
         3. OWL2: Web Ontology Language
            1. Overview
            2. OWL Language
            3. OWL2 DL
         4. SPARQL
            1. Overview
            2. SPARQL Queries
            3. Entailment regimes
               1. Materialisation
               2. Query expansion
         5. Tools & Development Software for Ontologies

      2. Secure Computations
         1. Homomorphic Encryption (Add section about additive homomorphic encryption)
         2. Searchable Encryption
            1. Overview
            2. Searchable Encryption (SE) General Index-Based Model
            3. Client/Server Model
            4. Information Leakage
         3. Symmetric Searchable Encryption
            1. Overview
            2. Security Definitions
            3. Symmetric Searchable Encryption (SSE) Schemes (explain in more detail)
         4. Other approaches (explain in more detail the CryptDB approach)
      3. Summary

   2. ## Theoretical/System/Solution Model

      1. Security/Functional Requirements
      2. Adversary Model
      3. Solution
         1. System Model
            1.  Components...
         2. SSE Protocols (1. Regular approach using DET/RND encryption scheme; 2. Equality tags using the modified DGK cryptosystem), for each:
            1. Overview
            2. Formal definition of operations
            3. Leakage/Security proof

   3. ## Implementation

      1. For each component, specify concrete implementations (e.g technologies, libs used...)

   4. ## Experimental Evaluation & Results

      1. Evaluation/ Validation methodology
         1. Specify dataset & benchmark used (LUBM)
         2. Specify the system used for testing (e.g hardware specifications, deployment)
         3. Explain experiments (artillery scenarios)
            1. For each query, repeat with larger datasets (against a system using fuseki w/ inference model)
               1. For each dataset repeat a consistent number of times with each protocol/no protocol, with inference/ no inference w/ query expansion or materialisation: Evaluate % of correct results, % fully correct results, % wrong results, latency  
            2. For each protocol, evaluate storage expansion/ time to upload/ generate secrets & keys
      2. Results & Discussion

   5. ## Future work

      1. Discuss future work (e.g better query expansion strategies, missing operations, expansion to distributed model, adding concurrency/parallelism)

   6. ## Conclusion