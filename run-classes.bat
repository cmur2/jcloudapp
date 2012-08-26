@echo off

@setlocal

rem username and password here:
set ARGS=

set CLASSPATH=bin/;libs/*
set DARGS=-Xms16m -Xmx64m -Dswing.defaultlaf=com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel
set MAINCLASS=de.jcloudapp.Main

start javaw -cp %CLASSPATH% %DARGS% %MAINCLASS% %ARGS%

@endlocal

rem pause