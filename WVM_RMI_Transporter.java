package psl.worklets;

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

class WVM_RMI_Transporter extends WVM_Transporter {
  
  private RTU rtu;
  private boolean rmiService;
  private int _port = WVM_Host.PORT;
  private Registry rmiRegistry;
  private boolean registryService;
  private Date creation;

  private WVM _wvm;
  
  WVM_RMI_Transporter(WVM wvm, String host, String name) {
    this(wvm, host, name, WVM_Host.PORT);
  }
  WVM_RMI_Transporter(WVM wvm, String host, String name, int port) {
    // start the basic sockets transporter layer
    super(wvm, host, name, port);
    creation = new Date();
    
    System.out.println("Creating the RMI transporter layer for the WVM");

    // setup RMI_Security Manager
    if (System.getSecurityManager() == null) {
      WVM_RMISecurityManager secMgr = new WVM_RMISecurityManager();
      secMgr.addReadableFiles(_webserver.getAliases());
      System.setSecurityManager(secMgr);
      // System.out.println ("Security Manager is: " + System.getSecurityManager().getClass());
    }

    setupRegistry();
    setupRMI();
  }

  void setupRegistry() {
    // Create RMI Registry
    try {
      System.out.println("Creating RMI Registry: " + _host + ":" + _port);
      rmiRegistry = LocateRegistry.createRegistry(9100);
      registryService = true;
    } catch (RemoteException e) {
      System.out.println("Could not create the RMI Registry");
      rmiRegistry = null;
      registryService = false;
    }
  }

  void setupRMI() {
    // Setup RMI registration
    final int RMI_RETRIES = 3;
    int rmiRegistrationCount = 0;
    while (rmiRegistrationCount++ <= RMI_RETRIES) {
      try {
        rtu = new RTU();
        rmiService = true;
        _wvm = super._wvm;
        return;
      } catch (RemoteException e) {
        try {
          Thread.currentThread().sleep(500);
        } catch (InterruptedException ie) { }
        continue;
      }
    }
    System.out.println("Could not create the RMI transporter layer for the WVM");
    rtu = null;
    rmiService = false;
  }

