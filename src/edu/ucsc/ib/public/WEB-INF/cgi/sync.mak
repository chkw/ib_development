
REMOTE_PATH = hustle.cse.ucsc.edu:/projects/sysbio/apps/java/IB-Nightly/ib_cgi

LOCAL_PATH = .

test:

sync_export:
	rsync --existing -avP $(LOCAL_PATH)/* $(REMOTE_PATH)/.

sync_import:
	rsync --existing -avP $(REMOTE_PATH)/* $(LOCAL_PATH)/.

