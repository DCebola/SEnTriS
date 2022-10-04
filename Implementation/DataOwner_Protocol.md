# Data Owner
 Search Token: st ← E(k, idx || idx_ctr)
 Counter Token: ct ← E(k,  idx)
 Encrypted Node: n' ← E(k, n ||  st)
 Encrypted Index Counter: ctr' ← E(k, ctr || ct)

```pseudocode
function EncodeTriplestore(T = {t0, t1, ... tn}, k1, k2):
	Encoded_T ←  {}
	Eq ←  {} 
	values ← {}
	foreach t = (s,p,o) in T:
		encodeNode(Encoded_T, Eq, values, k1, k2, s, p)
		encodeNode(Encoded_T, Eq, values, k1, k2, s, o)
		encodeNode(Encoded_T, Eq, values, k1, k2, p, s)
		encodeNode(Encoded_T, Eq, values, k1, k2, p, o)
		encodeNode(Encoded_T, Eq, values, k1, k2, o, s)
		encodeNode(Encoded_T, Eq, values, k1, k2, o, p)
		encodeNode(Encoded_T, Eq, values, k1, k2, s, p||o)
		encodeNode(Encoded_T, Eq, values, k1, k2, p, s||o)
		encodeNode(Encoded_T, Eq, values, k1, k2, o, s||p)			

	foreach (v, ctr) in values:
		ct := E(k1, v)
		enc_ctr := E(k2, ctr || ct)
		Encoded_T[ct] ← enc_ctr
	
	foreach (v, l) in Eq:
		foreach v1 in l:
			foreach v2 in l:
				if v1 ≠ v2:
					Encoded_T[v1] ← v2 or Encoded_T[E(k3, v1)] ← v2 //Depends on final design

return Encoded_T, Eq
```

```pseudocode
function encodeTriple(Encoded_T, Eq, values, k1, k2, n, idx):
	i ← values[idx]
	if i = ⊥:
		values[idx] ← 0
		i ← 0
	else:
		values[idx] ← i + 1

	st ← E(k1, idx || i)
	enc_n ← E(k2, n || st)
	
	Encoded_T[st] ← enc_n
	
	l ← Eq[n]
	if l = ⊥:
		Eq[n] ← [enc_n]
	else
		l ← l U enc_n		
```
