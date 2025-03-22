
## LUBM: Upload Size

**Setup:** 

1. Create 1 priviledged user

**Scenario:**

1. Loop over datasets (lubm-1, lubm-5, lubm-10, lubm-20)
2. Loop over protocols
   1. Repeat 10
      1.  Create triplestore
      2.  Upload dataset
      3.  Upload ontology
      4.  Get info (size expansion)
      5.  Delete triplestore

## LUBM: Query Completeness & Soundness (lubm-1) & Latency (lubm-1, lubm-5)

**Setup:** 

1. Create 1 priviledged user
2. Loop over protocols
   1. Loop over datasets
      1. Create triplestore 
         1. Upload dataset
         2. Upload ontology

**Scenario:**

1. Loop over protocols
   1. Loop over datasets
      1. Loop over queries
         1.  Repeat 10 (if lubm-1, evaluate completeness & soundness)

## Validation: RBAC (roles & access sharing)

**Setup**:

1. Create 5 privileged users 

**Scenario**:

1. Loop over protocols
   1. Loop over 5 users (owner)
      1. Create triplestore
      2. **[owner-write]** test_owner_access :check pass | http_200
      3. **[owner-read]**  (test_owner_access :check x?) | x? -> pass
      4. **[owner-write]** test_shared_read_access :check pass | http_200
      5. Loop over **[No access, read access, write access, remove write access, remove read access]**
      6. Loop over other 4 other users
         1. Give access to user, respecting the current access policy
         2. **[user-read]** (test_shared_read_access :check x?) | x? -> pass or http_403
         3. **[user-write]** (test_shared_write_access :check pass) | http_200 or http_403
         4. If http_200, **[user-read]** (test_shared_write_access :check x?) | x? -> pass
      7. Transfer ownership to rnd user
      8. **[rnd-user-delete]** Delete transfered triplestore
2. Set BASIC role to 4 users, keep 1 privileged
3. Loop over protocols
   1. **[priviledged]** Create triplestore
   2. **[owner-write]** test_owner_access :check pass | http_200
   3. **[owner-read]**  (test_owner_access :check x?) | x? -> pass
   4. **[owner-write]** test_shared_read_access :check pass | http_200
      1. Loop over **[No access, read access, write access, remove write access, remove read access]**
      2. Loop over other 4 basic users
         1. Issue access request to triplestore, respecting the current access policy
         2. Owner validates access request
         3. **[user-read]** (test_shared_read_access :check x?) | x? -> pass or http_403
         4. **[user-write]** (test_shared_write_access :check pass) | http_200 or http_403
         5. If http_200, [user-read] (test_shared_write_access :check x?) | x? -> pass
         6. Transfer ownership to user | expect error

## Validation: Concurrent writes

**Setup:**

1. Create 5 priviledged users, 
   1. Select 1 user
   2. Loop over protocols
      1. Create triplestore
      2. Loop over other 4 users
         1. Give write access

**Scenario**

1. **In parallel**: Try to write, if success no timeout, assert write succeeded 

## Validation: Concurrent Reads

**Setup:**

1. Create 5 priviledged users, 
   1. Select 1 user
   2. Loop over protocols
      1. Create triplestore
      2. Upload dataset (lubm-0)
      3. Loop over other 4 users
         1. Give read access

**Scenario**

1. **In parallel**: Try to read (do same simple query), assert read succeeded 

## Validation: SPARQL Operations

Will not test Select queries nor the Distinct, OrderBy modifiers, because they are already tested with lubm queries.

**Setup**:

1. Create 1 priviledged user
 	1. Loop over protocols
     1. Create triplestore
     2. Upload dataset (lubm-0)

**Scenario:**

1. Loop over protocols
   1. Loop over queries (sparql-ask, sparql-construct, sparql-describe, sparql-union, sparql-minus, sparql-optional, sparql-insert-data, sparql-delete-data, sparql-delete-where, sparql-insert-delete)
      1. Execute query & validate answer

## LUBM (volume analysis)
1. LUBM - Upload: 4 datasets * 2 protocols * 10 repetitions = 80 creation, 80 dataset upload, 80 ontology uploads, 80 deletions
2. LUBM - Latency: 2 datasets * 2 protocols * 14 queries * 10 repetitions = 560 queries
3. LUBM - Correcteness & Soundness: 1 dataset * 2 protocols * 14 queries * 3 repetitions = 140 queries