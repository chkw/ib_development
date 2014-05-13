## This makefile should update the local IB code repo and then compile/deploy to tomcat web app container.

THIS_DIR_PATH = $(shell pwd)

TOMCATPATH = /projects/sysbio/apps/java/tomcat
WEBAPPSPATH = $(TOMCATPATH)/webapps

PRODUCTION_WAR_DIR = ROOT
WAR_DIR = ib_dev
PROPERTIESDIR = $(WAR_DIR)/WEB-INF/classes/edu/ucsc/ib/server/
PROPERTIESFILE = ib.properties

UIBUILDDIR = uibuild
UIBUILDCLASS = edu.ucsc.ib.NetVizTest

UICOMPILECLASS = com.google.gwt.dev.Compiler
UICOMPILEFLAGS = -style OBF -war $(UIBUILDDIR) $(UIBUILDCLASS) -logLevel TRACE

CLIENT_SOURCE_DIR = src/edu/ucsc/ib/client
SERVER_SOURCE_DIR = src/edu/ucsc/ib/server

#SERVERCOMPILETARGETS = src/edu/ucsc/ib/server/*.java
SERVERCOMPILETARGETS = $(shell find src/edu/ucsc/ib/server -name "*.java")
SERVERBUILDDIR = serverbuild

GWT_DIR = /projects/sysbio/apps/java/gwt
GWT_JARS = $(GWT_DIR)/gwt-dev.jar:$(GWT_DIR)/gwt-user.jar:$(GWT_DIR)/validation-api-1.0.0.GA.jar:$(GWT_DIR)/validation-api-1.0.0.GA-sources.jar

WEBINF_LIB_DIR = src/edu/ucsc/ib/public/WEB-INF/lib

JSON_JAR = $(WEBINF_LIB_DIR)/json.jar
Commons_IO_JAR = $(WEBINF_LIB_DIR)/commons-io-1.4.jar
Commons_FileUpload_JAR = $(WEBINF_LIB_DIR)/commons-fileupload-1.2.1.jar
XGMML_JAR = $(WEBINF_LIB_DIR)/xgmml.jar
BATIK_JARS = $(WEBINF_LIB_DIR)/batik-awt-util.jar:$(WEBINF_LIB_DIR)/batik-svggen.jar:$(WEBINF_LIB_DIR)/batik-dom.jar:$(WEBINF_LIB_DIR)/batik-xml.jar:$(WEBINF_LIB_DIR)/batik-util.jar:$(WEBINF_LIB_DIR)/batik-ext.jar)

CLASSPATH = $(GWT_JARS):$(JSON_JAR):$(Commons_IO_JAR):$(Commons_FileUpload_JAR):$(XGMML_JAR):src:$(SERVERBUILDDIR):$(BATIK_JARS)
export CLASSPATH

IB_CGI_WORKING_DIR = $(THIS_DIR_PATH)/ib_cgi

URL = https://github.com/chkw/ib_development.git
BRANCH = master

test:

#: deploy to production
deploy_to_prod: war 
	stop_tomcat_service.sh ;
	\
	echo "remove previous webapp" ;
	\
	rm -rf $(WEBAPPSPATH)/$(PRODUCTION_WAR_DIR) ;
	\
	echo "copy new webapp" ;
	\
	cp -r $(WAR_DIR) $(WEBAPPSPATH)/$(PRODUCTION_WAR_DIR) ;
	\
	cat $(WAR_DIR)/ibWelcome.html \
	| sed -e 's,ibTutorial01.html,/nets/ibTutorial01.html,' \
	| sed -e 's,NetVizTest.html,/nets/NetVizTest.html,' \
	| sed -e 's,ibTutorialCircleMap01.html,/nets/ibTutorialCircleMap01.html,' \
	| sed -e 's,ibTutorialLoadData01.html,/nets/ibTutorialLoadData01.html,' \
	> 1.tmp ;
	\
	mv 1.tmp $(WEBAPPSPATH)/$(PRODUCTION_WAR_DIR)/ibWelcome.html ;
	\
	start_tomcat_service.sh ;

#: java classpath
classpath:
	echo $(CLASSPATH)

#: GWT version
gwt_version:
	java $(UICOMPILECLASS) -version

#: deploy webapp to tomcat server
deploy: war
	stop_tomcat_service.sh ;
	\
	echo "remove previous webapp" ;
	\
	rm -rf $(WEBAPPSPATH)/$(WAR_DIR) ;
	\
	echo "copy new webapp" ;
	\
	cp -r $(WAR_DIR) $(WEBAPPSPATH)/$(WAR_DIR) ;
	\
	start_tomcat_service.sh ;
	\

#: pack webapp into a war
pack_war:
	jar cfv $(WAR_DIR).war $(WAR_DIR)

#: create war directory
war: compile
	rm -rf $(WAR_DIR) ;
	\
	echo "install ib properties"
	mkdir -p $(PROPERTIESDIR)
	cp $(PROPERTIESFILE) $(PROPERTIESDIR)
	echo "copy serverside"
	cp -r $(SERVERBUILDDIR)/* $(WAR_DIR)/WEB-INF/classes
	echo "copy clientside"
	cp -r $(UIBUILDDIR)/$(UIBUILDCLASS)/* $(WAR_DIR)
	\
	mkdir -p $(WAR_DIR)/circleOutput

#: compile server classes
compile_server:
	rm -rf $(SERVERBUILDDIR)
	mkdir -p $(SERVERBUILDDIR)
	javac -d $(SERVERBUILDDIR) \
		$(SERVERCOMPILETARGETS)

#: compile GWT UI
compile_ui:
	rm -rf $(UIBUILDDIR)
	java $(UICOMPILECLASS) $(UICOMPILEFLAGS)

#: compile server and UI
compile: compile_server compile_ui

#: pull from code repository 
update:
	git pull $(URL) $(BRANCH)

#: ##################################

clean_war:
	rm -rf $(WAR_DIR)

clean_builds:
	rm -rf $(SERVERBUILDDIR) $(UIBUILDDIR)

clean_all: clean_war clean_builds
