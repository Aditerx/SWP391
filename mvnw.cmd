@echo off
setlocal

for %%i in ("%~dp0.") do set "BASE_DIR=%%~fi"
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)

if not exist "%JAVA_EXE%" if not "%JAVA_EXE%"=="java" (
  echo JAVA_HOME is invalid: %JAVA_HOME% 1>&2
  exit /b 1
)

"%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -classpath "%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
