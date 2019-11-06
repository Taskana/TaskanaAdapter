#!/bin/bash
set -e # fail fast

#H Usage:
#H test.sh -h | test.sh --help
#H
#H prints this help and exits
#H
#H test.sh <database|module>
#H
#H   tests the taskana application. See documentation for further testing details.
#H
#H database:
#H   - POSTGRES_10_4
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
  POSTGRES_10_4)
    set -x
    eval "$REL/prepare_db.sh '$1'"
    ### INSTALL ###
    mvn -q install -B -f $REL/.. -P postgres -am -T 4C -pl :taskana-adapter-test  -DskipTests -Dmaven.javadoc.skip -Dcheckstyle.skip

    ### TEST ###
    mvn -q verify -B -f $REL/.. -Dmaven.javadoc.skip -Dcheckstyle.skip -pl :taskana-adapter-test
    # Same as above (H2) we can not use the fancy '-f' maven option
    (cd $REL/.. && mvn -q verify -B -pl :taskana-adapter-test -P postgres -Dmaven.javadoc.skip -Dcheckstyle.skip)
    ;;
  ADAPTER)
    set -x
    ### INSTALL ###
    mvn -q install -B -f $REL/.. -am -T 4C -pl :taskana-adapter-sample -DskipTests -Dmaven.javadoc.skip -Dcheckstyle.skip
    ;;
  esac
}

main "$@"
