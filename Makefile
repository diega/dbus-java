#
# For profiling/debug builds use:
#
# make CFLAGS="" STRIP=touch JCFLAGS="-Xlint:all"
# JFLAGS="-Xrunhprof:heap=sites,cpu=samples,monitor=y,thread=y,doe=y -classic" check
#

# Variables controlling compilation. May be overridden on the command line for
# debug builds etc
#
JAVAC?=javac
JAVA?=java
JAVAH?=javah
JAVADOC?=javadoc
JAR?=jar
INCLUDES?=`pkg-config --cflags dbus-1` -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
CFLAGS?= -Os -Wall -Werror 
CFLAGS+=$(INCLUDES)
CFLAGS+=$(call cc-option,-fno-stack-protector,)
LIBS?=`pkg-config --libs dbus-1`
CC?=gcc
LD?=ld
STRIP?=strip
CPFLAG?=-classpath
JCFLAGS?=-Xlint:all -O -g:none
JCFLAGS+=-cp classes:$(CLASSPATH)
JFLAGS+=-Djava.library.path=.:/usr/lib
SRCDIR=org/freedesktop
CLASSDIR=classes/org/freedesktop/dbus

# Installation variables. Controls the location of make install.  May be
# overridden in the make command line to install to different locations
#
PREFIX?=$(DESTDIR)/usr/local
JARPREFIX?=$(PREFIX)/share/java
LIBPREFIX?=$(PREFIX)/lib/jni
BINPREFIX?=$(PREFIX)/bin
DOCPREFIX?=$(PREFIX)/share/doc/libdbus-java
MANPREFIX?=$(PREFIX)/share/man/man1
RUNPREFIX?=$(PREFIX)
RUNJARPREFIX?=$(RUNPREFIX)/share/java
RUNLIBPREFIX?=$(RUNPREFIX)/lib/jni

VERSION = 1.11
RELEASEVERSION = 1.10

# Usage: cflags-y += $(call cc-option, -march=winchip-c6, -march=i586)
cc-option = $(shell if $(CC) $(1) -S -o /dev/null -xc /dev/null \
> /dev/null 2>&1; then echo "$(1)"; else echo "$(2)"; fi ;)

 
all: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-viewer-$(VERSION).jar

clean:
	rm -rf doc
	rm -rf classes
	rm -rf libdbus-$(VERSION)
	rm -rf libdbus-$(RELEASEVERSION)
	rm -f *.1 *.o *.so *.h .dist .classes .testclasses .doc *.jar *.log pid address tmp-session-bus *.gz .viewerclasses
	
