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

import psl.worklets.http.ClassServer;

class WVM_ClassLoader_New extends WVM_ClassLoader {
  private final URLSet _urlSet;
  WVM_ClassLoader_New(URL[] urls) {  
    super(urls);
    // store these urls into our private ordered, data structure
    _urlSet = new URLSet();
    _urlSet.add(urls);
  }
  public Class findClass(String name) throws ClassNotFoundException {
    // WVM.out.println(WVM.time() + "WVM_ClassLoader asked to findClass(" + name + ")");
    byte bytecode[] = null;
GOOD_BLOCK:
    if (ClassServer.containsKey(name)) {
      bytecode = ClassServer.get(name);
    } else {
      Enumeration e = _urlSet.elements();
      while (e.hasMoreElements()) {
        try {
          URL url = new URL(e.nextElement() + name + ".class");
          // WVM.out.println(url);
          HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
          urlCon.connect();
          InputStream is = urlCon.getInputStream();
          // WVM.out.println("checking on data stream @ " + WVM.time());
          // int size = is.available();
          int size = urlCon.getContentLength();
          // if (size == 0) continue;
          bytecode = new byte[size];

          int total = 0;
          while (total < size) {
            int actual;
            actual = is.read(bytecode, total, size-total);
            total += actual;
            // WVM.out.println("Got " + total + " bytes from ClassServer for class: " + name);
          }

          is.close();
          urlCon.disconnect();
          // add bytecode to local Webserver cache
          ClassServer.put(name, bytecode);
          
          // Upgrade url in _urlSet
          break GOOD_BLOCK;
        } catch (IOException ioe) {
          // ioe.printStackTrace();
          WVM.out.println("IOException: ioe " + ioe.getMessage());
        }
      }
      throw (new ClassNotFoundException("Class: " + name + " not found in URLs"));
    }
    return defineClass(name, bytecode, 0, bytecode.length);
  }

  public URL findResource(String name) {
    // WVM.out.println(WVM.time() + "WVM_ClassLoader asked to findResource(" + name + ")");
    byte binary[] = null;
    if (! ClassServer.containsKey(name)) {
      Enumeration e = _urlSet.elements();
      while (e.hasMoreElements()) {
        try {
          URL url = new URL(e.nextElement() + name);
          // WVM.out.println("fndResource testing URL: " + url);
          HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
          urlCon.connect();
          InputStream is = urlCon.getInputStream();
          // WVM.out.println("checking on data stream @ " + WVM.time());
          // int size = is.available();
          int size = urlCon.getContentLength();
          // if (size == 0) continue;
          binary = new byte[size];

          int total = 0;
          while (total < size) {
            int actual;
            actual = is.read(binary, total, size-total);
            total += actual;
            // WVM.out.println("Got " + total + " bytes from ClassServer for class: " + name);
          }

          is.close();
          urlCon.disconnect();
          // add binary to local Webserver cache
          ClassServer.put(name, binary);
          
          // Upgrade url in _urlSet
          // WVM.out.println("findResource returning URL: " + url);
          return url;
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    }
    return super.findResource(name);
  }
  public void addCodebase(URL[] urls) {
    _urlSet.add(urls);
  }
  
  private class URLSet extends Vector {
    void add(URL url) {
      if (url!= null && !contains(url)) {
        // WVM.out.println("Adding URL at top: " + url);
        // insertElementAt(url, 0);
        addElement(url);
      }
    }
    void add(URL[] urls) {
      for (int i=0; i<urls.length; i++) {
        add(urls[i]);
      }
    }
    public String toString() {
      StringBuffer sb = new StringBuffer();
      for (Enumeration e = elements(); e.hasMoreElements(); ) {
        sb.append(" " + e.nextElement().toString());
      }
      return sb.toString();
    }
  }
}
