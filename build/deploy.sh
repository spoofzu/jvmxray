#!/usr/bin/env bash
#

# -e exit on error
# -x print command prior to execution (warn: info leakage)
set -e

#
# Process if new tag assigned
#
if [ ! -z "$TRAVIS_TAG" ]; then
	
	echo "*** deploy.sh, deploying release."
	
	# Required by mvn release:prepare, fatal: empty ident name <> not allowed
	#
	git config --global user.email "noreply@travisci.com"
	git config --global user.name "JVMXRay BuildBot (via TravisCI)"
	
	# 
	# Assign pom.xml from $TRAVIS_TAG
	#
	mvn deploy \
    	-DskipTests=true \
		--batch-mode \
		-DnewVersion="$TRAVIS_TAG" \
		-Prelease \
	    -X

	echo "*** deploy.sh, deployment complete."
fi