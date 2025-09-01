package net.coplanar.app;

import com.github.jpmsilva.jsystemd.Systemd;
import com.github.jpmsilva.jsystemd.SystemdHeapStatusProvider;
import com.github.jpmsilva.jsystemd.SystemdNonHeapStatusProvider;
import com.kerb4j.client.SpnegoClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Boolean.TRUE;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.ssl.KeyStoreScanner;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory;
import org.eclipse.jetty.quic.quiche.server.QuicheServerConnector;
import org.eclipse.jetty.quic.quiche.server.QuicheServerQuicConfiguration;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import java.nio.file.Path;
import java.util.Hashtable;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import java.util.List;
import java.util.Properties;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import static net.coplanar.app.KerberosKeytabReader.createGSSContext;
import static net.coplanar.app.KerberosKeytabReader.createGSSCredential;
import static net.coplanar.app.KerberosKeytabReader.getCredentialFromKeytab;
import static net.coplanar.app.KerberosKeytabReader.readKeytabFile;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import net.coplanar.ents.*;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.session.SessionHandler;
import org.hibernate.HibernateException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SrvApp
{
    public static SessionFactory sf;
    public static Hashtable<String, String> ldap_env;
    public static GSSCredential srv_cred;
    public static SpnegoClient sp_client;
    public static Server server;
    public static AppSessions as;
    

    //private static final ShutdownObject shutdownObject = new ShutdownObject();

    private static class ShutdownObject {
        int foo;
        // This is the shared object used for signaling
    }
 
    public static void main(String[] args) throws Exception
    {
        ShutdownObject shutdownObject;
        shutdownObject = new ShutdownObject();
        var mt = Thread.currentThread();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
            System.out.println("running a shutdown hook.");
            synchronized (shutdownObject) {
                System.out.println("Thread B is sending a signal...");
                shutdownObject.notify();
            }
            //System.out.println("Thread B is waiting for ...");
            //try {
                //mt.join();
                //Thread.sleep(20000); // Simulate some work in Thread B
            //} catch (InterruptedException ex) {
            //    Logger.getLogger(SrvApp.class.getName()).log(Level.SEVERE, null, ex);
            //}
            //System.out.println("Thread B is done waiting...");
            Runtime.getRuntime().halt(0);
        }));
        
        // create systemd instance
        var hsp = new SystemdHeapStatusProvider();
        var nhsp = new SystemdNonHeapStatusProvider();
        Systemd systemd = Systemd.builder()
                //.extendTimeout(10, TimeUnit.SECONDS, 10)
                //.watchdog(10, TimeUnit.SECONDS )
                //.statusUpdate(10, TimeUnit.SECONDS)
                .build();
        systemd.addStatusProviders(hsp, nhsp);

        //if (systemd.isReady()) {
            //System.out.println("Systemd is active. Managing application.");
            systemd.logStatus();
            //systemd.logStatus();
            //runApplicationLogic();
            //systemd.notifyStopping();  // Notify systemd that the application is stopping
        //} else {
        //    System.out.println("Systemd is not active. Running application without systemd management.");
	//    we should use setproctitle() here instead
            //runApplicationLogic();
        //}
        
        // GET CONFIG FILE SETTINGS
        Properties srvProp = new Properties();
        InputStream input = null;

        String keytabFilePath = null;
        try { // use try-with-resources?
            // Specify the path to your properties file
            input = new FileInputStream("config.properties");

            // Load the properties file
            srvProp.load(input);

            // Get the property values
            //String password = prop.getProperty("password");
            keytabFilePath = srvProp.getProperty("keytab");

            // Print the property values
            System.out.println("Username: " + keytabFilePath);
            //System.out.println("Password: " + password);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }   
        
        // not working from mvn exec:run
        // System.setProperty("java.util.logging.config.file", "logging.properties");
        
        // for LDAP client
        System.setProperty("java.net.preferIPv6Addresses", "true"); // java21 still not default - in 2024!
        //System.setProperty("java.net.preferIPv4Stack", "false"); // java21 at least default is false
        
        // for Kerberos auth:
        // does server even use these?  only using tickets supplied by client, not *getting* ticket from KDC using credentials
        // getting domain-realm mapping from somewhere would be nice though.
        // System.setProperty("java.security.krb5.conf", "/etc/krb5.conf"); // linux default /etc/krb5.conf shouold be OK
        // sun.security.krb5.Config.refresh(); in order to reload configuration from new file. ?
        //
        // service principal is set in this file:
        // -Djava.security.auth.login.config=jaas.conf
        //System.setProperty("java.security.auth.login.config", "jaas.conf");
        //
        // secret sauce to use less of the irrelevant JVM baked in security framework
        // -Djavax.security.auth.useSubjectCredsOnly=false
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        
        // KERBEROS STUFF
        // https://github.com/bedrin/kerb4j stop the legacy BS!
        
        System.out.println("reading keytab");


        // unfortunately we need to set SPN... maybe read it from keytab first with Apache DS?
        SpnegoClient spnegoClient = SpnegoClient.loginWithKeyTab("HTTP/mjolnir.ad.coplanar.net", keytabFilePath, true);
        sp_client = spnegoClient;
        
        srv_cred = getCredentialFromKeytab(keytabFilePath);
/*
        for (KeytabEntry entry : keytabEntries) {
            System.out.println(entry.getPrincipalName());
            System.out.println(entry.getKey().getKeyType());
            System.out.println(entry.getPrincipalType());
        }
        
        if (!keytabEntries.isEmpty()) {
            try {
                KeytabEntry entry = keytabEntries.get(0);  // Use the first entry for example
                GSSCredential credential = createGSSCredential(entry);
                srv_cred = credential;
                GSSContext context = createGSSContext(credential, entry.getPrincipalName(), "AD.COPLANAR.NET");
                System.out.println("Successfully created GSSContext.");
            } catch (GSSException e) {
                e.printStackTrace();
            }
        } */
        //System.exit(0);
        
        // HIBERNATE STUFF

        System.out.println("postgres connection setup and test...");
        // connection is initiated here, maybe only for Action.SPEC_ACTION_DROP_AND_CREATE? - but definitely to discover dialect
        // a simple bad JDBC URL gives a crazy stack trace, hard for sysadmin to interpret
        // it seems that hibernate-core/src/main/java/org/hibernate/engine/jdbc/env/internal/JdbcEnvironmentInitiator.java
        // tries to use defaults, but if dialect isn't set, this cannot succeed.
        // it should pass along (re-throw) the SQLException (at least in the case of connection problems) if dialect isn't set
        // so a simple typo in configuration file can cause an appropriate error message to be displayed to the sysadmin
        // around line 359 log.unableToObtainConnectionToQueryMetadata( e );
        // if dialect is set, the JDBC error is displayed later on when first query is run (at least you get some clue then)
        // https://discourse.hibernate.org/t/missing-sqlexceptionhelper-for-warn-hhh000342-could-not-obtain-connection-to-query-metadata/9774
        // turns out it's only happening when Agroal connection pool is used (but haven't tried others like c3p0 yet
        
        sf = new Configuration()
                        .addAnnotatedClass(Project.class)
                        .addAnnotatedClass(Rep.class)
                        .addAnnotatedClass(StatDay.class)
                        .addAnnotatedClass(TsCell.class)
                        .addAnnotatedClass(TsUser.class)
                        //.addAnnotatedClass(UserProject.class)
                        .addAnnotatedClass(ProjectSite.class)
                        .addAnnotatedClass(Province.class)
                        .addAnnotatedClass(ProjectState.class)
                        .addAnnotatedClass(ProjectStateImpl.class)
                        //.addResource("net/coplanar/ents/ProjectState.hbm.xml")
                        // PostgreSQL
                        .setProperty(AvailableSettings.JAKARTA_JDBC_URL, "jdbc:postgresql:///ts?socketFactory=org.newsclub.net.unix.AFUNIXSocketFactory$FactoryArg&socketFactoryArg=/var/run/postgresql/.s.PGSQL.5432")
                        // Credentials - not needed when using UNIX socket
                        //.setProperty(AvailableSettings.JAKARTA_JDBC_USER, "jjackson")
                        //.setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, "fool")
                        // Automatic schema export
                        
                        // SQL statement logging
                        .setProperty(AvailableSettings.SHOW_SQL, TRUE.toString())
                        .setProperty(AvailableSettings.FORMAT_SQL, TRUE.toString())
                        .setProperty(AvailableSettings.HIGHLIGHT_SQL, TRUE.toString())

                        .setProperty("hibernate.agroal.maxSize", "10")
                        // TURN OFF FOR PROD!
                        .setProperty("jakarta.persistence.schema-generation.database.action", "update") // DANGEROUS in production
                        // TURN OFF FOR PROD!  DROPS TABLES AND ALL DATA IS LOST!
                        //.setProperty(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.SPEC_ACTION_DROP_AND_CREATE) // is this same as above?

                        // in 6.5.2 had to use to get db connection error messages
                        //.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")

                        // Create a new SessionFactory
                        .buildSessionFactory();
                        // without Dialect set, 2 stack traces if postgres connection fails in 6.5.2 but 7.1.0 seems to give same error as Dialset Set case
                        // with Dialect set, org.postgresql.util.PSQLException: The connection attempt failed. is re-thrown, maybe catch that
                        // and present to user

        System.out.println("postgres server version check...");
        try (org.hibernate.Session hsession = sf.openSession()) {
            List<Object> result = hsession.createNativeQuery("SELECT version()", Object.class).getResultList();

            // Print the result
            System.out.println("Postgresql server version: " + result);
            hsession.close();

        } catch (HibernateException e) {
            System.out.println("cannot connect to DB server");
            e.printStackTrace();
            System.exit(1);
        }

        // LDAP STUFF
        
        // put CA cert used to authenticate server certificates in this JKS file
	// if not set, Debian/Ubuntu use /etc/ssl/certs/java/cacerts
	// which might make sense but is less restrictive
	// "sscep getca" might be used to auto-generate and/or update this private one
        System.setProperty("javax.net.ssl.trustStore", "tls/coplanar.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "secret");
	// PKCS12 is rumoured to work but I vaguely recall having issues
        //System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        
        var env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        // DNS SRV lookups are possible - are they ActiveDirectory compatible?
        env.put(Context.PROVIDER_URL, "ldaps://doug.ad.coplanar.net:636");  // no debug output for SSL issues? -Djavax.net.debug=all
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        //env.put(Context.SECURITY_AUTHENTICATION, "none");
        env.put(Context.SECURITY_PRINCIPAL, "CN=http-mjolnir,CN=Users,DC=ad,DC=coplanar,DC=net");
        // would be nice to be able to use the kerberos srvtab instead of a password
        env.put(Context.SECURITY_CREDENTIALS, "arglebargleZZ!!");
        // something about Active Directory and unprocessed referals
        env.put(Context.REFERRAL, "follow");

	env.put("com.sun.jndi.ldap.connect.timeout", "5000"); // with no timeout, SSL errors are mysterious 

        // Optional read timeout (per operation)
        //env.put("com.sun.jndi.ldap.read.timeout", "5000");

        // Enable connection pooling
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        
        // Use custom socket factory for detailed SSL diagnostics
        //env.put("java.naming.ldap.factory.socket", "net.coplanar.app.DiagnosticSSLSocketFactory");
        
        // logging: -Dcom.sun.jndi.ldap.connect.pool.debug=fine
        ldap_env = env;

        try {
            DirContext ctx;
            ctx = new InitialDirContext(ldap_env);
            // do test lookup
            String searchFilter = "(objectClass=*)";
            String searchBase = "";
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.OBJECT_SCOPE);
            // make results less verbose
            //searchControls.setReturningAttributes(new String[] { "rootDomainNamingContext" });

            NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);

            System.out.println("test LDAP lookup: ");

            while (results.hasMore()) {
                SearchResult searchResult = results.next();
                Attributes attrs = searchResult.getAttributes();

                // Print all attributes
                NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
                while (allAttrs.hasMore()) {
                    Attribute attr = allAttrs.next();
                    System.out.println(attr.getID() + ": " + attr);
                }
            }
            ctx.close();
        } catch (javax.naming.CommunicationException e) {
            System.err.println("LDAP Communication Error:");
            System.err.println("  Server: " + env.get(Context.PROVIDER_URL));
            System.err.println("  Error: " + e.getMessage());
            
            // Walk through the entire exception chain for detailed error information
            System.err.println("\nException Chain Analysis:");
            Throwable current = e;
            int level = 0;
            
            while (current != null) {
                String indent = "  ".repeat(level + 1);
                System.err.println(indent + "Level " + level + ": " + current.getClass().getSimpleName());
                System.err.println(indent + "Message: " + current.getMessage());
                
                // Look for specific exception types and extract details
                if (current instanceof javax.net.ssl.SSLHandshakeException) {
                    System.err.println(indent + "SSL Handshake Details:");
                    System.err.println(indent + "  - Certificate validation failed");
                    System.err.println(indent + "  - Check certificate expiration, hostname, or trust chain");
                    
                    // Check if the message contains PKIX validation details
                    String message = current.getMessage();
                    if (message != null && message.contains("PKIX path validation failed")) {
                        System.err.println(indent + "  - PKIX Path Validation Failed");
                        if (message.contains("Path does not chain with any of the trust anchors")) {
                            System.err.println(indent + "  - Certificate chain does not chain with trust anchors");
                            System.err.println(indent + "  - Root CA certificate missing from truststore");
                            System.err.println(indent + "  - Add the server's CA certificate to: " + System.getProperty("javax.net.ssl.trustStore"));
                        }
                        if (message.contains("validity check failed")) {
                            System.err.println(indent + "  - Certificate validity check failed (likely expired/not yet valid)");
                        }
                    }
                } else if (current instanceof javax.net.ssl.SSLPeerUnverifiedException) {
                    System.err.println(indent + "SSL Peer Verification Failed:");
                    System.err.println(indent + "  - Server certificate not trusted");
                    System.err.println(indent + "  - Certificate may be self-signed or expired");
                } else if (current instanceof java.security.cert.CertificateExpiredException) {
                    System.err.println(indent + "Certificate Expired!");
                    System.err.println(indent + "  - Server certificate has expired");
                    System.err.println(indent + "  - Contact server administrator to renew certificate");
                } else if (current instanceof java.security.cert.CertificateNotYetValidException) {
                    System.err.println(indent + "Certificate Not Yet Valid:");
                    System.err.println(indent + "  - Certificate valid date is in the future");
                    System.err.println(indent + "  - Check system clock or certificate dates");
                } else if (current instanceof java.security.cert.CertPathValidatorException) {
                    System.err.println(indent + "Certificate Path Validation Error:");
                    System.err.println(indent + "  - Certificate chain validation failed");
                    System.err.println(indent + "  - Missing intermediate certificates or untrusted root CA");
                    
                    // Extract more details from CertPathValidatorException
                    java.security.cert.CertPathValidatorException cpve = (java.security.cert.CertPathValidatorException) current;
                    if (cpve.getReason() != null) {
                        System.err.println(indent + "  - Failure reason: " + cpve.getReason());
                    }
                    if (cpve.getIndex() >= 0) {
                        System.err.println(indent + "  - Failed at certificate index: " + cpve.getIndex());
                    }
                } else if (current instanceof java.net.ConnectException) {
                    System.err.println(indent + "Connection Refused:");
                    System.err.println(indent + "  - Server not responding on specified port");
                    System.err.println(indent + "  - Check if LDAP service is running");
                } else if (current instanceof java.net.SocketTimeoutException) {
                    System.err.println(indent + "Connection Timeout:");
                    System.err.println(indent + "  - Network timeout occurred");
                    System.err.println(indent + "  - Server may be overloaded or unreachable");
                } else if (current instanceof java.net.UnknownHostException) {
                    System.err.println(indent + "DNS Resolution Failed:");
                    System.err.println(indent + "  - Cannot resolve hostname: " + current.getMessage());
                    System.err.println(indent + "  - Check DNS settings and hostname spelling");
                }
                
                // Generic message analysis for PKIX errors that might appear in any exception
                String message = current.getMessage();
                if (message != null) {
                    boolean foundPkixError = false;
                    
                    if (message.contains("PKIX path validation failed") || 
                        message.contains("Path does not chain with any of the trust anchors") ||
                        message.contains("PKIX path building failed")) {
                        
                        System.err.println(indent + "PKIX Path Validation Issue Detected:");
                        foundPkixError = true;
                        
                        if (message.contains("Path does not chain with any of the trust anchors")) {
                            System.err.println(indent + "  - Root CA certificate is not in the truststore");
                            System.err.println(indent + "  - The certificate chain cannot be validated back to a trusted root");
                            System.err.println(indent + "  - Solution: Add server's root CA certificate to truststore");
                            System.err.println(indent + "  - Command: keytool -import -alias ldap-ca -file ca-cert.crt -keystore " + System.getProperty("javax.net.ssl.trustStore"));
                        }
                        
                        if (message.contains("PKIX path building failed")) {
                            System.err.println(indent + "  - Cannot build certificate path to trusted root");
                            System.err.println(indent + "  - Missing root CA or intermediate certificates");
                        }
                        
                        if (message.contains("validity check failed")) {
                            System.err.println(indent + "  - Certificate date validation failed");
                            System.err.println(indent + "  - Certificate may be expired or not yet valid");
                        }
                    }
                    
                    // Check for other certificate-related keywords
                    if (!foundPkixError && (message.toLowerCase().contains("certificate") || 
                                           message.toLowerCase().contains("trust anchor") ||
                                           message.toLowerCase().contains("cert path"))) {
                        System.err.println(indent + "Certificate-related error detected in message:");
                        System.err.println(indent + "  Full message: " + message);
                    }
                }
                
                // Print suppressed exceptions if any
                Throwable[] suppressed = current.getSuppressed();
                if (suppressed.length > 0) {
                    System.err.println(indent + "Suppressed exceptions:");
                    for (int i = 0; i < suppressed.length; i++) {
                        System.err.println(indent + "  [" + i + "] " + suppressed[i].getClass().getSimpleName() + ": " + suppressed[i].getMessage());
                    }
                }
                
                // Move to the next cause - check multiple ways to get the cause
                Throwable nextCause = current.getCause();
                
                // For NamingException, also check getRootCause() which might have SSL details
                if (current instanceof javax.naming.NamingException) {
                    javax.naming.NamingException ne = (javax.naming.NamingException) current;
                    Throwable rootCause = ne.getRootCause();
                    if (rootCause != null && rootCause != nextCause) {
                        System.err.println(indent + "NamingException Root Cause: " + rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage());
                        // If we haven't seen this as the regular cause, use it as next cause
                        if (nextCause == null) {
                            nextCause = rootCause;
                        }
                    }
                }
                
                current = nextCause;
                level++;
                
                // Prevent infinite loops
                if (level > 10) {
                    System.err.println(indent + "... (truncated - exception chain too deep)");
                    break;
                }
            }
            
            System.err.println("\nTruststore Configuration:");
            System.err.println("  - Truststore path: " + System.getProperty("javax.net.ssl.trustStore"));
            System.err.println("  - Truststore type: " + System.getProperty("javax.net.ssl.trustStoreType", "JKS"));
            
            System.err.println("\nLDAP connection failed. Server cannot start without LDAP.");
            System.exit(1);
        } catch (javax.naming.AuthenticationException e) {
            System.err.println("LDAP Authentication Error:");
            System.err.println("  Server: " + env.get(Context.PROVIDER_URL));
            System.err.println("  Principal: " + env.get(Context.SECURITY_PRINCIPAL));
            System.err.println("  Error: " + e.getMessage());
            System.err.println("  Check username/password credentials");
            System.exit(1);
        } catch (javax.naming.ServiceUnavailableException e) {
            System.err.println("LDAP Service Unavailable:");
            System.err.println("  Server: " + env.get(Context.PROVIDER_URL));
            System.err.println("  Error: " + e.getMessage());
            System.err.println("  Check if LDAP server is running and reachable");
            System.exit(1);
        } catch (NamingException e) {
            System.err.println("LDAP General Error:");
            System.err.println("  Server: " + env.get(Context.PROVIDER_URL));
            System.err.println("  Error type: " + e.getClass().getSimpleName());
            System.err.println("  Message: " + e.getMessage());
            
            // Print remaining name if available
            if (e.getRemainingName() != null) {
                System.err.println("  Remaining name: " + e.getRemainingName());
            }
            
            // Print resolved name if available  
            if (e.getResolvedName() != null) {
                System.err.println("  Resolved name: " + e.getResolvedName());
            }
            
            System.err.println("LDAP connection failed. Server cannot start without LDAP.");
            System.exit(1);
        }

        // JETTY STUFF
        
