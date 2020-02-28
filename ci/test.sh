#!/bin/bash
set -e # fail fast

#H Usage:
#H test.sh -h | test.sh --help
#H
#H prints this help and exits
#H
#H test.sh <database|module> [sonar project key]
#H
#H   tests the taskana adapter application. See documentation for further testing details.
#H
#H database:
#H   - H2
#H sonar project key:
#H   the key of the sonarqube project where the coverage will be sent to.
#H   If empty nothing will be sent

# Arguments:
#   $1: exit code
function helpAndExit() {
  cat "$0" | grep "^#H" | cut -c4-
  exit "$1"
}

function main() {
  [[ $# -eq 0 || "$1" == '-h' || "$1" == '--help' ]] && helpAndExit 0
  REL=$(dirname "$0")
  case "$1" in
  H2)
    set -x
    mvn -q verify  -f $REL/.. -B -T 4C -am -Pcoverage -Dmaven.javadoc.skip -Dcheckstyle.skip
    # disabling sonarqube for PRs because it's not supported yet. See https://jira.sonarsource.com/browse/MMF-1371
    if [ -n "$2" ]; then
     #-Pcoverage to activate jacoco and test coverage reports
     # send test coverage and build information to sonarcloud
     mvn sonar:sonar -f $REL/.. -Pcoverage -Dsonar.projectKey="$2"
    fi
    ;;
  esac
}

main "$@"
