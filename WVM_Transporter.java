package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/**
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Gaurav S. Kc
 * 
*/

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.*;
import psl.worklets.http.*;

class WVM_Transporter extends Thread {
  
  WVM _wvm;
  
  int     _port;
  String  _host; 
  String  _name;
  protected ServerSocket _serverSocket;		// Regular Server socket
  protected SSLServerSocket _SSLServerSocket;	// Secure Server socket

  // set to friendly access so that all the classes in this package can reuse this factory.
  protected WVM_SSLSocketFactory _WVM_sf;	// our local socket factory.
  protected int _securityLevel;			// the security level of the WVM.

  private boolean _isActive = false;

  private WVM_ClassLoader_New _loader;
  ClassFileServer _webserver;
  int _webPort;
  boolean _isClassServer;
  
  ClassFileServer _sslwebserver;
  int _sslwebPort;

  HashSet classHashSet;

  protected static final String PING_REQUEST = "Hi, I am pinging you :)";
  protected static final String PING_RESPONSE = "Ok, pinging you back!";

  protected static final String SENDMSG_REQUEST = "Hi, I am sending you a message :)";
  protected static final String SENDMSG_RESPONSE = "Ok, I received your message!";

  protected static final String GETMSG_REQUEST = "Hi, I am want something that you have :)";
  protected static final String GETMSG_RESPONSE = "Ok, here you go, you can have it!";
  
  protected static final String REJOIN_REGISTRY_REQUEST = "Hi, my registry gonna go down";
  protected static final String CREATE_REGISTRY_REQUEST = "Hi, ya gotta be the new registry";

  protected static final String WORKLET_XFER = "Yo, I am sending a worklet";
  protected static final String WORKLET_RECV = "Yo, I got your worklet";

  WVM_Transporter(WVM wvm, String host, String name, int port) {
    this(wvm, host, name, port, null, null, null, null, null, null, 0);
  }

  WVM_Transporter(WVM wvm, String host, String name, int port, 
                  String keysfile, String password, String ctx, String kmf, String ks, String rng,
                  int securityLevel) {

    // Setup socket
    _wvm = wvm;
    _host = host;
    _name = name;
    _port = port;
    _securityLevel = securityLevel;

    WVM.out.println("Creating the sockets transporter layer for the WVM");
    // initialize the WVM_SSLSocketFactory
    if (keysfile != null && password != null) {
      try {
        _WVM_sf = new WVM_SSLSocketFactory(keysfile, password, ctx, kmf, ks, rng);
      } catch (Exception e){
        WVM.err.println("Error creating WVM_SSLSocketFactory: " + e);
      }
    }

    // dp2041: 29 Aug 2002, always have a plain port for rmi broadcasting.
    // if (_securityLevel < WVM.HIGH_SECURITY)
    createServerSocket(port, "plain");
    
    if (_securityLevel > WVM.NO_SECURITY)
      createServerSocket(port, "secure");

    WVM.out.println ("Creating the Class server(s) layer for the WVM.");

    if (_securityLevel < WVM.HIGH_SECURITY) {
      if ((_webserver = initWebserver(null, "http")) != null) {
        _webPort = _webserver.getWebPort();
      }
    }
    
    if (_securityLevel > WVM.NO_SECURITY) {
      if ((_sslwebserver = initWebserver(_WVM_sf, "https")) != null) {
        _sslwebPort = _sslwebserver.getWebPort();
      }
    }
    
    // Load the Class Loader
    Vector urlsVec = null;
    if (_securityLevel < WVM.HIGH_SECURITY)
      urlsVec = _webserver.getAliases();
    if (_securityLevel > WVM.NO_SECURITY)    
      urlsVec = _sslwebserver.getAliases();

    URL urls[] = new URL[urlsVec.size()];
    String protocol = "";
    if (_securityLevel > WVM.NO_SECURITY)
      protocol = "https:";
    else 
      protocol = "file:";
    for (int i=0; i<urls.length; i++) {
      try {
          urls[i] = new URL(protocol + urlsVec.elementAt(i));
      } catch (MalformedURLException murle) {	  
        // WVM.err.println("Exception from loading urls in class loader: " + murle);	  
      }
    }
    if (_securityLevel > WVM.NO_SECURITY) _loader = new WVM_ClassLoader_New(urls, _WVM_sf);
    else _loader = new WVM_ClassLoader_New(urls);
  }