//        QueuedThreadPool threadPool = new QueuedThreadPool();
//        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
//        server = new Server(threadPool);
        server = new Server();

	// check that these are not expired - log message + systemd degraded status
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("tls/mjolnir-le.p12"); // FIXME security - fail if world readable
        sslContextFactory.setKeyStorePassword("secret");
        sslContextFactory.setTrustStorePath("tls/coplanar.jks"); // FIXME security - fail if world writeable
        sslContextFactory.setTrustStorePassword("secret");
        sslContextFactory.setWantClientAuth(true);
        System.out.println("SNI REQ: " + sslContextFactory.isSniRequired());

	// watch & hot-reload the keystore:
	KeyStoreScanner scanner = new KeyStoreScanner(sslContextFactory);
	scanner.setScanInterval(60); // seconds
	server.addBean(scanner);

        
        //server.setHandler(new SrvApp());

        // The plain HTTP configuration.
        HttpConfiguration plainConfig = new HttpConfiguration();
        
        // The secure HTTP configuration.
        HttpConfiguration secureConfig = new HttpConfiguration(plainConfig);
        SecureRequestCustomizer src = new SecureRequestCustomizer();
        System.out.println("SRC SNI REQ before disabling: " + src.isSniHostCheck());
        src.setSniHostCheck(false); // INSECURE
        secureConfig.addCustomizer(src);
        secureConfig.addCustomizer(new AltSvcHeaderCustomizer("h3=\":443\"; ma=86400, h2=\":443\"; ma=86400"));
	// what about HSTS?
        
        // First, create the secure connector for HTTPS and HTTP/2.
        HttpConnectionFactory https = new HttpConnectionFactory(secureConfig);
        HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(secureConfig);
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(https.getProtocol());
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, https.getProtocol());
        ServerConnector secureConnector = new ServerConnector(server, 1, 1, ssl, alpn, http2, https);
        secureConnector.setPort(443);
        server.addConnector(secureConnector);
        
        // Second, create the plain connector for HTTP.
        HttpConnectionFactory http = new HttpConnectionFactory(plainConfig);
        ServerConnector plainConnector = new ServerConnector(server, 1, 1, http);
        plainConnector.setPort(80);
        server.addConnector(plainConnector);
        
        // Third, create the connector for HTTP/3.
        Path pemWorkDir = Path.of("tls");
        //ServerQuicheConfiguration serverQuicConfig = new ServerQuicheConfiguration(sslContextFactory, pemWorkDir);
        //QuicServerConnector http3Connector = new QuicServerConnector(server, serverQuicConfig, new HTTP3ServerConnectionFactory(serverQuicConfig));
	QuicheServerQuicConfiguration serverQuicConfig = new QuicheServerQuicConfiguration(pemWorkDir);
	QuicheServerConnector http3Connector = new QuicheServerConnector(server, sslContextFactory, serverQuicConfig, new HTTP3ServerConnectionFactory());

        http3Connector.setPort(443);
        server.addConnector(http3Connector);
        
        // OUR HANDLER STUFF
      
        // Create and configure a ResourceHandler.
        ResourceHandler handler = new ResourceHandler();
        // Configure the directory where static resources are located.
        handler.setBaseResource(ResourceFactory.of(handler).newResource("src/main/html/"));
        // Configure directory listing.
        handler.setDirAllowed(false);
        // Configure welcome files.
        handler.setWelcomeFiles(List.of("index.html"));
        // Configure whether to accept range requests.
        handler.setAcceptRanges(true);

        ContextHandler resourceHandler = new ContextHandler(handler, "/");

        // Link the context to the server.
        //resourceHandler.setHandler(handler);
        ContextHandler hwctx = new ContextHandler(new SseHandler(server.getThreadPool()), "/sse2");
        ContextHandler postit = new ContextHandler(new Tsc2(), "/tsc2");
                
        
        ContextHandlerCollection contexts = new ContextHandlerCollection(false);  // keep things nonblocking
        contexts.addHandler(resourceHandler);
        contexts.addHandler(postit);
        contexts.addHandler(hwctx);

        // create our custom AppSessions
        as = new AppSessions();
        
        //Handler sthread = new SessionThreadHandler(contexts);
        var sthread = new ThreadTypeHandler(contexts);  // *after* session created
        sthread.setAppSession(as);

        var kh = new KerberosHandler(sthread);
        // configure kh to use it
        kh.setAppSession(as);
        
        // Create and link the SessionHandler.
        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setHandler(kh);
        //sessionHandler.setHandler(contexts);
        sessionHandler.setSessionPath("/");
 
        // F1
        //var threadListener = new ThreadSessionLifecycleListener();
        //server.setAttribute("sessionListener", threadListener);
        
        //server.setHandler(new KerberosHandler(sessionHandler));
        server.setHandler(sessionHandler);
        
        //server.setHandler(contexts);

        
        // Set up a listener so that when the secure connector starts,
        // it configures the other connectors that have not started yet.
        secureConnector.addEventListener(new NetworkConnector.Listener()
        {
            @Override
            public void onOpen(NetworkConnector connector)
            {
                int port = connector.getLocalPort();
        
                // Configure the plain connector for secure redirects from http to https.
                plainConfig.setSecurePort(443);
        
                // Configure the HTTP3 connector port to be the same as HTTPS/HTTP2.
                //http3Connector.setPort(8443);
            }
        });
        
	try {
		server.start();
		// use lifecycle listener to notify() us
		// wait() until notified start is complete
		systemd.ready();
		synchronized (shutdownObject) {
			try {
				System.out.println("Thread A is waiting for a signal...");
				shutdownObject.wait();
				System.out.println("Thread A received a signal!");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	} catch (Throwable t) {
                System.out.println("server.start() Exception!");
		t.printStackTrace(System.err);
                System.out.println("server.start() Exception!");
		// ensure we don’t leave a thread pool running
		//try { server.stop(); } catch (Exception ignore) {}
		//System.exit(1);            // <- make the wrapper/systemd see failure
	} 


        // notify all sessions of shutdown, wait a few seconds
        server.stop();

        System.out.println("Notifying systemd of stop");
        systemd.stopping(); // nothing is logged by systemd
        System.out.println("Systemd notified of stop");

        server.join();

	if (sf != null && !sf.isClosed()) {
		sf.close();
	}
        systemd.close(); // optional? nothing is logged by systemd
        System.out.println("Thread A done.");
    }
}
