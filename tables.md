 **Client API** 

| **Method** | **Endpoint**                                                 | **Description**                                 | **Access Level** | **Algorithm**                       |
| ---------- | ------------------------------------------------------------ | ----------------------------------------------- | ---------------- | ----------------------------------- |
| GET        | /ctrl/version                                                | Get service version                             | Basic            | N/A                                 |
| POST       | /users/auth                                                  | Authenticate user                               | Basic            | `authenticateUser()`                |
| POST       | /users/                                                      | Register user                                   | Basic            | `registerUser()`                    |
| DELETE     | /users/{username}                                            | Delete user                                     | Basic            | `deleteUser()`                      |
| POST       | /users/{username}/upgrade                                    | Request privileged rights                       | Basic            | `requestUpgrade()`                  |
| POST       | /users/{username}/downgrade                                  | Revoke privileged rights                        | Privileged       | `revokeUpgrade()`                   |
| GET        | /users/{username}/requests                                   | List pending upgrade requests                   | Admin            | `listUpgradeRequests()`             |
| PUT        | /users/{username}/requests/{requestID}                       | Process upgrade request                         | Admin            | `processUpgradeRequest()`           |
| PUT        | /triplestores/{triplestoreID}/{issuer}/owner/{target}        | Change triplestore owner                        | Owner            | `changeOwner()`                     |
| DELETE     | /triplestores/{triplestoreID}/{issuer}/access/{target}       | Revoke access                                   | Basic            | `revokeAccess()`                    |
| PUT        | /triplestores/{triplestoreID}/{issuer}/access/{target}       | Grant access                                    | Owner            | `grantAccess()`                     |
| GET        | /triplestores/{triplestoreID}/{issuer}/access/users          | List users with access                          | Owner            | `listAccessUsers()`                 |
| POST       | /triplestores/{triplestoreID}/{issuer}/access/requests       | Issue access request                            | Basic            | `issueAccessRequest()`              |
| GET        | /triplestores/{triplestoreID}/{issuer}/access/requests       | List pending access requests                    | Owner            | `listAccessRequests()`              |
| PUT        | /triplestores/{triplestoreID}/{issuer}/access/requests/{requestID} | Process access request                          | Owner            | `processAccessRequest()`            |
| GET        | /triplestores/{issuer}                                       | List triplestores                               | Basic            | `listTriplestores()`                |
| GET        | /triplestores/{triplestoreID}/{issuer}                       | Fetch triplestore info                          | Read Access      | `fetchTriplestoreInfo()`            |
| DELETE     | /triplestores/{triplestoreID}/{issuer}                       | Delete triplestore                              | Owner            | `deleteTriplestore()`               |
| POST       | /triplestores/encrypted/{version}                            | Create encrypted triplestore                    | Privileged       | `createEncryptedTriplestore()`      |
| POST       | /triplestores/encrypted/{version}/upload                     | Upload data to encrypted triplestore            | Write Access     | `uploadToEncryptedTriplestore()`    |
| POST       | /triplestores/encrypted/{version}/query                      | Execute SPARQL query over encrypted triplestore | Read Access      | `executeEncryptedSPARQLQuery()`     |
| POST       | /triplestores/encrypted/{version}/schema                     | Fetch encrypted triplestore schema              | Read Access      | `fetchEncryptedTriplestoreSchema()` |
| DELETE     | /triplestores/encrypted/{version}/{triplestoreID}/{issuer}   | Delete encrypted triplestore                    | Owner            | `deleteEncryptedTriplestore()`      |


### Client API (testing endpoints)

| **Method** | **Endpoint**         | **Description**                      |
| ---------- | -------------------- | ------------------------------------ |
| POST       | /triplestores        | Create a plaintext triplestore       |
| POST       | /triplestores/upload | Upload plaintext data to triplestore |
| POST       | /triplestore/query   | Execute a plaintext SPARQL query     |
| POST       | /triplestore/schema  | Fetch plaintext triplestore schema   |

### **IAM Provider API**

