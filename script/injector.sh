#!/bin/sh
#
# **************************************************************************
# EXECUTE JVMXRAY INJECTOR
# Use the injector to insert the agent into the target process to monitor.
# Once the agent is injected, the agent will start monitoring events.
# **************************************************************************
#
# UPDATE:  JVMXRAY_HOME is where you want xrays runtime properties, data, to live.
export JVMXRAY_HOME="/Users/milton/jvmxray-agent/"
#
# UPDATE: Uncomment to use your own logging configuration or leave commented to use
#         the default include in the jar.
#export LOGCONFIG="/Users/milton/jvmxray-agent/logconfig/"
#
# Uncomment to show debug gui or leave commented for unattended headless operation.
export DEBUGGUI="--debugui"
#
java -cp "$LOGCONFIG:../jvmxray-agent/target/jvmxray-agent-0.0.1-jar-with-dependencies.jar" org.jvmxray.agent.driver.jvmxrayinjector \
 "$DEBUGGUI" --agentbasepath="$JVMXRAY_HOME" --agentpayload="../jvmxray-agent/target/jvmxray-agent-0.0.1-jar-with-dependencies.jar"