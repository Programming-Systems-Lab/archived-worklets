package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @Dan Phung
 *
*/

import java.rmi.server.*;
import java.net.*;

/**
 * WVM_RMIClassLoaderSpi
 * questions: do i need to add some disclaimer when i'm using sun.stuff?
 *
 * need System property WVM_FILE to be set.  In the WVM file you 
 * to have at least the WVM_KEYSFILE and WVM_PASSWORD setting.
 *
 * 
 *
 */

public class WVM_RMIClassLoaderSpi extends RMIClassLoaderSpi {
  private WVM_SSLSocketFactory _WVM_sf;
  private ClassLoader _default;

  public WVM_RMIClassLoaderSpi(){ 
    this(ClassLoader.getSystemClassLoader());
  }

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

  public Class loadClass(URL codebase, String name) throws MalformedURLException, ClassNotFoundException {
    return loadClass("" + codebase, name, _default);
  }

  public Class loadClass(String codebase, String name) 
    throws MalformedURLException, ClassNotFoundException { 
    return loadClass(codebase, name, _default);
  }

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
      WVM_ClassLoader _loader = new WVM_ClassLoader_New(urls, _WVM_sf);

      try {
	return _loader.findClass(name);
      } catch (ClassNotFoundException e) {
	// WVM.err.println("Couldn't find the class <"+name+"> through our methods, going to defaultLoader");
      }
    }

    // WVM.out.println("defaultLoader ClassLoader loading: " + codebase + ", " + name);
    return sun.rmi.server.LoaderHandler.loadClass(codebase, name, defaultLoader);
  }

  // All of these methods use the default sun LoaderHandler implementation
  public Class loadProxyClass(String codebase ,String[] interfaces, ClassLoader defaultLoader) 
    throws MalformedURLException, ClassNotFoundException {     
    return sun.rmi.server.LoaderHandler.loadProxyClass(codebase, interfaces, defaultLoader); 
  }
  public ClassLoader getClassLoader(String codebase) throws MalformedURLException { 
    return sun.rmi.server.LoaderHandler.getClassLoader(codebase); 
  }
  public String getClassAnnotation(Class c){ 
    return sun.rmi.server.LoaderHandler.getClassAnnotation(c); 
  }
}
