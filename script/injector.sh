#!/bin/sh
#
# **************************************************************************
# EXECUTE JVMXRAY INJECTOR
#
# The injector is used to insert the Agent into the target process to
#   monitor in near-realtime.  Once the agent is injected, monitoring will
#   begin.
#
# Note: use of the injector is optional or helpful during
#   development/debugging.  The usual method of deploying the agent into
#   the target to monitor is via the --javaagent command line switch
#   when starting the target.
#
# **************************************************************************
#
# JVMXRAY_BASE is root of the 'jvmxray' folder. The path is used by
#    all modules.  For example, your base path may look like the following,
#    $JVMXRAY_BASE/
#       +jvmxray/
#          +agent/
#             agent.properties
#          +injector/
#             injector.properties
#          +logs/
#             jvmxray-agent-events.log
#             jvmxray-agent-platform.log
#          logback.xml
#
# The directory structure from jvmxray/.. and all the files are created by
# jvmxray by default.
export JVMXRAY_BASE="/Users/milton/"
#
# Uncomment to show debug gui or leave commented for unattended headless operation.
export DEBUGGUI="--debugui"
#
java -cp "../agent/target/agent-0.0.1-jar-with-dependencies.jar" org.jvmxray.platform.agent.bin.jvmxrayinjector \
 "$DEBUGGUI" --agentbasepath="$JVMXRAY_BASE" --agentpayload="../agent/target/agent-0.0.1-jar-with-dependencies.jar"