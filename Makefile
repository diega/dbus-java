#
# For profiling/debug builds use:
#
# make CFLAGS="" STRIP=touch JCFLAGS="-Xlint:all"
# JFLAGS="-Xrunhprof:heap=sites,cpu=samples,monitor=y,thread=y,doe=y -classic" check
#

# Variables controlling compilation. May be overridden on the command line for
# debug builds etc

# Programs
JAVAC?=javac
JAVA?=java
JAVADOC?=javadoc
JAR?=jar
MAKE?=make

# Program parameters
CPFLAG?=-classpath
JCFLAGS?=-Xlint:all -O -g:none
JFLAGS+=-Djava.library.path=$(JAVAUNIXLIBDIR)

# Source/Class locations
SRCDIR=org/freedesktop
CLASSDIR=classes/org/freedesktop/dbus

# Installation variables. Controls the location of make install.  May be
# overridden in the make command line to install to different locations
#
PREFIX?=/usr/local
JARPREFIX?=$(PREFIX)/share/java
BINPREFIX?=$(PREFIX)/bin
DOCPREFIX?=$(PREFIX)/share/doc/libdbus-java
MANPREFIX?=$(PREFIX)/share/man/man1

# Installation directory of the java-unix libraries
JAVAUNIXLIBDIR?=/usr/lib/jni
# Installation directory of the java-unix jars
JAVAUNIXJARDIR?=/usr/share/java
DEBUG=disable

# Version numbering
VERSION = 2.2
RELEASEVERSION = 2.1

DISTFILES=dbus-java.tex Makefile org tmp-session.conf CreateInterface.sgml DBusDaemon.sgml ListDBus.sgml DBusViewer.sgml changelog AUTHORS COPYING README INSTALL CreateInterface.sh DBusDaemon.sh ListDBus.sh DBusViewer.sh DBusDaemon.bat CreateInterface.bat ListDBus.bat DBusViewer.bat compile.bat DBusCall.bat DBusCall.sh DBusCall.sgml

all: libdbus-java-$(VERSION).jar dbus-java-viewer-$(VERSION).jar bin/DBusDaemon bin/ListDBus bin/CreateInterface bin/DBusViewer dbus-java-bin-$(VERSION).jar bin/DBusCall

clean:
	rm -rf doc bin classes testbin win
	rm -f *.1 *.o *.so *.h .dist .classes .testclasses .doc *.jar *.log pid address tmp-session-bus *.gz .viewerclasses .bin .testbin .win .binclasses
	rm -rf dbus-$(VERSION)
	rm -rf dbus-$(RELEASEVERSION)
	
