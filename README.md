# SEnTri: Searchable Encrypted Triplestore

A searchable encrypted triplestore solution with (limited) support for SPARQL queries.

### Client API

```perl
GET    Client/api/ctrl/version                                        #Get service version.                    (Basic)

POST   Client/api/users/auth                                          #Authenticate user.                      (Basic)
POST   Client/api/users/                                              #Register user.                          (Basic)
DELETE Client/api/users/{username}                                    #Delete user.                            (Basic)
POST * Client/api/users/{username}/upgrade                            #Ask for privileged rights.              (Basic)
POST * Client/api/users/{username}/downgrade                          #Go back to basic rights.                (Privileged)
GET  * Client/api/users/{username}/upgrade/requests                   #List pending upgrade requests.          (Admin)
PUT  * Client/api/users/{username}/upgrade/requests/{requestID}       #Process upgrade request.                (Admin)

POST   Client/api/triplestores/{TriplestoreID}/access                                #Issue access request.         (Basic)
PUT    Client/api/triplestores/{TriplestoreID}/{issuer}/owner/{username}             #Change triplestore owner.     (Owner)
DELETE Client/api/triplestores/{TriplestoreID}/{issuer}/access/{username}            #Revoke access.                (Basic)
PUT    Client/api/triplestores/{TriplestoreID}/{issuer}/access/{username}            #Grant access.                 (Owner)
GET *  Client/api/triplestores/{TriplestoreID}/{issuer}/access/users                 #List users with access.       (Owner)
GET *  Client/api/triplestores/{TriplestoreID}/{issuer}/access/requests              #List pending access requests. (Owner)
PUT *  Client/api/triplestores/{TriplestoreID}/{issuer}/access/requests/{requestID}  #Process access request.       (Owner)

GET    Client/api/triplestores/{issuer}                               #List triplestores.                      (Basic)
POST   Client/api/triplestores                                        #Create triplestore.                     (Privileged)
POST   Client/api/triplestores/{TriplestoreID}                        #Upload data to triplestore.             (Write Access)
POST   Client/api/triplestore/query                                   #Execute a SPARQL query.                 (Read Access)
DELETE Client/api/triplestores/{TriplestoreID}/{username}             #Delete triplestore.                     (Owner)

POST   Client/api/triplestores/secure                                 #Create encrypted triplestore.           (Privileged)
POST   Client/api/triplestores/secure/{TriplestoreID}                 #Upload data to encrypted triplestore.   (Write Access)
POST   Client/api/triplestores/secure/query/                          #Execute a secure SPARQL query.          (Read Access)
DELETE Client/api/triplestores/secure/{TriplestoreID}/{username}      #Delete encrypted triplestore.           (Owner)
```
### IAM Provider API
```perl
GET    IAMProvider/api/ctrl/version                                             #Get service version.            (Basic)
          
POST   IAMProvider/api/users/auth                                               #Authenticate user.              (Basic)
POST   IAMProvider/api/users                                                    #Register user.                  (Basic)
DELETE IAMProvider/api/users/{username}                                         #Delete user.                    (Basic)
POST   IAMProvider/api/users/{username}/role/requests                           #Grant or issue role request.    (Admin)
GET    IAMProvider/api/users/{username}/role/requests                           #List pending role requests.     (Admin)
PUT    IAMProvider/api/users/{username}/role/requests/{requestID}               #Process pending role request.   (Admin)

POST   IAMProvider/api/triplestores                                             #Create triplestore.             (Privileged)
GET    IAMProvider/api/triplestores/{username}                                  #List triplestores.              (Basic)
DELETE IAMProvider/api/triplestores/{TriplestoreID}                             #Delete triplestore.             (Owner)
PUT    IAMProvider/api/triplestores/{TriplestoreID}/owner/{username}            #Change triplestore owner.       (Owner)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/users                #Get users with access.          (Owner)
PUT    IAMProvider/api/triplestores/{TriplestoreID}/access/{username}           #Grant triplestore access        (Owner)
DELETE IAMProvider/api/triplestores/{TriplestoreID}/access/{username}           #Revoke triplestore access.      (Owner)
POST   IAMProvider/api/triplestores/{TriplestoreID}/access/requests             #Issue triplestore access request. (Basic)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/requests             #List pending access requests.   (Owner)
PUT    IAMProvider/api/triplestores/{TriplestoreID}/access/requests/{requestID} #Process pending access request. (Owner)
POST   IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/{username}    #Create access token for user.   (Basic)
DELETE IAMProvider/api/triplestores/{TriplestoreID}/access/tokens               #Delete access token.            (Token owner)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/read          #Check if has read access        (Basic)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/write         #Check if has write access       (Basic)
GET    IAMProvider/api/triplestores/{TriplestoreID}/access/tokens/owner         #Check if has owner access       (Basic)
POST   IAMProvider/api/triplestores/{TriplestoreID}/access/locks                #Lock access to triplestore.    (Write access)
DELETE IAMProvider/api/triplestores/{TriplestoreID}/access/locks                #Unlock access to triplestore.  (Write access)
```
### Secrets' Vault API
```perl
GET    Vault/api/ctrl/version             #Get service version.            (Basic)

POST   Vault/api/secrets/{TriplestoreID}  #Create triplestore secrets.     (Owner)
GET    Vault/api/secrets/{TriplestoreID}  #Get triplestore secrets.        (Read Access)
DELETE Vault/api/secrets/{TriplestoreID}  #Delete triplestore secrets.     (Owner)
```
### Triplestore API
```perl
GET    Triplestore/api/ctrl/version                   #Get service version                            (Basic)

POST   Triplestore/api/{TriplestoreID}                #Upload data to triplestore.                    (Write Access)
DELETE Triplestore/api/{TriplestoreID}                #Delete triplestore.                            (Owner)
POST   Triplestore/api/{TriplestoreID}/delete         #Delete data from triplestore.                  (Write Access)
POST   Triplestore/api/query/{TriplestoreID}          #Execute a SPARQL query over triplestore.       (Read Access)

POST   Triplestore/api/secure/{TriplestoreID}         #Upload data to encrypted triplestore.          (Write Access)
POST   Triplestore/api/secure/{TriplestoreID}/delete  #Delete data from encrypted triplestore.        (Write Access)
POST   Triplestore/api/secure/{TriplestoreID}/search  #Search data from encrypted triplestore.        (Read Access)
DELETE Triplestore/api/secure/{TriplestoreID}         #Delete encrypted triplestore.                  (Owner)
```

### Proxy API
```perl
GET    Proxy/api/ctrl/version           #Get service version.                                    (Basic)

POST   Proxy/api/queries                #Execute a SPARQL query.                                 (Read Access)
POST   Proxy/api/queries/prepare        #Save auxiliary search values results associated.        (Read Access)
```