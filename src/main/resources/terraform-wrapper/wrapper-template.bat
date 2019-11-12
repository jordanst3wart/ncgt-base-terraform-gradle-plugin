@REM
@REM Copyright 2017-2019 the original author or authors.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

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

set APP_LOCATION_FILE=%DIRNAME%\%DOT_GRADLE_RELATIVE_PATH%\~~APP_LOCATION_FILE~~

@rem If the app location is not available, set it first via Gradle
if not exist %APP_LOCATION_FILE% call :run_gradle -q ~~CACHE_TASK_NAME~~

@rem Read settings in from app location properties
@rem - APP_LOCATION
@rem - USE_GLOBAL_CONFIG
@rem - CONFIG_LOCATION
call %APP_LOCATION_FILE%

@rem If the app is not available, download it first via Gradle
if not exist %APP_LOCATION% call :run_gradle -q ~~CACHE_TASK_NAME~~

@rem If global configuration is disabled which is the default, then
@rem  point the Terraform config to the generated configuration file
@rem  if it exists.
if "%TF_CLI_CONFIG_FILE%" == "" (
    if "%USE_GLOBAL_CONFIG%"=="true" goto cliconfigset
    if exist %CONFIG_LOCATION% (
        set TF_CLI_CONFIG_FILE=%CONFIG_LOCATION%
    ) else (
        echo Config location specified as %CONFIG_LOCATION%, but file does not exist. 1>&2
        echo Please run the ~~TERRAFORMRC_TASK_NAME~~ Gradle task before using %APP_BASE_NAME% again 1>&2
    )
)
:cliconfigset

@rem  If we are in a project containing a default Terraform source set
@rem  then point the data directory to the default location.
if "%TF_DATA_DIR%" == "" (
    if exist %CD%\src\tf\main (
        set TF_DATA_DIR=%CD%\build\tf\main
        echo %TF_DATA_DIR% will be used as data directory 1>&2
    )
)


if "%TF_LOG_PATH%"=="" set TF_LOG_PATH=%DIRNAME%\terraform.log

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
if  exist %GRADLE_WRAPPER_RELATIVE_PATH%\gradlew.bat (
    call %GRADLE_WRAPPER_RELATIVE_PATH%\gradlew.bat %*
) else (
    call gradle %*
)
exit /b 0

