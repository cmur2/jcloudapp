#!/bin/bash

# username and password here:
ARGS=

CLASSPATH="bin/:libs/*"
DARGS="-Xms16m -Xmx64m -Dswing.defaultlaf=com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"
MAINCLASS="de.jcloudapp.Main"

java -cp $CLASSPATH $DARGS $MAINCLASS $ARGS
