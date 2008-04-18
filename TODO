 ** check that the blocking is correct on DBusAsyncReply and add an explicit timeout value
 * document that you need to do connect; setup objects; get bus name for activation
 * dbus 'patterns', like watching for disconnects of registered listeners
 * named pipes under windows: http://www.koders.com/csharp/fid2CD89EB558B80CE4947D53BAC3D667F2F1188FB9.aspx
 * document that if you don't hold a reference, I will lose it! (now not default)
 * Javadoc
 * document that we handle Introspect for you 
 * add header checks to daemon
 * add TERM/etc handler to DBusDaemon
 * autolaunch
 * run a daemon in the same JVM as the application for autolaunch??
 * autodetection of float support
 * extend dbus-viewer to make calls
 * check that this is true:
   objects should not return ObjectUnknown if the path has an existant
   child node, but should if not (for introspection) It should return
   UnknownInterface or Method
 * support Enums as UInt32s
 * make it work with free VM/Compilers:
   make CFLAGS="`pkg-config --cflags dbus-1` -I/opt/java-generics/include" CLASSPATH=/opt/java-generics/share/classpath/glibj.zip:. LD_LIBRARY_PATH=/opt/java-generics/lib/classpath/ JAVAC="ecj -1.5" JAVA_HOME=/opt/java-generics/ JCFLAGS="-cp classes -bootclasspath /opt/java-generics/share/classpath/glibj.zip" JAVA=jamvm  check
   after installing eclipse eclipse-ecj jamvm and compiling classpath-generics with prefix=/opt/java-generics
   Current failure with 0.92:java.lang.IncompatibleClassChangeError: unimplemented interface on AnnotatedElement.getAnnotations().
   http://builder.classpath.org/japi/jdk15-generics.html suggests that this is fixed in HEAD