classes: .classes
testclasses: .testclasses
viewerclasses: .viewerclasses
.testclasses: $(SRCDIR)/dbus/test/*.java .classes
	mkdir -p classes
	$(JAVAC) -d classes $(JCFLAGS) $(SRCDIR)/dbus/test/*.java
	touch .testclasses 
.viewerclasses: $(SRCDIR)/dbus/viewer/*.java .classes
	mkdir -p classes
	$(JAVAC) -d classes $(JCFLAGS) $(SRCDIR)/dbus/viewer/*.java
	touch .viewerclasses 
.classes: $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/Hal/*.java  
	mkdir -p classes
	$(JAVAC) -d classes $(JCFLAGS) $^
	touch .classes

$(CLASSDIR)/DBusConnection.class: $(SRCDIR)/dbus/DBusConnection.java
	make classes
$(CLASSDIR)/DBusErrorMessage.class: $(SRCDIR)/dbus/DBusErrorMessage.java
	make classes
$(CLASSDIR)/MethodCall.class: $(SRCDIR)/dbus/MethodCall.java
	make classes
org_freedesktop_dbus_DBusConnection.h: $(CLASSDIR)/DBusConnection.class
	$(JAVAH) -classpath classes:$(CLASSPATH) -d . org.freedesktop.dbus.DBusConnection
	touch $@
org_freedesktop_dbus_DBusErrorMessage.h: $(CLASSDIR)/DBusErrorMessage.class
	$(JAVAH) -classpath classes:$(CLASSPATH) -d . org.freedesktop.dbus.DBusErrorMessage
	touch $@
org_freedesktop_dbus_MethodCall.h: $(CLASSDIR)/MethodCall.class
	$(JAVAH) -classpath classes:$(CLASSPATH) -d . org.freedesktop.dbus.MethodCall
	touch $@
dbus-java.o: dbus-java.c org_freedesktop_dbus_DBusConnection.h org_freedesktop_dbus_DBusErrorMessage.h org_freedesktop_dbus_MethodCall.h
	$(CC) $(CFLAGS) -fpic -c $< -o $@

libdbus-java.so: dbus-java.o
	$(LD) $(LDFLAGS) -fpic -shared -o $@ $^ $(LIBS)
	$(STRIP) $@
libdbus-java-$(VERSION).jar: .classes
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/*.class org/freedesktop/*.class org/freedesktop/Hal/*.class)
dbus-java-test-$(VERSION).jar: .testclasses
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/test/*.class)
dbus-java-viewer-$(VERSION).jar: .viewerclasses
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/viewer/*.class)
	

jar: libdbus-java-$(VERSION).jar
doc: doc/dbus-java.dvi doc/dbus-java.ps doc/dbus-java.pdf doc/dbus-java/index.html doc/api/index.html
.doc:
	mkdir -p doc
	mkdir -p doc/dbus-java
	touch .doc
doc/dbus-java.dvi: dbus-java.tex .doc
	(cd doc; latex ../dbus-java.tex)
	(cd doc; latex ../dbus-java.tex)
	(cd doc; latex ../dbus-java.tex)
doc/dbus-java.ps: doc/dbus-java.dvi .doc
	(cd doc; dvips -o dbus-java.ps dbus-java.dvi)
doc/dbus-java.pdf: doc/dbus-java.dvi .doc
	(cd doc; pdflatex ../dbus-java.tex)
doc/dbus-java/index.html: dbus-java.tex .doc
	latex2html -local_icons -dir doc/dbus-java dbus-java.tex
doc/api/index.html: $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/Hal/*.java .doc
	$(JAVADOC) -quiet -author -link http://java.sun.com/j2se/1.5.0/docs/api/  -d doc/api $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/Hal/*.java

%.1: %.sgml
	docbook-to-man $< > $@
	
testrun: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.test

cross-test-server: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.cross_test_server

cross-test-client: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.cross_test_client

two-part-server: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.two_part_test_server

two-part-client: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.two_part_test_client

profilerun: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.profile $(PROFILE)

viewer: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-viewer-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):libdbus-java-$(VERSION).jar:dbus-java-viewer-$(VERSION).jar org.freedesktop.dbus.viewer.DBusViewer

check:
	( PASS=false; \
	  dbus-daemon --config-file=tmp-session.conf --print-pid --print-address=5 --fork >pid 5>address ; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  if make testrun ; then export PASS=true; fi  ; \
	  kill $$(cat pid) ; \
	  if [[ "$$PASS" == "true" ]]; then exit 0; else exit 1; fi )

cross-test-compile: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar

internal-cross-test: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	( dbus-daemon --config-file=tmp-session.conf --print-pid --print-address=5 --fork >pid 5>address ; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  make -s cross-test-server > server.log &\
	  make -s cross-test-client > client.log ;\
	  kill $$(cat pid) ; )

two-part-test: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	( dbus-daemon --config-file=tmp-session.conf --print-pid --print-address=5 --fork >pid 5>address ; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  make -s two-part-server &\
	  sleep 1;\
	  make -s two-part-client  ;\
	  kill $$(cat pid) ; )

profile:
	( PASS=false; \
	  dbus-daemon --config-file=tmp-session.conf --print-pid --print-address=5 --fork >pid 5>address ; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  if make profilerun ; then export PASS=true; fi  ; \
	  kill $$(cat pid) ; \
	  if [[ "$$PASS" == "true" ]]; then exit 0; else exit 1; fi )

uninstall: 
	rm -f $(JARPREFIX)/dbus.jar $(JARPREFIX)/dbus-$(VERSION).jar $(JARPREFIX)/dbus-viewer.jar $(JARPREFIX)/dbus-viewer-$(VERSION).jar
	rm -f $(LIBPREFIX)/libdbus-java.so
	rm -rf $(DOCPREFIX)
	rm -f $(MANPREFIX)/CreateInterface.1 $(MANPREFIX)/ListDBus.1  $(MANPREFIX)/DBusViewer.1
	rm -f $(BINPREFIX)/CreateInterface $(BINPREFIX)/ListDBus  $(BINPREFIX)/DBusViewer

install: dbus-java-viewer-$(VERSION).jar libdbus-java-$(VERSION).jar libdbus-java.so bin/CreateInterface bin/ListDBus bin/DBusViewer 
	install -d $(JARPREFIX)
	install -m 644 libdbus-java-$(VERSION).jar $(JARPREFIX)/dbus-$(VERSION).jar
	install -m 644 dbus-java-viewer-$(VERSION).jar $(JARPREFIX)/dbus-viewer-$(VERSION).jar
	ln -sf dbus-$(VERSION).jar $(JARPREFIX)/dbus.jar
	ln -sf dbus-viewer-$(VERSION).jar $(JARPREFIX)/dbus-viewer.jar
	install -d $(LIBPREFIX)
	install libdbus-java.so $(LIBPREFIX)
	install -d $(BINPREFIX)
	sed 's,\%JARPATH\%,$(RUNJARPREFIX),;s,\%LIBPATH\%,$(RUNLIBPREFIX),' < bin/DBusViewer > $(BINPREFIX)/DBusViewer
	chmod +x $(BINPREFIX)/DBusViewer
	sed 's,\%JARPATH\%,$(RUNJARPREFIX),;s,\%LIBPATH\%,$(RUNLIBPREFIX),' < bin/CreateInterface > $(BINPREFIX)/CreateInterface
	chmod +x $(BINPREFIX)/CreateInterface
	sed 's,\%JARPATH\%,$(RUNJARPREFIX),;s,\%LIBPATH\%,$(RUNLIBPREFIX),' < bin/ListDBus > $(BINPREFIX)/ListDBus
	chmod +x $(BINPREFIX)/ListDBus

install-man: CreateInterface.1 ListDBus.1 DBusViewer.1 changelog AUTHORS COPYING README INSTALL
	install -d $(DOCPREFIX)
	install -m 644 changelog $(DOCPREFIX)
	install -m 644 COPYING $(DOCPREFIX)
	install -m 644 AUTHORS $(DOCPREFIX)
	install -m 644 README $(DOCPREFIX)
	install -m 644 INSTALL $(DOCPREFIX)
	install -d $(MANPREFIX)
	install -m 644 CreateInterface.1 $(MANPREFIX)/CreateInterface.1
	install -m 644 ListDBus.1 $(MANPREFIX)/ListDBus.1
	install -m 644 DBusViewer.1 $(MANPREFIX)/DBusViewer.1

install-doc: doc 
	install -d $(DOCPREFIX)
	install -m 644 doc/dbus-java.dvi $(DOCPREFIX)
	install -m 644 doc/dbus-java.ps $(DOCPREFIX)
	install -m 644 doc/dbus-java.pdf $(DOCPREFIX)
	install -d $(DOCPREFIX)/dbus-java
	install -m 644 doc/dbus-java/*.html $(DOCPREFIX)/dbus-java
	install -m 644 doc/dbus-java/*.css $(DOCPREFIX)/dbus-java
	install -m 644 doc/dbus-java/*.png $(DOCPREFIX)/dbus-java
	install -d $(DOCPREFIX)/api
	cp -a doc/api/* $(DOCPREFIX)/api

dist: .dist
.dist: bin dbus-java.c dbus-java.tex Makefile org tmp-session.conf CreateInterface.sgml ListDBus.sgml DBusViewer.sgml changelog AUTHORS COPYING README INSTALL
	mkdir -p libdbus-java-$(VERSION)
	cp -fa $^ libdbus-java-$(VERSION)
	touch .dist
tar: libdbus-java-$(VERSION).tar.gz

distclean:
	rm -rf libdbus-java-$(VERSION)
	rm -rf libdbus-java-$(VERSION).tar.gz
	rm -f .dist

libdbus-java-$(VERSION): .dist

libdbus-java-$(VERSION).tar.gz: .dist
	tar zcf $@ libdbus-java-$(VERSION)
	
libdbus-java-$(RELEASEVERSION).tar.gz: bin dbus-java.c dbus-java.tex Makefile org tmp-session.conf CreateInterface.sgml ListDBus.sgml DBusViewer.sgml changelog AUTHORS COPYING README INSTALL
	mkdir -p libdbus-java-$(RELEASEVERSION)/
	cp -fa $^ libdbus-java-$(RELEASEVERSION)/
	tar zcf $@ libdbus-java-$(RELEASEVERSION)

