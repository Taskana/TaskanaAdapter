# How to deploy

## Configure datasources:

* run util\configureDS.script via **\<JBoss-Home\>\bin\jboss-cli.bat --file=util\condigureDS.script**

        notes: embed-server starts an embedded server. So JBoss doesn't need to run...
        Before running the script, place the driver jar into a directory relative to jboss-home. So the /Driver directory in util\configureDS.script is below jboss-home...

## Deploy / Undeploy adapter web app

* To deploy adapter webapp:
  mvn -P postgres wildfly:deploy

* To remove adapter webapp:
  mvn -P postgres wildfly:undeploy
