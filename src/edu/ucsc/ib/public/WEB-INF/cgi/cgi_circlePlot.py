#!/usr/local/bin/python2.7
# debugging framework for CGI program
# parts taken from http://www.python.org/doc/essays/ppt/sd99east/sld001.htm
# JAN 2012 chrisw

# imports

import os
import cgi
import sys

# output errors/exceptions to client
import cgitb
cgitb.enable()

import urlparse
import json

import re
import subprocess

import sqlalchemy

from collections import defaultdict

import circlePlot

from time import time

# global vars
user = "slg"
password = "stuartLabGuest"
host = "localhost"
port = "3306"
dbname = "sysbioData"

metatable = "matrixlist"

outputDir = "../../circleOutput"

# methods and functions

def __getDbEngine():
	"""Get a DB engine for mysql using sqlalchemy."""
	## http://www.sqlalchemy.org/docs/dialects/mysql.html#module-sqlalchemy.dialects.mysql.mysqldb
	## mysql+mysqldb://<user>:<password>@<host>[:<port>]/<dbname>
	engine = sqlalchemy.create_engine('mysql+mysqldb://' + user + ':' + password + '@' + host + ':' + port + '/' + dbname)
	return engine

def getQueryStringParams():
	"""get a hash mapping of parameter key/value pairs from the query string.  requires urlparse module"""
	query = os.environ[ "QUERY_STRING" ]
	pairs = urlparse.parse_qs(query)
	return pairs


def getMatrixList(organism):
	"""Get a list of available score matrices and some properties about them."""

	# set up engine, metadata, and table
	engine = __getDbEngine()
	metadata = sqlalchemy.MetaData(engine)
	matrixListTable = sqlalchemy.Table(metatable, metadata, autoload=True, schema=dbname)

	# build up an sql statement
	connection = engine.connect()
	statement = sqlalchemy.sql.select([matrixListTable.c.name, matrixListTable.c.description, matrixListTable.c.category], matrixListTable.c.NCBI_species == organism)

	# execute sql statement
	result = connection.execute(statement)

	# get sql results
	matricesList = list()

	for row in result:
		# each row in the result is a dictionary keyed on column names
		matrixProps = {}
		matrixProps['name'] = row['name']
		matrixProps['description'] = row['description']
		matrixProps['category'] = row['category']

		matricesList.append(matrixProps)

	result.close()

	connection.close()

	return matricesList


def getTableNamesDict(matrixNameList):
	"""Get the names of a tables from the matrix names.  A dictionary is returned."""

	# set up engine, metadata, and table
	engine = __getDbEngine()
	metadata = sqlalchemy.MetaData(engine)
	matrixListTable = sqlalchemy.Table(metatable, metadata, autoload=True, schema=dbname)

	# build up an sql statement
	connection = engine.connect()

	tableNameDict = {}

	for matrixName in matrixNameList:
		statement = sqlalchemy.sql.select([matrixListTable.c.name, matrixListTable.c.tableName], matrixListTable.c.name == matrixName)

		# execute sql statement
		result = connection.execute(statement)

		# each row in the result is a dictionary keyed on column names
		row = result.fetchone()
		tableNameDict[matrixName] = row['tableName']

		result.close()

	connection.close()

	return tableNameDict


def getFeatureListSingle(matrixTableName):
	"""Get the list of features available in the named matrix table."""

	# set up engine, metadata, and table
	engine = __getDbEngine()
	metadata = sqlalchemy.MetaData(engine)
	matrixTable = sqlalchemy.Table(matrixTableName, metadata, autoload=True, schema=dbname)

	# build up an sql statement
	connection = engine.connect()

	statement = sqlalchemy.sql.select([matrixTable.c.feature])

	# execute sql statement
	result = connection.execute(statement)

	featureList = list()
	for row in result:
		featureList.append(row['feature'])

	result.close()
	connection.close()

	return featureList


def getFeatureListMulti(matrixNameList):
	"""Get the list of features available in any of the tables named in the list."""
	featuresDict = defaultdict(int)

	tableNamesDict = getTableNamesDict(matrixNameList)

	for matrixName in tableNamesDict.keys():
		matrixTableName = tableNamesDict[matrixName]
		featureList = getFeatureListSingle(matrixTableName)
		for feature in featureList:
			featuresDict[feature] += 1

	return featuresDict.keys()

