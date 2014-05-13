#!/bin/sh
# 13MAY14 chrisw
# Stop disco's Tomcat service if it is running.

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
	echo "$TOMCAT_SERVICE will be stopped"
	$TOMCAT_SERVICE/bin/stop
else
	echo "$TOMCAT_SERVICE not running" > /dev/null
fi

