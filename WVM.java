package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *  
 * Copyright (c) 2001: @author Gaurav S. Kc 
 * 
*/

import java.io.*;
import java.net.*;
import java.util.*;

import psl.worklets.WVMRSL.Registration;

public final class WVM extends Thread {
  public static PrintStream out = WVM.out;
  WVM_Transporter transporter;
  private final Object _system;
  private final Hashtable _peers = new Hashtable();
  private final Vector _installedWorklets = new Vector();
  private final MultiMap _installedJunctions = new MultiMap();

  private static Registration reg; 
  private static boolean registered;
  private static Properties reg_ini;

  public static boolean NO_BYTECODE_RETRIEVAL_WORKLET = false;
  public final static String time() {
    Calendar c = new GregorianCalendar();
    return ("@ " + (1000 + c.get(Calendar.SECOND))%1000 + ":" + (1000 + c.get(Calendar.MILLISECOND))%1000 + " - ");
  }

  public static final int DEBUG = Integer.parseInt(System.getProperty("DEBUG", "0"));
  public static final boolean DEBUG(int d) { return d<DEBUG; }
  private static final String ini_file = new String(System.getProperty("INIFILE",""));

  public WVM(Object system) {
    this(system, null, "WVM");
  }

  public WVM(Object system, String host, String name) {
    this(system, host, name, WVM_Host.PORT+1);
    // cannot have the WVM_Transporter listen on port: WVM_Host.PORT !!!
  }  