def getSampleNamesListSingle(matrixTableName):
	"""Get a list of sample names for a score matrix."""

	# set up engine, metadata, and table
	engine = __getDbEngine()
	metadata = sqlalchemy.MetaData(engine)
	matrixListTable = sqlalchemy.Table(metatable, metadata, autoload=True, schema=dbname)

	# build up an sql statement
	connection = engine.connect()
	statement = sqlalchemy.sql.select([matrixListTable.c.vector], matrixListTable.c.tableName == matrixTableName)

	# execute sql statement
	result = connection.execute(statement)

	# get sql results
	row = result.fetchone()

	sampleNamesList = row['vector'].split(',')

	result.close()
	connection.close()

	return sampleNamesList

def getSampleNamesDictMulti(matrixNameList):
	"""Get the list of sample names for matrices.  Returns a dictionary with matrix names as keys.  The value for each key is a list of sample names in the matrix.  The order of the names is meaningful, as they are in the order that they appear in the original sample data vector."""
	sampleNamesDict = defaultdict(int)

	tableNamesDict = getTableNamesDict(matrixNameList)

	for matrixName in tableNamesDict.keys():
		matrixTableName = tableNamesDict[matrixName]
		sampleNamesList = getSampleNamesListSingle(matrixTableName)

		sampleNamesDict[matrixName] = sampleNamesList

	return sampleNamesDict


def getFeatureSampleScoresDict_single(matrixTableName, featureList, sampleNameList):
	"""Get score data for each feature in featureList.  The names in sampleNameList MUST match up with the scores in the dbTable's vector column.  The result is a dictionary of feature:sampleName:sampleScoreValue."""
	# set up engine, metadata, and table
	engine = __getDbEngine()
	metadata = sqlalchemy.MetaData(engine)
	dbTable = sqlalchemy.Table(matrixTableName, metadata, autoload=True, schema=dbname)

	# build up an sql statement
	connection = engine.connect()

	featureScoresDict = {}

	for feature in featureList:
		statement = sqlalchemy.sql.select([dbTable.c.feature, dbTable.c.vector], dbTable.c.feature == feature)

		result = connection.execute(statement)

		row = result.fetchone()

		# what if matrix has no row for this feature
		if (row is None): 
			result.close()
			continue
		
		scoresList = row['vector'].split(",")

		scoresDict = {}
		# sampleNames and score values MUST share the same index in their respective lists.
		for index in range(len(sampleNameList)):
#			scoresDict[sampleNameList[index]] = float(scoresList[index])
			try:
				scoresDict[sampleNameList[index]] = float(scoresList[index])
			except ValueError:
				# if not a number, store raw value
				scoresDict[sampleNameList[index]] = scoresList[index]

		featureScoresDict[feature] = scoresDict

		result.close()

	connection.close()

	return featureScoresDict


def getFeatureSampleScoresDict(featureList, sampleNamesDict):
	"""Get the sample scores for matrices.  sampleNamesDict is a dictionary of a list of sample names keyed on a matrixName."""
	tableNamesDict = getTableNamesDict(sampleNamesDict.keys())

	# matrixName:feature:sample:score
	scoresDict = {}

	for matrixName in tableNamesDict.keys():
		matrixTableName = tableNamesDict[matrixName]
		sampleNameList = sampleNamesDict[matrixName]

		featureScoresDict = getFeatureSampleScoresDict_single(matrixTableName, featureList, sampleNameList)

		scoresDict[matrixName] = featureScoresDict

	return scoresDict


