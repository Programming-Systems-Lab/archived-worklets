package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc, @author Peppo Valetto
 *
*/

import java.net.*;

class WVM_ClassLoader extends URLClassLoader {
  WVM_ClassLoader(URL[] urls) {	
    super(urls);
  }
  public Class findClass( String name) throws ClassNotFoundException {
    return ((Class) super.findClass(name));
  }
  public void addCodebase(URL[] urls) {
    for (int i=0; i<urls.length; i++) {
      addURL(urls[i]);
    }
  }
}
