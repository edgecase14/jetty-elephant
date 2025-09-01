production configuration settings:

jaas.conf: Kerberos keytab and service principal name
-get from Samba AD DC # samba-tool ext-keytab ....
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