| **Method** | **Endpoint**                                              | **Description**                              | **Access Level** | **Algorithm**               |
| ---------- | --------------------------------------------------------- | -------------------------------------------- | ---------------- | --------------------------- |
| GET        | /ctrl/version                                             | Get service version                          | Basic            | N/A                         |
| POST       | /users/auth                                               | Authenticate user                            | Basic            | `authenticateUser()`        |
| POST       | /users                                                    | Register user                                | Basic            | `registerUser()`            |
| DELETE     | /users/{issuer}                                           | Delete user                                  | Basic            | `deleteUser()`              |
| POST       | /users/{username}/role/requests                           | Grant or issue role request                  | Admin            | `issueRoleRequest()`        |
| GET        | /users/{username}/role/requests                           | List pending role requests                   | Admin            | `listRoleRequests()`        |
| PUT        | /users/{username}/role/requests/{requestID}               | Process pending role request                 | Admin            | `processRoleRequest()`      |
| GET        | /users/tokens/active                                      | Check if access token belongs to active user | Basic            | `checkActiveToken()`        |
| POST       | /triplestores                                             | Create triplestore                           | Privileged       | `createTriplestore()`       |
| GET        | /triplestores/{issuer}                                    | List triplestores                            | Basic            | `listTriplestores()`        |
| DELETE     | /triplestores/{triplestoreID}                             | Delete triplestore                           | Owner or Admin   | `deleteTriplestore()`       |
| PUT        | /triplestores/{triplestoreID}/owner/{target}              | Change triplestore owner                     | Owner            | `changeOwner()`             |
| GET        | /triplestores/{triplestoreID}/access/users                | Get users with access                        | Owner            | `getAccessUsers()`          |
| PUT        | /triplestores/{triplestoreID}/access/{target}             | Grant triplestore access                     | Owner            | `grantTriplestoreAccess()`  |
| DELETE     | /triplestores/{triplestoreID}/access/{target}             | Revoke triplestore access                    | Owner            | `revokeTriplestoreAccess()` |
| POST       | /triplestores/{triplestoreID}/access/requests/{target}    | Issue triplestore access request             | Basic            | `issueAccessRequest()`      |
| GET        | /triplestores/{triplestoreID}/access/requests             | List pending access requests                 | Owner            | `listAccessRequests()`      |
| PUT        | /triplestores/{triplestoreID}/access/requests/{requestID} | Process pending access request               | Owner            | `processAccessRequest()`    |
| POST       | /triplestores/{triplestoreID}/access/tokens/{target}      | Create access token for user                 | Basic            | `createAccessToken()`       |
| DELETE     | /triplestores/{triplestoreID}/access/tokens               | Delete access token                          | Token owner      | `deleteAccessToken()`       |
| GET        | /triplestores/{triplestoreID}/access/tokens/read          | Check if has read access                     | Basic            | `checkReadAccess()`         |
| GET        | /triplestores/{triplestoreID}/access/tokens/write         | Check if has write access                    | Basic            | `checkWriteAccess()`        |
| GET        | /triplestores/{triplestoreID}/access/tokens/owner         | Check if has owner access                    | Basic            | `checkOwnerAccess()`        |
| POST       | /triplestores/{triplestoreID}/access/locks                | Lock access to triplestore                   | Write Access     | `lockAccess()`              |
| DELETE     | /triplestores/{triplestoreID}/access/locks                | Unlock access to triplestore                 | Write Access     | `unlockAccess()`            |

### **Secrets'

 Vault API**

| **Method** | **Endpoint**                                          | **Description**                        | **Access Level** | **Algorithm**           |
|------------|-------------------------------------------------------|----------------------------------------|------------------|--------------------------|
| GET        | /ctrl/version                                | Get service version                    | Basic            | N/A                      |
| POST       | /secrets/{triplestoreID}                     | Create triplestore secrets             | Owner            | `createTriplestoreSecrets()` |
| GET        | /secrets/{triplestoreID}                     | Get triplestore secrets                | Read Access      | `getTriplestoreSecrets()` |
| DELETE     | /secrets/{triplestoreID}                     | Delete triplestore secrets             | Owner            | `deleteTriplestoreSecrets()` |

### **Triplestore API**

| **Method** | **Endpoint**                                      | **Description**                        | **Access Level** | **Algorithm**         |
| ---------- | ------------------------------------------------- | -------------------------------------- | ---------------- | --------------------- |
| GET        | /ctrl/version                                     | Get service version                    | Basic            | N/A                   |
| DELETE     | /{triplestoreID}                                  | Delete triplestore                     | Owner            | `deleteTriplestore()` |
| POST       | /encrypted/{version}/{triplestoreID}              | Upload data to encrypted triplestore   | Write Access     | `prepareUpload()`     |
| POST       | /encrypted/{version}/{triplestoreID}/delete       | Delete data from encrypted triplestore | Write Access     | `prepareDelete()`     |
| POST       | /encrypted/{version}/{triplestoreID}/update       | Update data from encrypted triplestore | Write Access     | `update()`            |
| POST       | /encrypted/{version}/{triplestoreID}/search       | Search data from encrypted triplestore | Read Access      | `search()`            |
| POST       | /encrypted/{version}/proxy/{triplestoreID}/search | Prepare auxiliary search for SPARQL    | Basic            | `prepareSearch()`     |
### **Triplestore API (testing endpoints)**

| **Method** | **Endpoint**            | **Description**                        |
| ---------- | ----------------------- | -------------------------------------- |
| POST       | /{triplestoreID}        | Upload plaintext data to triplestore   |
| DELETE     | /{triplestoreID}        | Delete triplestore                     |
| POST       | /{triplestoreID}/delete | Delete plaintext data from triplestore |
| POST       | /{triplestoreID}/query  | Execute plaintext SPARQL query         |
| GET        | /{triplestoreID}/schema | Fetch triplestore schema               |
| GET        | /{triplestoreID}        | Fetch triplestore metrics (e.g. size)  |

### Proxy API

| **Method** | **Endpoint**     | **Description**                                        | **Access Level** | **Algorithm** |
| ---------- | ---------------- | ------------------------------------------------------ | ---------------- | ------------- |
| GET        | /ctrl/version    | Get service version                                    | Basic            |               |
| POST       | /queries         | Execute a SPARQl query                                 | Read Access      |               |
| POST       | /queries/prepare | Save auxiliary search results associated with a  query | Read Access      |               |

