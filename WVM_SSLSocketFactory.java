package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/**
 * WVM_SSLSocketFactory @author Dan Phung
 * 
 * 
 * 
 * 
*/

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.rmi.server.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/*
 *
 * NOTE: sets setNeedClientAuth(true) inside this class.
 */
 
public class WVM_SSLSocketFactory 
  extends RMISocketFactory
  implements RMIServerSocketFactory, RMIClientSocketFactory, Serializable {

  private String _keysFile;	// the key file to use for the factory
  private String _password;	// the password for the keyfile
  private String _ctxInstance;	// the context instance (default: TLS)
  private String _kmfInstance;	// the key manager factory instance (default: SunX509)
  private String _ksInstance;	// the key store instance (default: JKS)
  private String  _rng; // the algorithm used in the random number generator

  transient private SSLServerSocketFactory ssf;
  transient private SSLSocketFactory sf;

  static public int _instance = 0;

  public WVM_SSLSocketFactory(String keysfile, String password) {
    this(keysfile, password, null, null, null, null); 
  } 

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

  public SSLSocketFactory getSSLSocketFactory() {
    // WVM.out.println("WVM_SSLSocketFactory: returning a SSLSocketFactory: <" + _instance + ">");
    if (sf == null) initFactories();
    return sf;
  }

  public SSLServerSocketFactory getSSLServerSocketFactory() {
    // WVM.out.println("WVM_SSLSocketFactory: returning a SSLServerSocketFactory: <" + _instance + ">");
    if (ssf == null) initFactories();
    return ssf;
  }

  public ServerSocket createServerSocket(int port) throws IOException{
    // WVM.out.println("WVM_SSLSocketFactory: returning a Secure Server Socket: <" + _instance + ">");
    if (ssf == null) initFactories();
    SSLServerSocket server_socket = (SSLServerSocket) ssf.createServerSocket(port);
    server_socket.setNeedClientAuth(true);
    return server_socket;
  }

  public Socket createSocket(String host, int port) throws IOException{
    // WVM.out.println("WVM_SSLSocketFactory: returning a Secure Socket: <" + _instance + ">");
    if (sf == null) initFactories();
    return (SSLSocket) sf.createSocket(host, port);
  }
}
