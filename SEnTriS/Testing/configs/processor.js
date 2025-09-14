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
	uploadABox,
	uploadTBox,
	processTriplestoreSize,

	genLUBMQuery,
	processLUBMQueryAnswer
}

const { faker } = require('@faker-js/faker')
const fs = require('fs')
const FormData = require('form-data')

setTimeout(console.log, 2147483647)

var users = []
var admin_cookie = ""
var sessions = new Map()
var datasets = new Map()
var ontologies = new Map()
var queries = new Map()
var answers = new Map()

// Loads dataset from disk
function loadData() {
	fs.readdirSync('./data/datasets').forEach((dataset, i) => { datasets.set(dataset, fs.readFileSync('./data/datasets/' + dataset)) })
	fs.readdirSync('./data/ontologies').forEach((ontology, i) => { ontologies.set(ontology, fs.readFileSync('./data/ontologies/' + ontology)) })
	if (fs.existsSync('./data/queries.json')) {
		JSON.parse(fs.readFileSync('./data/queries.json', 'utf8')).forEach(query => { queries.set(query.name, query) })
		fs.readdirSync('./data/answers').forEach(
			answer => {
				let queryName = answer.split('.')[0]
				let referenceAnswer = JSON.parse(fs.readFileSync('./data/answers/' + answer))
				answers.set(queryName, referenceAnswer) 
			}
		)
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
				//console.log("[extract_cookie] - " + header)
				context.vars.session_cookie = header.split(';')[0].split("=")[1]
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
				//console.log("[extract_admin_cookie] - " + header)
				admin_cookie = header.split(';')[0].split("=")[1]
				context.vars.admin_session_cookie = admin_cookie
			}
		}
	}
	return done()
}

/**
 * Generate data for a new user using faker
 */
function genUser(context, events, done) {
	context.vars.username = faker.internet.username(faker.person.firstName(), faker.person.lastName())
	context.vars.password = `${faker.internet.password()}`
	//console.log("[generate-user] - " + context.vars.username + " | " + context.vars.password)
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
		//console.log("[process-generate-user] - Saved User: " + JSON.stringify(users))
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
		let roleRequest = JSON.parse(response.body).filter(r => {
			if (r.username === context.vars.username)
				return r
		})[0]
		if (roleRequest != undefined)
			context.vars.role_request_id = roleRequest.requestID
		//console.log(roleRequest.requestID)
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
		//console.log("[select-user] - " + user.username + " | " + user.password)
		if (sessions.has(user.username)) {
			context.vars.session_cookie = sessions.get(user.username)
			//console.log("[select-user-session] - " + sessions.get(user.username))
		}
	}
	return done()
}

/**
 * Generate data for a new triplestore
 */
function genTriplestore(context, events, next) {
	context.vars.triplestoreID = `${faker.string.nanoid()}`
	//console.log("[generate-triplestore] - " + context.vars.triplestoreID)
	return next()
}

/**
 * Prepares dataset upload
 */
function uploadABox(requestParams, context, events, next) {
	const form = new FormData()
	form.append('issuer', context.vars.username)
	form.append('triplestoreID', context.vars.triplestoreID)
	form.append('syntax', "rdf/xml")
	form.append('contents', datasets.get(context.vars.dataset + ".owl"))
	requestParams.body = form
	return next()
}


/**
 * Prepares ontology upload
 */
function uploadTBox(requestParams, context, events, next) {
	const form = new FormData()
	form.append('issuer', context.vars.username)
	form.append('triplestoreID', context.vars.triplestoreID)
	form.append('syntax', "rdf/xml")
	form.append('contents', ontologies.get('lubm-ontology.owl'))
	requestParams.body = form
	return next()
}


function processTriplestoreSize(requestParams, response, context, events, next) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		events.emit("histogram", context.vars.version + "." + context.vars.dataset + ".size", JSON.parse(response.body))
	}
	return next()
}

/**
 * Extracts the triplestore list
 */
function extractTriplestoreList(requestParams, response, context, events, next) {
	context.vars.triplestoreList = []
	if (response.statusCode >= 200 && response.statusCode < 300) {
		context.vars.triplestoreList = JSON.parse(response.body)
	}
	return next()
}

/**
 * Select a random triplestore from the list of available
 */
function selectTriplestoreFromList(context, events, next) {
	if (typeof context.vars.triplestoreList !== 'undefined' && context.vars.triplestoreList.length > 0)
		context.vars.triplestoreID = context.vars.triplestoreList.sample()
	else
		delete context.vars.triplestoreID
	return next()
}

/**
 * Prepares LUBM query
 */
function genLUBMQuery(requestParams, context, events, next) {
	let query = queries.get(context.vars.queryName)
	const form = new FormData()
	form.append('issuer', context.vars.username)
	form.append('triplestoreID', context.vars.triplestoreID)
	form.append('query', query.body)
	form.append('inference', "true")
	form.append('transitivityDepth', query.transitivityDepth)
	form.append('expansionDepth', query.expansionDepth)
	requestParams.body = form
	return next()
}

/**
 * Processes SPARQL query answer
 * Assertion: Bindings are sorted w/ no duplicate bindings
 */
function processLUBMQueryAnswer(requestParams, response, context, events, next) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		if (context.vars.dataset == 'lubm-1') {
			if (response.statusCode >= 200 && response.statusCode < 300) {
				let referenceAnswer = answers.get(context.vars.queryName)
				let receivedAnswer = JSON.parse(response.body)
				let receivedVars = createSet(receivedAnswer.head.vars)
				let referenceVars = createSet(referenceAnswer.head.vars)
				if (receivedVars.symmetricDifference(referenceVars).size == 0) {
					let referenceBindings = createSet(referenceAnswer.results.bindings)
					let receivedBindings = createSet(receivedAnswer.results.bindings)
					let correctBindings = receivedBindings.intersection(referenceBindings)
					let completeness = referenceBindings.size > 0 ? (correctBindings.size / referenceBindings.size) * 100 : 100
					let soundness = receivedBindings.size > 0 ? (correctBindings.size / receivedBindings.size) * 100 : 100
					events.emit("histogram", context.vars.queryName + ".completeness", completeness)
					events.emit("histogram", context.vars.queryName + ".soundness", soundness)
					events.emit("histogram", context.vars.queryName + ".received", receivedBindings.size)
					events.emit("histogram", context.vars.queryName + ".expected", referenceBindings.size)
				} else
					events.emit("counter", context.vars.queryName + ".wrong", 1)
			} else
				events.emit("counter", context.vars.queryName + ".wrong", 1)
		}
	} else {
		console.log("ERROR:" + JSON.stringify(response.body))
	}
	return next()
}

function createSet(items) {
	let set = new Set()
	items.forEach(item => {set.add(JSON.stringify(item))})
	return set
}