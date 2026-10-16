@echo off
rem Kormcrete Order Desk on your own Java (8 or newer): starts Order Desk with the swing-ui-agent agent on port 8770.
rem Uses %JAVA_HOME%\bin\java.exe when JAVA_HOME is set, else the java on your PATH. The agent's lines appear here.
setlocal
set "DIR=%~dp0"
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -version >nul 2>&1
if errorlevel 1 (
  echo Kormcrete Order Desk needs Java 8 or newer: install it, or set JAVA_HOME.
  pause
  exit /b 1
)
"%JAVA%" "-javaagent:%DIR%swing-ui-agent-agent.jar=port=8770" -jar "%DIR%order-desk.jar"
