org.osgi.framework.system.packages = \
 javax.accessibility, \
 javax.activity, \
 javax.crypto, \
 javax.crypto.interfaces, \
 javax.crypto.spec, \
 javax.imageio, \
 javax.imageio.event, \
 javax.imageio.metadata, \
 javax.imageio.plugins.bmp, \
 javax.imageio.plugins.jpeg, \
 javax.imageio.spi, \
 javax.imageio.stream, \
 javax.jws;version="2.0", \
 javax.jws.soap;version="2.0", \
 javax.lang.model, \
 javax.lang.model.element, \
 javax.lang.model.type, \
 javax.lang.model.util, \
 javax.management, \
 javax.management.loading, \
 javax.management.modelmbean, \
 javax.management.monitor, \
 javax.management.openmbean, \
 javax.management.relation, \
 javax.management.remote, \
 javax.management.remote.rmi, \
 javax.management.timer, \
 javax.naming, \
 javax.naming.directory, \
 javax.naming.event, \
 javax.naming.ldap, \
 javax.naming.spi, \
 javax.net, \
 javax.net.ssl, \
 javax.print, \
 javax.print.attribute, \
 javax.print.attribute.standard, \
 javax.print.event, \
 javax.rmi, \
 javax.rmi.CORBA, \
 javax.rmi.ssl, \
 javax.script, \
 javax.security.auth, \
 javax.security.auth.callback, \
 javax.security.auth.kerberos, \
 javax.security.auth.login, \
 javax.security.auth.spi, \
 javax.security.auth.x500, \
 javax.security.cert, \
 javax.security.sasl, \
 javax.sound.midi, \
 javax.sound.midi.spi, \
 javax.sound.sampled, \
 javax.sound.sampled.spi, \
 javax.sql, \
 javax.sql.rowset, \
 javax.sql.rowset.serial, \
 javax.sql.rowset.spi, \
 javax.swing, \
 javax.swing.border, \
 javax.swing.colorchooser, \
 javax.swing.event, \
 javax.swing.filechooser, \
 javax.swing.plaf, \
 javax.swing.plaf.basic, \
 javax.swing.plaf.metal, \
 javax.swing.plaf.multi, \
 javax.swing.plaf.synth, \
 javax.swing.table, \
 javax.swing.text, \
 javax.swing.text.html, \
 javax.swing.text.html.parser, \
 javax.swing.text.rtf, \
 javax.swing.tree, \
 javax.swing.undo, \
 javax.tools, \
 javax.transaction; javax.transaction.xa; version=1.1; partial=true; mandatory:=partial, \
 javax.xml, \
 javax.xml.crypto, \
 javax.xml.crypto.dom, \
 javax.xml.crypto.dsig, \
 javax.xml.crypto.dsig.dom, \
 javax.xml.crypto.dsig.keyinfo, \
 javax.xml.crypto.dsig.spec, \
 javax.xml.datatype, \
 javax.xml.namespace;version=1.0, \
 javax.xml.parsers, \
 javax.xml.transform, \
 javax.xml.transform.dom, \
 javax.xml.transform.sax, \
 javax.xml.transform.stax, \
 javax.xml.transform.stream, \
 javax.xml.validation, \
 javax.xml.xpath, \
 org.ietf.jgss, \
 org.omg.CORBA, \
 org.omg.CORBA_2_3, \
 org.omg.CORBA_2_3.portable, \
 org.omg.CORBA.DynAnyPackage, \
 org.omg.CORBA.ORBPackage, \
 org.omg.CORBA.portable, \
 org.omg.CORBA.TypeCodePackage, \
 org.omg.CosNaming, \
 org.omg.CosNaming.NamingContextExtPackage, \
 org.omg.CosNaming.NamingContextPackage, \
 org.omg.Dynamic, \
 org.omg.DynamicAny, \
 org.omg.DynamicAny.DynAnyFactoryPackage, \
 org.omg.DynamicAny.DynAnyPackage, \
 org.omg.IOP, \
 org.omg.IOP.CodecFactoryPackage, \
 org.omg.IOP.CodecPackage, \
 org.omg.Messaging, \
 org.omg.PortableInterceptor, \
 org.omg.PortableInterceptor.ORBInitInfoPackage, \
 org.omg.PortableServer, \
 org.omg.PortableServer.CurrentPackage, \
 org.omg.PortableServer.POAManagerPackage, \
 org.omg.PortableServer.POAPackage, \
 org.omg.PortableServer.portable, \
 org.omg.PortableServer.ServantLocatorPackage, \
 org.omg.SendingContext, \
 org.omg.stub.java.rmi, \
 org.omg.stub.javax.management.remote.rmi, \
 org.w3c.dom, \
 org.w3c.dom.bootstrap, \
 org.w3c.dom.css, \
 org.w3c.dom.events, \
 org.w3c.dom.html, \
 org.w3c.dom.ls, \
 org.w3c.dom.ranges, \
 org.w3c.dom.stylesheets, \
 org.w3c.dom.traversal, \
 org.w3c.dom.views, \
 org.w3c.dom.xpath, \
 org.xml.sax, \
 org.xml.sax.ext, \
 org.xml.sax.helpers

org.osgi.framework.system.packages.extra = \
 sun.misc,\
 sun.reflect,\
 org.apache.geronimo.transformer,\
 org.apache.felix.karaf.jaas.boot;version=1.5.0

org.osgi.framework.bootdelegation = \
 javax.*,\
 org.ietf.jgss,\
 org.omg.*,\
 org.w3c.*,\
 org.xml.*,\
 sun.*,\
 com.sun.*

org.osgi.framework.executionenvironment = \
 OSGi/Minimum-1.0,\
 OSGi/Minimum-1.1,\
 OSGi/Minimum-1.2,\
 JRE-1.1,\
 J2SE-1.2,\
 J2SE-1.3,\
 J2SE-1.4,\
 J2SE-1.5,\
 JavaSE-1.6

osgi.java.profile.name = Geronimo-1.6
