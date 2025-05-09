#!/bin/bash

# Script to run WebGoat with a JVMXRay Agent
# Author: Milton Smith
# Date: April 24, 2025
# Purpose: Configures and runs WebGoat with the JVMXRay Agent.  If your running Xray in other
#   scenarios and sending logs to other places, like your centralized log server, you will
#   need to make improvements to meet your requirements.
#
# Base configuration variables
#
# Base folder where GIT is installed.  Xray and WebGoat clonned repos should be here, if your testing.
GIT_DIR="/Users/milton/tmp"
#
# Base folder for Xray's log output, xray configs, etc.
JVMXRAY_BASE="${GIT_DIR}/jvmxraybase"
#
# WebGoat cloned repo, if your testing
WEBGOAT_DIR="${GIT_DIR}/WebGoat"
#
# Xray logs.  We use it to store WebGoat console log in same folder.
LOG_DIR="${JVMXRAY_BASE}/jvmxray/logs"
#
# WebGoat console log.  Rewritten each run.
WEB_GOAT_CONSOLE_LOG="${LOG_DIR}/maven.log"
#
# Location of the shaded Xray Agent JAR
AGENT_JAR="${GIT_DIR}/jvmxray/agent/target/agent-0.0.1-shaded.jar"
#
# You shouldn't need to modify anything below here for simple testing.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MVN_COMMAND="mvn spring-boot:run"
JVM_ARGS="-javaagent:${AGENT_JAR} -Djvmxray.base=${JVMXRAY_BASE}"

# Function to log messages
log_message() {
    local message="$1"
    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $message" >> "${WEB_GOAT_CONSOLE_LOG}" 2>/dev/null || echo "[$timestamp] $message"
}

# Function to check if a file or directory exists
check_path() {
    local path="$1"
    local type="$2"  # 'file' or 'dir'
    if [ "$type" = "file" ] && [ ! -f "$path" ]; then
        log_message "ERROR: File $path does not exist."
        exit 1
    elif [ "$type" = "dir" ] && [ ! -d "$path" ]; then
        log_message "ERROR: Directory $path does not exist."
        exit 1
    fi
}

# Function to initialize log file
init_log() {
    log_message "Initializing log file: ${WEB_GOAT_CONSOLE_LOG}"
    if ! mkdir -p "${LOG_DIR}"; then
        log_message "ERROR: Failed to create log directory ${LOG_DIR}"
        exit 1
    fi
    # Remove existing log file if it exists
    if [ -f "${WEB_GOAT_CONSOLE_LOG}" ]; then
        rm "${WEB_GOAT_CONSOLE_LOG}" 2>/dev/null || {
            log_message "ERROR: Failed to remove existing log file ${WEB_GOAT_CONSOLE_LOG}"
            exit 1
        }
    fi
    # Create new log file
    touch "${WEB_GOAT_CONSOLE_LOG}" 2>/dev/null || {
        log_message "ERROR: Failed to create log file ${WEB_GOAT_CONSOLE_LOG}"
        exit 1
    }
}

# Trap errors and exit gracefully
trap 'log_message "ERROR: Script terminated unexpectedly."; exit 1' ERR

# Initialize script
log_message "Starting WebGoat with JVMXRay agent..."

# Initialize log file
init_log

# Validate prerequisites
check_path "${WEBGOAT_DIR}" "dir"
check_path "${AGENT_JAR}" "file"

# Check if already in WebGoat directory
CURRENT_DIR="$(pwd)"
if [ "${CURRENT_DIR}" != "${WEBGOAT_DIR}" ]; then
    log_message "Changing to WebGoat directory: ${WEBGOAT_DIR}"
    if ! cd "${WEBGOAT_DIR}"; then
        log_message "ERROR: Failed to change to ${WEBGOAT_DIR}"
        exit 1
    fi
else
    log_message "Already in WebGoat directory: ${WEBGOAT_DIR}"
fi

# Check if Maven is installed
if ! command -v mvn >/dev/null 2>&1; then
    log_message "ERROR: Maven is not installed or not found in PATH."
    exit 1
fi

# Run Maven Spring Boot with agent
log_message "Running Maven Spring Boot with JVMXRay agent..."
if ! ${MVN_COMMAND} -Dspring-boot.run.jvmArguments="${JVM_ARGS}" >> "${WEB_GOAT_CONSOLE_LOG}" 2>&1; then
    log_message "ERROR: Failed to run Maven Spring Boot."
    exit 1
fi

# Return to original directory if we changed directories
if [ "${CURRENT_DIR}" != "${WEBGOAT_DIR}" ]; then
    log_message "Returning to original directory: ${CURRENT_DIR}"
    if ! cd "${CURRENT_DIR}"; then
        log_message "ERROR: Failed to return to original directory."
        exit 1
    fi
else
    log_message "No need to return to original directory (already in WebGoat root)."
fi

log_message "Script completed successfully."
exit 0