#!/usr/local/bin/python2.7
# Hello world python program

# imports

import os
import cgi

# output errors/exceptions to client
import cgitb
cgitb.enable()

import urlparse

# methods & functions

def printHeader( title ):
	"print HTML header with specified title"
	print """Content-type: text/html

<?xml version = "1.0" encoding = "UTF-8"?>
<html>
<head><title>%s</title></head>
<body>""" % title

def getQueryStringParams(  ):
	"get a hash mapping of parameter key/value pairs from the query string.  requires urlparse module"
	query = os.environ[ "QUERY_STRING" ]
	pairs = urlparse.parse_qs( query )
	return pairs

def main():
	"main method"
	printHeader( "Hello with query string parameters" )

	print "<h1>Hello world!</h1>"

	print "<h1>Name/Value Pairs</h1>"

	query = os.environ[ "QUERY_STRING" ]

	if len( query ) == 0:
		print """<paragraph><br />
			Please add some name-value pairs to the URL above.
			Or try
			<a href = "hello.py?name=YourName&amp;age=23">this</a>.
			</paragraph>"""
	else:
		print """<paragraph style = "font-style: italic">
			The query string is '%s'.</paragraph>""" % cgi.escape( query )
		# pairs = cgi.parse_qs( query )
		pairs = getQueryStringParams()
		for key, value in pairs.items():
			print "<paragraph>You set '%s' to value %s</paragraph>" % ( key, value )

# main program section

# cgi.test()
main()
