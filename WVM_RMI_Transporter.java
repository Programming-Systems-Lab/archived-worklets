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
import java.util.*;
import java.security.*;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

class WVM_RMI_Transporter extends WVM_Transporter{
  
  /** RMI Transportation Unit: the actual object that registers with the rmiRegistry */
  private RTU rtu;

  /** flag to denote whether the rmi transportation layer is active or not */
  private boolean rmiService;

  /** the locally-hosted rmiRegistry */
  private WVM_Registry rmiRegistry;

  /** flag to denote whether the rmiRegistry is locally hosted */
  private boolean registryService;

  int _port = -1;
  private RTU_RegistrarImpl rtuRegistrar;
  private String rmibind;
  private Date creation;

  /** reference to the host Worklet Virtual Machine */
  private WVM _wvm;

  /**
   * @deprecated never used
  */
  WVM_RMI_Transporter(WVM wvm, String host, String name) {
    this(wvm, host, name, WVM_Host.PORT);
  }

  WVM_RMI_Transporter(WVM wvm, String host, String name, int port) {
    this(wvm, host, name, port, null, null, null, null, null, null, 0);
  }

  /**
   * Create the RMI transporter layer for the host WVM
   */
  WVM_RMI_Transporter(WVM wvm, String host, String name, int port,
                      String keysfile, String password, String ctx, String kmf, String ks, String rng,
                      int securityLevel) {
    
    // start the basic sockets transporter layer
    super(wvm, host, name, port+1, keysfile, password, ctx, kmf, ks, rng, securityLevel);
    creation = new Date();    
    _port = port;
    rmibind = "rmi://" + _host + ":" + _port + "/" + _name;

    WVM.out.println("  creating the RMI transporter layer for the WVM");

    // setup RMI_Security Manager
    if (System.getSecurityManager() == null) {
      WVM_RMISecurityManager secMgr = new WVM_RMISecurityManager();
      if (_securityLevel == WVM.NO_SECURITY)
        secMgr.addReadableFiles(_webserver.getAliases());
      if (_securityLevel > WVM.NO_SECURITY)
        secMgr.addReadableFiles(_sslwebserver.getAliases());
      System.setSecurityManager(secMgr);
      // WVM.out.println ("Security Manager is: " + System.getSecurityManager().getClass());
    }

    if (_securityLevel > WVM.LOW_SECURITY) {
      try { RMISocketFactory.setSocketFactory((RMISocketFactory) _WVM_sf); }
      catch (IOException e) { WVM.err.println("Caught IOException in trying to setSocketFactory: " + e); }
    }
    
    setupRegistry();

    try {
      rtuRegistrar = new RTU_RegistrarImpl(_name, _host, _serverSocket.getLocalPort(), rng, creation);
    } catch (RemoteException e){
      WVM.err.println("Error creating RTU Registrar: " + e);
    }
    
    setupRMI();
  }

  /**
   * Attempt to create and host the RMI Registry locally
   */
  void setupRegistry() {
    // Create RMI Registry
    WVM.out.println("Creating RMI Registry: " + _host + ":" + _port);
    try {
      rmiRegistry = new WVM_Registry(_port);
      registryService = true;
      return;
    } catch (RemoteException e){
      rmiRegistry = null;
      registryService = false;
      WVM.out.println("  Could not create the RMI Registry");
    }
  }

  /**
   * Register the host WVM with the rmiRegistry
   */
  void setupRMI() {
    // Setup RMI registration
    final int RMI_RETRIES = 2; 
    int rmiRegistrationCount = 0;
    while (rmiRegistrationCount++ < RMI_RETRIES) {
      try {
	if (_securityLevel > WVM.NO_SECURITY) rtu = new RTU(_WVM_sf, _WVM_sf);
	else rtu = new RTU();
	_wvm = super._wvm;
	return;
      } catch (RemoteException e) {
        WVM.out.println("  Error creating the RMI Server, trying again ...");
	// e.printStackTrace();
        try {
          Thread.currentThread().sleep(1500);
        } catch (InterruptedException ie) { }
        continue;
      }
    }

    rtu = null;
    rmiService = false;
    WVM.out.println("  RMI Server could not be bound to: " + rmibind);
    WVM.out.println("Could not create the RMI transporter layer for the WVM");
  }

