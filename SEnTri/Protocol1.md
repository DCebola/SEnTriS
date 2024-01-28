# SSE-SPARQL-DET: Encryption Scheme



```pseudocode
State:
    kMASTER ← generateKey() 
    kRND ← generateKey() 
    kDET ← generateKey()
    ivDET ← generateRandomIV()
    frequencyIV ← generateZeroFilledIV()
    schemaKeyword ← generateRandomString()
    encryptedNodes ← {}
    keywordFrequencies ← {}
    derivedKeys ← {}
    
function initState(master, rnd, det, iv, schema):
    kMASTER ← master 
    kRND ← rnd
    kDET ← det
    ivDET ← iv
    frequencyIV ← newZeroFilledIV()
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
        st ← DET(derivedKeys[keyword], keyword, frequencyIV)
        ct ← RND(kRND, keywordFrequencies[keyword])
        encryptedNode[st] ← ct

function encodeSchemaNode(node):
    f ← incrementKeywordFrequency(schemaKeyword)
    st ← DET(derivedKey(schemaKeyword), schemaKeyword, f)
    ct ← RND(kRND, node)
    encryptedNode[st] ← ct
    
function encodeNode(node, keyword):
    f ← incrementKeywordFrequency(keyword)
    st ← DET(derivedKey(keyword), keyword, f)
    ct ← RND(kRND, DET(kDET, node, ivDET))
    encryptedNode[st] ← ct
	 return f
 
function encodeTriple(keyword, frequencies): 
    i ← 0;
    foreach f in frequencies:
        st ← DET(getDerivedKey(keyword), keyword, i)
        encryptedNode[st] ← RND(kRND, f)
        i++; 

function deriveKey(context):
	key ← derivedKeys[context];
    if key == ⊥:
		key ← generateKey(kMASTER, context)
        derivedKeys[context] ← key
	return key

function incrementKeywordFrequency(keyword):
	idx ← keywordFrequencies[keyword]
	if idx == ⊥:
		idx ← 1
		keywords[keyword] ← idx
	else:
		keywords[keyword] ← i + 1
	return idx;

function generateTrapdoor(keyword, i):
    return DET(derivedKey(keyword), keyword, i)

function generateTrapdoorAndIncrementIV(keyword):
	return DET(derivedKey(keyword), keyword, incrementKeywordFrequency(keyword))
    
function generateFrequencyTrapdoor(keyword):
	return DET(derivedKey(keyword), keyword, frequencyIV)

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
            trapdoors ← trapdoors U {DET(derivedKey(t_keyword), t_keyword,i)}
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

