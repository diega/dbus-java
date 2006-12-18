#!/bin/sh --

JARPATH=%JARPATH%
JAVAUNIXLIBPATH=%DJAVAUNIXLIBPATH%
JAVAUNIXJARPATH=%DJAVAUNIXJARPATH%

java -Djava.library.path=$JAVAUNIXLIBPATH -cp $JAVAUNIXJARPATH/unix.jar:$JARPATH/dbus.jar org.freedesktop.dbus.viewer.DBusViewer "$@"
