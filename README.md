# SEnTriS: Searchable Encrypted Triplestore Sharing Protocol

A searchable encrypted triplestore sharing solution with (limited) support for SPARQL query evaluation.

### Client API

```perl
GET    Client/api/ctrl/version                                        #Get service version.                    (Basic)

POST   Client/api/users/auth                                          #Authenticate user.                      (Basic)
POST   Client/api/users/                                              #Register user.                          (Basic)
DELETE Client/api/users/{username}                                    #Delete user.                            (Basic)
POST   Client/api/users/{username}/upgrade                            #Ask for privileged rights.              (Basic)
POST   Client/api/users/{username}/downgrade                          #Go back to basic rights.                (Privileged)
GET    Client/api/users/{username}/requests                           #List pending upgrade requests.          (Admin)
PUT    Client/api/users/{username}/requests/{requestID}               #Process upgrade request.                (Admin)

PUT    Client/api/triplestores/{TriplestoreID}/{issuer}/owner/{target}               #Change triplestore owner.     (Owner)
DELETE Client/api/triplestores/{TriplestoreID}/{issuer}/access/{target}              #Revoke access.                (Basic)
PUT    Client/api/triplestores/{TriplestoreID}/{issuer}/access/{target}              #Grant access.                 (Owner)
GET    Client/api/triplestores/{TriplestoreID}/{issuer}/access/users                 #List users with access.       (Owner)
POST   Client/api/triplestores/{TriplestoreID}/{issuer}/access/requests              #Issue access request.         (Basic)
GET    Client/api/triplestores/{TriplestoreID}/{issuer}/access/requests              #List pending access requests. (Owner)
PUT    Client/api/triplestores/{TriplestoreID}/{issuer}/access/requests/{requestID}  #Process access request.       (Owner)

GET    Client/api/triplestores/{issuer}                               #List triplestores.                      (Basic)
GET    Client/api/triplestores/{TriplestoreID}/{issuer}               #Fecth triplestore info.                 (Read Access)
POST   Client/api/triplestores                                        #Create triplestore.                     (Privileged)
POST   Client/api/triplestores/upload                                 #Upload data to triplestore.             (Write Access)
POST   Client/api/triplestore/query                                   #Execute a SPARQL query.                 (Read Access)
POST   Client/api/triplestore/schema                                  #Fetch triplestore schema.               (Read Access)
DELETE Client/api/triplestores/{TriplestoreID}/{issuer}               #Delete triplestore.                     (Owner)


POST   Client/api/triplestores/encrypted/{version}                    #Create encrypted triplestore.           (Privileged)
POST   Client/api/triplestores/encrypted/{version}/upload             #Upload data to encrypted triplestore.   (Write Access)
POST   Client/api/triplestores/encrypted/{version}/query  #Exec. a SPARQL query over an enc. triplestore.      (Read Access)
POST   Client/api/triplestore/encrypted/{version}/schema              #Fetch triplestore schema.               (Read Access)
DELETE Client/api/triplestores/encrypted/{version}/{TriplestoreID}/{issuer} #Delete encrypted triplestore.     (Owner)
```
### IAM Provider API
```perl
GET    IAMProvider/api/ctrl/version                                             #Get service version.            (Basic)
          
POST   IAMProvider/api/users/auth                                               #Authenticate user.              (Basic)
POST   IAMProvider/api/users                                                    #Register user.                  (Basic)
DELETE IAMProvider/api/users/{issuer}                                           #Delete user.                    (Basic)
POST   IAMProvider/api/users/{username}/role/requests                           #Grant or issue role request.    (Admin)
GET    IAMProvider/api/users/{username}/role/requests                           #List pending role requests.     (Admin)
PUT    IAMProvider/api/users/{username}/role/requests/{requestID}               #Process pending role request.   (Admin)
GET    IAMProvider/api/users/tokens/active                         #Check if access token belongs to active user (Basic)

POST   IAMProvider/api/triplestores                                             #Create triplestore.             (Privileged)
GET    IAMProvider/api/triplestores/{issuer}                                    #List triplestores.              (Basic)
DELETE IAMProvider/api/triplestores/{TriplestoreID}                             #Delete triplestore.          (Owner or Admin)
PUT    IAMProvider/api/triplestores/{TriplestoreID}/owner/{target}              #Change triplestore owner.       (Owner)

GET    IAMProvider/api/triplestores/{TriplestoreID}/access/users                #Get users with access.          (Owner)
PUT    IAMProvider/api/triplestores/{TriplestoreID}/access/{target}             #Grant triplestore access        (Owner)
DELETE IAMProvider/api/triplestores/{TriplestoreID}/access/{target}             #Revoke triplestore access.      (Owner)
POST   IAMProvider/api/triplestores/{TriplestoreID}/access/requests/{target}    #Issue triplestore access request. (Basic)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/requests             #List pending access requests.   (Owner)
PUT    IAMProvider/api/triplestores/{TriplestoreID}/access/requests/{requestID} #Process pending access request. (Owner)

POST   IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/{target}      #Create access token for user.   (Basic)
DELETE IAMProvider/api/triplestores/{TriplestoreID}/access/tokens               #Delete access token.            (Token owner)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/read          #Check if has read access        (Basic)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/write         #Check if has write access       (Basic)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/owner         #Check if has owner access       (Basic)

POST   IAMProvider/api/triplestores/{TriplestoreID}/access/locks                #Lock access to triplestore.    (Write access)
DELETE IAMProvider/api/triplestores/{TriplestoreID}/access/locks                #Unlock access to triplestore.  (Write access)
```
### Secret Vault API
```perl
GET    Vault/api/ctrl/version             #Get service version.            (Basic)

POST   Vault/api/secrets/{TriplestoreID}  #Create triplestore secrets.     (Owner)
GET    Vault/api/secrets/{TriplestoreID}  #Get triplestore secrets.        (Read Access)
DELETE Vault/api/secrets/{TriplestoreID}  #Delete triplestore secrets.     (Owner)
```
### Triplestore API
```perl
GET    Triplestore/api/ctrl/version                      #Get service version                            (Basic)

POST   Triplestore/api/{TriplestoreID}                   #Upload data to triplestore.                    (Write Access)
DELETE Triplestore/api/{TriplestoreID}                   #Delete triplestore.                            (Owner)
POST   Triplestore/api/{TriplestoreID}/delete            #Delete data from triplestore.                  (Write Access)
POST   Triplestore/api/{TriplestoreID}/query             #Execute a SPARQL query over triplestore.       (Read Access)
GET    Triplestore/api/{TriplestoreID}                   #Fecth triplestore info.                        (Read Access)
GET    Triplestore/api/{TriplestoreID}/schema            #Fetch triplestore schema.                      (Read Access)


POST   Triplestore/api/encrypted/{TriplestoreID}         #Upload data to encrypted triplestore.          (Write Access)
POST   Triplestore/api/encrypted/{TriplestoreID}/delete  #Delete data from encrypted triplestore.        (Write Access)
POST   Triplestore/api/encrypted/{TriplestoreID}/swap    #Swap data from source trapdoors to target trapdoors. (Write Access)
POST   Triplestore/api/encrypted/{TriplestoreID}/search  #Search data from encrypted triplestore.        (Read Access)
POST   Triplestore/api/encrypted/proxy/{TriplestoreID}/search  #Prepare auxiliary search for SPARQL query. (Read Access)
DELETE Triplestore/api/encrypted/{TriplestoreID}         #Delete encrypted triplestore.                  (Owner)
```

### Proxy API
```perl
GET    Proxy/api/ctrl/version           #Get service version.                                         (Basic)

POST   Proxy/api/queries                #Execute a SPARQL query.                                      (Read Access)
POST   Proxy/api/queries/prepare        #Save auxiliary search results associated w/ a query.         (Read Access)
```