  protected void shutdown() {
    // shut down communications
    rtu.shutdown();
    super.shutdown();

    // shut down RMIRegistry
    try {
      if (registryService) {
        System.out.println("Shutting down the RMI Registry: " + _host + ":" + _port);
        Date oldestDate = new Date();
        int nextRegistry = -1;
        WVM_Host wvmH = null;
        String rServers[] = rmiRegistry.list();
        for (int i=0; i<rServers.length; i++) {
          String name = rServers[i];
          WVM_Host tmpWVM_H = null;
          try {
            tmpWVM_H = (WVM_Host) Naming.lookup("rmi://" + _host + ":" + _port + "/" + name);
          } catch (NotBoundException e) {
            System.out.println("RMI Service was not bound: " + name);
          } catch (MalformedURLException e) {
            System.out.println("MalformedURLException: " + name);
          } finally {
            System.out.println("    unregistered service: " + name);
          }
          Date tmpDate = tmpWVM_H.rejoinRegistry();
          if (tmpDate.before(oldestDate)) {
            oldestDate = tmpDate;
            nextRegistry = i;
            wvmH = tmpWVM_H;
          }
        }
        if (nextRegistry >= 0) {
          wvmH.createRegistry();
        }
        rmiRegistry = null;
        registryService = false;
      }
    } catch (RemoteException e) {
      System.out.println("RemoteException: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  protected void sendWorklet(Worklet wkl, WorkletJunction wj) {
    if (rmiService) {
      try {
        System.out.println("  --  Sending worklet thru RMI");
        rtu.sendWorklet(wkl, wj);
        return;
      } catch (RemoteException e) {
        System.out.println("  --  Cannot send worklets thru RMI, defaulting to sockets");
        e.printStackTrace();
      }
    }
    super.sendWorklet(wkl, wj);
  }

  class RTU extends UnicastRemoteObject implements WVM_Host {
    
    RTU() throws RemoteException {
      try {
        if (_isClassServer) {
          setCodebase();
        }
        Naming.rebind("rmi://" + _host + ":" + _port + "/" + _name, this);
        System.out.println("  RMI Listener: " + " rmi://" + _host + ":" + _port + "/" + _name);
        return;
      } catch (java.rmi.ConnectException e) {
        System.err.println("Shutting down; rmiregistry not running on port: " + _port);
      } catch (java.rmi.ServerException e) {
        System.err.println("Shutting down; cannot bind to a non-local host: " + _host);
      } catch (java.rmi.UnknownHostException e) {
        System.err.println("Shutting down; invalid host specified: " + _host);
      } catch (java.net.MalformedURLException e) {
        System.err.println("Shutting down; invalid host specified: " + _host);
      }
      shutdown();
      throw (new RemoteException());
    }

    private void setCodebase() {
      URL codebaseURL = null;
      String rmiCodebase = null;
      String codebase = "http://" + _host + ":" + _webPort + "/"; 
      try {
        codebaseURL = new URL (codebase); // just to check whether it is malformed
        Properties props = System.getProperties();
        rmiCodebase = props.getProperty("java.rmi.server.codebase");
        if (rmiCodebase != null && !rmiCodebase.equals(codebase)) {
          rmiCodebase = codebase + " " + rmiCodebase;
        } else {
          rmiCodebase = codebase;
        }
        System.out.println ("Setting RMI codebase to: " + rmiCodebase);
        props.setProperty ("java.rmi.server.codebase", rmiCodebase);
        
      } catch (MalformedURLException e) {
        e.printStackTrace();
        // System.out.println ("serving class URL for this WVM is invalid: " + codebaseURL);  
        _webserver.shutdown();
        _webserver = null;
        _isClassServer = false;
        // System.out.println ("Cannot serve classes from this WVM");
      }
    }

    void shutdown() {
      try {
        _wvm = null;
        if (rmiService) Naming.unbind("rmi://" + _host + ":" + _port + "/" + _name);
      } catch (java.rmi.ConnectException e) {
        System.err.println("Cannot unbind because rmiregistry is not running on port: " + _port);
      } catch (Exception e) {
        System.err.println("Exception: " + e);
        e.printStackTrace();
      } finally {
        System.out.println("Shut down the RMI transporter layer for the WVM");
      }
    }
      
    public void receiveWorklet(Worklet wkl) throws RemoteException {
      // adding to WVM's in-tray
      _wvm.installWorklet(wkl);
    }

    public void sendWorklet(Worklet wkl, WorkletJunction wj) throws RemoteException {
      try {
        WVM_Host wvmHost = (WVM_Host) Naming.lookup("//" + wj._host + ":" + _port + "/" + wj._name);
        wvmHost.receiveWorklet(wkl);
      } catch (NotBoundException e) {
        System.out.println("NotBoundException: " + e.getMessage());
        // e.printStackTrace();
        throw(new RemoteException(e.getMessage()));
      } catch (MalformedURLException e) {
        System.out.println("MalformedURLException: " + e.getMessage());
        // e.printStackTrace();
        throw(new RemoteException(e.getMessage()));
      } catch (RemoteException e) {
        System.out.println("RemoteException: " + e.getMessage());
        // e.printStackTrace();
        throw(e);
      }
    }

    public Date rejoinRegistry() throws RemoteException {
      System.out.println("asked to rejoin rmiregistry");
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
          System.out.println("  -- going to re-join rmiregistry");
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
              System.out.print(" * ");
            } catch (InterruptedException e) { }
          }
          setupRegistry();
        }
      };
      t.start();
    }

  }

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
   * File Read       Limited     NO
   *
   */
  
  
  public class WVM_RMISecurityManager extends RMISecurityManager {
    
    private Set readableFiles = Collections.synchronizedSet(new HashSet());
    
    public void addReadableFiles (Collection c) {
      readableFiles.addAll(c);
      // System.out.println ("**** ReadableFiles size " + readableFiles.size() + " ****");
    }
    
    public void addReadableFile (String filePath) {
      readableFiles.add(filePath);
      // System.out.println ("**** ReadableFiles size " + readableFiles.size() + " ****");
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
  }
  
}

