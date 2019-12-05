#!/bin/bash
set -e # fail fast
#H Usage:
#H compile.sh -h | compile.sh --help
#H
#H prints this help and exits
#H
#H compile.sh <module>
#H
#H   compiles the taskana application. Does not package and install artifacts.
#H
#H module:
#H   - ADAPTER
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
  ADAPTER)
    set -x
    mvn -q compile -B -f $REL/../
    ;;
  esac
}

main "$@"
