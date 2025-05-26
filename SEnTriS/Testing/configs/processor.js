'use strict'

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
	extractCookie,
	extractAdminCookie,

	genUser,
	selectUser,
	processGenUserReply,
	selectRoleRequest,
	generateAuthRequest,

	genTriplestore,
	uploadDataset,
	uploadOntology,
	processTriplestoreSize,
}

const Faker = require("faker")
const fs = require('fs')
const FormData = require('form-data');

setTimeout(console.log, 2147483647)

var users = []
var admin_cookie = ""
var sessions = new Map()
var queries = []
var datasetNames = []
var datasets = new Map()
var ontologyNames = []
var ontologies = new Map()
var queries = []
var answers = new Map()

// Loads dataset from disk
function loadData() {
	if (fs.existsSync('./data/datasets.json')) {
		JSON.parse(fs.readFileSync('./data/datasets.json', 'utf8')).forEach(i => { datasetNames.push(i) })
		fs.readdirSync('./data/datasets').forEach((data, i) => { datasets.set(datasetNames[i], data) })
	}
	if (fs.existsSync('./data/ontologies.json')) {
		JSON.parse(fs.readFileSync('./data/ontologies.json', 'utf8')).forEach(i => { ontologyNames.push(i) })
		fs.readdirSync('./data/ontologies').forEach((data, i) => { ontologies.set(ontologyNames[i], data) })
	}
	if (fs.existsSync('./data/queries.json')) {
		JSON.parse(fs.readFileSync('./data/queries.json', 'utf8')).forEach(i => { queries.push(i) })
		fs.readdirSync('./data/answers').forEach((data, i) => { answers.set(queries[i].name, data) })
	}
	if (fs.existsSync('./data/users.json')) {
		JSON.parse(fs.readFileSync('./data/users.json', 'utf8')).forEach(i => { users.push(i) })
	}
}
loadData()

// Auxiliary function to select an element from an array
Array.prototype.sample = function () {
	return this[Math.floor(Math.random() * this.length)]
}

/**
 * Return true with specified probability
 */
function random(context, next) {
	const continueLooping = Math.random() < context.vars.rnd
	return next(continueLooping)
}

/**
 * Extracts the session cookie.
 */
function extractCookie(requestParams, response, context, events, done) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		for (let header of response.rawHeaders) {
			if (header.startsWith("session")) {
				console.log("[extract_cookie] - " + header)
				context.vars.session_cookie = header.split(';')[0];
				sessions.set(context.vars.username, context.vars.session_cookie)
			}
		}
	}
	return done()
}

/**
 * Extracts the session cookie.
 */
function extractAdminCookie(requestParams, response, context, events, done) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		for (let header of response.rawHeaders) {
			if (header.startsWith("session")) {
				console.log("[extract_admin_cookie] - " + header)
				admin_cookie = header.split(';')[0];
				context.vars.admin_session_cookie = admin_cookie
			}
		}
	}
	return done()
}

/**
 * Generate data for a new user using Faker
 */
function genUser(context, events, done) {
	context.vars.username = Faker.internet.userName(Faker.name.firstName(), Faker.name.lastName())
	context.vars.password = `${Faker.internet.password()}`
	console.log("[generate-user] - " + context.vars.username + " | " + context.vars.password)
	return done()
}

/**
 * Stores new user data
 */
function processGenUserReply(requestParams, response, context, events, done) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		users.push({
			"username": context.vars.username,
			"password": context.vars.password
		})
		console.log("[process-generate-user] - Saved User: " + JSON.stringify(users))
	}
	return done()
}

function generateAuthRequest(requestParams, context, events, done) {
	const form = new FormData()
	form.append('username', context.vars.username)
	form.append('password', context.vars.password)
	requestParams.formData = form
	return done()
}


/**
 * Extracts the role requestParams id
 */
function selectRoleRequest(requestParams, response, context, events, done) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		let requestParamsID = JSON.parse(response.body).filter(r => {
			if (r.username === context.vars.username)
				return r.requestParamsID
		});
		if (requestParamsID != undefined)
			context.vars.role_requestParams_id = requestParamsID
	}
	return done()
}

/**
 * Select an user.
 */
