#!/bin/bash

# username and password here in the form ARGS="user pass":
ARGS=

DARGS="-Xms16m -Xmx64m -Dswing.defaultlaf=com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"

java $DARGS -jar jcloudapp.jar $ARGS
