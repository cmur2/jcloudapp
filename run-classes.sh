#!/bin/bash

# username and password here:
ARGS=

CLASSPATH="build/classes/:libs/*"
DARGS="-Xms16m -Xmx64m"
MAINCLASS="jcloudapp.Main"

java -cp $CLASSPATH $DARGS $MAINCLASS $ARGS