function selectUser(context, events, done) {
	if (users.length > 0) {
		let user = users.sample()
		context.vars.username = user.username
		context.vars.password = user.password
		console.log("[select-user] - " + user.username + " | " + user.password)
		if (sessions.has(user.username)) {
			context.vars.session_cookie = sessions.get(user.username)
			console.log("[select-user-session] - " + sessions.get(user.username))
		}
	}
	return done()
}

/**
 * Generate data for a new triplestore
 */
function genTriplestore(context, done) {
	console.log("genTriplestore")
	context.vars.triplestoreID = `${Faker.string.nanoid()}`
	return done()
}

/**
 * Prepares dataset upload
 */
function uploadDataset(requestParams, context, next) {
	const form = new FormData()
	form.append('issuer', context.vars.username)
	form.append('triplestoreID', context.vars.triplestoreID)
	form.append('syntax', "rdf/xml")
	form.append('contents', datasets.get(context.vars.datasetNames.slice(1)))
	requestParams.body = form
	return next()
}

/**
 * Prepares ontology upload
 */
function uploadOntology(requestParams, context, next) {
	const form = new FormData()
	form.append('issuer', context.vars.username)
	form.append('triplestoreID', context.vars.triplestoreID)
	form.append('syntax', "rdf/xml")
	form.append('contents', ontologies.get(ontologyNames[0]))
	requestParams.body = form
	return next()
}

function processTriplestoreSize(response, context, next) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		console.log(JSON.parse(response.body))
		events.emit("histogram", queryName + ".size", JSON.parse(response.body));
	}
	return next()
}

/**
 * Extracts the triplestore list
 */
function extractTriplestoreList(response, context, next) {
	context.vars.triplestoreList = []
	if (response.statusCode >= 200 && response.statusCode < 300) {
		context.vars.triplestoreList = JSON.parse(response.body)
	}
	return next()
}

/**
 * Select a random triplestore from the list of available
 */
function selectTriplestoreFromList(context, next) {
	if (typeof context.vars.triplestoreList !== 'undefined' && context.vars.triplestoreList.length > 0)
		context.vars.triplestoreID = context.vars.triplestoreList.sample()
	else
		delete context.vars.triplestoreID
	return next()
}

/**
 * Select a the LUBM query to send
 */
function selectLUBMQuery(context, next) {

	return next()
}

/**
 * Processes SPARQL query answer
 * Assertion: Bindings are sorted w/ no duplicate bindings
 */
function processLUBMQueryAnswer(response, context, events, next) {
	let queryName = context.vars.queryName
	if (response.statusCode >= 200 && response.statusCode < 300) {
		let referenceAnswer = answers.get(queryName)
		let receivedAnswer = JSON.parse(response.body)
		if (hasEqualVars(new Set(referenceAnswer.head.vars), new Set(receivedAnswer.head.vars))) {
			let correctSet = extractBindings(referenceAnswer);
			let receivedSet = extractBindings(receivedAnswer);
			let correctlyReturned = new Set([...receivedSet].filter(ans => correctSet.has(ans)));
			let completeness = correctSet.size > 0 ? (correctlyReturned.size / correctSet.size) * 100 : 100;
			let soundness = receivedSet.size > 0 ? (correctlyReturned.size / receivedSet.size) * 100 : 100;
			events.emit("histogram", queryName + ".completeness", completeness);
			events.emit("histogram", queryName + ".soundness", soundness);
		} else
			events.emit("counter", queryName + ".wrong", 1);
	} else
		events.emit("counter", queryName + ".wrong", 1);
	return next()
}

function extractBindings(result) {
	return new Set(result.results.bindings.map(binding =>
		JSON.stringify(binding)
	));
}

function hasEqualVars(vars, receveivedVars) {
	return vars.difference(receveivedVars).length == 0;
}


function calculateCompletnessAndSoundness(nonEntailedBindings, expectedBindings, receivedBindings) {
	let completeness, soundness
	if (expectedBindings.length >= receivedBindings.length) {
		completeness = expectedBindings.filter(x => receivedBindings.includes(x)) / expectedBindings.length
		soundness = nonEntailedBindings.filter(x => receivedBindings.includes(x)) / receivedBindings.length
	}
	return [completeness, soundness]
}

