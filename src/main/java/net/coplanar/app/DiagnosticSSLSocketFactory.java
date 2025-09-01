package net.coplanar.app;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Custom SSL socket factory that provides detailed exception information
 * for SSL connection failures, then passes the connection to LDAP
 */
public class DiagnosticSSLSocketFactory extends SSLSocketFactory {
    
    private SSLSocketFactory defaultFactory;
    
    public DiagnosticSSLSocketFactory() {
        defaultFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }
    
    public static SocketFactory getDefault() {
        return new DiagnosticSSLSocketFactory();
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        System.out.println("DiagnosticSSLSocketFactory: Creating SSL socket to " + host + ":" + port);
        
        try {
            SSLSocket socket = (SSLSocket) defaultFactory.createSocket(host, port);
            socket.setSoTimeout(10000); // 10 second timeout
            
            // Perform handshake and capture detailed error information
            System.out.println("DiagnosticSSLSocketFactory: Starting SSL handshake...");
            socket.startHandshake();
            
            System.out.println("DiagnosticSSLSocketFactory: SSL handshake successful!");
            System.out.println("  Protocol: " + socket.getSession().getProtocol());
            System.out.println("  Cipher: " + socket.getSession().getCipherSuite());
            
            // Print certificate details
            try {
                java.security.cert.Certificate[] peerCerts = socket.getSession().getPeerCertificates();
                System.out.println("  Server certificates:");
                for (int i = 0; i < peerCerts.length; i++) {
                    if (peerCerts[i] instanceof java.security.cert.X509Certificate) {
                        java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) peerCerts[i];
                        System.out.println("    [" + i + "] Subject: " + cert.getSubjectX500Principal());
                        System.out.println("        Issuer: " + cert.getIssuerX500Principal());
                        System.out.println("        Valid: " + cert.getNotBefore() + " to " + cert.getNotAfter());
                        
                        // Check certificate validity
                        try {
                            cert.checkValidity();
                            System.out.println("        Status: Valid");
                        } catch (java.security.cert.CertificateExpiredException ex) {
                            System.out.println("        Status: *** EXPIRED ***");
                        } catch (java.security.cert.CertificateNotYetValidException ex) {
                            System.out.println("        Status: *** NOT YET VALID ***");
                        }
                    }
                }
            } catch (javax.net.ssl.SSLPeerUnverifiedException ex) {
                System.out.println("  Could not get peer certificates: " + ex.getMessage());
            }
            
            return socket;
            
        } catch (Exception ex) {
            System.err.println("DiagnosticSSLSocketFactory: SSL connection failed!");
            System.err.println("Exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            
            // Print detailed exception chain
            Throwable cause = ex;
            int depth = 0;
            while (cause != null) {
                System.err.println("  Level " + depth + ": " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                
                // Look for specific SSL/certificate errors
                if (cause instanceof javax.net.ssl.SSLHandshakeException) {
                    System.err.println("    >> SSL Handshake failed");
                } else if (cause instanceof java.security.cert.CertPathValidatorException) {
                    System.err.println("    >> Certificate path validation failed");
                    java.security.cert.CertPathValidatorException cpve = (java.security.cert.CertPathValidatorException) cause;
                    if (cpve.getReason() != null) {
                        System.err.println("    >> Reason: " + cpve.getReason());
                    }
                } else if (cause instanceof java.security.cert.CertificateExpiredException) {
                    System.err.println("    >> Certificate has expired");
                } else if (cause instanceof java.security.cert.CertificateNotYetValidException) {
                    System.err.println("    >> Certificate is not yet valid");
                }
                
                // Check message for PKIX errors
                String message = cause.getMessage();
                if (message != null) {
                    if (message.contains("PKIX path validation failed")) {
                        System.err.println("    >> PKIX path validation failed");
                    }
                    if (message.contains("Path does not chain with any of the trust anchors")) {
                        System.err.println("    >> Certificate chain does not link to trusted root CA");
                        System.err.println("    >> Add server's root CA to truststore: " + System.getProperty("javax.net.ssl.trustStore"));
                    }
                }
                
                cause = cause.getCause();
                depth++;
            }
            
            // Re-throw the original exception so LDAP sees the failure
            throw new IOException("SSL connection failed: " + ex.getMessage(), ex);
        }
    }
    
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(host.getHostName(), port);
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, port);
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(address.getHostName(), port);
    }
    
    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return defaultFactory.createSocket(s, host, port, autoClose);
    }
    
    @Override
    public String[] getDefaultCipherSuites() {
        return defaultFactory.getDefaultCipherSuites();
    }
    
    @Override
    public String[] getSupportedCipherSuites() {
        return defaultFactory.getSupportedCipherSuites();
    }
}