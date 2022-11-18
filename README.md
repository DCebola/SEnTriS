# SEnTri: Searchable Encrypted Triplestore

A searchable encrypted triplestore solution with (limited) support for SPARQL queries.

## **Endpoints**

### Client

```perl
GET  /Client/api/ctrl/version                                      #Get service version.

POST /Client/api/secure-triplestores/                              #Create encrypted triplestore.
POST /Client/api/secure-triplestores/{storeID}                     #Upload data to encrypted triplestore.
POST /Client/api/secure-triplestores/query/{storeID}               #Execute SPARQL query over encrypted triplestore.

POST /Client/api/triplestores/                                     #Create triplestore.
POST /Client/api/triplestores/{storeID}                            #Upload data to triplestore.
POST /Client/api/triplestore/query/{storeID}                       #Execute SPARQL query.
```
### IAM Provider
```perl
GET    IAMProvider/api/ctrl/version                                #Get service version.

POST   IAMProvider/api/auth                                        #Authenticate user.

POST   IAMProvider/api/users                                       #Register user.
DELETE IAMProvider/api/users/{username}                            #Delete user.
POST   IAMProvider/api/users/{username}/access                     #Grant user access.
DELETE IAMProvider/api/users/{username}/access                     #Revoke user access.
POST   IAMProvider/api/users/{username}/role                       #Grant user role.

GET    IAMProvider/api/pending/{username}/access                   #List pending access requests.
GET    IAMProvider/api/pending/{username}/access/{requestID}       #Get pending access request.
DELETE IAMProvider/api/pending/access/{requestID}                  #Process pending access request.
GET    IAMProvider/api/pending/{username}/role                     #List pending role requests.
GET    IAMProvider/api/pending/{username}/role/{requestID}         #Get pending role request.
DELETE IAMProvider/api/pending/role/{requestID}                    #Process pending role request.

POST   IAMProvider/api/{username}/stores/{storeID}                 #Create store.
DELETE IAMProvider/api/{username}/stores/{storeID}                 #Delete store.
GET    IAMProvider/api/stores/{storeID}/access/read/{username}     #Check if user has read access 
GET    IAMProvider/api/stores/{storeID}/access/write/{username}    #Check if user has write access
GET    IAMProvider/api/stores/{storeID}/access/owner/{username}    #Check if user has owner access


GET    IAMProvider/api/locks/{username}/stores/{storeID}           #Acquire lock on store.
DELETE IAMProvider/api/locks/{lockID}/{username}/stores/{storeID}  #Release lock on store.
```
### Secrets' Vault
```perl
GET    vault/api/ctrl/version                                      #Get service version.

POST   vault/api/secrets/{username}                                #Create triplestore secrets.
GET    vault/api/secrets/{username}/{storeID}                      #Get triplestore secrets.
DELETE vault/api/secrets/{username}/{storeID}                      #Delete triplestore secrets.
```
### Triplestore
```perl
GET    Triplestore/api/ctrl/version                                 #Get service version

POST   Triplestore/api/                                             #Create triplestore.
POST   Triplestore/api/{storeID}                                    #Upload data to triplestore.
DELETE Triplestore/api/{storeID}                                    #Delete triplestore.
POST   Triplestore/api/query/{storeID}                              #Execute SPARQL query over triplestore.

POST   Triplestore/api/secure                                       #Create encrypted triplestore.
POST   Triplestore/api/secure/{storeID}                             #Upload data to encrypted triplestore.
POST   Triplestore/api/secure/{storeID}/delete                      #Delete data from encrypted triplestore.
POST   Triplestore/api/secure/{storeID}/search                      #Search data from encrypted triplestore.
DELETE Triplestore/api/secure/{storeID}                             #Delete encrypted triplestore. 
POST   Triplestore/api/secure/query/{storeID}                       #Execute SPARQL query over encrypted triplestore.

```