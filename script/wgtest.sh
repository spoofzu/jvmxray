#!/bin/sh

# **************************************************************************
# WEBGOAT INTEGRATION TEST
#
# PREQUALIFICATIONS:  1) JVMXRAY INSTALLED, 2) WEBGOAT INSTALLED
#
# **************************************************************************

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

# JVMXRAY HOME DIRECTORY
export JVMXRAY_BASE="/Users/milton/"

# ALLOW SECURITYMANAGER.  UNCOMMENT AS REQUIRED 
#   java.lang.UnsupportedOperationException: The Security Manager is deprecated
#   and will be removed in a future release
export JAVA_SECURITYMGR="-Djava.security.manager=allow"

# CHANGE TO WEBGOATS DIRECTORY
cd ../../WebGoat/ || { echo "Error changing directory"; exit 1; }

# EXECUTE WEBGOAT W/JVMXRAY
export MAVEN_OPTS="-javaagent:../jvmxray/agent/target/agent-0.0.1-agent-w-deps.jar -Djvmxray.base=$JVMXRAY_BASE $JAVA_SECURITYMGR"
mvn spring-boot:run || { cd - >/dev/null; exit 1; }

# NORMAL EXIT
cd - >/dev/null