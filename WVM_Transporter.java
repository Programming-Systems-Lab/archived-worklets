/*
 * @(#)WVM_Transporter.java
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
import javax.net.ssl.*;
import java.util.*;
import psl.worklets.http.*;

/**
 * The {@link WVM}'s network transport layer that handles
 * communication of {@link Worklet}s
 */
public class WVM_Transporter extends Thread {

  /** The {@link WVM} that this WVM_Transporter provides services for */
  WVM _wvm;

  /** plain network socket number */
  int     _port;
  /** secure network socket number*/
  int     _SSLport;
  /** hostname of local machine */
  String  _host;
  /** name that the RMI server is bound to */
  String  _name;

  /** plain server socket */
  protected ServerSocket _serverSocket;
  /** secure server socket */
  protected SSLServerSocket _SSLServerSocket;

  // set to friendly access so that all the classes in this package can reuse this factory.
  /** secure socket factory */
  protected WVM_SSLSocketFactory _WVM_sf;
  /** security level representative of the {@link WVM} */
  protected int _securityLevel;

  /** Checks to see if the WVM_Transporter is ready for communication */
  private boolean _isActive = false;

  /** ClassLoader used to load remote classes */
  private WVM_ClassLoader _loader;
  /** plain web server used to serve Worklet classes */
  ClassFileServer _webserver;
  /** port number that the plain web server is on*/
  int _webPort;
  /** Checks to see if this WVM_Transporter is serving classes */
  boolean _isClassServer;

  /** secure web server used to serve Worklet classes */
  ClassFileServer _sslwebserver;
  /** port number that the secure web server is on*/
  int _sslwebPort;

  /** hash of classes locally available */
  HashSet classHashSet;

  /** identifier for sending a ping request */
  protected static final String PING_REQUEST = "Hi, I am pinging you :)";
  /** identifier for receiving a ping response */
  protected static final String PING_RESPONSE = "Ok, pinging you back!";

  /** identifier for sending a message request */
  protected static final String SENDMSG_REQUEST = "Hi, I am sending you a message :)";
  /** identifier for receiving a message response */
  protected static final String SENDMSG_RESPONSE = "Ok, I received your message!";

  /** identifier for getting a message request */
  protected static final String GETMSG_REQUEST = "Hi, I am want something that you have :)";
  /** identifier for getting a message response */
  protected static final String GETMSG_RESPONSE = "Ok, here you go, you can have it!";

  /** identifier for a request to rejoin the registry */
  protected static final String REJOIN_REGISTRY_REQUEST = "Hi, my registry gonna go down";
  /** identifier for a request to create the registry */
  protected static final String CREATE_REGISTRY_REQUEST = "Hi, ya gotta be the new registry";

  /** identifier for sending a worklet */
  protected static final String WORKLET_XFER = "Yo, I am sending a worklet";
  /** identifier for reception of a worklet */
  protected static final String WORKLET_RECV = "Yo, I got your worklet";

    protected static final String BYTECODE_XFER = "Yo, I am sending SOME BYTECODE";
    /** identifier for reception of a bytecode */
    protected static final String BYTECODE_RECV = "Yo, I got your BYTECODE";
   
    //loader to use when receiving a worklet
    WorkletClassLoader _ldr;
  /**
   * Creates the {@link WVM} related network layer composed of sockets
   *
   * @param wvm: {@link WVM} that this WVM_tranporter will be providing services for
   * @param host: hostname of the local machine
   * @param name: name that the RMI server is bound to
   * @param port: port number to begin trying to create the network sockets on
   */
  WVM_Transporter(WVM wvm, String host, String name, int port) {
    this(wvm, host, name, port, null, null, null, null, null, null, 0);
  }

  /**
   * Creates the {@link WVM} related network layer composed of sockets
   *
   * @param wvm: {@link WVM} that this WVM_tranporter will be providing services for
   * @param host: hostname of the local machine
   * @param name: name that the RMI server is bound to
   * @param port: port number to begin trying to create the network sockets on
   * @param keysfile: File holding the public/private keys.
   * @param password: Password into the keysfile.
   * @param ctx: <code>SSLContext</code> to use for the secure sockets.
   * @param kmf: <code>KeyManagerFactory</code> type to use.
   * @param ks: <code>KeyStore</code> type to use.
   * @param rng: <code>SecureRandom</code> (random number generator algorithm) to use.
   * @param securityLevel: Security level of the transporter.
   */
  WVM_Transporter(WVM wvm, String host, String name, int port,
                  String keysfile, String password, String ctx, String kmf, String ks, String rng,
                  int securityLevel) {

    // Setup socket
    _wvm = wvm;
    _host = host;
    _name = name;
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
    _port = createServerSocket(port, "plain");

    if (_securityLevel > WVM.NO_SECURITY)
      _SSLport = createServerSocket(port, "secure");

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
    // if (_securityLevel > WVM.NO_SECURITY) _loader = new WVM_ClassLoader(urls, _WVM_sf);
    //else _loader = new WVM_ClassLoader(urls);
  }