  /**
   * shutting down the WVM's rmi-transporter layer
   */
  void shutdown() {
    // shut down communications
    if (rtu != null) rtu.shutdown();

    if (registryService) {
      WVM.out.println("  Shutting down the RMI Registry: " + _host + ":" + _port);
      Date oldestDate = new Date();
      String nextRegistry = null;
      Map oriRegKeymap = rmiRegistry.getRegistrationKeys();
      HashMap regKeymap = new HashMap(oriRegKeymap);
      
      // this while loop does two functions. 
      // 1) contact that other servers and tell them to rejoin 
      //    the registry in a little while.
      // 2) try to find the oldest RMI server out there 
      //    to have it recreate the registry.  
      Set rkeys = regKeymap.keySet();
      Iterator itr = rkeys.iterator();

      while (itr.hasNext()) {
	String wvmUrl = (String) itr.next();	
	String regkey = (String) regKeymap.get(wvmUrl);
	WVM.out.println("  Broadcasting to: " + wvmUrl);
	Date tmpDate = (Date) getMessage(REJOIN_REGISTRY_REQUEST+regkey, wvmUrl);
	if (tmpDate.before(oldestDate)) {
	  oldestDate = tmpDate;
	  nextRegistry = wvmUrl;
	}
      }

      if (nextRegistry != null) {
	sendMessage(CREATE_REGISTRY_REQUEST, regKeymap.get(nextRegistry), nextRegistry);
      }

      // 2-do: verify that new registry is indeed up!!!
      rmiRegistry = null;
      registryService = false;
    }
      
    super.shutdown();
  }

  /**
  * alternate way of letting the RTU know that the registry is going
  * down
  */
  Date rejoinRegistry(String key) {
    try {
      return (key.equals(rtuRegistrar.getRegistrationKey()) ? rtu.rejoinRegistry() : null);
    } catch (RemoteException re) {
      // this will not really happen ;)
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * alternate way of getting the RMI_Transporter to start the
   * registry
   */
  void createRegistry(String key) {
    try {
      if (key.equals(rtuRegistrar.getRegistrationKey())) rtu.createRegistry();
    } catch (RemoteException re) {
      // this will not really happen ;)
    } catch (Exception e) {
    }
  }

  /**
   * send a worklet via rmi-transportation
   * @param wkl: worklet to send
   * @param wj: junction that determines the destination
   */
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

      else if (methods[i].equals("secureRMI"))
        try {
          WVM.err.println("  --  Trying to send through secureRMI");
          success = sendRMI(wkl, wj, true);
        } catch (Exception e){
          WVM.err.println("  --  Error sending through secureRMI: " + e);
        }

      else if (methods[i].equals("plainRMI"))
        try {
          WVM.err.println("  --  Trying to send through plainRMI");
          success = sendRMI(wkl, wj, false);
        } catch (Exception e){
          WVM.err.println("  --  Error sending through plainRMI: " + e);
        }

      if (success)
        break;
    }
    return success;
  }

  private boolean sendRMI(Worklet wkl, WorkletJunction wj, boolean secure){
    // WVM.out.println("  --  Sending worklet thru RMI");
    String rmiDestination = "//" + wj._host + ":" + wj._rmiport + "/" + wj._name;
    WVM.out.println("      RMI Destination: " + rmiDestination);

    try {
      WVM_Host wvmHost = null;
      Registry reg = null;
      
      if (secure)
	reg = LocateRegistry.getRegistry(wj._host, wj._rmiport, _WVM_sf);
      else
	reg = LocateRegistry.getRegistry(wj._host, wj._rmiport);

      wvmHost = (WVM_Host)reg.lookup(wj._name);

      // TODO: set up the BAG-MULTISET in the ClassFileServer so that the 
      // incoming BytecodeRetrieverWJ can get the data it needs
      wvmHost.receiveWorklet(wkl);
      return true;
    } catch (NotBoundException e) {
      WVM.out.println("      NotBoundException in SendWorklet: " + e);
      // e.printStackTrace();
    } catch (RemoteException e) {
      WVM.out.println("      RemoteException in SendWorklet: " + e);
      // e.printStackTrace();
    }
    return false;
  }

  public String toString() {
    if (rmiService) return (_name + " @ " + _host + " : " + super._port);
    return (" @ " + _host + " : " + super._port);
  }

  /**
   * parses a url used for representing a WVM's addressing information
   */
  private class WVM_Address {
    final String host;     /** ip address / hostName for WVM */
    final String name;     /** rmi-registration name for the WVM */
    final int port;        /** port that the WVM is listening on for its TCP communication layer */
    WVM_Address(String rh, String rn, int rp) {
      host = rh; name = rn; port = rp;
    }
  }
    
