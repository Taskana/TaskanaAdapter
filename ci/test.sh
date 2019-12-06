#!/bin/bash
set -e # fail fast

#H Usage:
#H test.sh -h | test.sh --help
#H
#H prints this help and exits
#H
#H test.sh <database>
#H
#H   tests the taskana adapter application. See documentation for further testing details.
#H
#H database:
#H   - H2

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
    mvn -q verify  -f $REL/.. -B -T 4C -am -Dmaven.javadoc.skip -Dcheckstyle.skip
    ;;
  esac
}

main "$@"
