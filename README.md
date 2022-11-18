# SEnTri: Searchable Encrypted Triplestore

A searchable encrypted triplestore solution with (limited) support for SPARQL queries.

## **Endpoints**

### ClientProxy

```perl
GET  /ClientProxy/api/ctrl/version                           #Get service version.

POST /ClientProxy/api/secure-triplestores/                   #Create encrypted triplestore.
POST /ClientProxy/api/secure-triplestores/{storeID}          #Upload data to encrypted triplestore.
POST /ClientProxy/api/secure-triplestores/query/{storeID}    #Execute SPARQL query over encrypted triplestore.

POST /ClientProxy/api/triplestores/                          #Create triplestore.
POST /ClientProxy/api/triplestores/{storeID}                 #Upload data to triplestore.
POST /ClientProxy/api/triplestore/query/{storeID}            #Execute SPARQL query.
```
### IAM Provider
```perl
GET    IAMProvider/api/ctrl/version                          #Get service version.

POST   IAMProvider/api/auth                                  #Authenticate user.

POST   IAMProvider/api/users                                 #Register user.
DELETE IAMProvider/api/users/{username}                      #Delete user.
POST   IAMProvider/api/users/{username}/access               #Grant user access.
DELETE IAMProvider/api/users/{username}/access               #Revoke user access.
POST   IAMProvider/api/users/{username}/role                 #Grant user role.

GET    IAMProvider/api/pending/{username}/access             #List pending access requests.
GET    IAMProvider/api/pending/{username}/access/{requestID} #Get pending access request.
DELETE IAMProvider/api/pending/access/{requestID}            #Process pending access request.
GET    IAMProvider/api/pending/{username}/role               #List pending role requests.
GET    IAMProvider/api/pending/{username}/role/{requestID}   #Get pending role request.
DELETE IAMProvider/api/pending/role/{requestID}              #Process pending role request.

POST   IAMProvider/api/{username}/stores/{storeID}           #Create store.
DELETE IAMProvider/api/{username}/stores/{storeID}           #Delete store.
GET    IAMProvider/api/stores/{storeID}/access/{username}    #Get store access policy for the specified user.

GET    IAMProvider/api/locks/users/{username}                #Acquire lock on user.
DELETE IAMProvider/api/locks/{lockID}/users/{username}       #Release lock on user.
GET    IAMProvider/api/locks/stores/{storeID}                #Acquire lock on store.
DELETE IAMProvider/api/locks/{lockID}/stores/{storeID}       #Release lock on store.
```
### Secrets' Vault
```perl
GET    vault/api/ctrl/version                                #Get service version.

POST   vault/api/secrets                                     #Create triplestore secrets.
GET    vault/api/secrets/{storeID}                           #Get triplestore secrets.
DELETE vault/api/secrets                                     #Delete triplestore secrets.
```
### Triplestore
```perl
GET    Triplestore/api/ctrl/version                           #Get service version

POST   Triplestore/api/                                       #Create triplestore.
POST   Triplestore/api/{storeID}                              #Upload data to triplestore.
DELETE Triplestore/api/{storeID}                              #Delete triplestore.
POST   Triplestore/api/query/{storeID}                        #Execute SPARQL query over triplestore.

POST   Triplestore/api/secure                                 #Create encrypted triplestore.
POST   Triplestore/api/secure/{storeID}                       #Upload data to encrypted triplestore.
POST   Triplestore/api/secure/{storeID}/delete                #Delete data from encrypted triplestore.
POST   Triplestore/api/secure/{storeID}/search                #Search data from encrypted triplestore.
DELETE Triplestore/api/secure/{storeID}                       #Delete encrypted triplestore. 
POST   Triplestore/api/secure/query/{storeID}                 #Execute SPARQL query over encrypted triplestore.

```