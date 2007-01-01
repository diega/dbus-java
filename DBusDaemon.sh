#!/bin/sh --

VERSION=%VERSION%
JARPATH=%JARPATH%
JAVAUNIXLIBPATH=%JAVAUNIXLIBPATH%
JAVAUNIXJARPATH=%JAVAUNIXJARPATH%

exec java -DPid=$$ -DVersion=$VERSION -Djava.library.path=$JAVAUNIXLIBPATH -cp $JAVAUNIXJARPATH/unix.jar:$JAVAUNIXJARPATH/debug-enable.jar:$JAVAUNIXJARPATH/hexdump.jar:$JARPATH/dbus.jar org.freedesktop.dbus.bin.DBusDaemon "$@"
