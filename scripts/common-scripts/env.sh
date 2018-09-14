SPM_HOME="/opt/spm"

if [ -e ${SPM_HOME}/properties/java.properties ]; then
  . ${SPM_HOME}/properties/java.properties
fi

[ -n "$JAVA_HOME" ] && JAVA=${JAVA_HOME}/bin/java

if [ -z "$JAVA" ]
then
  JAVA=$(command -v java)
fi
