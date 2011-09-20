@echo off

@setlocal

rem username and password here:
set ARGS=me there

set CLASSPATH=build/classes/;libs/*
set DARGS=-Xms16m -Xmx64m
set MAINCLASS=jcloudapp.Main

start javaw -cp %CLASSPATH% %DARGS% %MAINCLASS% %ARGS%

@endlocal

rem pause