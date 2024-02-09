# SSE-SPARQL-DET: Encryption Scheme



```pseudocode
State:
    kMASTER ← generateKey() 
    kRND ← generateKey() 
    kDET ← generateKey()
    ivDET ← generateRandomIV()
    zeroIV ← generateZeroFilledIV()
    schemaKeyword ← generateRandomString()
    encryptedNodes ← {}
    keywordFrequencies ← {}
    derivedKeys ← {}
    
function loadState(master, rnd, det, iv, schema):
    kMASTER ← master 
    kRND ← rnd
    kDET ← det
    ivDET ← iv
    zeroIV ← newZeroFilledIV()
    schemaKeyword ← schema
    encryptedNodes ← {}
    keywordFrequencies ← {}
    derivedKeys ← {}

function exec(T = {t0, t1, ... tn}, schema):
    if (schema == true)
        encryptSchemaTriples(triples);
    else
        encryptTriples(triples);
    encryptKeywordInfo();

function EncryptSchemaTriples(T = {t0, t1, ... tn}):
	foreach t = (s,p,o) in T:
		encodeSchemaNode(parseNode(s));
        encodeSchemaNode(parseNode(p));
        encodeSchemaNode(parseNode(o));

function EncryptTriples(T = {t0, t1, ... tn}):
	foreach t = (s,p,o) in T:
        freq ← {}
	    parsed_s = parseNode(s)
        parsed_p = parseNode(p)
        parsed_o = parseNode(o)
        s_keyword = parseKeyword(s)
        p_keyword = parseKeyword(p)
        o_keyword = parseKeyword(o)
        freq ← freq U {encodeNode(parsed_p, "PO" || s_keyword)}
        freq ← freq U {encodeNode(parsed_o, "PO" || s_keyword)}
        freq ← freq U {encodeNode(parsed_s, "SO" || p_keyword)}
        freq ← freq U {encodeNode(parsed_o, "SO" || p_keyword)}
        freq ← freq U {encodeNode(parsed_s, "SP" || o_keyword)}
        freq ← freq U {encodeNode(parsed_p, "SP" || o_keyword)}
        freq ← freq U {encodeNode(parsed_s, "S" || p_keyword || o_keyword)}
        freq ← freq U {encodeNode(parsed_p, "P" || s_keyword || o_keyword)}
        freq ← freq U {encodeNode(parsed_o, "O" || s_keyword || p_keyword)}
		encodeTriple("SPO" || s_keyword || p_keyword || o_keyword, freq);

function encryptKeywordInfo() {
    foreach keyword in keywordFrequencies:
        st ← ENC(derivedKeys[keyword], keyword, zeroIV)
        ct ← ENC(kRND, keywordFrequencies[keyword])
        encryptedNode[st] ← ct

function encodeSchemaNode(node):
    f ← incrementKeywordFrequency(schemaKeyword)
    st ← ENC(derivedKey(schemaKeyword), schemaKeyword, f)
    ct ← ENC(kRND, node)
    encryptedNode[st] ← ct
    
function encodeNode(node, keyword):
    f ← incrementKeywordFrequency(keyword)
    st ← ENC(derivedKey(keyword), keyword, f)
    ct ← ENC(kRND, ENC(kDET, node, ivDET))
    encryptedNode[st] ← ct
	return f
 
function encodeTriple(keyword, frequencies): 
    i ← 0;
    foreach f in frequencies:
        st ← ENC(getDerivedKey(keyword), keyword, i)
        encryptedNode[st] ← ENC(kRND, f)
        i++; 

function deriveKey(context):
	key ← derivedKeys[context];
    if key == ⊥:
		key ← generateKey(kMASTER, context)
        derivedKeys[context] ← key
	return key

function incrementKeywordFrequency(keyword):
	f ← keywordFrequencies[keyword]
	if f == ⊥:
		f ← 1
		keywords[keyword] ← f
	else:
		keywords[keyword] ← i + 1
	return f;

function generateTrapdoor(keyword, i):
    return ENC(derivedKey(keyword), keyword, i)
    
function generateTrapdoor(keyword):
	return ENC(derivedKey(keyword), keyword, zeroIV)
	
function generateTrapdoorAndIncrementIV(keyword):
	return ENC(derivedKey(keyword), keyword, incrementKeywordFrequency(keyword))

function generateTriplePatternTrapdoors(T = {t0, t1, ... tn}):
	keywordPatternTrapdoors ← {}
	foreach t = (s,p,o) in T:
        keywords ← {}
        i ← 0
	    s_keyword = parseKeyword(s)
        p_keyword = parseKeyword(p)
        o_keyword = parseKeyword(o)
        t_keyword = "SPO" || s_keyword || p_keyword || o_keyword
        keywords ← keywords U {"PO" || s_keyword}
        keywords ← keywords U {"PO" || s_keyword}
        keywords ← keywords U {"SO" || p_keyword}
        keywords ← keywords U {"SO" || p_keyword}
        keywords ← keywords U {"SP" || o_keyword}
        keywords ← keywords U {"SP" || o_keyword}
        keywords ← keywords U {"S" || p_keyword || o_keyword}
        keywords ← keywords U {"P" || s_keyword || o_keyword}
        keywords ← keywords U {"O" || s_keyword || p_keyword}
        generatePatternTrapdoors(res, t_keyword, keywords);	
        foreach keyword in keywords:
        	trapdoors ← keywordPatternTrapdoors[keyword]
            if (trapdoors == ⊥):
            	trapdoors ← {}
            trapdoors ← trapdoors U {ENC(derivedKey(t_keyword), t_keyword, i)}
            keywordPatternTrapdoors[keyword] ← trapdoors;
            i++;
	return keywordPatternTrapdoors

function setKeywordFrequencies(values):
    foreach keyword in values:
        f ← values[keyword]
        if (f > 0)
            keywordFrequencies[keyword] ← f
            
function getSchemaKeyword():
    return schemaKeyword

function getIvDET():
    return ivDET

function getKeywordsMasterKey():
    return kMASTER

function getRNDKey():
    return kRND

function getDETKey():
    return kDET

function getEncryptedNodes():
    return encryptedNodes

function clearNodes():
    encryptedNodes ← {}

function clearFrequencies():
    keywordFrequencies ← {}
```

