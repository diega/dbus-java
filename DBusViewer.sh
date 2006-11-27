#!/bin/sh --

JARPATH=%JARPATH%
LIBPATH=%LIBPATH%
DBUSLIBPATH=%DBUSLIBPATH%

java -Djava.library.path=$DBUSLIBPATH:$LIBPATH -cp $JARPATH/dbus.jar:$JARPATH/dbus-viewer.jar org.freedesktop.dbus.viewer.DBusViewer "$@"
