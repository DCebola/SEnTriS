# Encryption Protocols

**Option 1:** Deterministic encryption of values, under a random encryption layer.

```pseudocode
// Search Token: st ← E(k1, idx, keyword)
// Encrypted Node: ct ← E(k2, DET(k3, node)) 
State:
	Encoded_T ←  {}
	keywords ← {}
	rand ← newSecureRandom()

function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2, k3):
	foreach t = (s,p,o) in T:
		encodeNode(k1, k2, k3, s, "P", p)
		encodeNode(k1, k2, k3, s, "O", o)
		encodeNode(k1, k2, k3, p, "S", s)
		encodeNode(k1, k2, k3, p, "O", o)
		encodeNode(k1, k2, k3, o, "S", s)
		encodeNode(k1, k2, k3, o, "P", p)
		encodeNode(k1, k2, k3, s, "PO", p||o)
		encodeNode(k1, k2, k3, p, "SO", s||o)
		encodeNode(k1, k2, k3, o, "SP", s||p)
	foreach (total, keyword) in keywords:
		Encoded_T[DET(k1, rand, keyword)] ←  RND(k2, total)
return Encoded_T

function encodeNode(k1, k2, k3, node, patternType, keyword):
	i ← getKeywordIdx(keyword)
	st ← DET(k1, rand + i, patternType || keyword)
    ct ← RND(k2, DET(k3, node))
	Encoded_T[st] ← ct
		
function getKeywordIdx(keyword):
	idx ← keywords[keyword]
	if idx == ⊥:
		idx ← 1
		keywords[keyword] ← idx
	else:
		keywords[keyword] ← i + 1
return idx;
```

**Option 2:** Homomorphic encryption of equality index (for each node), under a random encryption layer.

```pseudocode
// Search Token: st ← DET(k1, idx, keyword)
// Equality tag: eq_tag ← HOM(k2, eq_idx)
// Encrypted Node: ct ← RND(k3, n)
// st ← eq_tag, ct
State:
	Eq ←  {} 
	Encoded_T ←  {}
	keywords ← {}
	rand ← newSecureRandom()

function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2 = (pk, sk), k3):
	foreach t = (s,p,o) in T:
		foreach n in t:
			nCtr ← setEQ(n)
		generateTrapdoor(k1, s, "P", p)
		generateTrapdoor(k1, s, "O", o)
		generateTrapdoor(k1, p, "S", s)
		generateTrapdoor(k1, p, "O", o)
		generateTrapdoor(k1, o, "S", s)
		generateTrapdoor(k1, o, "P", p)
		generateTrapdoor(k1, s, "PO", p||o)
		generateTrapdoor(k1, p, "SO", s||o)
		generateTrapdoor(k1, o, "SP", s||p)			
	
	foreach (st, n) in Encoded_T:
		eq_tag ← Eq[n]
		Encoded_T[st] ← (HOM(k2, eq_tag), RND(k3, n))
		key ← DET(k1, rand, n)
		if Encoded_T[key] = ⊥:
			Encoded_T[key] ← RND(k3, eq_tag)
		
    foreach (total, keyword) in keywords:
		Encoded_T[DET(k1, rand, keyword)] ←  RND(k2, total)
return Encoded_T

function generateTrapdoor(k, node, patternType, keyword):
	i ← getKeywordIdx(keyword)
	st ← DET(k1, rand + i, patternType || keyword)
	Encoded_T[st] ← node

function setEQ(node):
	eq_tag ← Eq[node]
	if eq_tag = ⊥:
		Eq[node] ← Eq.size()
		
function getKeywordIdx(keyword):
	idx ← keywords[keyword]
	if idx = ⊥:
		idx ← 1
		keywords[keyword] ← idx
	else:
		keywords[keyword] ← idx + 1
return idx;
```

Notes:

RDN: ChaCha20-Poly1305

DET: ChaCha20-Poly1305 w/ nonce based on seeded random sequence

HOM: DGK cryptographic system
