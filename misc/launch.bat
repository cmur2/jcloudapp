@echo off

@setlocal

rem username and password here in the form "ARGS=user pass":
set ARGS=

set DARGS=-Xms16m -Xmx64m -Dswing.defaultlaf=com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel

start javaw %DARGS% -jar jcloudapp.jar %ARGS%

@endlocal

rem pause