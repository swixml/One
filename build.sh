#!/bin/bash

echo
echo "SWIXML Build System"
echo "-------------------"
echo
echo "JAVA_HOME"
if [ -z "$JAVA_HOME" ] ; then
  echo 'ERROR: JAVA_HOME not found in your environment.'
  echo
  echo 'Please, set the JAVA_HOME variable in your environment to match the'
  echo 'location of the Java Virtual Machine you want to use.'
  exit 1
fi

if [ `uname | grep -n CYGWIN` ] ; then
  PS=";"
elif [ `uname | grep -n Windows` ] ; then
  PS=";"
else
  PS=":"
fi

LOCALCLASSPATH=${JAVA_HOME}/lib/tools.jar${PS}./lib/ant.jar${PS}./lib/ant-launcher.jar${PS}./lib/jdom.jar${PS}./lib/j2h.jar${PS}./lib/jgoodies-forms-1.4.0.jar
ANT_HOME=./lib

echo Starting Ant...
echo
echo $LOCALCLASSPATH


$JAVA_HOME/bin/java -Dant.home=$ANT_HOME -classpath $LOCALCLASSPATH org.apache.tools.ant.Main $*
