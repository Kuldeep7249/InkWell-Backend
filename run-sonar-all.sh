#!/usr/bin/env bash
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SONAR_HOST_URL="${SONAR_HOST_URL:-https://sonarcloud.io}"
MAVEN_GOALS="${MAVEN_GOALS:-clean verify sonar:sonar}"

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "SONAR_TOKEN is not set. Generate a token in SonarCloud and set SONAR_TOKEN before running scans."
  exit 1
fi

services=(
  "api-gateway"
  "auth"
  "category"
  "commentservice"
  "media"
  "notification"
  "post"
  "service-registry"
)

failed_services=()

for service in "${services[@]}"; do
  echo
  echo "=================================================="
  echo "Scanning ${service}"
  echo "=================================================="

  if (cd "${ROOT_DIR}/${service}" && mvn ${MAVEN_GOALS} -Dsonar.host.url="${SONAR_HOST_URL}" -Dsonar.token="${SONAR_TOKEN}"); then
    echo "SonarQube scan completed for ${service}"
  else
    echo "SonarQube scan failed for ${service}"
    failed_services+=("${service}")
  fi
done

echo
if (( ${#failed_services[@]} > 0 )); then
  echo "The following services failed: ${failed_services[*]}"
  exit 1
fi

echo "All SonarQube scans completed successfully."