  /**
   * Creates a web server for the tranporter layer
   *
   * @param sf: Socket factory to use in the web server
   * @param pcol: protocol to use (ftp, http or https)
   * @return web server
   */
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

  /**
   * Creates a server socket
   *
   * @param startPort: port number to begin trying to create a server socket on.  It will increment
   * the port number until a succesful server socket is created.
   * @param type: "secure" or "plain" socket server
   * @return port number of succesful server socket
   */
  private int createServerSocket(int startPort, String type){
      
   	//   System.out.println("WVM_Transporter: CreateServerSocket " + startPort);
    int currentPort = startPort;
    while (currentPort >= startPort) {
      try {

        if (type.equals("secure")){
          _SSLServerSocket = (SSLServerSocket) _WVM_sf.createServerSocket(currentPort);
          WVM.out.println("  Secure SocketListener: " + _host + ":" + currentPort);
        } else {
          _serverSocket = new ServerSocket(currentPort);

	  // dp2041, I added this so that it "looks" like we don't have any
	  // plain sockets if we're at HIGH_SECURITY
	  if(_securityLevel != WVM.HIGH_SECURITY)
	      WVM.out.println("  Plain SocketListener: " + _host + ":" + currentPort);
        }
        _isActive = true;
        return currentPort;

      } catch (UnknownHostException e) {
        // whut? not possible!
	break;
      } catch (IllegalArgumentException e) {
          WVM.err.println("Caught exception in createServerSocket: " + e);
	  break;
      } catch (IOException e) {
          // WVM.out.println("IOException, couldn't open port, trying another: " + e);
          // oops, must try another port number;
          currentPort++;
          continue;
      }
    }
    return -1;
  }

  /** shutdown the webservers and server sockets */
  void shutdown() {
    if (_webserver != null) _webserver.shutdown();
    if (_sslwebserver != null) _sslwebserver.shutdown();
    _isActive = false;
    try {
      if (_serverSocket != null) _serverSocket.close();
      if (_SSLServerSocket != null) _SSLServerSocket.close();
    } catch (IOException e) { }
  }

  /** Thread related function.  Waits if it is not active, listens on server sockets if it is active */
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

