/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

/**
 *
 * @author jjackson
 */
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.ietf.jgss.*;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.List;


public class KerberosKeytabReader {

    static GSSName gssServiceName;
    
    public static GSSCredential getCredentialFromKeytab(String keytabFilePath) {
        GSSCredential credential = null;

            List<KeytabEntry> keytabEntries = readKeytabFile(keytabFilePath);

        if (keytabEntries != null && !keytabEntries.isEmpty()) {
            for (KeytabEntry entry : keytabEntries) {
                System.out.println(entry);
                System.out.println(entry.getPrincipalName());
                System.out.println(entry.getKey().getKeyType().getOrdinal());
                System.out.println(entry.getKeyVersion());
            }


            try {
                //KeytabEntry entry = keytabEntries.get(3);  // Use the first entry for example
                credential = createGSSCredential(keytabEntries);
                System.out.println("\nSuccessfully created credential: " + credential.toString());
                // rest for testing
/*
                GSSContext context = createGSSContext(credential, entry.getPrincipalName().split("@")[0], "AD.COPLANAR.NET");
                //System.out.println("\nSuccessfully created GSSContext." + context.getSrcName() + "\n");
                System.out.println("\nSuccessfully created GSSContext." + context.getTargName() + "\n");

                // Assuming we have a token to accept
                byte[] token = {}; // Replace with actual token to accept
                byte[] outToken = new byte[4096]; // Buffer for output token
                context.acceptSecContext(token, 0, token.length);

                if (context.isEstablished()) {
                    System.out.println("Security context successfully established.");
                }
*/
            } catch (GSSException e) {
                e.printStackTrace();
            }
        }
        return credential;
    }

    public static List<KeytabEntry> readKeytabFile(String filePath) {
        try {
            Keytab keytab = Keytab.read(new File(filePath));
            return keytab.getEntries();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static GSSCredential createGSSCredential(List<KeytabEntry> entries) throws GSSException {
        GSSManager manager = GSSManager.getInstance();
        // needed for HTTP Negotiate auth
        Oid krb5Oid = new Oid("1.3.6.1.5.5.2");

        for (KeytabEntry entry : entries) {
            String principalName = entry.getPrincipalName().split("@")[0];  // remove realm - does it break for cross-realm names?
            GSSName gssName = manager.createName(principalName, GSSName.NT_USER_NAME);
            gssServiceName = gssName;
        }

        // Create GSSCredential directly
        try {
            return manager.createCredential(gssServiceName, GSSCredential.DEFAULT_LIFETIME, krb5Oid, GSSCredential.ACCEPT_ONLY);
        } catch (GSSException e) {
            throw new RuntimeException(e);
        }
    }

    public static GSSContext createGSSContext(GSSCredential credential, String serviceName, String realm) throws GSSException {
        GSSManager manager = GSSManager.getInstance();

        GSSName servicePrincipal = manager.createName(serviceName + "@" + realm, GSSName.NT_USER_NAME);
        Oid krb5Oid = new Oid("1.2.840.113554.1.2.2");

        return manager.createContext(servicePrincipal, krb5Oid, credential, GSSContext.DEFAULT_LIFETIME);
    }

    

}