  private ClassFileServer initWebserver(WVM_SSLSocketFactory sf, String pcol) {
    // setup [secure/plain] classFileServer
    ClassFileServer ws;
    try {
      ws = new ClassFileServer(pcol, _host, _port, null, sf);
      WVM.out.println("  serving classes on " + ws);
      _isClassServer = true;
      return ws;
    } catch (IOException e) {
      WVM.out.println("ClassFileServer cannot start: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  private void createServerSocket(int start_port, String type){
    while (_port >= start_port) {
      try {

        if (type.equals("secure")){
          _SSLServerSocket = (SSLServerSocket) _WVM_sf.createServerSocket(_port);
          WVM.out.println("  Secure SocketListener: " + _host + ":" + _port);
        } else {
          _serverSocket = new ServerSocket(_port);

	  // dp2041, I added this so that it "looks" like we don't have any 
	  // plain sockets if we're at HIGH_SECURITY
	  if(_securityLevel != WVM.HIGH_SECURITY)
	      WVM.out.println("  Plain SocketListener: " + _host + ":" + _port);
        }
        _isActive = true;
        break;

      } catch (UnknownHostException e) {
        // whut? not possible!
      } catch (IllegalArgumentException e) {
          WVM.err.println("Caught exception in createServerSocket: " + e);

      } catch (IOException e) {
          // WVM.out.println("IOException, couldn't open port, trying another: " + e);
          // oops, must try another port number;
          _port++;
          continue;
      }
    }
  }

  void shutdown() {
    if (_webserver != null) _webserver.shutdown();
    if (_sslwebserver != null) _sslwebserver.shutdown();
    _isActive = false;
    try {
      if (_serverSocket != null) _serverSocket.close();
      if (_SSLServerSocket != null) _SSLServerSocket.close();
    } catch (IOException e) { }
  }
  
  public void run() {
    while (!_isActive) {
      try {
        WVM.out.print(".");
        sleep(50);
      } catch (InterruptedException e) { }
    }
    
    listenSocket(_SSLServerSocket, "secure");
    listenSocket(_serverSocket, "plain");
  }

  private void listenSocket(final ServerSocket s, final String socketType) {
    if (s == null) return;
    new Thread() {
      public void run() {
        while (_isActive) {
          try {
            processSocketStream(s.accept(), socketType);
          } catch (SocketException se) {
            if (_isActive) WVM.err.println ("caught SocketException trying to accept " + socketType + " connection: " + se);
          } catch (IOException ioe) {
            if (_isActive) WVM.err.println ("caught IOException trying to accept " + socketType + " connection: " + ioe);
          }
        }
        WVM.out.println("  WVM " + socketType + " socket shutdown");
      }
    }.start();
  }

  private void processSocketStream(Socket s, String socketType){
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;

    try { 
      ois = new ObjectInputStream(s.getInputStream()) {
        protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
        String name = v.getName();
        Class c = Class.forName(name, true, _loader);
        // WVM.out.println("In custom ObjectInputStream, trying to resolve class: " + c);
        return ( (c == null) ? super.resolveClass(v) : c );
        }
        };

      oos = new ObjectOutputStream(s.getOutputStream());
      String requestType = ois.readUTF();
      
      if (socketType.equals("plain") && _securityLevel == 3 
	  && !(requestType.equals(SENDMSG_REQUEST) || requestType.equals(GETMSG_REQUEST))){
	// dp2041: we only want this socket to serve the rejoin registry request in this case.
	// WVM.err.println("Error, secure server proccessing request other than a rejoin registry request");
	return;
      } else if (requestType.equals(PING_REQUEST)) {
        // ok, this is a ping request
        WVM.out.println("  --  being PINGED thru sockets");
        oos.writeUTF(PING_RESPONSE); oos.flush();
        return;
      } else if (requestType.equals(SENDMSG_REQUEST)) {
        // ok, peer hostSystem @ WVM is attempting to send a message
        WVM.out.println(" -- received a message thru sockets");
        Object messageKey = ois.readObject();
        Object message = ois.readObject();
        if (messageKey.equals(CREATE_REGISTRY_REQUEST)) {
          // ok, presumably RMI-registry is going to shutdown
          WVM.out.println(" -- received an RMI-register create request thru sockets");
          ((WVM_RMI_Transporter) this).createRegistry(message.toString());
        } else {
          _wvm.receiveMessage(messageKey, message);
        }
        oos.writeUTF(SENDMSG_RESPONSE); oos.flush();
        return;
      } else if (requestType.equals(GETMSG_REQUEST)) {
        // ok, peer hostSystem @ WVM is asking for a message
        WVM.out.println(" -- received a message request thru sockets");
        Object messageKey = ois.readObject();
        if (messageKey.toString().startsWith(REJOIN_REGISTRY_REQUEST)) {
          // ok, presumably RMI-registry is going to shutdown
          WVM.out.println(" -- received an RMI-deregister request thru sockets");
          String registrationKey = messageKey.toString().substring(REJOIN_REGISTRY_REQUEST.length());
          Date date = ((WVM_RMI_Transporter) this).rejoinRegistry(registrationKey);
          oos.writeObject(date);
        } else {
          oos.writeObject(_wvm.requestMessage(messageKey));
        }
        oos.writeUTF(GETMSG_RESPONSE); oos.flush();
        return;
      } else if (! requestType.equals(WORKLET_XFER)) {
        // what kinda request is this anyway?
        WVM.out.println("  --  received a random request!");
        return;
      }

      WVM.out.println("    received a worklet");
      classHashSet = new HashSet();

      String rHost = ois.readUTF();
      String rName = ois.readUTF();
      int rPort = ois.readInt();

      int numJunctions = ois.readInt();
      while (numJunctions-- > 0) {
        String cName = ois.readUTF();
        String codebase = ois.readUTF();
        try {
          addCodebase(codebase);
          Class loadCl = _loader.loadClass(cName);
          // WVM.out.println("finally loaded class: " + loadCl);
        } catch (ClassNotFoundException e) {
          WVM.out.println ("WVM Transporter Exception: Class " + cName + " could not be loaded from codebase: " + codebase);
          // e.printStackTrace();
          return;
        }
      }

      Worklet wkl = null;
      try {
        wkl = (Worklet) ois.readObject();
        // TODO: Okay, now that the LEAST REQUIRED SET (LRS) of class bytecode
        // has been downloaded from the source WVM, send out a BytecodeRetrieverWJ
        // to get the relevant classes, viz. all those classes that the source
        // HTTP server served up to this WVM
        {
          // send out BytecodeRetrieverWJ w/ a Worklet to retrieve all URLLoaded classes
          // new BytecodeRetrieval(classHashSet, _wvm, _host, _name, _port, rHost, rName, rPort);
        }
        
        // Now, send acknowledgement back to sender WVM
        oos.writeUTF(WORKLET_RECV);
        oos.flush();

      } catch (Exception e) {
        WVM.out.println("This is BAD!");
        e.printStackTrace();
        System.exit(0);
      }

      // adding to WVM's in-tray, and *do* need to send out BytecodeRetrieval worklet
      wkl.retrieveBytecode = true;
      _wvm.installWorklet(wkl);
    } catch (ClassNotFoundException cnfe) {
      WVM.out.println("ClassNotFoundException when receiving message from peer, cnfe: " + cnfe);
      cnfe.printStackTrace();
    } catch (SocketException se) {
      WVM.out.println("WVM Socket died se: " + se);
      se.printStackTrace();
    } catch (java.io.IOException ioe) {
      WVM.out.println("IOException in Worklet receive loop, ioe: " + ioe);
      ioe.printStackTrace();
    } finally {
      WVM.out.println ("    getting ready to accept worklets again");
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
    }
    WVM.out.println();
    return;
  }

  private void addCodebase(String codebase) {
    StringTokenizer st = new StringTokenizer (codebase, " ");
    Vector urlsVec = new Vector();
    int count = 0;
    while (st.hasMoreTokens()) {
      try {
        URL url = new URL(st.nextToken());
        urlsVec.add(url);
        count++;
      } catch (MalformedURLException e) {
        WVM.out.println("MalformedURLException in addCodebase, e: " + e.getMessage());
      }
    }

    // urlsVec = new Vector(new HashSet(urlsVec));
    URL urls[] = new URL[urlsVec.size()];
    urlsVec.toArray(urls);
    if (_loader == null) {
      _loader = new WVM_ClassLoader_New(urls);
    } else {
      _loader.addCodebase(urls);
    }
  }

  boolean sendWorklet(Worklet wkl, WorkletJunction wj) {
    String[] methods = wj.getTransportMethods();

    boolean success = false;
    for (int i = 0; i < methods.length; i++){
      if (methods[i].equals("secureSocket"))
        try {
          WVM.err.println("  --  Trying to send through secureSocket");
          success = sendSocket(wkl, wj, true);
        } catch (Exception e){
          WVM.err.println("  --  Error sending through secureSocket: " + e);
        }

      else if (methods[i].equals("plainSocket"))
        try {
          WVM.err.println("  --  Trying to send through plainSocket");
          success = sendSocket(wkl, wj, false);
        } catch (Exception e){
          WVM.err.println("  --  Error sending through plainSocket: " + e);
        }
      
      if (success)
        break;
    }
    return success;
  }

  boolean sendSocket(Worklet wkl, WorkletJunction wj, boolean secure){    
    String targetHost = wj._host;
    int targetPort = wj._port;
    boolean transmissionComplete = false;
    Socket s = null;
    
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      if (secure && _WVM_sf != null)
        s = _WVM_sf.createSocket(targetHost, targetPort);
      else if (!secure) 
        s = new Socket(targetHost, targetPort);
      else 
        return false;

      oos = new ObjectOutputStream(s.getOutputStream());
      oos.writeUTF(WORKLET_XFER);
      oos.writeUTF(_host);
      oos.writeUTF(_name);
      oos.writeInt(_port);

      {
        ClassLoader sysCll = ClassLoader.getSystemClassLoader();
        Enumeration e = wkl.getClasses();
        oos.writeInt(wkl.getNumClasses());
        String cName = null;
        String codebase = "";
        while (e.hasMoreElements()) {
          Class c = (Class) e.nextElement();
          ClassLoader cl = c.getClassLoader();
          cName = c.getName();
          /* Sending multiple codebases: 19 June 2001 - gskc
          if (sysCll == cl) {
          codebase = "http://" + _host + ":" + _webPort + "/";
          // WVM.out.println("class: " + cName + ", codebase: " + codebase);
          } else {
          URL url = cl.getResource(cName.replace('.', '/') + ".class");
          codebase = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
          // WVM.out.println("class: " + cName + ", codebase: " + codebase);
          }
          */

          if (_securityLevel < WVM.HIGH_SECURITY)
            codebase += " http://" + _host + ":" + _webPort + "/";
          if (_securityLevel > WVM.NO_SECURITY)
	    codebase += " https://" + _host + ":" + _sslwebPort + "/";

          if (sysCll != cl) {
            // This class was originally loaded from a remote server
            URL url = cl.getResource(cName.replace('.', '/') + ".class");
            if (url != null) {
              // append remote http classServer to the 'local' codebase
              codebase += " " + url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
            } else {
              WVM.out.println("mySource unavailable, sending codebase: " + codebase);
            }
          }
          
          // WVM.out.println("codebase of workletJunction: " + codebase);
          oos.writeUTF(cName);
          oos.writeUTF(codebase);
        }
      }

      // TODO: set up the BAG-MULTISET in the ClassFileServer so that the 
      // incoming BytecodeRetrieverWJ can get the data it needs
      oos.writeObject(wkl);
      oos.flush();

      // Receive ACK from the target WVM
      ois = new ObjectInputStream(s.getInputStream());
      transmissionComplete = ois.readUTF().equals(WORKLET_RECV);
    } catch (InvalidClassException e) {
      WVM.out.println("      InvalidClassException in sendWorklet: " + e.getMessage());
      // e.printStackTrace();
    } catch (NotSerializableException e) {
        WVM.out.println("      NotSerializableException in sendWorklet: " + e.getMessage());
        // e.printStackTrace();
    } catch (UnknownHostException e) {
        WVM.out.println("      UnknownHostException in sendWorklet: " + e.getMessage());
        // e.printStackTrace();
    } catch (SecurityException e) {
        WVM.out.println("      SecurityException in sendWorklet: " + e.getMessage());
        // e.printStackTrace();
    } catch (IOException e) {
        WVM.out.println("      IOException in sendWorklet: " + e.getMessage());
        // e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }

      return transmissionComplete;
    }
  }
  