  /**
   * parse out the different addressing fields for representing
   * the a WVM's location
   * @param wvmURL: a url that represents a wvm location
   * WVM_URLs: rhost@rName:rPort
   */
  private WVM_Address parseWVM_URL(String wvmURL) {
    StringTokenizer st = new StringTokenizer(wvmURL, "@: ", true);
    // returns the delimiter characters as tokens as well
     
    String rHost = "";
    String rName = "";
    int rPort = 0;
    
    boolean found_AT = false;
    boolean found_COLON = false;
    
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.equals(":")) {
        found_COLON = true;
        continue;
      }
      
      if (token.equals("@")) {
        found_AT = true;
        continue;
      }
      
      if (found_COLON) {
        rPort = Integer.parseInt(token);
      } else if (found_AT) {
        rHost = token;
      } else {
        rName = token;
      }
    }
    return new WVM_Address(rHost, rName, rPort);
  }

  // Client-side //////////////////////////////////////////////////////////////
  protected boolean ping(String wvmURL) {
    WVM_Address wa = parseWVM_URL(wvmURL);
    String rHost = wa.host;
    String rName = wa.name;
    int rPort = wa.port;
    return ((rmiService && rtu.ping(rHost, rName)) || ping(rHost, rPort));
  }

  protected boolean sendMessage(Object messageKey, Object message, String wvmURL) {
    WVM_Address wa = parseWVM_URL(wvmURL);
    String rHost = wa.host;
    String rName = wa.name;
    int rPort = wa.port;
    return ((rmiService && rtu.sendMessage(messageKey, message, rHost, rName)) ||
      sendMessage(messageKey, message, rHost, rPort));
  }
  protected Object getMessage(Object messageKey, String wvmURL) {
    WVM_Address wa = parseWVM_URL(wvmURL);
    String rHost = wa.host;
    String rName = wa.name;
    int rPort = wa.port;
    Object message = null;
    if (rmiService) message = rtu.getMessage(messageKey, rHost, rName);
    return message!=null ? message : getMessage(messageKey, rHost, rPort);
  }
  // END: Client-side /////////////////////////////////////////////////////////

  /**
   * RMI Transportation Unit: the actual object that registers with the rmiRegistry
   */
  class RTU extends UnicastRemoteObject implements WVM_Host {
    RTU() throws RemoteException {
      this(null, null);
    }

    RTU(RMIClientSocketFactory csf, RMIServerSocketFactory ssf ) throws RemoteException {
      super(0, csf, ssf);

      try {
        if (_isClassServer) setCodebase();

	rtuRegistrar.bind(rmibind, this);
        rmiService = true;
        WVM.out.println("  RMI Listener: " + rmibind);

	if (_securityLevel > WVM.NO_SECURITY)
	  System.setProperty("java.rmi.server.RMIClassLoaderSpi", "psl.worklets.WVM_RMIClassLoaderSpi");
        return;

      } catch (java.rmi.AlreadyBoundException e) {
        if (!ping(_host, _name)){
	// If we get to here, the name is bound, but not to any active WVM
          try {
            WVM.out.println("Found the RMI name to be previously bound, but that server is dead.");
	    rtuRegistrar.rebind(rmibind, this);
            rmiService = true;
            WVM.out.println("  RMI Listener: " + rmibind);
            return;
          } catch (Exception rebind_e){
            WVM.err.println("Shutting down: " + rebind_e); 
          }
        }
      } catch (java.rmi.ConnectException e) {
        WVM.err.println("Shutting down; rmiregistry not running on port: " + _port);
      } catch (java.rmi.ServerException e) {
        WVM.err.println("Shutting down; cannot bind to a non-local host: " + _host + e);
      } catch (java.rmi.UnknownHostException e) {
        WVM.err.println("Shutting down; invalid host specified: " + _host);
      } catch (MalformedURLException e) {
	WVM.err.println("Shutting down; invalid host specified: " + _host);
      }

      rmiService = false;
      shutdown();        
      throw (new RemoteException());
    }
    
    private void setCodebase() {
      URL codebaseURL = null;
      String rmiCodebase = null;
      String codebase = "";

      if (_securityLevel > WVM.NO_SECURITY)
	codebase += " https://" + _host + ":" + _sslwebPort + "/"; 

      if (_securityLevel < WVM.HIGH_SECURITY)
        codebase += " http://" + _host + ":" + _webPort + "/"; 

      try {
        codebaseURL = new URL (codebase); // just to check whether it is malformed
        Properties props = System.getProperties();
        rmiCodebase = props.getProperty("java.rmi.server.codebase");
        if (rmiCodebase != null && !rmiCodebase.equals(codebase)) {
          rmiCodebase = codebase + " " + rmiCodebase;
        } else {
          rmiCodebase = codebase;
        }
        WVM.out.println ("  setting RMI codebase to: " + rmiCodebase);
        props.setProperty ("java.rmi.server.codebase", rmiCodebase);
        
      } catch (MalformedURLException e) {
        e.printStackTrace();
        // WVM.out.println ("serving class URL for this WVM is invalid: " + codebaseURL);  
        if (_securityLevel < WVM.HIGH_SECURITY) {
          _webserver.shutdown();
          _webserver = null;
        }
        if (_securityLevel > WVM.NO_SECURITY) {
          _sslwebserver.shutdown();
          _sslwebserver = null;
        }
        _isClassServer = false;
        // WVM.out.println ("Cannot serve classes from this WVM");
      }
    }

    void shutdown() {
      try {
        _wvm = null;
        if (rmiService) Naming.unbind("rmi://" + _host + ":" + _port + "/" + _name);
      } catch (java.rmi.ConnectException e) {
        WVM.err.println("Cannot unbind because rmiregistry is not running on port: " + _port);
      } catch (Exception e) {
        WVM.err.println("Exception: " + e);
        e.printStackTrace();
      } finally {
        WVM.out.println("Shut down the RMI transporter layer for the WVM");
      }
    }
      
    public void receiveWorklet(Worklet wkl) throws RemoteException {
      // TODO: Okay, now that the LEAST REQUIRED SET (LRS) of class bytecode
      // has been downloaded from the source WVM, send out a BytecodeRetrieverWJ
      // to get the relevant classes, viz. all those classes that the source
      // HTTP server served up to this WVM

      // adding to WVM's in-tray, and don't need to send out BytecodeRetrieval worklet
      wkl.retrieveBytecode = false;
      _wvm.installWorklet(wkl);
    }

    public Date rejoinRegistry() throws RemoteException {
      WVM.out.println("asked to rejoin rmiregistry");
      if (rmiService) rtu.shutdown();
      rmiService = false;
      Thread t = new Thread() {
        public void run() {
          // this thread will cause the RTU to join the new Registry
          for (int i=0; i<5; i++) {
            try {
              sleep(1000);
            } catch (InterruptedException e) { }
          }
          WVM.out.println("  -- going to re-join rmiregistry");
          setupRMI();
        }
      };
      t.start();
      return (creation);
    }

    public void createRegistry() throws RemoteException {
      Thread t = new Thread() {
        public void run() {
          // this thread will cause the RTU to create the new Registry
          for (int i=0; i<3; i++) {
            try {
              sleep(1000);
              WVM.out.print(" * ");
            } catch (InterruptedException e) { }
          }
          setupRegistry();
        }
      };
      t.start();
    }
    
    // Client-side ////////////////////////////////////////////////////////////
    boolean ping(String rHost, String rName) {
      try {
        // WVM.out.println("  --  pinging peer WVM thru RMI: " + rName + "@" + rHost);
        return ((WVM_Host) Naming.lookup("//" + rHost + ":" + _port + "/" + rName)).ping();
      } catch (NotBoundException e) {
        // WVM.out.println("NotBoundException: " + e);
        // e.printStackTrace();
      } catch (MalformedURLException e) {
        // WVM.out.println("NotBoundException: " + e);
        // e.printStackTrace();
      } catch (RemoteException rmie) {
        // WVM.out.println("RemoteException: " + rmie);
      }
      return false;
    }
    boolean sendMessage(Object messageKey, Object message, String rHost, String rName) {
      try {
        // WVM.out.println("  --  sending a message to a peer WVM thru RMI: " + rName + "@" + rHost);
        return ((WVM_Host) Naming.lookup("//" + rHost + ":" + _port + "/" + rName)).receiveMessage(messageKey, message);
      } catch (NotBoundException e) {
        // WVM.out.println("NotBoundException: " + e);
        // e.printStackTrace();
      } catch (MalformedURLException e) {
        // WVM.out.println("NotBoundException: " + e);
        // e.printStackTrace();
      } catch (RemoteException rmie) {
        // WVM.out.println("RemoteException: " + rmie);
      }
      return false;
    }
    Object getMessage(Object messageKey, String rHost, String rName) {
      try {
        // WVM.out.println("  --  getting a message from a peer WVM thru RMI: " + rName + "@" + rHost);
        return ((WVM_Host) Naming.lookup("//" + rHost + ":" + _port + "/" + rName)).requestMessage(messageKey);
      } catch (NotBoundException e) {
        // WVM.out.println("NotBoundException: " + e);
        // e.printStackTrace();
      } catch (MalformedURLException e) {
        // WVM.out.println("NotBoundException: " + e);
        // e.printStackTrace();
      } catch (RemoteException rmie) {
        // WVM.out.println("RemoteException: " + rmie);
      }
      return null;
    }
    // END: Client-side ///////////////////////////////////////////////////////

    // Server-side ////////////////////////////////////////////////////////////
    public boolean ping() throws RemoteException {
      // WVM.out.println("  --  being PINGED thru RMI");
      return true;
    }
    public boolean receiveMessage(Object messageKey, Object message) throws RemoteException {
      // WVM.out.println("  --  received a message thru RMI");
      return _wvm.receiveMessage(messageKey, message);
    }
    public Object requestMessage(Object messageKey) throws RemoteException {
      // WVM.out.println("  --  received a message request thru RMI");
      return _wvm.requestMessage(messageKey);
    }
    // END: Server-side ///////////////////////////////////////////////////////

    public boolean isSecure() throws RemoteException{
      if (_securityLevel > WVM.NO_SECURITY)
        return true;
      else 
        return false;
    }
  } // END: class RTU

  /**
   * This class defines a security policy for RMI applications
   * that are bootstrap loaded from a server. The relaxation in
   * security provided by this class is the minimal amount that
   * is required to bootstrap load and run a RMI client application.
   *
   * The policy changes from RMISecurityManager are:
   *
   * Security Check                    This Policy  RMISecurityManager
   * ------------------------------   ------------  ------------------
   * Access to Thread Groups               YES              NO
   * Access to Threads                     YES              NO
   * Create Class Loader                   YES              NO
   * System Properties Access              YES              NO
   * Connections                           YES              NO
   * File Read                                 Limited            NO
   *
   */
  
  
  public class WVM_RMISecurityManager extends RMISecurityManager {
    
    private Set readableFiles = Collections.synchronizedSet(new HashSet());
    
    public void addReadableFiles (Collection c) {
      readableFiles.addAll(c);
      // WVM.out.println ("**** ReadableFiles size " + readableFiles.size() + " ****");
    }
    
    public void addReadableFile (String filePath) {
      readableFiles.add(filePath);
      // WVM.out.println ("**** ReadableFiles size " + readableFiles.size() + " ****");
    }
    
    public void removeReadableFiles (Collection c) {
      readableFiles.removeAll(c);
    }
    
    public void removeReadableFile (String filePath) {
      readableFiles.remove(filePath);
    }
    
    /**
     * Loaded classes are allowed to create class loaders.
     */
    public synchronized void checkCreateClassLoader() { }
    
    /**
     * Connections to other machines are allowed
     */
    public synchronized void checkConnect(String host, int port) { }
  
    /**
     * Connections to other machines are allowed
     */
    public synchronized void checkConnect(String host, int port, Object context) { }
    
    /**
     * Connections to other machines are allowed
     */
    public synchronized void checkListen(int port) { }
    
    /**
     * Connections from other machines are allowed
     */
    public synchronized void checkAccept(String host, int port) { }
    
    /**
     * Loaded classes are allowed to manipulate threads.
     */
    public synchronized void checkAccess(Thread t) { }
    
    /**
     * Loaded classes are allowed to manipulate thread groups.
     */
    public synchronized void checkAccess(ThreadGroup g) { }
    
    /**
     * Loaded classes are allowed to access the system properties list.
     */
    public synchronized void checkPropertiesAccess() { }
    
    /**
     * Loaded classes are allowed to access s.
     */
    public synchronized void checkPropertyAccess(String key) { }
    public synchronized void checkRead(String aFile)  { }
    public synchronized void checkPermission(Permission perm)   {
      try {
        Class sockPerm = Class.forName("java.net.SocketPermission");
        if (sockPerm.isAssignableFrom(perm.getClass())) {
          // is perm a SocketPermission?
          checkSocketPermission((SocketPermission) perm);
        }
      } catch (ClassNotFoundException e) { }
    }
    private synchronized void checkSocketPermission (SocketPermission perm) { }

  } // END: class WVM_RMISecurityManager
} // END: class WVM_Transporter
