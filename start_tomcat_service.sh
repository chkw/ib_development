#!/bin/sh
# 13MAY14 chrisw
# Start disco's Tomcat service if it is not running.

HOST_NAME=disco
TOMCAT_SERVICE=/projects/sysbio/apps/java/tomcat;

if hostname | grep $HOST_NAME > /dev/null
then
  echo "host is $HOST_NAME" > /dev/null
else
  echo "This machine is NOT $HOST_NAME.  Exit."
  exit
fi

if ps -Af | grep -v 'grep' | grep $TOMCAT_SERVICE > /dev/null
then
	echo "$TOMCAT_SERVICE running" > /dev/null
else
	echo "$TOMCAT_SERVICE needs to be started"
	$TOMCAT_SERVICE/bin/start
fi

