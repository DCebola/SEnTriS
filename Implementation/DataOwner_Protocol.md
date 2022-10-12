# Data Owner

**Option 1:** Deterministic encryption of values, under a random encryption layer.

```pseudocode
// Search Token: st ← E(k1, idx || idx_ctr)
// Encrypted Node: ct ← E(k2, DET(k3, node)) 
State:
	Encoded_T ←  {}
	keywords ← {}

function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2, k3):
	foreach t = (s,p,o) in T:
		encodeNode(k1, k2, k3, s, p)
		encodeNode(k1, k2, k3, s, o)
		encodeNode(k1, k2, k3, p, s)
		encodeNode(k1, k2, k3, p, o)
		encodeNode(k1, k2, k3, o, s)
		encodeNode(k1, k2, k3, o, p)
		encodeNode(k1, k2, k3, s, p||o)
		encodeNode(k1, k2, k3, p, s||o)
		encodeNode(k1, k2, k3, o, s||p)			
return Encoded_T

function encodeNode(k1, k2, k3, node, keyword):
	idx ← getKeywordIdx(keyword)
	st ← DET(k1, idx || i)
    ct ← RND(k2, DET(k3, node))
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

**Option 2:** Homomorphic encryption of equality index (for each node), under a random encryption layer.

```pseudocode
// Search Token: st ← DET(k1, idx || idx_ctr)
// Equality tag: eq_tag ← HOM(k2, eq_idx)
// Encrypted Node: ct ← RND(k3, n)
// st ← eq_tag, ct
State:
	Eq ←  {} 
	Encoded_T ←  {}
	keywords ← {}

function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2, k3):
	foreach t = (s,p,o) in T:
		foreach n in t:
			nCtr ← setEQ(n)
		generateTrapdoor(k1, s, p)
		generateTrapdoor(k1, s, o)
		generateTrapdoor(k1, p, s)
		generateTrapdoor(k1, p, o)
		generateTrapdoor(k1, o, s)
		generateTrapdoor(k1, o, p)
		generateTrapdoor(k1, s, p||o)
		generateTrapdoor(k1, p, s||o)
		generateTrapdoor(k1, o, s||p)			

	foreach (eq_t, n) in Encoded_T:
		eq_tag ← HOM(k2, Eq[n])
		ct ← RND(k5, n)
		Encoded_T[st] ← (eq_tag, ct)
return Encoded_T

function generateTrapdoor(k, node, keyword):
	idx ← getKeywordIdx(keyword)
	st ← DET(k1, idx || i)
	Encoded_T[st] ← node

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