  public WVM(Object system, String host, String name, int port) {
    WVM.out.println("WVM created");
    // URL.setURLStreamHandlerFactory(new WVM_URLStreamHandlerFactory());
    _system = system;
    try {
      if (host == null) {
        try {
          host = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
          host = "127.0.0.1";
        }
      }

      if (name == null) name =  _system.hashCode() + "_" +  System.currentTimeMillis();

      transporter = new WVM_RMI_Transporter(this, host, name, port);
      transporter.start();
    } catch (Exception e) {
      WVM.out.println(e.getMessage());
      e.printStackTrace();
    }
    
    if (initRegistration()) {      
      if (Register()){
        registered = true;
        _myStats.log("Registration succesful");
      } else {
        registered = false;
        _myStats.log("Registration UNSUCCESFUL");
      }
    } else {
      WVM.out.println("Could not init Registration object");
    }
    
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (registered){
          //	WVM.out.println("Trying to unregister");
          if (Unregister(true)){
            _myStats.log("UnRegistrationl succesful");
          } else {
            _myStats.log("UnRegistrationl UNSUCCESFUL");
          } 
        }
        shutdown();
      }
    });
    start();
  }

  public void shutdown() {
    _peers.clear();
    _installedWorklets.clear();

    if (transporter != null) {
      transporter.shutdown();
      transporter = null;
    }
    // _system = null;
    WVM.out.println("WVM destroyed");
  }

  /**
   * Used for keeping track of local worklets, to help determine
   * which worklet has completed execution, and must move on
   */
  final static Hashtable _activeWorklets = new Hashtable();
  
  /**
   * Main WVM loop, wait indefinitely for installed worklets
   */
  public void run() {
    synchronized (this) {
      while (true) {
        // wait indefinitely to execute installed worklets
        if (_installedWorklets.isEmpty()) {
          try { 
            wait();
          } catch (InterruptedException e) { } 
        }
        
        Worklet _worklet = (Worklet) _installedWorklets.firstElement();
        _installedWorklets.removeElement(_worklet);
        String hashCode = (new Integer(_worklet.hashCode())).toString();
        
        // this is used to lookup the worklet when it is ready to leave
        _activeWorklets.put(hashCode, _worklet);
        executeWorklet(_worklet, hashCode);
      }
    }
  }

  /**
   * Used by the transportation layers to install a newly arrived worklet in the 
   * WVM's repository, so that the WVM can execute it at its earliest convenience
   * @param _worklet: newly arrived worklet
   */
  synchronized void installWorklet(Worklet _worklet) {
    _worklet.init(_system, this);
    _installedWorklets.addElement(_worklet);
    if (_installedWorklets.size() == 1) {
      // WVM was asleep coz' in-tray was empty, must wake up!
      this.notifyAll();
    }
  }

  /**
   * Start executing this worklet
   * @param _worklet: worklet to execute
   * @param _hashCode: identifier for locating this worklet among all local worklets
   */
  private void executeWorklet(Worklet _worklet, String _hashCode) {
    Worklet _executingWorklet = _worklet;
    _executingWorklet._hashCode = _hashCode;
    // (new Thread(_executingWorklet, _hashCode)).start();
    _executingWorklet.execute();
  }

  public String toString() {
    return (transporter.toString());
  }

  // WVM Registration and Lookup ///////////////////////////////////////////////
  public boolean initRegistration(){
    Properties p = new Properties();
    try {
      p.load(new FileInputStream(ini_file));
    } catch(Exception e){
      _myStats.log("Error reading properties from file: " + ini_file);
      return false;
    }
    
    String ip = p.getProperty("IP"); //multicast ip
    if (ip == null) {
      _myStats.log("Property missing: IP");
      return false;
    }
    
    String port = p.getProperty("PORT");//multicast port
    if (port == null) {
      _myStats.log("Property missing: PORT");
      return false;
    }
    int portt = Integer.parseInt(port);

    // local server port for registration response
    String local_port = p.getProperty("LOCAL_PORT");
    if(local_port == null){
      _myStats.log("Property missing: LOCAL_PORT");
      return false;
    }
    int local_portt = Integer.parseInt(local_port);

    try {
      reg = new Registration(ip, portt, local_portt, "", getWVMName(), getWVMAddr(), getWVMPort());
    }	catch (Exception e) {
      return false;
    }
    return true;
  }

  public boolean Register(){
    return reg.Register();
  }

  public boolean Unregister(){
    return reg.Unregister(false);
  }

  public boolean Unregister(boolean dying){
    WVM.out.println("Calling unregistration");
    return reg.Unregister(dying);
  }
  // END: WVM Registration and Lookup //////////////////////////////////////////

  public int getWVMPort() { return (transporter._port); }
  public String getWVMName() { return (transporter._name); }
  public String getWVMAddr() { return (transporter._host); }
  public int getRMIPort() { return (((WVM_RMI_Transporter) transporter)._port); }
  
  // Client-side ///////////////////////////////////////////////////////////////
  public boolean ping(String wvmURL) {
    return (transporter.ping(wvmURL));
  }
  public boolean sendMessage(Object messageKey, Object message, String wvmURL) {
    return (transporter.sendMessage(messageKey, message, wvmURL));
  }
  public Object getMessage(Object messageKey, String wvmURL) {
    return (transporter.getMessage(messageKey, wvmURL));
  }
  // END: Client-side //////////////////////////////////////////////////////////

  // Server-side ///////////////////////////////////////////////////////////////
  public Runnable requestHandler = null;
  // Not used ... final Vector messageQueue = new Vector();
  public final Hashtable messageQueueKeys = new Hashtable();
  public final Hashtable messageQueueMsgs = new Hashtable();

  boolean receiveMessage(Object messageKey, Object message) {
    Thread t = new Thread(requestHandler);
    Integer i = new Integer(t.hashCode());
    String uniqueKey = i + "-" + messageKey;

    messageQueueMsgs.put(uniqueKey, message);
    messageQueueMsgs.put(i, uniqueKey);
    messageQueueKeys.put(i, messageKey);

    if (requestHandler != null) {
      t.start();
      try { t.join(200); } catch (InterruptedException ie) { }
    }

    return true;
  }

  Object requestMessage(Object messageKey) {
    Thread t = new Thread(requestHandler);
    Integer i = new Integer(t.hashCode());
    String uniqueKey = i + "-" + messageKey;

    messageQueueMsgs.put(i, uniqueKey);
    messageQueueKeys.put(i, messageKey);

    if (requestHandler != null) {
      t.start();
      try { t.join(5000); } catch (InterruptedException ie) { }
      return messageQueueMsgs.get(uniqueKey);
    } else return null;
  }
  // END: Server-side //////////////////////////////////////////////////////////


  public static void main(String args[]) throws UnknownHostException {
    WVM.out.println("usage: java psl.worklets.WVM <wvmName>");
    String rmiName = args.length == 0 ? "WVM" : args[0];
    WVM wvm = new WVM(new Object(), null, rmiName);
  }


  // -------------- ADDED VARS AND FXNS TO MANAGE WORKLETJUNCTIONS --------------- //

  private WVM_Stats _myStats = new WVM_Stats();
  
  // register a junction to the WVM.
  void registerJunction(WorkletJunction wj) {
    _installedJunctions.put(wj.id(), wj);
    _myStats.log("Added Junction: ", wj);
    _myStats.accept();
  }

  // Get a reference to a certain worklet junction.
  Vector getJunctions(WorkletID id) {
    return (Vector)_installedJunctions.get(id);
  }

  // there are two remove workletJunction functions because 
  // we can either remove 
  // 1) a specific worklet junction
  // 2) all junctions with a specific workletID (vector of junctions) or
  boolean removeJunction(WorkletJunction wj) {
    Object o = _installedJunctions.remove(wj.id(), wj);
    
    if (o != null) {
      _myStats.log("Removal succesful: ", wj);
      _myStats.remove();
      return true;
    } else { 
      _myStats.log("Removal unsuccesful: ", wj);
      return false;
    }
  }

  boolean removeJunction(WorkletID wj_id) {
    Object o = _installedJunctions.remove(wj_id);
    
    if (o != null) {
      Vector v = (Vector)o;
      for (int i = 0; i < v.size(); i++)
        _myStats.remove();

      _myStats.log("Removal of " + v.size() + " WorkletJunction(s) succesful: " + wj_id);

      return true;

    } else { 
      _myStats.log("Removal unsuccesful: " + wj_id);
      return false;
    }
  }
  
  void stats(){
    _myStats.dumpStats();
  }
  
  // -------------- ADDED CLASS AND FXNS TO GET WORKLETJUNCTION STATS --------------- //

  /* WVM_Stats: keep track of the WVM stats.  Currently only keeps basic stats 
  * on WorkletJunctions.
  *
  *
  * TODO: 
  * - add more capability and statistics
  *
  * NOTES: 
  * - currently only keeps these stats:
  *   > log of junction accepted and removed
  *   > number of total junctions accepted and removed
  * 
  * - it could easily keep the same stats for a set of Worklets.
  * 
  * - to add worklets capability add a log() function specific for
  * Worklets, add a printCurrentWorklets similar to the
  * printCurrentJunctions, and add the Worklet specific info to the
  * summary.  We might also want to keep the accepted and removed  
  * number for worklets and worklet junctions separate. 
  * 
  * 
  * 
  */

  private class WVM_Stats {
    private Vector _wvmLog = new Vector();	// keeps a log of certain events
    private int _accepted;			// total number of objects accepted
    private int _removed;			// total number of objects removed

    public WVM_Stats(){}

    // add generic message to log
    public void log(String message){
      _wvmLog.addElement(new Date() + ": " + message);
    }

    // add workletJunction specific message to the log.
    public void log(String message, WorkletJunction wj){
      _wvmLog.addElement(new Date() + ": " + message + wj + "." + wj.id() + ": " + wj.sstate());
    }

    // dump some stats about the WVM.
    public void dumpStats(){
      WVM.out.println("Printing WVM stats (" + new Date() + ")");
      WVM.out.println();

      printLog();
      WVM.out.println();

      printCurrentJunctions();
      WVM.out.println();
      
      printSummary();
      WVM.out.println();
    }

    // print the current log.
    private void printLog(){
      Iterator itr = _wvmLog.iterator();
      WVM.out.println("WVM log:");
      while (itr.hasNext())
        WVM.out.println("" + itr.next());
    }

    // print the current worklet junctions in the WVM.  We
    // access the _installedJunctions private member in the WVM.
    private void printCurrentJunctions(){
      WVM.out.println("Printing current WVM Junctions: " +  "(" + new Date() + ")");	    
      Set s = _installedJunctions.keySet();
      Iterator setItr = s.iterator();

      while (setItr.hasNext()) {
        // Print the WorkletID
        WorkletID id = (WorkletID)setItr.next();
        WVM.out.println("WorkletID: " + id + ":");
        Vector v = (Vector)_installedJunctions.get(id);
        
        // Print the WorkletJunctions associated with this WorkletID
        Iterator vecItr = v.iterator();
        while (vecItr.hasNext()) {
          WorkletJunction wj = (WorkletJunction)vecItr.next();
          WVM.out.println("  " + wj + ": " + wj.sstate());
        }
      }
    }

    // print the summary
    private void printSummary(){
      WVM.out.println("Total accepted: " + _accepted);
      WVM.out.println("Total removed: " + _removed);

      // WorkletJunction specific summary
      WVM.out.println("Total of " + _installedJunctions.size() + " WorkletID's and " 
                         + _installedJunctions.sizeValues() + " WorkletJunction(s)");
    }
    
    // Accept an object into the stats
    public void accept(){
      _accepted++;
    }

    // Remove an object from the stats
    public void remove(){
      _removed++;
    }

    // Clear the wvm stats log
    public void clearLog(){
      _wvmLog.clear();
    }
  }
}
