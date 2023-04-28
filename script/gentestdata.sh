#!/bin/sh
#
# **************************************************************************
# GENERATE TEST DATA
# This tool generates test data for data science experiements.  The tool
# is benefitial since the event logs generated include no access to
# senstive production data.  The tool generates 100k logged events by
# default but update static constant RECORDS_MAX = 100000 and recompile
# to create different sized test data.
# **************************************************************************
#
# UPDATE:  JVMXRAY_HOME is where you want xrays runtime properties, data, to live.
export JVMXRAY_HOME="/Users/milton/jvmxray-agent/"
#
# UPDATE: Uncomment to use your own logging configuration or leave commented to use
#         the default include in the jar.
#export LOGCONFIG="/Users/milton/jvmxray-agent/logconfig/"
#
java -cp "$LOGCONFIG:../integration-tests/target/test-classes/:../jvmxray-agent/target/jvmxray-agent-0.0.1-jar-with-dependencies.jar" org.jvmxray.test.bin.GenRandomBigData \
--agentbasepath="$JVMXRAY_HOME"