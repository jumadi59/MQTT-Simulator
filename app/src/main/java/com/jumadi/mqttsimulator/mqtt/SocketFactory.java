package com.jumadi.mqttsimulator.mqtt;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

/**
 * Original SocketFactory file taken from https://github.com/owntracks/android
 */

public class SocketFactory extends javax.net.ssl.SSLSocketFactory {
    private javax.net.ssl.SSLSocketFactory factory;

    public static class SocketFactoryOptions {

        private InputStream caCrtInputStream;
        private InputStream caClientP12InputStream;
        private String caClientP12Password;

        public SocketFactoryOptions withCaInputStream(InputStream stream) {
            this.caCrtInputStream = stream;
            return this;
        }

        public SocketFactoryOptions withClientP12InputStream(InputStream stream) {
            this.caClientP12InputStream = stream;
            return this;
        }

        public SocketFactoryOptions withClientP12Password(String password) {
            this.caClientP12Password = password;
            return this;
        }

        public boolean hasCaCrt() {
            return caCrtInputStream != null;
        }

        public boolean hasClientP12Crt() {
            return caClientP12Password != null;
        }

        public InputStream getCaCrtInputStream() {
            return caCrtInputStream;
        }

        public InputStream getCaClientP12InputStream() {
            return caClientP12InputStream;
        }

        public String getCaClientP12Password() {
            return caClientP12Password;
        }

        public boolean hasClientP12Password() {
            return (caClientP12Password != null) && !caClientP12Password.equals("");
        }
    }

    public SocketFactory() throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException, java.security.cert.CertificateException, UnrecoverableKeyException {
        this(new SocketFactoryOptions());
    }


    private TrustManagerFactory tmf;

    public SocketFactory(SocketFactoryOptions options) throws KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException, java.security.cert.CertificateException, UnrecoverableKeyException {
        Log.v(this.toString(), "initializing CustomSocketFactory");

        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");


        if (options.hasCaCrt()) {
            Log.v(this.toString(), "MQTT_CONNECTION_OPTIONS.hasCaCrt(): true");

            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);

            CertificateFactory caCF = CertificateFactory.getInstance("X.509");
            X509Certificate ca = (X509Certificate) caCF.generateCertificate(options.getCaCrtInputStream());
            String alias = ca.getSubjectX500Principal().getName();
            // Set propper alias name
            caKeyStore.setCertificateEntry(alias, ca);
            tmf.init(caKeyStore);

            Log.d(this.toString(), "Certificate Owner: %s" + ca.getSubjectDN().toString());
            Log.d(this.toString(), "Certificate Issuer: %s" + ca.getIssuerDN().toString());
            Log.d(this.toString(), "Certificate Serial Number: %s" + ca.getSerialNumber().toString());
            Log.d(this.toString(), "Certificate Algorithm: %s" + ca.getSigAlgName());
            Log.d(this.toString(), "Certificate Version: %s" + ca.getVersion());
            Log.d(this.toString(), "Certificate OID: %s" + ca.getSigAlgOID());
            Enumeration<String> aliasesCA = caKeyStore.aliases();
            for (; aliasesCA.hasMoreElements(); ) {
                String o = aliasesCA.nextElement();
                Log.d(this.toString(), "Alias: %s isKeyEntry:%s isCertificateEntry:%s" + o + caKeyStore.isKeyEntry(o) + caKeyStore.isCertificateEntry(o));
            }

        } else {
            Log.d(this.toString(), "CA sideload: false, using system keystore");
            KeyStore keyStore = KeyStore.getInstance("AndroidCAStore");
            keyStore.load(null);
            tmf.init(keyStore);
        }

        if (options.hasClientP12Crt()) {
            Log.v(this.toString(), "MQTT_CONNECTION_OPTIONS.hasClientP12Crt(): true");

            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");

            clientKeyStore.load(options.getCaClientP12InputStream(), options.hasClientP12Password() ? options.getCaClientP12Password().toCharArray() : new char[0]);
            kmf.init(clientKeyStore, options.hasClientP12Password() ? options.getCaClientP12Password().toCharArray() : new char[0]);

            Log.v(this.toString(), "Client .p12 Keystore content: ");
            Enumeration<String> aliasesClientCert = clientKeyStore.aliases();
            for (; aliasesClientCert.hasMoreElements(); ) {
                String o = aliasesClientCert.nextElement();
                Log.d(this.toString(), "Alias: %s" + o);
            }
        } else {
            Log.v(this.toString(), "Client .p12 sideload: false, using null CLIENT cert");
            kmf.init(null, null);
        }

        // Create an SSLContext that uses our TrustManager
        TrustManager[] wrappedTrustManagers = getWrappedTrustManagers(tmf.getTrustManagers());
        SSLContext context = SSLContext.getInstance("TLSv1.1");
        context.init(kmf.getKeyManagers(), wrappedTrustManagers, null);
        this.factory = context.getSocketFactory();

    }

    @Override
    public String[] getDefaultCipherSuites() {
        return this.factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return this.factory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        SSLSocket r = (SSLSocket) this.factory.createSocket();
        r.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket r = (SSLSocket) this.factory.createSocket(s, host, port, autoClose);
        r.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {

        SSLSocket r = (SSLSocket) this.factory.createSocket(host, port);
        r.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        SSLSocket r = (SSLSocket) this.factory.createSocket(host, port, localHost, localPort);
        r.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket r = (SSLSocket) this.factory.createSocket(host, port);
        r.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket r = (SSLSocket) this.factory.createSocket(address, port, localAddress, localPort);
        r.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
        return r;
    }

    private TrustManager[] getWrappedTrustManagers(TrustManager[] trustManagers) {
        final X509TrustManager originalTrustManager = (X509TrustManager) trustManagers[0];
        return new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return originalTrustManager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        try {
                            if (certs != null && certs.length > 0) {
                                certs[0].checkValidity();
                            } else {
                                originalTrustManager.checkClientTrusted(certs, authType);
                            }
                        } catch (CertificateNotYetValidException e) {
                            e.printStackTrace();
                        } catch (CertificateExpiredException e) {
                            e.printStackTrace();
                        } catch (java.security.cert.CertificateException e) {
                            e.printStackTrace();
                        }
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        try {
                            if (certs != null && certs.length > 0) {
                                certs[0].checkValidity();
                            } else {
                                originalTrustManager.checkServerTrusted(certs, authType);
                            }
                        } catch (CertificateNotYetValidException e) {
                            e.printStackTrace();
                        } catch (CertificateExpiredException e) {
                            e.printStackTrace();
                        } catch (java.security.cert.CertificateException e) {
                            e.printStackTrace();
                        }
                    }
                }
        };
    }
}