def main():
	"""main method: Outputs a JSON-RPC response to the client, which contains a result object and an error object.  If there is an error, then the error object will contain an error message instead of the null in the non-error state.  In addition to the main results of the program, the result object will contain a query object, which contains the parameters passed via URL query string."""
	# must specify Content-type at beginning of cgi output
	print "Content-type: text/html\n"
	try:
		start = time()
		time0 = start

		# get params from query string
		query_pairs = getQueryStringParams()

		resultJO = dict()

		# query string params sent back to client
		# resultJO = {'query':query_pairs}

		# initialize jsonDump to indicate no error
		jsonDump = {'error':None}

		# TODO write an error if parameter validation fails


		############# SERVICE PARAMS #############

		# default method is to do a listing
		method = 'list'
		if query_pairs.has_key('method'):
			method = query_pairs['method'][0]

		############# SERVICES #############

		if (method.lower() == "list"):
			# handle query for available matrices
			# test with http://sysbio.soe.ucsc.edu/nets/cgi-bin/cgi_circlePlot.py?method=list&organism=9606

			# default organism is ''
			organism = ''
			if query_pairs.has_key('organism'):
				organism = query_pairs['organism'][0]

			resultJO['matrices'] = getMatrixList(organism)

			resultJO['elapsed'] = time() - start

		elif (method.lower() == "getimages"):
			#test with http://sysbio.soe.ucsc.edu/nets/cgi-bin/cgi_circlePlot.py?matrixPriority=tcgaBRCA_Expression_vCohort,tcgaBRCA_Expression_vNormal&orderFeature=MAP2K4&method=getImages&features=PSEN2,MAP2K4,THPO,DYNLT3,PPWD1,VEZT,SLC5A9,TLR2,C12orf48,RUSC1,C1orf150,GCHFR,ATP1A1,EXOC3,PRPF4B,TTK,PDE6D,OR10T2,NTS,EXTL3,SHC3&matrixNameList=tcgaBRCA_Expression_vNormal,tcgaBRCA_Expression_vCohort

			# Here is the help for circlePlot.py:
			# circlePlot.py:
			#
			# Usage:
			#   circlePlot.py [options] outputDir inputFile [inputFile ...]
			#
			# Options:
			#   -s str        list file containing samples to include
			#   -f str        list file containing features to include
			#   -o str        feature;file[,file ...] or feature
			#   -c str        file to use as center colors
			#   -l            print the feature identifier in the circle or not (default: FALSE)
			#   -q            run quietly

			# default matrix name is ''
			matrixDisplayOrder = list()
			if query_pairs.has_key('matrixNameList'):
				matrixDisplayOrder = (query_pairs['matrixNameList'][0]).split(",")
			
			resultJO['matrixDisplayOrder'] = matrixDisplayOrder

			# get features
			featuresList = list()
			if query_pairs.has_key('features'):
				featuresList = (query_pairs['features'][0]).split(",")
			
			# orderFeature will be the first one in the list by default
			# sample ordering is based on the orderFeature
			orderFeature = featuresList[0]
			if query_pairs.has_key('orderFeature'):
				orderFeature = query_pairs['orderFeature'][0]
			
			resultJO['orderFeature'] = orderFeature
			
			# matrix sorting priority
			# If there are ties in sample ordering, the orderFeature's sample score in the next matrix is used.
			matrixPriorityList = matrixDisplayOrder
			if query_pairs.has_key('matrixPriority'):
				matrixPriorityList = query_pairs['matrixPriority'][0].split(",")

			resultJO['matrixPriority'] = matrixPriorityList
			
			cohortMinMax = False
			if (query_pairs.has_key('cohortMinMax')) and (query_pairs['cohortMinMax'][0].lower() == "true"):
					cohortMinMax = True

			resultJO['cohortMinMax'] = str(cohortMinMax)
			
			# get featuresList as set
			featureSet = set(featuresList)

			# get sample names lists for each matrix
			sampleNamesDict = getSampleNamesDictMulti(matrixDisplayOrder)
			
			# get the set of all sample names
			sampleNamesSet = set()
			for sampleNamesList in sampleNamesDict.values():
				sampleNamesSet = sampleNamesSet.union(set(sampleNamesList))

			# get sample values for features
			# dataMatrixDict --> matrixName:feature:sampleName:sampleScore
			dataMatrixDict = getFeatureSampleScoresDict(featuresList, sampleNamesDict)
			#resultJO['dataMatrix'] = dataMatrixDict
			
			# rework parameters to be compatible with circlePlot.py
			# circlePlot.py expects dict[sampleName][featureName]=score for each matrix
			circleDataList = []
			for matrixName in matrixDisplayOrder:
				oldMatrix = dataMatrixDict[matrixName]
				reworkedMatrix = dict()
				
				for feature in oldMatrix.keys():
					samplesDict = oldMatrix[feature]
					for sample in samplesDict.keys():
						score = samplesDict[sample]
						
						if sample not in reworkedMatrix:
							reworkedMatrix[sample] = dict()
						
						reworkedMatrix[sample][feature] = score
				
				circleDataList.append(reworkedMatrix)

			#resultJO['circleDataList'] = circleDataList
			
			resultJO['elapsed_prep'] = time() - time0

			time0 = time()

			# business part of code should write results to resultJO
			(circleImageFileDict, sortedSamplesNames) = circlePlot.cgi_routine(outputDir, matrixDisplayOrder, circleDataList, list(sampleNamesSet), list(featureSet), orderFeature, matrixPriorityList=matrixPriorityList, printLabel=False, cohortMinMax=cohortMinMax)

			resultJO['elapsed_drawing'] = time() - time0

			time0 = time()

			resultJO['circleImageURLs'] = circleImageFileDict
			
			resultJO['sortedSamplesNames'] = sortedSamplesNames

			resultJO['elapsed_total'] = time() - start
		# results returned to client in the "result" JSONObject
		jsonDump['result'] = resultJO

		# output jsonDump to client
		print json.dumps(jsonDump)


	except:
		print "<!-- --><hr><h1>Oops. An error occured.</h1>"

		# print traceback
		cgi.print_exception()

# main program section

# Use the following line in place of main() to do some CGI debugging.
#cgi.test()
main()