  // Client-side - should be overridden ////////////////////////////////////////
  protected boolean ping(String wvmURL) {
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.ping(String)");
    return false;
  }
  protected boolean sendMessage(Object messageKey, Object message, String wvmURL) {
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.sendMessage(Object, Object, String)");
    return false;
  }
  protected Object getMessage(Object messageKey, String wvmURL) {
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.getMessage(Object, String)");
    return null;
  }
  // END: Client-side - should be overridden ///////////////////////////////////
  
  // Client-side ///////////////////////////////////////////////////////////////
  final boolean ping(String host, int port) {
    // 2-do: really!
    boolean transmissionComplete = false;
    
    Socket s = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      WVM.out.println("  --  pinging peer WVM thru sockets: " + host + ":" + port);
      s = new Socket(host, port);

      // Send request to the peer WVM
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.writeUTF(PING_REQUEST);
      oos.flush();

      // Receive ACK from the peer WVM
      ois = new ObjectInputStream(s.getInputStream());
      transmissionComplete = ois.readUTF().equals(PING_RESPONSE);
    } catch (UnknownHostException e) {
      WVM.out.println("UnknownHostException in ping, e: " + e.getMessage());
      // e.printStackTrace();
    } catch (IOException e) {
        WVM.out.println("IOException in ping, e: " + e.getMessage());
        // e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
      return transmissionComplete;
    }
  }
  final boolean sendMessage(Object messageKey, Object message, String host, int port) {
    // 2-do: really!
    boolean transmissionComplete = false;
    
    Socket s = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      WVM.out.println("  --  sending message to peer WVM thru sockets: " + host + ":" + port);
      s = new Socket(host, port);

      // Send request to the peer WVM
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.writeUTF(SENDMSG_REQUEST);
      oos.writeObject(messageKey);
      oos.writeObject(message);
      oos.flush();

      // Receive ACK from the peer WVM
      ois = new ObjectInputStream(s.getInputStream());
      transmissionComplete = ois.readUTF().equals(SENDMSG_RESPONSE);

    } catch (UnknownHostException uhe) {
      WVM.out.println("UnknownHostException in sendMessage, uhe: " + uhe);
      // e.printStackTrace();
    } catch (IOException ioe) {
        WVM.out.println("IOException in sendMessage, ioe: " + ioe);
        // e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
      if (transmissionComplete) WVM.out.println("  --  SUCCESSFUL: sending message to peer WVM thru sockets: " + host + ":" + port);
      else WVM.out.println("  --  FAILURE: sending message to peer WVM thru sockets: " + host + ":" + port);
      return transmissionComplete;
    }
  }
  final Object getMessage(Object messageKey, String host, int port) {
    // 2-do: really!
    boolean transmissionComplete = false;
    Object message = null;
    
    Socket s = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    
    try {
      WVM.out.println("  --  getting message from peer WVM thru sockets: " + host + ":" + port);
      s = new Socket(host, port);

      // Send request to the peer WVM
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.writeUTF(GETMSG_REQUEST);
      oos.writeObject(messageKey);
      oos.flush();

      // Receive ACK from the peer WVM
      ois = new ObjectInputStream(s.getInputStream());
      message = ois.readObject();
      transmissionComplete = ois.readUTF().equals(GETMSG_RESPONSE);

    } catch (ClassNotFoundException cnfe) {
      WVM.out.println("ClassNotFoundException in getMessage, cnfe: " + cnfe);
    } catch (UnknownHostException uhe) {
        WVM.out.println("UnknownHostException in getMessage, uhe: " + uhe);
        // e.printStackTrace();
    } catch (IOException ioe) {
        WVM.out.println("IOException in getMessage, ioe: " + ioe);
        // e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }
      if (transmissionComplete) WVM.out.println("  --  SUCCESSFUL: getting message from peer WVM thru sockets: " + host + ":" + port);
      else WVM.out.println("  --  FAILURE: getting message from peer WVM thru sockets: " + host + ":" + port);
      return transmissionComplete ? message : null;
    }
  }
  // END: Client-side //////////////////////////////////////////////////////////

  boolean isSecure() {
    return (_securityLevel > WVM.NO_SECURITY);
  }
}
