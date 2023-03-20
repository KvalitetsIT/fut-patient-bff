#!/bin/sh

echo "${GITHUB_REPOSITORY}"
echo "${DOCKER_SERVICE}"
if [ "${GITHUB_REPOSITORY}" != "KvalitetsIT/fut-patient-bff" ] && [ "${DOCKER_SERVICE}" = "kvalitetsit/fut-patient-bff" ]; then
  echo "Please run setup.sh REPOSITORY_NAME"
  exit 1
fi
