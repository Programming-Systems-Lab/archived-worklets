package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc, @author Peppo Valetto
 *
*/

import java.net.*;
import java.util.*;

class WVM_ClassLoader extends URLClassLoader {
  WVM_ClassLoader(URL[] urls) {  
    super(urls);
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
      Class c = (Class) super.findClass(name);
      if (hs != null) {
        hs.add(name);
      }
      return (c);
    } catch (Exception e) {
      WVM.out.println("in WVM_ClassLoader, Exception e: " + e.getMessage());
    }
    return ((Class) super.findClass(name));
  }
  public void addCodebase(URL[] urls) {
    for (int i=0; i<urls.length; i++) {
      addURL(urls[i]);
    }
  }
}
