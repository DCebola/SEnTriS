# Data Owner

**Option 1:** Deterministic encryption of values (that can be re-encrypted), under a random encryption layer.

```pseudocode
// Search Token: st ← E(k1, idx || idx_ctr)
// Encrypted Node: ct ← E(k2, JOIN(k3, n))
State:
	Encoded_T ←  {}
	keywords ← {}

function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2, k3, k4):
	foreach t = (s,p,o) in T:
		encodeNode(k1, k2, s, p)
		encodeNode(k1, k2, s, o)
		encodeNode(k1, k2, p, s)
		encodeNode(k1, k2, p, o)
		encodeNode(k1, k2, o, s)
		encodeNode(k1, k2, o, p)
		encodeNode(k1, k2, s, p||o)
		encodeNode(k1, k2, p, s||o)
		encodeNode(k1, k2, o, s||p)			
return Encoded_T

function encodeNode(k1, k2, node, keyword):
	idx ← getKeywordIdx(keyword)
	st ← DET(k1, idx || i)
    ct ← RND(k2, CRYPT_DB_JOIN(k4, node))
	Encoded_T[st] ← ct
		
function getKeywordIdx(keyword):
	i ← keywords[idx]
	if i = ⊥:
		keywords[idx] ← 0
		i ← 0
	else:
		keywords[idx] ← i + 1
return i;
```

**Option 2:** Deterministic encryption of equality index (for each node), under a random encryption layer.

```pseudocode
// Equality Token: et ← DET(k1, idx || idx_ctr)
// Equality tag: eq_tag ← RND(k2, JOIN(k3, eq_idx))
// Search Token: st ← DET(k4, et)
// Encrypted Node: ct ← RND(k5, n)
State:
	Eq ←  {} 
	Encoded_T ←  {}
	keywords ← {}

function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2, k3, k4, k5):
	foreach t = (s,p,o) in T:
		foreach n in t:
			nCtr ← setEQ(n)
		generateEQTrapdoor(k1, s, p)
		generateEQTrapdoor(k1, s, o)
		generateEQTrapdoor(k1, p, s)
		generateEQTrapdoor(k1, p, o)
		generateEQTrapdoor(k1, o, s)
		generateEQTrapdoor(k1, o, p)
		generateEQTrapdoor(k1, s, p||o)
		generateEQTrapdoor(k1, p, s||o)
		generateEQTrapdoor(k1, o, s||p)			

	foreach (eq_t, n) in Encoded_T:
		eq_tag ← RND(k2, CRYPT_DB_JOIN(k3, Eq[n])))
		Encoded_T[eq_t] ← eq_tag
		st ← DET(k4, st)
		ct ← RND(k5, n)
		Encoded_T[st] ← ct
return Encoded_T

function generateEQTrapdoor(k, node, keyword):
	idx ← getKeywordIdx(keyword)
	eq_t ← DET(k1, idx || i)
	Encoded_T[eq_t] ← node

function setEQ(node):
	eq_tag ← Eq[node]
	if eq_tag = ⊥:
		Eq[node] ← Eq.size()
		
function getKeywordIdx(keyword):
	i ← keywords[idx]
	if i = ⊥:
		keywords[idx] ← 0
		i ← 0
	else:
		keywords[idx] ← i + 1
return i;
```

