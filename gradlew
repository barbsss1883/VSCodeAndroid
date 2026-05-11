#!/bin/sh
#
# Gradle start up script for POSIX compatible shells

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Resolve links: $0 may be a link
PRG="$0"
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

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

MAX_FD="maximum"
warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }

case "`uname`" in
    CYGWIN*) cygwin=true;;
    Darwin*) darwin=true;;
    MSYS* | MINGW*) msys=true;;
    NONSTOP*) nonstop=true;;
esac

# For Darwin, add options to specify how the JVM should handle signals
GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""

if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
fi

exec "$JAVACMD" "$@"
