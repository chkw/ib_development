#!/usr/local/bin/python2.7
# debugging framework for CGI program
# parts taken from http://www.python.org/doc/essays/ppt/sd99east/sld001.htm

# imports

import os
import cgi

# output errors/exceptions to client
import cgitb
cgitb.enable()

import urlparse
import json

# methods and functions

def getQueryStringParams():
	"get a hash mapping of parameter key/value pairs from the query string.  requires urlparse module"
	query = os.environ[ "QUERY_STRING" ]
	pairs = urlparse.parse_qs( query )
	return pairs


def main():
	"main method: Outputs a JSON-RPC response to the client, which contains a result object and an error object.  If there is an error, then the error object will contain an error message instead of the null in the non-error state.  In addition to the main results of the program, the result object will contain a query object, which contains the parameters passed via URL query string."
	# must specify Content-type at beginning of output
	print "Content-type: text/html\n"
	try:
		# import the module that does the actual work
		#import worker

		# get params from query string
		pairs = getQueryStringParams()

		# query string params sent back to client
		resultJO = {'query':pairs}

		# initialize jsonDump to indicate no error
		jsonDump = {'error':None}
		
		# TODO write an error if parameter validation fails

		# TODO worker part of code should write results to resultJO

		# results returned to client in the "result" JSONObject
		jsonDump['result'] = resultJO

		# output jsonDump to client
		print json.dumps(jsonDump)


	except:
		print "<!-- --><hr><h1>Oops. An error occured.</h1>"

		# print traceback
		cgi.print_exception()

# main program section

# cgi.test()
main()
