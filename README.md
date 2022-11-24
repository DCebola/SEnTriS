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
GET    IAMProvider/api/ctrl/version                                 #Get service version.            (Any)
          
POST   IAMProvider/api/users/auth                                   #Authenticate user.              (Any)
POST   IAMProvider/api/users                                        #Register user.                  (Any)
DELETE IAMProvider/api/users/{username}                             #Delete user.                    (Any)
POST   IAMProvider/api/users/{username}/access                      #Grant user access.              (Admin/Store Owner)
DELETE IAMProvider/api/users/{username}/access                      #Revoke user access.             (Admin/Store Owner)
POST   IAMProvider/api/users/{username}/role                        #Grant user role.                (Admin)
GET    IAMProvider/api/users/{username}/requests/access             #List pending access requests.   (Admin/Store Owner)
GET    IAMProvider/api/users/{username}/requests/access/{requestID} #Get pending access request.     (Admin/Store Owner)
POST   IAMProvider/api/users/{username}/requests/access/{requestID} #Process pending access request. (Admin/Store Owner)
GET    IAMProvider/api/users/{username}/requests/roles              #List pending role requests.     (Admin)
GET    IAMProvider/api/users/{username}/requests/roles/{requestID}  #Get pending role request.       (Admin)
POST   IAMProvider/api/users/{username}/requests/roles/{requestID}  #Process pending role request.   (Admin)

POST   IAMProvider/api/stores                                       #Create store.                   (Admin/Priviledged)
PUT    IAMProvider/api/stores/{username}                            #Change owner.                   (Admin/Store Owner)
DELETE IAMProvider/api/stores/{storeID}                             #Delete store.                   (Admin/Store Owner)

POST   IAMProvider/api/access/tokens                                #Create access token for user.   (Any)
DELETE IAMProvider/api/access/tokens                                #Delete access token.            (Admin/Token owner)
GET    IAMProvider/api/access/locks                                 #Lock access to store.           (Write access)
DELETE IAMProvider/api/access/locks                                 #Unlock access to store.         (Write access)
GET    IAMProvider/api/access/read/{storeID}                        #Check if has read access        (Any)
GET    IAMProvider/api/access/write/{storeID}                       #Check if has write access       (Any)
GET    IAMProvider/api/access/owner/{storeID}                       #Check if has owner access       (Any)
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