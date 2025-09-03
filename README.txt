Demonstration of all of the following thrown together.  Shaken, not stirred.

-Java VirtualThreads, 1 thread per HTTP session, application uses plain blocking code
-HTTP/3, Jetty Embedded, event driven Javascript + Java backend
-Server Sent Events (SSE) aka browser Javascript EventSource
-Hibernate with Unix Domain Sockets via junixsocket
-PostGis and OpenLayers
-HTML CustomElement
-browser importmap
-IPv6
-HTTP authentication Negitiate/Kerberos or mTLS (Client Certificate)
-Active Directory user lookup via LDAP
-systemd service status
-ACME certificate management, live reloading
-Umple state machine, persisted with Hibernate
-Typescript using exported types from Java

production configuration settings:

jaas.conf: Kerberos keytab and service principal name
-get from Samba AD DC # samba-tool domain exportkeytab 
-or domain member
 # net ads enctypes list http-mjolnir
 # samba-tool domain exportkeytab http-mjolnir.keytab --principal=HTTP/mjolnir.ad.coplanar.net
-newer implementation gets SPN from keytab

/etc/krb5.conf: maybe needed for Kerberos realm and KDC
-should already be setup following Samba AD install guide

LDAP server URL: hardcoded

LDAP server: login user and password - hardcoded

LDAP server trusted CA cert: tls/coplanar.jks but can be set with JVM properties ie -Dfoo=bar

Postgres db: hardcoded
-set in MrTimesheet config file?

Postgres auth: uses unix socket, so no config needed here (add user running jvm to postgres)

DB WARNING: schema-generation set to UPDATE which is DANGEROUS IN PRODUCTION
-set in MrTimesheet config file?

logging: logging.properties is set on JVM commandline
-may not be possible to set in config file

SSL certs: tls/mjolnir.p12 pw:secret
-config file

SSL trust store for client auth CA cert: tls/coplanar.jks pw:secret
-config file

HTTP3 PEM working dir: /ldata/jjackson/build/elephant/tls can this be made relative?
-config file or sensible default /run/ ?

systemd unit file

port# and hostname for HTTP service


