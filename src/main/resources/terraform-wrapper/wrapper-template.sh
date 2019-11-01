#!/usr/bin/env sh
#
# Copyright 2017-2019 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


##############################################################################
##
##  ~~APP_BASE_NAME~~ wrapper up script for UN*X
##
##############################################################################
# Relative path from this script to the directory where the Gradle wrapper
# might be found.
GRADLE_WRAPPER_RELATIVE_PATH=~~GRADLE_WRAPPER_RELATIVE_PATH~~

# Relative path from this script to the project cache dir (usually .gradle).
DOT_GRADLE_RELATIVE_PATH=~~DOT_GRADLE_RELATIVE_PATH~~

PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done

SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\"$arg\""
        fi
        i=$((i+1))
    done
    case $i in
        (0) set -- ;;
        (1) set -- "$args0" ;;
        (2) set -- "$args0" "$args1" ;;
        (3) set -- "$args0" "$args1" "$args2" ;;
        (4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        (5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        (6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        (7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        (8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        (9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

APP_LOCATION_FILE=$DOT_GRADLE_RELATIVE_PATH/~~APP_LOCATION_FILE~~

run_gradle ( ) {
  if  [ -x "$GRADLE_WRAPPER_RELATIVE_PATH/gradlew" ] ; then
    $GRADLE_WRAPPER_RELATIVE_PATH/gradlew "$@"
  else
    gradle "$@"
  fi
}

app_property ( ) {
    echo `cat $APP_LOCATION_FILE | grep $1 | cut -f2 -d=`
}

# If the app location is not available, set it first via Gradle
if [ ! -f $APP_LOCATION_FILE ] ; then
  run_gradle -q ~~CACHE_TASK_NAME~~
fi

# Now read in the configuration values for later usage
. $APP_LOCATION_FILE

# Read the location of the wrapped binary from the location file
#APP_LOCATION=`app_property location`

# If the app is not available, download it first via Gradle
if [ ! -f $APP_LOCATION  ] ; then
  run_gradle -q ~~CACHE_TASK_NAME~~
fi

# If global configuration is disabled which is the default, then
# point the Terraform config to the generated configuration file
# if it exists.
if [ -z $TF_CLI_CONFIG_FILE ] ; then
#    grep -q 'useGlobalConfig=true' $APP_LOCATION_FILE
    if [ $USE_GLOBAL_CONFIG == 'false' ] ; then
        CONFIG_LOCATION=`app_property configLocation`
        if [ -f $CONFIG_LOCATION ] ; then
            export TF_CLI_CONFIG_FILE=$CONFIG_LOCATION
        else
          echo Config location specified as $CONFIG_LOCATION, but file does not exist. >&2
          echo Please run the ~~TERRAFORMRC_TASK_NAME~~ Gradle task before using $(basename $0) again >&2
        fi
    fi
fi

# If we are in a project containing a default Terraform source set
# then point the data directory to the default location.
if [ -z $TF_DATA_DIR ] ; then
    if [ -f $PWD/src/tf/main ] ; then
        export TF_DATA_DIR=$PWD/build/tf/main
        echo $TF_DATA_DIR will be used as data directory >&2
    fi
fi

exec $APP_LOCATION "$@"


