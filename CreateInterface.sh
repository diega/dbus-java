#!/bin/sh --

JARPATH=%JARPATH%
JAVAUNIXLIBPATH=%DJAVAUNIXLIBPATH%
JAVAUNIXJARPATH=%DJAVAUNIXJARPATH%

java -Djava.library.path=$JAVAUNIXLIBPATH -cp $JAVAUNIXJARPATH/unix.jar:$JAVAUNIXJARPATH/debug-enable.jar:$JAVAUNIXJARPATH/hexdump.jar:$JARPATH/dbus.jar org.freedesktop.dbus.bin.CreateInterface "$@"
