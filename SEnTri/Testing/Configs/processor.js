'use strict'

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
	extractCookie,
	genUser,
	genSPARQLQuery,
	genReadAccessRequest,
	genWriteAccessRequest,
	genRoleUpgradeRequest,
	genTriplestore,
	selectUser,
	selectNTriplestores,
	selectTriplestoreFromUser,
	random50
}

const Faker = require('faker')
const fs = require('fs')
const crypto = require('crypto');
const secret = crypto.randomBytes(64).toString('hex');

setTimeout(console.log, +Infinity)

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsRegExpr = [
	[/\/users\/auth/, "POST", "auth"],
	[/\/users/, "POST", "create-user"],
	[/\/users\/.*/, "DELETE", "delete-user"],
	[/\/users\/.*\/upgrade/, "POST", "upgrade-user"],
	[/\/users\/.*\/downgrade/, "POST", "downgrade-user"],
	[/\/users\/.*\/requests/, "POST", "list-role-requests"],
	[/\/users\/.*\/requests\/.*/, "PUT", "process-role-request"],

	[/\/triplestores\/.*\?write=.*\&read=.*\&owns=.*/, "GET", "list-triplestores"],
	[/\/triplestores\/.*\/access\/users\/.*/, "GET", "triplestore-list-users-with-access"],
	[/\/triplestores\/.*/, "GET", "triplestore-info"],
	[/\/triplestores\/.*\/owner\/.*/, "PUT", "triplestore-change-owner"],
	[/\/triplestores\/.*\/access\/requests\/.*/, "GET", "triplestore-list-pending-access-requests"],
	[/\/triplestores\/.*\/access\/requests\/.*/, "POST", "triplestore-access-request"],
	[/\/triplestores\/.*\/access\/requests\/.*/, "PUT", "triplestore-process-access-request"],
	[/\/triplestores\/.*\/access\/.*/, "PUT", "triplestore-grant-access"],
	[/\/triplestores\/.*\/access\/.*/, "DELETE", "triplestore-revoke-access"],

	//TODO: Encrypted triplestore endpoints
	//TODO: SPARQL Query Endpoints, separate by protocol, dataset, query
]

var datasets = []

var users = []
var queries = new Map()
var answers = new Map()
var nonEntailedAnswers = new Map()


global.myProcessEndpoint = function (str, method) {
	var i = 0
	for (i = 0; i < statsRegExpr.length; i++) {
		if (method == statsRegExpr[i][1] && str.match(statsRegExpr[i][0]) != null)
			return statsRegExpr[i][2]
	}
	return str
}

// Loads dataset from disk
function loadData() {
	if (fs.existsSync('./Data/LUBM/datasets.json')) {
		let datasetNames = []
		JSON.parse(fs.readFileSync('./Data/datasets.json', 'utf8')).forEach(i => { datasetNames.push(i) })
		fs.readdirSync('./Data/LUBM/datasets').forEach((data, i) => { datasets.set(datasetNames[i], data) })
	}
	if (fs.existsSync('./Data/users.json')) {
		JSON.parse(fs.readFileSync('./Data/users.json', 'utf8')).forEach(i => { users.push(i) })
	}
	if (fs.existsSync('./Data/LUBM/queries.json')) {
		JSON.parse(fs.readFileSync('./Data/users.json', 'utf8')).forEach(i => { users.push(i) })
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
 * Generate data for a new user using Faker
 */
function genUser(context, events, done) {
	context.vars.username = Faker.internet.userName(Faker.name.firstName(), Faker.name.lastName())
	context.vars.pwd = `${Faker.internet.password()}`
	return done()
}

/**
 * Stores new user data
 */
function processGenUserReply(requestParams, response, context, ee, next) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		users.push({
			"username": context.vars.username,
			"password": context.vars.password
		})
		fs.writeFileSync('./Data/users.json', JSON.stringify(users))
	}
	return next()
}


function processSPARQLQueryControlAnswer(response, context, events, next) {
	let dataset = context.vars.dataset
	let inference = context.vars.inference
	if (response.statusCode >= 200 && response.statusCode < 300) {
		let answerHash = crypto.createHmac('sha256', secret).update(response.body).digest('hex')
		let query = context.vars.query
		if (inference)
			saveSPARQLControlAnswer(events, dataset, answers, query, answerHash, JSON.parse(response.body))
		else
			saveSPARQLControlAnswer(events, dataset, nonEntailedAnswers, query, answerHash, JSON.parse(response.body))
	}
	return next()
}

