package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc, @author Peppo Valetto
 *
*/

import java.io.*;
import java.net.*;
import java.util.*;

class WVM_ClassLoader extends URLClassLoader {
  WVM_ClassLoader(URL[] urls) {  
    super(urls);
    // super(urls, null, new WVM_URLStreamHandlerFactory());
    // WVM.out.println("Added: gskc, 06-March-2001 -- customised URLStreamHandlerFactory in WVM_ClassLoader");
  }
  public Class findClass(String name) throws ClassNotFoundException {
    // WVM.out.println(" ... loading class through URLClassLoader: " + name);

    HashSet hs = null;
    Thread currentThread = Thread.currentThread();
    if (currentThread instanceof psl.worklets.WVM_RMI_Transporter) {
      // object is not completely loaded in yet ...
      // we can get the relevant class bytecodes via BytecodeRetrieval soon
      hs = ((WVM_RMI_Transporter) currentThread).classHashSet;
      // get HashSet for building up list of class names from Transporter
      // WVM.out.println("added " + name + " to classHashSet");
    } else if (currentThread instanceof Thread) {
      // object is completely loaded, damn thing still requires classes
      // from the origin site :(
      String wklName = currentThread.getName();
      Hashtable tmpHs = WVM._activeWorklets;
      Worklet wkl = (Worklet) tmpHs.get(wklName);
      hs = wkl.classHashSet;
      // get HashSet for building up list of class names from Worklet via WVM
      // WVM.out.println("adding class to hashSet for: " + wklName);
    }

    try {
      // here, we're using the given functionality of URLClassLoader 
      // to automatically download the bytecode for the specified class
      Class c = (Class) super.findClass(name);
      if (hs != null) {
        hs.add(name);
      }

      /*
      // 2-try: instead of adding NAME of class to hashset, add bytecode to http server's cache
      InputStream is = super.getResourceAsStream(name);
      WVM.out.println("input stream: " + is);
      WVM.out.println("name: " + name);
      byte []bytecode = new byte[is.available()];
      is.read(bytecode);
      WVM.out.println(bytecode);
      */

      return (c);
    } catch (Exception e) {
      WVM.out.println("in WVM_ClassLoader, Exception e: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }
  public void addCodebase(URL[] urls) {
    for (int i=0; i<urls.length; i++) {
      addURL(urls[i]);
    }
  }
}
