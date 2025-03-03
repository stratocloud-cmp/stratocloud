@echo off

setlocal
set STRATO_PROCESS_TITLE=StratoCloud
set STRATO_HOME=%~dp0%..
set JVM_OPTS=-Xms256M -Xmx8192M

if defined JAVA_HOME (
 set _EXECJAVA="%JAVA_HOME%\bin\java"
)

if not defined JAVA_HOME (
 echo "JAVA_HOME not set."
 set _EXECJAVA=java
)

set JAR_DIR=%STRATO_HOME%\jars\standalone-server.jar
set CONFIG_DIR=%STRATO_HOME%\config\application.yaml

%_EXECJAVA% -jar %JAR_DIR% %JVM_OPTS% --spring.config.location=%CONFIG_DIR%
endlocal