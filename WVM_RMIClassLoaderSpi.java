/*
 * @(#)WVM_RMIClassLoaderSpi.java
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

import java.rmi.server.*;
import java.net.*;

/**
 * The WVM based RMI ClassLoader that uses secure sockets to load
 * the classes.  Intended for interation with secure RMI servers. <br>
 * It is very important that the WVM_FILE system property be set, and
 * that that paramter is the file holding the security related parameters.
 * See the Worklet Documention for more information.
 */
/* IMPORTANT: need System property WVM_FILE to be set.  In the WVM file you
 * to have at least the WVM_KEYSFILE and WVM_PASSWORD setting.
 */
public class WVM_RMIClassLoaderSpi extends RMIClassLoaderSpi {
  /** SocketFactory to use */
  private WVM_SSLSocketFactory _WVM_sf;
  /** ClassLoader to fall back on */
  private ClassLoader _default;

  /** Creates a WVM_RMIClassLoaderSpi using the system default class loader */
  public WVM_RMIClassLoaderSpi(){
    this(ClassLoader.getSystemClassLoader());
  }

  /**
   * Creates a WVM_RMIClassLoaderSpi with a {@link ClassLoader}
   * default to try if our methods fail <br>
   * It is very important that the WVM_FILE system property be set, and
   * that that paramter is the file holding the security related parameters.
   * See the Worklet Documention for more information.
   *
   * @param dft: default {@link ClassLoader}
   */
  public WVM_RMIClassLoaderSpi(ClassLoader dft){
    _default = dft;
    String wvmfile = System.getProperty("WVM_FILE");
    OptionsParser op = new OptionsParser("WVM_RMIClassLoaderSpi");
    op.loadWVMFile(wvmfile);
    _WVM_sf = new WVM_SSLSocketFactory(op.keysfile, op.password,
				       System.getProperty("WVM_SSLCONTEXT"),
				       System.getProperty("WVM_KEYMANAGER"),
				       System.getProperty("WVM_KEYSTORE"),
				       System.getProperty("WVM_RNG"));
  }

  /**
   * Loads the class through {@link WVM_ClassLoader} if the class is not
   * from the java, javax, sun, sunw, or psl packages.
   *
   * @param codebase: <code>URL</code> of codebase of the class to load
   * @param name: name of the class to load
   * @return the class that was retrieved
   * @throws MalformedURLException if codebase is a malformed URL
   * @throws ClassNotFoundException if the class could not be loaded
   */
  public Class loadClass(URL codebase, String name) throws MalformedURLException, ClassNotFoundException {
    return loadClass("" + codebase, name, _default);
  }

  /**
   * Loads the class through {@link WVM_ClassLoader} if the class is not
   * from the java, javax, sun, sunw, or psl packages.
   *
   * @param codebase: codebase of the class
   * @param name: name of the class to load
   * @return the class that was retrieved
   * @throws MalformedURLException if codebase is a malformed URL
   * @throws ClassNotFoundException if the class could not be loaded
   */
  public Class loadClass(String codebase, String name)
    throws MalformedURLException, ClassNotFoundException {
    return loadClass(codebase, name, _default);
  }

  /**
   * Loads the class through {@link WVM_ClassLoader} if the class is not
   * from the java, javax, sun, sunw, or psl packages.
   *
   * @param codebase: codebase of the class
   * @param name: name of the class to load
   * @param defaultLoader: default loader to fall back on
   * @return the class that was retrieved
   * @throws MalformedURLException if codebase is a malformed URL
   * @throws ClassNotFoundException if the class could not be loaded
   */
  public Class loadClass(String codebase, String name, ClassLoader defaultLoader)
    throws MalformedURLException, ClassNotFoundException{

    // we do not want to handle the loading of these classes...
    // dp2041: to debug, why is it that the loading of psl classes cause a class cast exception?
    // also, what is this stuff, because we're having to load it:
    // + + + request is: GET /[Ljava.lang.StackTraceElement;.class HTTP/1.1
    if (!name.matches("^java[.].*") && !name.matches("^javax[.].*")
	&& !name.matches("^sun[.].*") && !name.matches("^sunw[.].*")
	&& !name.matches("^psl[.].*")
	) {
      // WVM.out.println("WVM_RMIClassLoaderSpi loading: " + codebase + ", " + name);
      URL urls[] = new URL[1];
      urls[0] = new URL(codebase);
      WVM_ClassLoader _loader = new WVM_ClassLoader(urls, _WVM_sf);

      try {
	return _loader.findClass(name);
      } catch (ClassNotFoundException e) {
	// WVM.err.println("Couldn't find the class <"+name+"> through our methods, going to defaultLoader");
      }
    }

    // WVM.out.println("defaultLoader ClassLoader loading: " + codebase + ", " + name);
    return sun.rmi.server.LoaderHandler.loadClass(codebase, name, defaultLoader);
  }

  /** use the default Sun <code>LoaderHandler</code> implementation */
  public Class loadProxyClass(String codebase ,String[] interfaces, ClassLoader defaultLoader)
    throws MalformedURLException, ClassNotFoundException {
    return sun.rmi.server.LoaderHandler.loadProxyClass(codebase, interfaces, defaultLoader);
  }
  /** use the default Sun <code>LoaderHandler</code> implementation */
  public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
    return sun.rmi.server.LoaderHandler.getClassLoader(codebase);
  }
  /** use the default Sun <code>LoaderHandler</code> implementation */
  public String getClassAnnotation(Class c){
    return sun.rmi.server.LoaderHandler.getClassAnnotation(c);
  }
}