  /**
   * Start a new thread to listen on the server socket
   *
   * @param s: the actual server socket
   * @param socketType: type of server socket ("plain" or "secure")
   */
  private void listenSocket(final ServerSocket s, final String socketType) {
   	//   System.out.println("WVM_Transporter: listenSocket");
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

  /**
   * After the server socket has accepted a connection, then this function processes the
   * socket's stream of data
   *
   * @param s: the actual server socket
   * @param socketType: type of server socket ("plain" or "secure")
   */
  private void processSocketStream(Socket s, String socketType){
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    WorkletClassLoader temp;

    try {
      ois = new ObjectInputStream(s.getInputStream()) {
        protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
        String name = v.getName();
	//	System.out.println("processSocketStream: Class.forName");
        Class c = Class.forName(name, true, _ldr);
        WVM.out.println("In custom ObjectInputStream, trying to resolve class: " + c);
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
      } else if (requestType.equals(BYTECODE_XFER)) {
	  //receive pre-sent byte code
	  String _rHost = ois.readUTF();
	  String _rName = ois.readUTF();
	  int _rPort = ois.readInt();
	  URL u = new URL((String)ois.readObject());
	  String _wid = (String)ois.readObject();
	  String name = (String)ois.readObject();
	//	  WVM.out.println("PROCESSING INCOMING BYTECODE FOR " + _wid + " " + name);
	  int length = ois.readInt();
	  byte []bc = new byte[length];
	  ois.readFully(bc,0,length);
	  URL [] us = new URL[1];
	  us[0] = u;
	  //store it in the corresponding class loader
	  // if not present , create new one...
	  if(WVM.wkltRepository.containsKey(_wid)){
	      WVM.wkltRepository.putByteCode(_wid, name, bc);
	  }else{
	      WorkletClassLoader _wldr = new WorkletClassLoader(us, _WVM_sf,_wid);
	      _wldr.putByteCode(name,bc);
	      WVM.wkltRepository.put(_wid,_wldr);
	  }
	   // Now, send acknowledgement back to sender WVM
	  oos.writeUTF(BYTECODE_RECV);
	  oos.flush();
	  return;
	  
      } else if (! requestType.equals(WORKLET_XFER)) {
        // what kinda request is this anyway?
        WVM.out.println("  --  received a random request!");
        return;
      }

      WVM.out.println("    received a worklet");
      classHashSet = new HashSet();


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
     

      String rHost = ois.readUTF();
      String rName = ois.readUTF();
      int rPort = ois.readInt();
      String wid = (String)ois.readObject();
      if(WVM.wkltRepository.containsKey(wid)){
	  _ldr = (WorkletClassLoader)WVM.wkltRepository.get(wid);
	  
      } else {
	//	  System.out.println("WORKLET ID IS " + wid);
	  if (_securityLevel > WVM.NO_SECURITY) temp = _ldr = new WorkletClassLoader(urls, _WVM_sf,wid);
	  else temp = _ldr = new WorkletClassLoader(urls,wid);
	  WVM.wkltRepository.put(wid,temp);
      }

      int numJunctions = ois.readInt();
      while (numJunctions-- > 0) {
        String cName = ois.readUTF();
        String codebase = ois.readUTF();
        try {
          addCodebase(codebase);
		//  System.out.println("LOADING CLASS: " + cName);
          Class loadCl = _ldr.loadClass(cName);
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

      // adding to WVM's in-tray
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
    _ldr = null;
    return;
  }

  /**
   * Adds the codebase to the current vector of class servers
   *
   * @param codebase: URL of server where code could be
   */
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
    if (_ldr == null) {
      _loader = new WVM_ClassLoader(urls);
    } else {
      _ldr.addCodebase(urls);
    }
  }

  /**
   * Sends the {@link Worklet} to the {@link WVM}.  This
   * function is a wrapper for <code>sendSocket</code> that collects
   * the information from the {@link WorkletJunction} to pass on
   * the transport method preferences
   *
   * @param wkl: {@link Worklet} to send
   * @param wj: current {@link WorkletJunction} holding
   * next addressing info
   * @return success of the send
   */
  boolean sendWorklet(Worklet wkl, WorkletJunction wj) {
    	//  System.out.println("WVM_Transporter: sendWorklet");
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

  /**
   * Sends the {@link Worklet} to the {@link WVM} as
   * directed by the {@link WorkletJunction}'s addressing info
   *
   * @param wkl: {@link Worklet} to send
   * @param wj: current {@link WorkletJunction} holding
   * next addressing info
   * @param secure: true if socket used should be a secure socket
   * @return success of the send
   */
  boolean sendSocket(Worklet wkl, WorkletJunction wj, boolean secure){
    	//  System.out.println("WVM_Transporter: sendSocket");
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
      oos.writeObject(wkl.wid);
    	//  System.out.println("SENDING WORKLET ID IS " + wkl.wid);
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
	//  WVM.out.println("      IOException in sendWorklet: " + e.getMessage());
        //e.printStackTrace();
    } finally {
      try {
        if (ois != null) ois.close();
        if (oos != null) oos.close();
        if (s != null) s.close();
      } catch (IOException ioe) { }

      return transmissionComplete;
    }
  }



    // pre-send byte code for .class files
    // used by ClassServer
    public boolean sendByteCode(String wid, String name,byte[] bytecode,String u){
	//	System.out.println("WVM_Transporter: sendBYTECODE");
	Vector v = _wvm.getRegJunctions(wid);
	if(v == null){
	 	//   System.out.println("JUNCTIONS CVECTOR IS NULL");
	}
	boolean transmissionComplete = false;
	for(int i = 0;i < v.size();i++){
	    WorkletJunction wj = (WorkletJunction)v.elementAt(i);
	    String targetHost = wj._host;
	    int targetPort = wj._port;
	  	//  System.out.println("Target: "+targetHost+" "+targetPort);
	    Socket s = null;
	    
	    ObjectOutputStream oos = null;
	    ObjectInputStream ois = null;
	    
	    try {
		if (_securityLevel > WVM.NO_SECURITY && _WVM_sf != null)
		    //	if (secure && _WVM_sf != null)
		    s = _WVM_sf.createSocket(targetHost, targetPort);
		else// if (!secure)
		    s = new Socket(targetHost, targetPort);
		//	else
		//   return false;
		
		oos = new ObjectOutputStream(s.getOutputStream());
		oos.writeUTF(BYTECODE_XFER);
		oos.writeUTF(_host);
		oos.writeUTF(_name);
		oos.writeInt(_port);
		oos.writeObject(u);
		oos.writeObject(wid);
		oos.writeObject(name);
		//	System.out.println("SENDING WORKLET ID IS " + wid);
		oos.writeInt(java.lang.reflect.Array.getLength(bytecode));
		oos.write(bytecode);
		
		oos.flush();
		
		// Receive ACK from the target WVM
		ois = new ObjectInputStream(s.getInputStream());
		transmissionComplete = ois.readUTF().equals(BYTECODE_RECV);
	    } catch (InvalidClassException e) {
		WVM.out.println("      InvalidClassException in sendByteCode: " + e.getMessage());
	    // e.printStackTrace();
	    } catch (NotSerializableException e) {
		WVM.out.println("      NotSerializableException in sendByteCode: " + e.getMessage());
		// e.printStackTrace();
	    } catch (UnknownHostException e) {
		WVM.out.println("      UnknownHostException in sendByteCode: " + e.getMessage());
		// e.printStackTrace();
	    } catch (SecurityException e) {
		WVM.out.println("      SecurityException in sendByteCode: " + e.getMessage());
		// e.printStackTrace();
	    } catch (IOException e) {
		WVM.out.println("      IOException in sendByteCode: " + e.getMessage());
	
	    } finally {
		try {
		    if (ois != null) ois.close();
		    if (oos != null) oos.close();
		    if (s != null) s.close();
		} catch (IOException ioe) { }
	    }
	}
	return transmissionComplete;
	
    
    }
    

























  /** @return whether the transport layer is using secure sockets */
  boolean isSecure() {
    return (_securityLevel > WVM.NO_SECURITY);
  }

  // Client-side - should be overridden ////////////////////////////////////////
  /**
   * Client side: Pings a remote {@link WVM}
   *
   * @param wvmURL: a URL that represents a wvm location in the form
   * of : remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional
   * @return success of ping
   */
  protected boolean ping(String wvmURL) {
      
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.ping(String)");
    return false;
  }
  /**
   * Client side: Sends a message to a remote {@link WVM}
   *
   * @param messageKey: type of message that you are sending, defined
   * in {@link WVM_Transporter}
   * @param message: actual message to send
   * @param wvmURL: a URL that represents a wvm location in the form
   * of : remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional
   * @return success of the send attempt
   */
  protected boolean sendMessage(Object messageKey, Object message, String wvmURL) {
    	//  System.out.println("WVM_Transporter: sendMessage");
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.sendMessage(Object, Object, String)");
    return false;
  }
  /**
   * Client side: Requests to get a message from the remote {@link WVM}
   *
   * @param messageKey: type of message being requested, defined
   * in {@link WVM_Transporter}
   * @param wvmURL: a URL that represents a wvm location in the form
   * of : remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional.
   * @return message that was received
   */
  protected Object getMessage(Object messageKey, String wvmURL) {
    	//  System.out.println("WVM_Transporter: getMessage");
    WVM.out.println("SHOULD-BE-OVERRIDDEN! WVM_Transporter.getMessage(Object, String)");
    return null;
  }
  // END: Client-side - should be overridden ///////////////////////////////////

  // Client-side ///////////////////////////////////////////////////////////////
  /**
   * Client side: Pings a remote {@link WVM}
   *
   * @param host: hostname to ping
   * @param port: port to ping through
   * @return success of ping
   */
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
  /**
   * Client side: Sends a message to a remote {@link WVM}
   *
   * @param messageKey: type of message that you are sending, defined
   * in {@link WVM_Transporter}
   * @param message: actual message to send
   * @param host: hostname to ping
   * @param port: port to ping through
   * @return success of the send attempt
   */
  final boolean sendMessage(Object messageKey, Object message, String host, int port) {
    // 2-do: really!
    	//  System.out.println("WVM_Transporter: sendMessage2");
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
  /**
   * Client side: Requests to get a message from the remote {@link WVM}
   *
   * @param messageKey: type of message being requested, defined
   * in {@link WVM_Transporter}
   * @param host: hostname to ping
   * @param port: port to ping through
   * @return message that was received
   */
  final Object getMessage(Object messageKey, String host, int port) {
    // 2-do: really!
    	//	//  System.out.println("WVM_Transporter: getMessage");
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
}
