# Data Owner
 Search Token: st ← E(k, idx || idx_ctr)
 Encrypted Node: n' ←E(k2, E(k3, n) || eq_hash)

```pseudocode
function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2, k3, k4):
	Eq ←  {} 
	Encoded_T ←  {}
	keywords ← {}
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

	foreach (st, n) in Encoded_T:
	 	//Option 1
		Encoded_T[st] ← RND(k2, RND(k3, n) || CRYPT_DB_JOIN(k4, Eq[n]))
		//Option 2
		eq_tag ← RND(k3, CRYPT_DB_JOIN(k2, Eq[n])))
		Encoded_T[st] ← eq_tag
		Encoded_T[DET(k4, st)] ← RND(k3, n)
return Encoded_T
```

```pseudocode
function generateTrapdoor(k, node, keyword):
	idx ← getKeywordIdx(keyword)
	st ← DET(k1, idx || i)
	Encoded_T[st] ← n
```

```pseudocode
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

