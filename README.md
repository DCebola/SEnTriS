# SEnTri: Searchable Encrypted Triplestore

A searchable encrypted triplestore solution with (limited) support for SPARQL queries.

## **Endpoints**

### Client

```perl
GET  /Client/api/ctrl/version                        #Get service version.

POST /Client/api/secure-triplestores/                #Create encrypted triplestore.
POST /Client/api/secure-triplestores/{storeID}       #Upload data to encrypted triplestore.
POST /Client/api/secure-triplestores/query/{storeID} #Execute SPARQL query over encrypted triplestore.

POST /Client/api/triplestores/                       #Create triplestore.
POST /Client/api/triplestores/{storeID}              #Upload data to triplestore.
POST /Client/api/triplestore/query/{storeID}         #Execute SPARQL query.
```
### IAM Provider
```perl
GET    IAMProvider/api/ctrl/version                                 #Get service version.                (Any)
          
POST   IAMProvider/api/users/auth                                   #Authenticate user.                  (Any)
POST   IAMProvider/api/users                                        #Register user.                      (Any)
DELETE IAMProvider/api/users/{username}                             #Delete user.                        (Any)
POST   IAMProvider/api/users/{username}/role/requests               #Grant or issue role request.        (Admin)
GET    IAMProvider/api/users/{username}/role/requests               #List pending role requests.         (Admin)
POST   IAMProvider/api/users/{username}/role/requests/{requestID}   #Process pending role request.       (Admin)


POST   IAMProvider/api/stores                                       #Create store.                       (Admin/Priviledged)
GET    IAMProvider/api/stores/{username}                            #List stores                         (Any)
PUT    IAMProvider/api/stores/{username}                            #Change store owner.                 (Admin/Store Owner)
DELETE IAMProvider/api/stores/{storeID}                             #Delete store.                       (Admin/Store Owner)
POST   IAMProvider/api/stores/{storeID}/access/{username}           #Grant store access                  (Admin/Store Owner)
DELETE IAMProvider/api/stores/{storeID}/access/{username}           #Revoke store access.                (Admin/Store Owner)
POST   IAMProvider/api/stores/{storeID}/access/requests             #Issue store access request          (Basic/Priviledged)
GET    IAMProvider/api/stores/{storeID}/access/requests             #List pending access requests.       (Admin/Store Owner)
POST   IAMProvider/api/stores/{storeID}/access/requests/{requestID} #Process pending access request.     (Admin/Store Owner)
GET    IAMProvider/api/stores/{storeID}/access/tokens/{username}    #Create access token for user.       (Any)
DELETE IAMProvider/api/stores/{storeID}/access/tokens               #Delete access token.                (Admin/Token owner)
GET    IAMProvider/api/stores/{storeID}/access/tokens/read          #Check if has read access            (Any)
GET    IAMProvider/api/stores/{storeID}/access/tokens/write         #Check if has write access           (Any)
GET    IAMProvider/api/stores/{storeID}/access/tokens/owner         #Check if has owner access           (Any)
GET    IAMProvider/api/stores/{storeID}/access/locks                #Lock access to store.               (Write access)
DELETE IAMProvider/api/stores/{storeID}/access/locks                #Unlock access to store.             (Write access)
```
### Secrets' Vault
```perl
GET    vault/api/ctrl/version       #Get service version.            (Any)

POST   vault/api/secrets/           #Create triplestore secrets.     (Admin/Store Owner)
GET    vault/api/secrets/{storeID}  #Get triplestore secrets.        (Read/Write Access)
DELETE vault/api/secrets/{storeID}  #Delete triplestore secrets.     (Admin/Store Owner)
```
### Triplestore
```perl
GET    Triplestore/api/ctrl/version             #Get service version                              (Any)

POST   Triplestore/api/{storeID}                #Upload data to triplestore.                      (Write Access)
DELETE Triplestore/api/{storeID}                #Delete triplestore.                              (Read/Write Access)
POST   Triplestore/api/query/{storeID}          #Execute SPARQL query over triplestore.           (Read/Write Access)

POST   Triplestore/api/secure/{storeID}         #Upload data to encrypted triplestore.            (Write Access)
POST   Triplestore/api/secure/{storeID}/delete  #Delete data from encrypted triplestore.          (Write Access)
POST   Triplestore/api/secure/{storeID}/search  #Search data from encrypted triplestore.          (Read/Write Access)
DELETE Triplestore/api/secure/{storeID}         #Delete encrypted triplestore.                    (Store Owner)
POST   Triplestore/api/secure/query/{storeID}   #Execute SPARQL query over encrypted triplestore. (Read/Write Access)

```