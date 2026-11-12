@echo off
REM ===========================================================================
REM  FEMZYK Enterprise Fleet & Transport Management System - desktop launcher
REM
REM  Double-click this file to start the desktop edition.
REM
REM  It needs a Java 17 or newer runtime. If you do not have one, download the
REM  Windows x64 "JRE" or "JDK" from https://adoptium.net and install it, then
REM  run this file again. Nothing else has to be installed.
REM ===========================================================================
setlocal

set "HERE=%~dp0"
set "APP=%HERE%FEMZYK-Fleet-Management.jar"

if not exist "%APP%" (
  echo.
  echo  ERROR: FEMZYK-Fleet-Management.jar was not found next to this file.
  echo  Keep the .bat file and the .jar together in the same folder.
  echo.
  pause
  exit /b 1
)

REM Prefer JAVA_HOME, then whatever "java" is on the PATH.
if defined JAVA_HOME (
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA=java"
)

"%JAVA%" -version >nul 2>&1
if errorlevel 1 (
  echo.
  echo  Java was not found on this computer.
  echo.
  echo  Install Java 17 or newer from https://adoptium.net
  echo  ^(choose "Windows x64" and the .msi installer^), then run this file again.
  echo.
  echo  If Java is installed but not on your PATH, set JAVA_HOME to the
  echo  folder that contains bin\java.exe.
  echo.
  pause
  exit /b 1
)

echo Starting FEMZYK Fleet Management...
"%JAVA%" -jar "%APP%" %*

REM Keep the window open if the application exits with an error, so the
REM message does not disappear before it can be read.
if errorlevel 1 pause
endlocal
