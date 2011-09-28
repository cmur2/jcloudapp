@echo off

@setlocal

rem username and password here:
set ARGS=

set CLASSPATH=bin/;libs/*
set DARGS=-Xms16m -Xmx64m -Dswing.defaultlaf=com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel -Dcontent.types.user.table=bin/de/jcloudapp/content-types.properties
set MAINCLASS=de.jcloudapp.Main

start javaw -cp %CLASSPATH% %DARGS% %MAINCLASS% %ARGS%

@endlocal

rem pause