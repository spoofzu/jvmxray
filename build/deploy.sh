#!/usr/bin/env bash
#
# deploy.sh: Automates deployment of a tagged release in Travis CI.
# Runs Maven deploy when a new tag is pushed, configuring Git credentials
# and setting the version based on the tag.

# Exit on error, treat unset variables as errors, and prevent pipeline errors
set -euo pipefail

# Log a message to stderr
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" >&2
}

# Validate required environment variables and tools
check_prerequisites() {
    if [[ -z "${TRAVIS_TAG:-}" ]]; then
        log "No TRAVIS_TAG set. Skipping deployment."
        exit 0
    fi

    # Validate tag format (e.g., v1.2.3 or 1.2.3)
    if ! [[ "$TRAVIS_TAG" =~ ^(v?[0-9]+\.[0-9]+\.[0-9]+)$ ]]; then
        log "Error: TRAVIS_TAG '$TRAVIS_TAG' does not match expected format (e.g., v1.2.3 or 1.2.3)."
        exit 1
    fi

    # Check for required tools
    command -v git >/dev/null 2>&1 || { log "Error: Git is not installed."; exit 1; }
    command -v mvn >/dev/null 2>&1 || { log "Error: Maven is not installed."; exit 1; }
}

# Configure Git credentials for Maven release
configure_git() {
    local git_email="${GIT_EMAIL:-noreply@travisci.com}"
    local git_name="${GIT_NAME:-JVMXRay BuildBot (via TravisCI)}"

    log "Configuring Git credentials: $git_name <$git_email>"
    git config --global user.email "$git_email"
    git config --global user.name "$git_name"
}

# Deploy the release using Maven
deploy_release() {
    log "Deploying release for tag: $TRAVIS_TAG"

    mvn deploy \
        --batch-mode \
        -DskipTests=true \
        -DnewVersion="$TRAVIS_TAG" \
        -Prelease \
        --settings settings.xml  # Optional: Use custom settings for credentials

    log "Deployment completed successfully for tag: $TRAVIS_TAG"
}

# Main execution
main() {
    check_prerequisites
    configure_git
    deploy_release
}

main "$@"