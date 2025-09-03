#!/bin/sh

##############################################################################
# Gradle启动脚本 for Unix
##############################################################################

# 尝试设置APP_HOME
# 解析链接
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

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# 添加默认JVM选项
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# 使用eval分割JVM_OPTS和GRADLE_OPTS的值
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# 确定Java命令
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
fi

# 执行Gradle
exec "$JAVACMD" "$@" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"