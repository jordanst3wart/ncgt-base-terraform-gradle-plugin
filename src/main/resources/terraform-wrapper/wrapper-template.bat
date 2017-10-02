@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  ~~APP_BASE_NAME~~ wrapper script for Windows
@rem
@rem ##########################################################################

@rem Relative path from this script to the directory where the Gradle wrapper
@rem might be found.
set GRADLE_WRAPPER_RELATIVE_PATH=~~GRADLE_WRAPPER_RELATIVE_PATH~~

@rem  Relative path from this script to the project cache dir (usually .gradle).
set DOT_GRADLE_RELATIVE_PATH=~~DOT_GRADLE_RELATIVE_PATH~~

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set APP_LOCATION_FILE=%DOT_GRADLE_RELATIVE_PATH%/~~APP_LOCATION_FILE~~

@rem If the app location is not available, set it first via Gradle
if not exist %APP_LOCATION_FILE% call :run_gradle -q ~~CACHE_TASK_NAME~~

@rem Read the location of the wrapped binary from the location file
for /f "delims== tokens=1,2 usebackq" %i in (`find location %APP_LOCATION_FILE%`) do set APP_LOCATION=%i
@rem for /f "delims== tokens=1,2" %i in (%APP_LOCATION_FILE%) do if (%i == "location") set APP_LOCATION=%j

@rem If the app is not available, download it first via Gradle
if not exist %APP_LOCATION% call :run_gradle -q ~~CACHE_TASK_NAME~~

@rem Execute ~~APP_BASE_NAME~~
%APP_LOCATION% %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
exit /b 0

:run_gradle
if  exists %GRADLE_WRAPPER_RELATIVE_PATH%\gradlew.bat (
    call %GRADLE_WRAPPER_RELATIVE_PATH%\gradlew.bat %*
) else (
    call gradle %*
)
exit /b 0

