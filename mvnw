#!/bin/sh

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA_EXE=java
if [ -n "$JAVA_HOME" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
fi

exec "$JAVA_EXE" -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  -classpath "$BASE_DIR/.mvn/wrapper/maven-wrapper.jar" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
