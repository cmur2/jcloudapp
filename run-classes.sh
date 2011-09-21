#!/bin/bash

# username and password here:
ARGS=

CLASSPATH="build/classes/:libs/*"
DARGS="-Xms16m -Xmx64m"
MAINCLASS="de.jcloudapp.Main"

java -cp $CLASSPATH $DARGS $MAINCLASS $ARGS