classes: .classes
testclasses: .testclasses
viewerclasses: .viewerclasses
binclasses: .binclasses
.testclasses: $(SRCDIR)/dbus/test/*.java .classes
	mkdir -p classes
	$(JAVAC) -cp classes:${JAVAUNIXJARDIR}/debug-$(DEBUG).jar:${JAVAUNIXJARDIR}/hexdump.jar:$(CLASSPATH) -d classes $(JCFLAGS) $(SRCDIR)/dbus/test/*.java
	touch .testclasses 
.viewerclasses: $(SRCDIR)/dbus/viewer/*.java .classes
	mkdir -p classes
	$(JAVAC) -cp classes:$(CLASSPATH):${JAVAUNIXJARDIR}/unix.jar:${JAVAUNIXJARDIR}/debug-$(DEBUG).jar:${JAVAUNIXJARDIR}/hexdump.jar -d classes $(JCFLAGS) $(SRCDIR)/dbus/viewer/*.java
	touch .viewerclasses 
.binclasses: $(SRCDIR)/dbus/bin/*.java .classes
	mkdir -p classes
	$(JAVAC) -cp classes:$(CLASSPATH):${JAVAUNIXJARDIR}/unix.jar:${JAVAUNIXJARDIR}/debug-$(DEBUG).jar:${JAVAUNIXJARDIR}/hexdump.jar -d classes $(JCFLAGS) $(SRCDIR)/dbus/bin/*.java
	touch .binclasses 
.classes: $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/dbus/exceptions/*.java $(SRCDIR)/dbus/types/*.java
	mkdir -p classes
	$(JAVAC) -d classes -cp classes:${JAVAUNIXJARDIR}/unix.jar:${JAVAUNIXJARDIR}/debug-$(DEBUG).jar:${JAVAUNIXJARDIR}/hexdump.jar:$(CLASSPATH) $(JCFLAGS) $^
	touch .classes

libdbus-java-$(VERSION).jar: .classes
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/*.class org/freedesktop/*.class org/freedesktop/dbus/types/*.class org/freedesktop/dbus/exceptions/*.class)
dbus-java-test-$(VERSION).jar: .testclasses
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/test/*.class)
dbus-java-viewer-$(VERSION).jar: .viewerclasses
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/viewer/*.class)
dbus-java-bin-$(VERSION).jar: .binclasses
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/bin/*.class)
dbus.jar: libdbus-java-$(VERSION).jar
	ln -sf $< $@
dbus-bin.jar: dbus-java-bin-$(VERSION).jar
	ln -sf $< $@
	
jar: libdbus-java-$(VERSION).jar
doc: doc/dbus-java.dvi doc/dbus-java.ps doc/dbus-java.pdf doc/dbus-java/index.html doc/api/index.html
.doc:
	mkdir -p doc
	mkdir -p doc/dbus-java
	touch .doc
.win:
	mkdir -p win
	touch .win
.bin:
	mkdir -p bin
	touch .bin
.testbin:
	mkdir -p testbin
	touch .testbin
doc/dbus-java.dvi: dbus-java.tex .doc
	(cd doc; latex ../dbus-java.tex)
	(cd doc; latex ../dbus-java.tex)
	(cd doc; latex ../dbus-java.tex)
doc/dbus-java.ps: doc/dbus-java.dvi .doc
	(cd doc; dvips -o dbus-java.ps dbus-java.dvi)
doc/dbus-java.pdf: doc/dbus-java.dvi .doc
	(cd doc; pdflatex ../dbus-java.tex)
doc/dbus-java/index.html: dbus-java.tex .doc
	mkdir -p doc/dbus-java/
	(cd doc/dbus-java; TEX4HTENV=/etc/tex4ht/tex4ht.env htlatex ../../dbus-java.tex "xhtml,2" "" "-cvalidate")
	rm -f doc/dbus-java/*{4ct,4tc,aux,dvi,idv,lg,log,tmp,xref}
	cp doc/dbus-java/dbus-java.html doc/dbus-java/index.html
doc/api/index.html: $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java .doc
	$(JAVADOC) -quiet -author -link http://java.sun.com/j2se/1.5.0/docs/api/ -classpath $(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar -d doc/api $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/dbus/{types,exceptions}/*.java

%.1: %.sgml
	docbook-to-man $< > $@
	
bin/%: %.sh .bin
	sed 's,\%JARPATH\%,$(JARPREFIX),;s,\%JAVAUNIXJARPATH\%,$(JAVAUNIXJARDIR),;s,\%JAVAUNIXLIBPATH\%,$(JAVAUNIXLIBDIR),;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@

win/%.bat: %.bat .win
	sed 's,\%WINJARPATH\%,$(JARPREFIX),;s,\%WINUNIXJARPATH\%,$(JAVAUNIXJARDIR),;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),' < $< > $@

testbin/%: %.sh .testbin libdbus-java-$(VERSION).jar dbus-java-bin-$(VERSION).jar dbus-bin.jar dbus.jar
	sed 's,\%JARPATH\%,.,;s,\%JAVAUNIXJARPATH\%,$(JAVAUNIXJARDIR),;s,\%JAVAUNIXLIBPATH\%,$(JAVAUNIXLIBDIR),;s,\%VERSION\%,$(VERSION),;s,\%DEBUG\%,$(DEBUG),;s,\%JAVA\%,$(JAVA),' < $< > $@
	chmod 755 $@

testrun: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.test

low-level-run: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.test_low_level

cross-test-server: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.cross_test_server

cross-test-client: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.cross_test_client

peer-server: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.test_p2p_server

peer-client: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.test_p2p_client

two-part-server: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.two_part_test_server

two-part-client: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.two_part_test_client

profilerun: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.profile $(PROFILE)

viewer: libdbus-java-$(VERSION).jar dbus-java-viewer-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) $(CLASSPATH):$(JAVAUNIXJARDIR)/unix.jar:$(JAVAUNIXJARDIR)/hexdump.jar:$(JAVAUNIXJARDIR)/debug-$(DEBUG).jar:libdbus-java-$(VERSION).jar:dbus-java-viewer-$(VERSION).jar org.freedesktop.dbus.viewer.DBusViewer

#dbus-daemon --config-file=tmp-session.conf --print-pid --print-address=5 --fork >pid 5>address ; \

low-level: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar testbin/DBusDaemon dbus.jar dbus-java-bin-$(VERSION).jar dbus-bin.jar
	( testbin/DBusDaemon --addressfile address --pidfile pid & \
	  sleep 1; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  $(MAKE) DBUS_JAVA_FLOATS=true low-level-run ;\
	  kill $$(cat pid))

check: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar testbin/DBusDaemon dbus.jar dbus-java-bin-$(VERSION).jar dbus-bin.jar
	( PASS=false; \
	  testbin/DBusDaemon --addressfile address --pidfile pid 2> server.log&\
	  sleep 1; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  if $(MAKE) DBUS_JAVA_FLOATS=true testrun 2> client.log; then export PASS=true; fi  ; \
	  kill $$(cat pid) ; \
	  if [[ "$$PASS" == "true" ]]; then exit 0; else exit 1; fi )

cross-test-compile: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar

internal-cross-test: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar testbin/DBusDaemon dbus.jar dbus-java-bin-$(VERSION).jar dbus-bin.jar
	( testbin/DBusDaemon --addressfile address --pidfile pid &\
	  sleep 1; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  $(MAKE) DBUS_JAVA_FLOATS=true -s cross-test-server | tee server.log &\
	  sleep 1;\
	  $(MAKE) DBUS_JAVA_FLOATS=true -s cross-test-client | tee client.log ;\
	  kill $$(cat pid) ; )

peer-to-peer-test: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	( $(MAKE) DBUS_JAVA_FLOATS=true -s peer-server 2>&1 | tee server.log &\
	  sleep 1;\
	  $(MAKE) DBUS_JAVA_FLOATS=true -s peer-client 2>&1 | tee client.log )

two-part-test: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar testbin/DBusDaemon dbus.jar dbus-java-bin-$(VERSION).jar dbus-bin.jar
	( testbin/DBusDaemon --addressfile address --pidfile pid &\
	  sleep 1; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  $(MAKE) DBUS_JAVA_FLOATS=true -s two-part-server | tee server.log &\
	  sleep 1;\
	  $(MAKE) DBUS_JAVA_FLOATS=true -s two-part-client | tee client.log ;\
	  kill $$(cat pid) ; )

profile: libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar testbin/DBusDaemon dbus.jar dbus-java-bin-$(VERSION).jar dbus-bin.jar
	( PASS=false; \
	  testbin/DBusDaemon --addressfile address --pidfile pid &\
	  sleep 1; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  if $(MAKE) DBUS_JAVA_FLOATS=true profilerun ; then export PASS=true; fi  ; \
	  kill $$(cat pid) ; \
	  if [[ "$$PASS" == "true" ]]; then exit 0; else exit 1; fi )

uninstall: 
	rm -f $(DESTDIR)$(JARPREFIX)/dbus.jar $(DESTDIR)$(JARPREFIX)/dbus-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus-viewer.jar $(DESTDIR)$(JARPREFIX)/dbus-viewer-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus-bin.jar $(DESTDIR)$(JARPREFIX)/dbus-bin-$(VERSION).jar
	rm -rf $(DESTDIR)$(DOCPREFIX)
	rm -f $(DESTDIR)$(MANPREFIX)/CreateInterface.1 $(DESTDIR)$(MANPREFIX)/ListDBus.1  $(DESTDIR)$(MANPREFIX)/DBusViewer.1 $(DESTDIR)$(MANPREFIX)/DBusDaemon.1 $(DESTDIR)$(MANPREFIX)/DBusCall.1
	rm -f $(DESTDIR)$(BINPREFIX)/CreateInterface $(DESTDIR)$(BINPREFIX)/ListDBus  $(DESTDIR)$(BINPREFIX)/DBusViewer $(DESTDIR)$(BINPREFIX)/DBusDaemon  $(DESTDIR)$(BINPREFIX)/DBusCall

install: install-bin install-man

install-bin: dbus-java-viewer-$(VERSION).jar libdbus-java-$(VERSION).jar bin/CreateInterface bin/ListDBus bin/DBusViewer bin/DBusDaemon dbus-java-bin-$(VERSION).jar bin/DBusCall
	install -d $(DESTDIR)$(JARPREFIX)
	install -m 644 libdbus-java-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus-$(VERSION).jar
	install -m 644 dbus-java-viewer-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus-viewer-$(VERSION).jar
	install -m 644 dbus-java-bin-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus-bin-$(VERSION).jar
	ln -sf dbus-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus.jar
	ln -sf dbus-viewer-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus-viewer.jar
	ln -sf dbus-bin-$(VERSION).jar $(DESTDIR)$(JARPREFIX)/dbus-bin.jar
	install -d $(DESTDIR)$(BINPREFIX)
	install bin/DBusViewer $(DESTDIR)$(BINPREFIX)
	install bin/DBusCall $(DESTDIR)$(BINPREFIX)
	install bin/CreateInterface $(DESTDIR)$(BINPREFIX)
	install bin/ListDBus $(DESTDIR)$(BINPREFIX)
	install bin/DBusDaemon $(DESTDIR)$(BINPREFIX)

install-man: CreateInterface.1 ListDBus.1 DBusDaemon.1 DBusViewer.1 changelog AUTHORS COPYING README INSTALL DBusCall.1
	install -d $(DESTDIR)$(DOCPREFIX)
	install -m 644 changelog $(DESTDIR)$(DOCPREFIX)
	install -m 644 COPYING $(DESTDIR)$(DOCPREFIX)
	install -m 644 AUTHORS $(DESTDIR)$(DOCPREFIX)
	install -m 644 README $(DESTDIR)$(DOCPREFIX)
	install -m 644 INSTALL $(DESTDIR)$(DOCPREFIX)
	install -d $(DESTDIR)$(MANPREFIX)
	install -m 644 CreateInterface.1 $(DESTDIR)$(MANPREFIX)/CreateInterface.1
	install -m 644 ListDBus.1 $(DESTDIR)$(MANPREFIX)/ListDBus.1
	install -m 644 DBusDaemon.1 $(DESTDIR)$(MANPREFIX)/DBusDaemon.1
	install -m 644 DBusViewer.1 $(DESTDIR)$(MANPREFIX)/DBusViewer.1
	install -m 644 DBusCall.1 $(DESTDIR)$(MANPREFIX)/DBusCall.1

install-doc: doc 
	install -d $(DESTDIR)$(DOCPREFIX)
	install -m 644 doc/dbus-java.dvi $(DESTDIR)$(DOCPREFIX)
	install -m 644 doc/dbus-java.ps $(DESTDIR)$(DOCPREFIX)
	install -m 644 doc/dbus-java.pdf $(DESTDIR)$(DOCPREFIX)
	install -d $(DESTDIR)$(DOCPREFIX)/dbus-java
	install -m 644 doc/dbus-java/*.html $(DESTDIR)$(DOCPREFIX)/dbus-java
	install -m 644 doc/dbus-java/*.css $(DESTDIR)$(DOCPREFIX)/dbus-java
	install -d $(DESTDIR)$(DOCPREFIX)/api
	cp -a doc/api/* $(DESTDIR)$(DOCPREFIX)/api

dist: .dist
.dist: $(DISTFILES)
	mkdir -p dbus-java-$(VERSION)
	cp -fa $^ dbus-java-$(VERSION)
	touch .dist
tar: dbus-java-$(VERSION).tar.gz

distclean:
	rm -rf dbus-java-$(VERSION)
	rm -rf dbus-java-$(VERSION).tar.gz
	rm -f .dist

libdbus-java-$(VERSION): .dist

dbus-java-$(VERSION).tar.gz: .dist
	tar zcf $@ dbus-java-$(VERSION)
dbus-java-$(VERSION).zip: .dist
	zip -r $@ dbus-java-$(VERSION)/
	
dbus-java-$(RELEASEVERSION).tar.gz: $(DISTFILES) 
	mkdir -p dbus-java-$(RELEASEVERSION)/
	cp -fa $^ dbus-java-$(RELEASEVERSION)/
	tar zcf $@ dbus-java-$(RELEASEVERSION)

dbus-java-win-$(VERSION).zip: dbus-java-bin-$(VERSION).jar libdbus-java-$(VERSION).jar win/CreateInterface.bat  win/DBusDaemon.bat  win/DBusViewer.bat  win/ListDBus.bat $(JAVAUNIXJARDIR)/hexdump.jar $(JAVAUNIXJARDIR)/debug-$(DEBUG).jar dbus-java-viewer-$(VERSION).jar win/DBusCall.bat
	mkdir -p dbus-java-win-$(VERSION)
	cp -fa dbus-java-bin-$(VERSION).jar dbus-java-win-$(VERSION)/dbus-bin.jar
	cp -fa dbus-java-viewer-$(VERSION).jar dbus-java-win-$(VERSION)/dbus-viewer.jar
	cp -fa libdbus-java-$(VERSION).jar dbus-java-win-$(VERSION)/dbus.jar
	cp -fa win/* dbus-java-win-$(VERSION)/
	cp -faL $(JAVAUNIXJARDIR)/hexdump.jar dbus-java-win-$(VERSION)/
	cp -faL $(JAVAUNIXJARDIR)/debug-$(DEBUG).jar dbus-java-win-$(VERSION)/
	zip -r $@ dbus-java-win-$(VERSION)/
