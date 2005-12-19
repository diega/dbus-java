JAVAC?=javac
JAVA?=java
JAVAH?=javah
JAR?=jar
CFLAGS?=`pkg-config --cflags --libs dbus-1`
CC?=gcc
LD?=ld
DBUSLIB?=/usr/lib/libdbus-1.a
CPFLAG?=-classpath
CLASSPATH?=../../..
JCFLAGS?=-cp $(CLASSPATH)
JFLAGS?=-Djava.library.path=.:/usr/lib

all: .classes libdbus-java.so

clean:
	-rm -rf doc
	-rm *.class *.o *.so *.h .classes .doc *.jar
	-rm ../*.class
	
classes: .classes
testclasses: .testclasses
.testclasses: test/*.java .classes
	$(JAVAC) $(JCFLAGS) test/*.java
	touch .testclasses 
.classes: *.java
	$(JAVAC) $(JCFLAGS) $^
	touch .classes

DBusConnection.class: DBusConnection.java
	make classes
org_freedesktop_dbus_DBusConnection.h: DBusConnection.class
	$(JAVAH) -classpath $(CLASSPATH) -d . org.freedesktop.dbus.DBusConnection
	touch $@
DBusConnection.o: DBusConnection.c org_freedesktop_dbus_DBusConnection.h
	$(CC) $(CFLAGS) -fpic -c $< -o $@

libdbus-java.so: DBusConnection.o
	$(LD) $(LDFLAGS) -fpic -shared -o $@ $^ $(DBUSLIB)
libdbus-java.jar: .classes
	(cd ../../../; $(JAR) -cf org/freedesktop/dbus/$@ org/freedesktop/dbus/*.class org/freedesktop/*.class)
dbus-java-test.jar: .testclasses
	(cd ../../../; $(JAR) -cf org/freedesktop/dbus/$@ org/freedesktop/dbus/test/*.class)
	
dist: tar

tar: ../../../DBus.tar.gz
jar: libdbus-java.jar
doc: doc/dbus-java.dvi doc/dbus-java.ps doc/dbus-java.pdf doc/dbus-java/index.html doc/api/index.html
.doc:
	-mkdir doc
	-mkdir doc/dbus-java
	touch .doc
doc/dbus-java.dvi: dbus-java.tex .doc
	(cd doc; latex ../dbus-java.tex)
	(cd doc; latex ../dbus-java.tex)
	(cd doc; latex ../dbus-java.tex)
doc/dbus-java.ps: doc/dbus-java.dvi .doc
	(cd doc; dvips dbus-java.dvi)
doc/dbus-java.pdf: doc/dbus-java.dvi .doc
	(cd doc; pdflatex ../dbus-java.tex)
doc/dbus-java/index.html: dbus-java.tex .doc
	latex2html -dir doc/dbus-java dbus-java.tex
doc/api/index.html: *.java ../DBus.java .doc
	javadoc -quiet -author -link http://java.sun.com/j2se/1.5.0/docs/api/  -d doc/api *.java ../DBus.java

../../../DBus.tar.gz: *.java *.c test/*.java Makefile
	(cd ../../../; tar -zcf DBus.tar.gz org/freedesktop/DBus.java org/freedesktop/dbus/{Makefile,*.{java,c},test})

testrun: .testclasses libdbus-java.so libdbus-java.jar dbus-java-test.jar
	$(JAVA) $(JFLAGS) $(CPFLAG) libdbus-java.jar:dbus-java-test.jar org.freedesktop.dbus.test.test
check: testrun
