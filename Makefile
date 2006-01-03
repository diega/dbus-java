JAVAC?=javac
JAVA?=java
JAVAH?=javah
JAR?=jar
CFLAGS?=`pkg-config --cflags --libs dbus-1`
CC?=gcc
LD?=ld
DBUSLIB?=/usr/lib/libdbus-1.a
CPFLAG?=-classpath
JCFLAGS?=-cp classes:$(CLASSPATH)
JFLAGS?=-Djava.library.path=.:/usr/lib
SRCDIR=org/freedesktop
CLASSDIR=classes/org/freedesktop/dbus

PREFIX?=$(DESTDIR)/usr
JARPREFIX?=$(PREFIX)/share/java
LIBPREFIX?=$(PREFIX)/lib/jni
DOCPREFIX?=$(PREFIX)/share/doc/libdbus-java

VERSION = 0.1
DEBVER =
DEB_ARCH ?= $(shell dpkg-architecture -qDEB_BUILD_ARCH)
 
all: libdbus-java.so libdbus-java-$(VERSION).jar

clean:
	-rm -rf doc
	-rm -rf classes
	-rm *.o *.so *.h .classes .testclasses .doc *.jar *.log pid address tmp-session-bus
	
classes: .classes
testclasses: .testclasses
.testclasses: $(SRCDIR)/dbus/test/*.java .classes
	-mkdir classes
	$(JAVAC) -d classes $(JCFLAGS) $(SRCDIR)/dbus/test/*.java
	touch .testclasses 
.classes: $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/Hal/*.java
	-mkdir classes
	$(JAVAC) -d classes $(JCFLAGS) $^
	touch .classes

$(CLASSDIR)/DBusConnection.class: $(SRCDIR)/dbus/DBusConnection.java
	make classes
org_freedesktop_dbus_DBusConnection.h: $(CLASSDIR)/DBusConnection.class
	$(JAVAH) -classpath classes:$(CLASSPATH) -d . org.freedesktop.dbus.DBusConnection
	touch $@
dbus-java.o: dbus-java.c org_freedesktop_dbus_DBusConnection.h
	$(CC) $(CFLAGS) -fpic -c $< -o $@

libdbus-java.so: dbus-java.o
	$(LD) $(LDFLAGS) -fpic -shared -o $@ $^ $(DBUSLIB)
libdbus-java-$(VERSION).jar: .classes
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/*.class org/freedesktop/*.class org/freedesktop/Hal/*.class)
dbus-java-test-$(VERSION).jar: .testclasses
	(cd classes; $(JAR) -cf ../$@ org/freedesktop/dbus/test/*.class)
	

jar: libdbus-java-$(VERSION).jar
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
doc/api/index.html: $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/Hal/*.java .doc
	javadoc -quiet -author -link http://java.sun.com/j2se/1.5.0/docs/api/  -d doc/api $(SRCDIR)/*.java $(SRCDIR)/dbus/*.java $(SRCDIR)/Hal/*.java

dbus-java.tar.gz: org *.c Makefile *.tex debian tmp-session.conf
	(tar -zcf dbus-java.tar.gz $^)

testrun: libdbus-java.so libdbus-java-$(VERSION).jar dbus-java-test-$(VERSION).jar
	$(JAVA) $(JFLAGS) $(CPFLAG) libdbus-java-$(VERSION).jar:dbus-java-test-$(VERSION).jar org.freedesktop.dbus.test.test

check:
	( PASS=false; \
	  dbus-daemon --config-file=tmp-session.conf --print-pid --print-address=5 --fork >pid 5>address ; \
	  export DBUS_SESSION_BUS_ADDRESS=$$(cat address) ;\
	  if make testrun ; then export PASS=true; fi  ; \
	  kill $$(cat pid) ; \
	  if [[ "$$PASS" == "true" ]]; then exit 0; else exit 1; fi )

uninstall: 
	rm $(JARPREFIX)/dbus.jar $(JARPREFIX)/dbus-$(VERSION).jar
	rm $(LIBPREFIX)/libdbus-java.so
	rm -rf $(DOCPREFIX)

install: libdbus-java-$(VERSION).jar libdbus-java.so doc
	install -d $(JARPREFIX)
	install -m 644 libdbus-java-$(VERSION).jar $(JARPREFIX)/dbus-$(VERSION).jar
	ln -s dbus-$(VERSION).jar $(JARPREFIX)/dbus.jar
	install -d $(LIBPREFIX)
	install libdbus-java.so $(LIBPREFIX)
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
.dist: dbus-java.c dbus-java.tex Makefile org tmp-session.conf
	-mkdir libdbus-java-$(VERSION)
	cp -fa $^ libdbus-java-$(VERSION)
	touch .dist
tar: libdbus-java-$(VERSION).tar.gz

distclean:
	-rm -rf libdbus-java-$(VERSION)
	-rm -rf libdbus-java-$(VERSION).tar.gz
	-rm libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).deb
	-rm libdbus-java_$(VERSION)$(DEBVER).dsc
	-rm libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).build
	-rm libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).changes
	-rm libdbus-java_$(VERSION)$(DEBVER).diff.gz
	-rm libdbus-java_$(VERSION).orig.tar.gz
	-rm .dist

libdbus-java-$(VERSION): .dist
	
libdbus-java-$(VERSION).tar.gz: .dist
	tar zcf $@ libdbus-java-$(VERSION)
	
libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).deb: .dist libdbus-java-$(VERSION).tar.gz
	cp libdbus-java-$(VERSION).tar.gz libdbus-java_$(VERSION).orig.tar.gz
	cp -fa debian libdbus-java-$(VERSION)
	(cd libdbus-java-$(VERSION); debuild -uc -us -rfakeroot)

deb: libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).deb
libdbus-java_$(VERSION)$(DEBVER).dsc: libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).deb
libdbus-java_$(VERSION)$(DEBVER).diff.gz: libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).deb
libdbus-java_$(VERSION).orig.tar.gz: libdbus-java_$(VERSION)$(DEBVER)_$(DEB_ARCH).deb


