/*
 * @(#)WVM_SSLSocketFactory.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2002: @author Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.rmi.server.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Secure Socket Layer (SSL) Socket Factory implemented for the Worklets
 * system
 * <br>
 * NOTE: internally sets setNeedClientAuth(true)
 */
public class WVM_SSLSocketFactory
  extends RMISocketFactory
  implements RMIServerSocketFactory, RMIClientSocketFactory, Serializable
{
  /** file holding the public/private keys */
  private String _keysFile;
  /** password into the keysfile */
  private String _password;
  /** String instance of <code>SSLContext</code> (default: TLS) */
  private String _ctxInstance;
  /** String instance of <code>KeyManagerFactory</code> (default: SunX509) */
  private String _kmfInstance;
  /** String instance of <code>KeyStore</code> (default: JKS)*/
  private String _ksInstance;
  /** algorithm used in the random number generator */
  private String  _rng;

  /** Local, internally created and used server socket factory */
  transient private SSLServerSocketFactory ssf;
  /** Local, internally created and used client socket factory */
  transient private SSLSocketFactory sf;

  /** Keeps track of the WVMSSLSocketFactories created */
  static private int _instance = 0;

  /**
   * Creates a {@link WVM} SSL Socket Factory
   *
   * @param keysfile: file holding the public/private keys
   * @param password: password into the keysfile
   */
  public WVM_SSLSocketFactory(String keysfile, String password) {
    this(keysfile, password, null, null, null, null);
  }

  /**
   * Creates a {@link WVM} SSL Socket Factory
   *
   * @param keysfile: file holding the public/private keys
   * @param password: password into the keysfile
   * @param ctxInstance: String instance of <code>SSLContext</code> (default: TLS)
   * @param kmfInstance: String instance of <code>KeyManagerFactory</code> (default: SunX509)
   * @param ksInstance: String instance of <code>KeyStore</code> (default: JKS)
   * @param rng: algorithm used in the random number generator
   */
  public WVM_SSLSocketFactory(String keysfile, String password, String ctxInstance,
			      String kmfInstance, String ksInstance, String rng) {
    if (keysfile != null && password != null){
      _keysFile = keysfile;
      _password = password;
    } else
      WVM.err.println("Warning, keysfile and password are null.");

    if (ctxInstance != null)
      _ctxInstance = ctxInstance;
    else
      _ctxInstance = "TLS";

    if (kmfInstance != null)
      _kmfInstance = kmfInstance;
    else
      _kmfInstance = "SunX509";

    if (ksInstance != null)
      _ksInstance = ksInstance;
    else
      _ksInstance = "JKS";

    if (rng != null)
      _rng = rng;
    else
      _rng = "SHA1PRNG";

    // WVM.out.println("created instance: <" + (++_instance) + "> of WVM socket factory");
  }

  /**
   * Initializes the factories.  We have to re-initialize the because
   * the WVM_SSLSocketFactory could be send over the network in which case the
   * socket factories would be null so they have to be recreated.
   */
  private void initFactories(){
    try {
      // private String _password is set at WVM_Transporter creation
      char[] passphrase = _password.toCharArray();

      SSLContext ctx = SSLContext.getInstance(_ctxInstance);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(_kmfInstance);
      KeyStore ks = KeyStore.getInstance(_ksInstance);
      TrustManager tm[] = null;
      SecureRandom srand = SecureRandom.getInstance(_rng);

      ks.load(new FileInputStream(_keysFile), passphrase);
      kmf.init(ks, passphrase);
      ctx.init(kmf.getKeyManagers(), tm, srand);

      ssf = (SSLServerSocketFactory) ctx.getServerSocketFactory();
      sf = (SSLSocketFactory) ctx.getSocketFactory();
    } catch (Exception e){
      WVM.err.println("Caught exception in WVM_SSLSocketFactory: " + e);
      e.printStackTrace();
    }
  }

  /**
   * Gets the SSL client socket factory
   *
   * @return SSL client socket factory
   */
  public SSLSocketFactory getSSLSocketFactory() {
    // WVM.out.println("WVM_SSLSocketFactory: returning a SSLSocketFactory: <" + _instance + ">");
    if (sf == null) initFactories();
    return sf;
  }

  /**
   * Gets the SSL  server socket factory
   *
   * @return SSL server socket factory
   */
  public SSLServerSocketFactory getSSLServerSocketFactory() {
    // WVM.out.println("WVM_SSLSocketFactory: returning a SSLServerSocketFactory: <" + _instance + ">");
    if (ssf == null) initFactories();
    return ssf;
  }

  /**
   * Creats a SSL server socket
   *
   * @return SSL server socket
   */
  public ServerSocket createServerSocket(int port) throws IOException{
    // WVM.out.println("WVM_SSLSocketFactory: returning a Secure Server Socket: <" + _instance + ">");
    if (ssf == null) initFactories();
    SSLServerSocket server_socket = (SSLServerSocket) ssf.createServerSocket(port);
    server_socket.setNeedClientAuth(true);
    return server_socket;
  }

  /**
   * Creats a SSL client socket
   *
   * @return SSL client socket
   */
  public Socket createSocket(String host, int port) throws IOException{
    // WVM.out.println("WVM_SSLSocketFactory: returning a Secure Socket: <" + _instance + ">");
    if (sf == null) initFactories();
    return (SSLSocket) sf.createSocket(host, port);
  }
}
