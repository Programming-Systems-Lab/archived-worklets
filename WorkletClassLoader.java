/*
 * @(#)WVM_ClassLoader.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
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
import java.util.*;
import javax.net.ssl.*;
import psl.worklets.http.ClassServer;

/** {@link WVM} associated Class Loader */
public class WorkletClassLoader extends URLClassLoader{
  /** <code>URLSet</code> holding the list of <code>URL</code>s*/
  private final URLSet _urlSet;
  /** socket factory to use, if not null */
  private WVM_SSLSocketFactory _WVM_sf;

    // associated worklet's id
    private String workletId;
    // stores byte code for required classes
    private byteCodeStore bcs;

    //this is false when junction for this wvm is sent as byte array
    // so that class server does not pre-send byte code to other wvm's
    private boolean sendWId = true;

  /**
   * Creates a plain WVM_ClassLoader to load the given <code>URL</code>s
   *
   * @param urls: array of <code>URL</code>s to load
   */
  public WorkletClassLoader (URL[] urls,String wid) {
    this(urls, null,wid);
  }

  /**
   * Creates a WVM_ClassLoader to load the given <code>URL</code>s
   * with the given SocketFactory
   *
   * @param urls: array of <code>URL</code>s to load
   * @param wvm_sf: socket factory to use
   */
  public WorkletClassLoader (URL[] urls, WVM_SSLSocketFactory wvm_sf,String wid) {
    super(urls);
    
    _WVM_sf = wvm_sf;
    // store these urls into our private ordered, data structure
    _urlSet = new URLSet();
    _urlSet.add(urls);
    workletId = new String(wid);
    bcs = new byteCodeStore(wid);
  }

    public void sendWorkletId(){
	sendWId = true;
    }
    public void dontSendWorkletId(){
	sendWId = false;
    }
    

  /**
   * Finds the given class
   *
   * @param name: Name of the class we want to find
   * @throws ClassNotFoundException if the given class could not be found
   * @return the Class, if found
   */
  public Class findClass(String name) throws ClassNotFoundException {
      
      // System.out.println("WORKLETClassLoader: findClass " + name + " " + workletId);
    // WVM.out.println(WVM.time() + "WVM_ClassLoader asked to findClass(" + name + ")");
    byte bytecode[] = null;
GOOD_BLOCK:
    if (bcs.containsKey(name)) {
	//found byte code in cache
      bytecode = bcs.get(name);
    } else {
      Enumeration e = _urlSet.elements();
     
      while (e.hasMoreElements()) {
        try {
	    String s = new String(e.nextElement() + name + ".class");
	    if(sendWId){
		s = s+"?" +workletId;
	    }
	    URL url = new URL(s);
	    //  System.out.println("TRYING TO GET URL: " + url.toString());
	    // WVM.out.println("PRINT URL: " +url);
	    URLConnection urlCon = url.openConnection();;
	    
	    String urlProtocol = url.getProtocol();
	    if (urlProtocol.equals("https") && _WVM_sf != null){
		((HttpsURLConnection) urlCon).setSSLSocketFactory(_WVM_sf.getSSLSocketFactory());
		((HttpsURLConnection) urlCon).setHostnameVerifier(new WVM_HostnameVerifier());
	    }
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
	    }
	    
	    is.close();
	    if (url.getProtocol().equals("http") || url.getProtocol().equals("https"))
		((HttpURLConnection) urlCon).disconnect();
	    
	    // add bytecode to local Webserver cache
	    ClassServer.put(name, bytecode);
	    // Upgrade url in _urlSet
	    break GOOD_BLOCK;
        } catch (IOException ioe) {
	    // this Exception could be because the HostnameVerifier
	    // refused the remote host.
	    // WVM.err.println("Caught IOException: " + ioe);
	    //ioe.printStackTrace();
        }
      }
      throw (new ClassNotFoundException("Class: " + name + " not found in URLs"));
    }
    return defineClass(name, bytecode, 0, bytecode.length);
  }

  /**
   * Finds the URL of the given resource
   *
   * @param name: Name of the resource we want to find
   * @return <code>URL</code> of the resource
   */
  public URL findResource(String name) {
      System.out.println("WVMClassLoader: findResource");
    // WVM.out.println(WVM.time() + "WVM_ClassLoader asked to findResource(" + name + ")");
    byte binary[] = null;
    if (! ClassServer.containsKey(name)) {
      Enumeration e = _urlSet.elements();
      while (e.hasMoreElements()) {
        try {
          URL url = new URL(e.nextElement() + name);
	  // WVM.out.println("fndResource testing URL: " + url);
	  URLConnection urlCon = url.openConnection();

	  String urlProtocol = url.getProtocol();
	  if (urlProtocol.equals("https") && _WVM_sf != null){
	    ((HttpsURLConnection) urlCon).setSSLSocketFactory(_WVM_sf.getSSLSocketFactory());
	    ((HttpsURLConnection) urlCon).setHostnameVerifier(new WVM_HostnameVerifier());
	  }
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
	  if (urlProtocol.equals("http") || urlProtocol.equals("https"))
	    ((HttpURLConnection) urlCon).disconnect();

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

  /**
   * Adds the given array of <code>URL</code>s to the codebase
   *
   * @param urls: <code>URL</code>s to add to the codebase
   */
  public void addCodebase(URL[] urls) {
      // System.out.println("WORKLETClassLoader: addCodeBase");
    _urlSet.add(urls);
  }

    public void addTopCodebase(URL u){
	_urlSet.add(0,u);
    }

  /** Container to hold the ClassLoader's <code>URL</code>s */
  private class URLSet extends Vector {
    
      /** Adds the given <code>URL</code> */
    void add(URL url) {
      if (url!= null && !contains(url)) {
        // WVM.out.println("Adding URL at top: " + url);
        // insertElementAt(url, 0);
        addElement(url);
      }
    }

    /** Adds the given array of <code>URL</code>s */
    void add(URL[] urls) {
      for (int i=0; i<urls.length; i++) {
        add(urls[i]);
      }
    }

    /** @return an String-ified enumeration of the <code>URL</code>s */
    public String toString() {
     StringBuffer sb = new StringBuffer();
      for (Enumeration e = elements(); e.hasMoreElements(); ) {
        sb.append(" " + e.nextElement().toString());
      }
      return sb.toString();
    }
  }


    public void putByteCode(String name, byte[] bc){
	bcs.put(name,bc);
    }
    public boolean containsKey(String key){
	return bcs.containsKey(key);
    }
    public byte[] get(String name){
	return bcs.get(name);
    }


    class byteCodeStore{
	private String workletId;
	private final Hashtable byteCodeHash;// = new Hashtable();

	byteCodeStore(String wklId){
	    workletId = new String(wklId);
	    byteCodeHash = new Hashtable();
	    //  wklLoader = ldr;
	}
	
	public void put(String name,byte []code){
	    if (! byteCodeHash.containsKey(name)) {
		byteCodeHash.put(name, code);
	    } 
	}
	
	public  byte[] get(String name) {
	    return ((byte[]) byteCodeHash.get(name));
	}
	public boolean containsKey(String name) {
	    return (byteCodeHash.containsKey(name));
	}
    }



}