function saveSPARQLControlAnswer(events, dataset, collection, query, answerHash, sparqlResults) {
	let datasetQueryAnswers = collection.get(dataset)
	if (datasetQueryAnswers == undefined)
		datasetQueryAnswers = new Map()
	let answer = datasetQueryAnswers.get(query)
	if (answer == undefined) {
		datasetQueryAnswers.set(query, {
			"hash": answerHash,
			"vars": sparqlResults.head.vars,
			"bindings": sparqlResults.results.bindings.map((binding) => JSON.stringify(binding))
		})
		collection.set(datasetQueryAnswers)
	} else if (answer.hash != answerHash)
		events.emit("counter", "diff_control", 1);
}

/**
 * Processes SPARQL query answer
 */
function processSPARQLQueryAnswer(response, context, events, next) {
	let dataset = context.vars.dataset
	let query = context.vars.query
	let inference = context.vars.inference
	if (response.statusCode >= 200 && response.statusCode < 300) {
		let nonEntailedAnswer = nonEntailedAnswers.get(dataset).get(query)
		let receivedAnswerHash = crypto.createHmac('sha256', secret).update(response.body).digest('hex')
		if (inference)
			generateLUBMMetrics(events, response.body, nonEntailedAnswer.bindings, answers.get(dataset).get(query), receivedAnswerHash)
		else
			generateLUBMNonEntailedMetrics(events, response.body, nonEntailedAnswer, receivedAnswerHash)
	}
	return next()
}

function generateLUBMMetrics(events, responseBody, nonEntailedBindings, expectedAnswer, receivedAnswerHash) {
	let completeness, soundness
	if (expectedAnswer.hash == receivedAnswerHash) {
		completeness = 1
		soundness = expectedAnswer.soundness
	} else {
		let receivedAnswer = JSON.parse(responseBody)
		if (hasEqualVars(expectedAnswer.vars, receivedAnswer.head.vars))
			[completeness, soundness] = calculateCompletnessAndSoundness(
				nonEntailedBindings,
				expectedAnswer.bindings,
				receivedAnswer.results.bindings.map((binding) => JSON.stringify(binding))
			);
	}
	if (completeness != undefined && soundness != undefined) {
		events.emit("histogram", "completeness", completeness);
		events.emit("histogram", "soundness", soundness);
	} else
		events.emit("counter", "wrong", 1);
}

function hasEqualVars(vars, receveivedVars) {
	return vars.length == receveivedVars.length && vars.every((v, i) => v == receveivedVars[i]);
}

//Assertion: Bindings are sorted w/ no duplicate bindings
function calculateCompletnessAndSoundness(nonEntailedBindings, expectedBindings, receivedBindings) {
	let completeness, soundness
	if (expectedBindings.length >= receivedBindings.length) {
		completeness = expectedBindings.filter(x => receivedBindings.includes(x)) / expectedBindings.length
		soundness = nonEntailedBindings.filter(x => receivedBindings.includes(x)) / receivedBindings.length
	}
	return [completeness, soundness]
}

function generateLUBMNonEntailedMetrics(events, responseBody, expectedAnswer, receivedAnswerHash) {
	let completeness
	if (expectedAnswer.hash == receivedAnswerHash)
		completeness = 1
	else {
		let receivedAnswer = JSON.parse(responseBody)
		if (hasEqualVars(expectedAnswer.vars, receivedAnswer.head.vars))
			completeness = calculateCompletness(
				expectedAnswer.bindings,
				receivedAnswer.results.bindings.map((binding) => JSON.stringify(binding))
			);
	}
	if (completeness != undefined)
		events.emit("histogram", "completeness", completeness);
	else
		events.emit("counter", "wrong", 1);
}

//Assertion: Bindings are sorted w/ no duplicate bindings
function calculateCompletness(expectedBindings, receivedBindings) {
	let completeness
	if (expectedBindings.length >= receivedBindings.length)
		completeness = expectedBindings.filter(x => receivedBindings.includes(x)) / expectedBindings.length
	return completeness
}


/**
 * Select an user.
 */
function selectUser(context, events, done) {
	if (users.length > 0) {
		let user = users.sample()
		context.vars.userID = user.userID
		context.vars.pwd = user.pwd
	} else {
		delete context.vars.userID
		delete context.vars.pwd
	}
	return done()
}


/**
 * Extracts the session cookie.
 */
function extractCookie(response, context, next) {
	if (response.statusCode >= 200 && response.statusCode < 300) {
		for (let header of response.rawHeaders) {
			if (header.startsWith("session")) {
				context.vars.session_cookie = header.split(';')[0];
			}
		}
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
 * Generate data for a new triplestore
 */
function genTriplestore(context, done) {
	context.vars.name = `${Faker.string.nanoid()}`
	return done()
}


/**
 * Select a random triplestore from the list of available
 */
function selectTriplestoreFromTriplestoreList(context, events, done) {
	if (typeof context.vars.triplestoreList !== 'undefined' && context.vars.triplestoreList.length > 0)
		context.vars.triplestoreID = context.vars.triplestoreList.sample()
	else
		delete context.vars.triplestoreID
	return done()
}

