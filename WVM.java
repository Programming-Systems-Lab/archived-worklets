/*
 * @(#)WVM.java
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
import psl.worklets.WVMRSL.Registration;

/**
 * The {@link WVM} (Worklet Virtual Machine) is the interface
 * between the system and incoming Worklets that provides the
 * execution environment.
 *
 * @version	$Revision$ $Date$
 * @author	Gaurav S. Kc (gskc@cs.columbia.edu)
 */
public final class WVM extends Thread {
  public static PrintStream out = System.out;
  public static PrintStream err = System.err;
    public static String wvm_id = "";
  /**
   * The {@link WVM_Transporter} handles the sending and receiving of worklets and
   * messages (ping, rejoin registry requests, etc.)
   */
  public static WVM_Transporter transporter;

  /** The host adapter that acts as a reference to the target system */
  private final Object _system;

  /**
   * Collection of peer WVM's known to this WVM
   */
  private final Hashtable _peers = new Hashtable();

  /** Collection of {@link Worklet}(s) currently installed in this WVM */
  private final Vector _installedWorklets = new Vector();

  /** Collection of {@link WorkletJunction}(s) currently installed in this WVM */
  private final MultiMap _installedJunctions = new MultiMap();

    /* collection of junctions of to send byte codes to*/
    private final Hashtable _junctions = new Hashtable();

  /**
   * dp2041 The registration object that handles <code>WVMRSL</code> related issues
   * dp2041: email alex to see if this is correct.
   */
  private static Registration _reg;

  /** Keeps track of whether this WVM is registered */
  private static boolean _registered;


    public static WorkletByteCodeCache wkltRepository = new WorkletByteCodeCache();


  /**
   * The file that holds the properties of the <code>Registration</code> object
   * dp2041: not used
   */
  private static Properties _reg_ini;

  /**
   * Returns the current time
   *
   * @return String indicating the current time
   */
  public final static String time() {
    Calendar c = new GregorianCalendar();
    return ("@ " + (1000 + c.get(Calendar.SECOND))%1000 + ":" + (1000 + c.get(Calendar.MILLISECOND))%1000 + " - ");
  }

  /** Sets the flag for the package from the environment value of "DEBUG" */
  public static final int DEBUG = Integer.parseInt(System.getProperty("DEBUG", "0"));

  /**
   * Sets the debug flag for the package through a function
   *
   * @param d: desired setting for DEBUG
   */
  public static final boolean DEBUG(int d) { return d<DEBUG; }

  /**
   * Sets the INI file from the environment value of "INIFILE", which
   * is used by the WVM registration feature
   */
  private static final String ini_file = new String(System.getProperty("INIFILE",""));

  /** securityLevel = 0: No security features are used */
  public static final int NO_SECURITY   = 0;

  /**
   * securityLevel = LOW = 1: plain RMI registry, secure RMI server:
   * plain and secure server sockets, and plain and secure class loaders
   */
  public static final int LOW_SECURITY  = 1;

  /**
   * securityLevel = MED = 2: secure RMI registry, secure RMI server:
   * plain and secure server sockets, and plain and secure class loaders
   */
  public static final int MED_SECURITY  = 2;

  /**
   * securityLevel = HIGH = 3: secure RMI registry, secure RMI server:
   * secure server sockets and associated secure class loaders
   */
  public static final int HIGH_SECURITY = 3;

  /**
   * Creates a WVM on the localhost with the default name "WVM"
   *
   * @param system: host adapter that acts as a reference to the system
   */
  public WVM(Object system) {
    this(system, null, "WVM");
  }

  /**
   * Creates a WVM with the parameters retrieved by the {@link OptionsParser}
   *
   * @param op: {@link OptionsParser} that is previously initialized and used
   * to get the parameters for the WVM, see {@link OptionsParser}
   */
  public WVM(OptionsParser op) {
    this(new Object(), null, op.name, op.port, op.keysfile, op.password,
	 op.ctx, op.kmf, op.ks, op.rng,	 op.securityLevel);
  }

  /**
   * Creates a WVM that with the specified name that interfaces with
   * the system on the specified host
   *
   * @param system: host adapter that acts as a reference to the system
   * @param host: host on which to create the WVM
   * @param name: label given to the WVM
   */
  public WVM(Object system, String host, String name) {
    this(system, host, name, WVM_Host.PORT, null, null, null, null, null, null, 0);
  }

  /**
   * Creates a WVM that with the specified parameters
   *
   * @param system: host adapter that acts as a reference to the system
   * @param host: host on which to create the WVM
   * @param name: RMI name to bind to
   * @param port: port on which to create the RMI registry
   * @param keysfile: file holding the public/private keys
   * @param password: password into the keysfile
   * @param ctx: <code>SSLContext</code> under which to create the WVM
   * @param kmf: <code>KeyManagerFactory</code> type of use
   * @param ks: <code>KeyStore</code> type to use
   * @param rng: <code>SecureRandom</code> (random number generator algorithm) to use
   * @param securityLevel: security level at which to create this WVM
   */
  public WVM(Object system, String host, String name, int port,
	     String keysfile, String password, String ctx, String kmf, String ks, String rng,
	     int securityLevel) {

    // first we check the security related paramters for completeness
    if (keysfile != null && password != null && System.getProperty("WVM_FILE") == null){
      WVM.err.println("WVM security parameters incomplete.  see -help or Worklet documentation for details.");
      System.exit(0);
    }
    // System.out.println("IN WVM CONSTRUCTOR");
    // then we initialize the WVM parameters
    _system = system;
    try {
      // try to get the localhost name
      if (host == null) {
        try { host = InetAddress.getLocalHost().getHostAddress(); }
	catch (IOException e) { host = "127.0.0.1"; }
      }

      if (name == null) name =  _system.hashCode() + "_" +  System.currentTimeMillis();
      wvm_id = name;

      transporter = new WVM_RMI_Transporter(this, host, name, port,
					    keysfile, password, ctx, kmf, ks, rng,
					    securityLevel);
      transporter.start();
      WVM.out.println("Ready to accept worklets");

    } catch (Exception e) {
      WVM.out.println(e.getMessage());
      e.printStackTrace();
    }

    // initialize the WVM Registration related information
    if (initRegistration()) {
      if (Register()){
        _registered = true;
        _myStats.log("Registration succesful");
      } else {
        _registered = false;
        _myStats.log("Registration UNSUCCESFUL");
      }
    } else {
      // WVM.out.println("Could not init Registration object");
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (_registered){
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

    WVM.out.println("WVM created");
  }

  /** Starts the shutdown cascade for the WVM and related components */
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
   * Keeps track of local worklets to help determine which worklet has
   * completed execution and must move on
   */
  final static Hashtable _activeWorklets = new Hashtable();

  /** Main WVM loop: waits indefinitely for installed worklets */
  public void run() {
    Worklet _worklet;
    synchronized (this) {
      while (true) {
        // wait indefinitely to execute installed worklets
        if (_installedWorklets.isEmpty()) {
          try {
            wait();
          } catch (InterruptedException e) { }
        }
        _worklet = (Worklet) _installedWorklets.firstElement();
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
   *
   * @param _worklet: newly arrived worklet
   */
  synchronized void installWorklet(Worklet _worklet) {
      //   System.out.println("WVM: inastallWorklet");
    _worklet.init(_system, this);
    _installedWorklets.addElement(_worklet);
    if (_installedWorklets.size() == 1) {
      // WVM was asleep coz' in-tray was empty, must wake up!
      this.notifyAll();
    }
  }


  /**
   * Starts executing this worklet
   *
   * @param _worklet: worklet to execute
   * @param _hashCode: identifier for locating this worklet among all local worklets
   */
  private void executeWorklet(Worklet _worklet, String _hashCode) {
      //   System.out.println("WVM: executeWorklet");
    Worklet _executingWorklet = _worklet;
    _executingWorklet._hashCode = _hashCode;
    // (new Thread(_executingWorklet, _hashCode)).start();
    _executingWorklet.execute();
  }

  /**
   * Returns the hostname and port of the transporter
   *
   * @return hostname and port of the transporter
   */
  public String toString() {
    return (transporter.toString());
  }

  /**
   * Checks to see security responsibility of the WVM
   *
   * @return true if WVM is secure
   */
  final boolean isSecure(){
    return transporter.isSecure();
  }

  // WVM Registration and Lookup ///////////////////////////////////////////////
  /**
   * Initializes the WVM registration related features.
   *
   * @return success of WVM Registration initialization and creation
   */
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
      _reg = new Registration(ip, portt, local_portt, "", getWVMName(), getWVMAddr(), getWVMPort());
    }	catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * dp2041: Registers ....something
   *
   * @return something...
   */
  public boolean Register(){
    return _reg.Register();
  }

  /**
   * dp2041: Unregisters ....something
   *
   * @return somthing...
   */
  public boolean Unregister(){
    return _reg.Unregister(false);
  }

  /**
   * dp2041: Unregisters the WVM.
   *
   * @return success of unregistration.
   */
  public boolean Unregister(boolean dying){
    WVM.out.println("Calling unregistration");
    return _reg.Unregister(dying);
  }
  // END: WVM Registration and Lookup //////////////////////////////////////////

  /**
   * Gets the transporter's available plain port
   *
   * @return transporter's available plain port
   */
  public int getWVMPort() { 
      // System.out.println("WVM: getWVMPort");
      return (transporter._port); }

  /**
   * Gets the name associated with the RMI server
   *
   * @return name associated with the RMI server
   */
    public String getWVMName() { // System.out.println("WVM: getWVMName");
  return (transporter._name); }

  /**
   * Gets the hostname or ip address
   *
   * @return hostname or ip address
   */
  public String getWVMAddr() {
      // System.out.println("WVM: getWVMAddr");
      return (transporter._host); }

  /**
   * Gets the port associated with the RMI server
   *
   * @return port associated with the RMI server
   */
  public int getRMIPort() {
      // System.out.println("WVM: getRMIPort");
      return (((WVM_RMI_Transporter) transporter)._port); }

  // Client-side ///////////////////////////////////////////////////////////////
  /**
   * Client side: Pings a remote WVM
   *
   * @param wvmURL: a URL that represents a wvm location in the form
   * of : remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional
   * @return success of ping
   */
  public boolean ping(String wvmURL) {
      //  System.out.println("WVM: ping");
    return (transporter.ping(wvmURL));
  }
  /**
   * Client side: Sends a message to a remote WVM
   *
   * @param messageKey: type of message that you are sending, defined
   * in {@link WVM_Transporter}
   * @param message: actual message to send
   * @param wvmURL: a URL that represents a wvm location in the form
   * of : remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional
   * @return success of the send attempt
   */
  public boolean sendMessage(Object messageKey, Object message, String wvmURL) {
      //  System.out.println("WVM: sendMessage");
    return (transporter.sendMessage(messageKey, message, wvmURL));
  }
  /**
   * Client side: Requests to get a message from the remote WVM
   *
   * @param messageKey: type of message being requested, defined
   * in {@link WVM_Transporter}
   * @param wvmURL: a URL that represents a wvm location in the form
   * of : remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional.
   * @return message that was received
   */
  public Object getMessage(Object messageKey, String wvmURL) {
      //  System.out.println("WVM: getMessage");
    return (transporter.getMessage(messageKey, wvmURL));
  }
  // END: Client-side //////////////////////////////////////////////////////////

  // Server-side ///////////////////////////////////////////////////////////////
  /**
   * Thread object whose run method is called to handle requests
   */
  public Runnable requestHandler = null;
  /**
   * Container that holds the received message keys
   */
  public final Hashtable messageQueueKeys = new Hashtable();
  /**
   * Container that holds the received messages
   */
  public final Hashtable messageQueueMsgs = new Hashtable();

  /**
   * Server side: Receives and handles the messages
   *
   * @param messageKey: Type of message that is being received, defined
   * in {@link WVM_Transporter}
   * @param message: actual message being received
   * @return true...always?(dp2041)
   */
  boolean receiveMessage(Object messageKey, Object message) {
      //  System.out.println("WVM: receiveMessage");
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

  /**
   * Server side: Handles message requests from clients
   *
   * @param messageKey: type of message being requested, defined
   * in {@link WVM_Transporter}
   * @return message that was received earlier of type <code>messageKey</code>
   */
  Object requestMessage(Object messageKey) {
      //  System.out.println("WVM: requestMessage");
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

  /**
   * Handles creation of WVM at the command prompt
   *
   * @param args[]: arguments passed from command line invocation
   */
  public static void main(String args[]){
    OptionsParser op = new OptionsParser("psl.worklets.WVM");
    if (op.ParseOptions(args) != 1)
      new WVM(new Object(), null, op.name, op.port,
	      op.keysfile, op.password, op.ctx, op.kmf, op.ks, op.rng,
	      op.securityLevel);
  }

  // Management of WorkletJunctions ////////////////////////////////////////////

  /**
   * Keeps track of WVM Stats related to WorkletJunctions
   */
  private WVM_Stats _myStats = new WVM_Stats();

  /**
   * Registers a junction to the WVM
   *
   * @param wj: {@link WorkletJunction} to register
   */
  void registerJunction(WorkletJunction wj) {
      //  System.out.println("WVM: registerJunction " + wj.id());
    _installedJunctions.put(wj.id(), wj);
    _myStats.log("Added Junction: ", wj);
    _myStats.accept();
  }
    void regJunctions(String wid,Vector wjv) {
	// System.out.println("WVM: registerJunction " + wid);
    _junctions.put(wid, wjv);
    //  _myStats.log("Added Junction: ", wj);
    // _myStats.accept();
  }
  /**
   * Gets a reference to a certain worklet junction
   *
   * @param id: {@link WorkletID} of the requested {@link WorkletJunction}(s)
   * @return Vector of {@link WorkletJunction}(s) associated with
   * the {@link WorkletID}
   */
  public Vector getJunctions(String id) {
      //  System.out.println("WVM: getJunctions " + id);
    return (Vector)_installedJunctions.get(id);
  }
    public Vector getRegJunctions(String id) {
	// System.out.println("WVM: getJunctions " + id);
    return (Vector)_junctions.get(id);
  }
  /**
   * Removes a {@link WorkletJunction} from the WVM stats
   *
   * @param wj: {@link WorkletJunction} to remove
   * @return success of removal
   */
  boolean removeJunction(WorkletJunction wj) {
      //  System.out.println("WVM: removeJunction");
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

  /**
   * Removes {@link WorkletJunction}(s) from the WVM stats
   *
   * @param wj_id: {@link WorketID} of the
   * {@link WorkletJunction}(s) to remove
   * @return success of removal
   */
  boolean removeJunction(String wj_id) {
      //  System.out.println("WVM: removeJunction");
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

  /** Prints the stats of the WVM */
  public void printStats(){
    _myStats.printStats();
  }

  /**
   * Keeps track of the WVM stats, currently only keeps basic stats on
   * WorkletJunctions. <br>
   * <p>
   * Currently only keeps these stats:
   * <ul>
   * <li>log of junction accepted and removed</li>
   * <li>number of total junctions accepted and removed</li>
   * </ul>
   * </p>
   * To add Worklets capability add a log() function specific for
   * Worklets, add a printCurrentWorklets similar to the
   * printCurrentJunctions, and add the Worklet specific info to the
   * summary.  You might also want to keep the accepted and removed
   * number for worklets and worklet junctions separate.
   */
  /* for internal:
     NOTES:
     - The reasoning behind just incrementing the number rather than
     actually accepting the WorkletJunction object is to keep the
     WVM_Stats general (not specific to the WorkletJunction, even though
     that's the only stats it keeps at this point).  Think of
     the accepted/removed as a parity counter.
    */
  private class WVM_Stats {
    /**
     * Keeps a log of certain events
     */
    private Vector _wvmLog = new Vector();

    /**
     * Total number of objects accepted
     */
    private int _accepted;

    /**
     * Total number of objects removed
     */
    private int _removed;

    /**
     * Creates an object to keep track of WVM statistics
     */
    public WVM_Stats(){}

    /**
     * Adds generic message to log
     *
     * @param message: message to add
     */
    public void log(String message){
      _wvmLog.addElement(new Date() + ": " + message);
    }

    /**
     * Adds {@link WorkletJunction} specific message to the log.
     *
     * @param message: message to add
     * @param wj: related {@link WorkletJunction}
     */
    public void log(String message, WorkletJunction wj){
      if (wj._junctionPlanner != null)
	_wvmLog.addElement(new Date() + ": " + message + wj + "." + wj.id() + ": " + wj.sstate());
      else
	_wvmLog.addElement(new Date() + ": " + message + wj + "." + wj.id());
    }

    /** Prints some stats about the WVM */
    public void printStats(){
      WVM.out.println("Printing WVM stats (" + new Date() + ")");
      WVM.out.println();

      printLog();
      WVM.out.println();

      printCurrentJunctions();
      WVM.out.println();

      printSummary();
      WVM.out.println();
    }

    /** Prints the current log */
    private void printLog(){
      Iterator itr = _wvmLog.iterator();
      WVM.out.println("WVM log:");
      while (itr.hasNext())
        WVM.out.println("" + itr.next());
    }

    /** Prints the current {@link WorkletJunction}(s) in the WVM */
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

    /** Print the summary of total accepted/removed {@link WorkletJunction}(s) */
    private void printSummary(){
      WVM.out.println("Total accepted: " + _accepted);
      WVM.out.println("Total removed: " + _removed);

      // WorkletJunction specific summary
      WVM.out.println("Total of " + _installedJunctions.size() + " WorkletID's and "
                         + _installedJunctions.sizeValues() + " WorkletJunction(s)");
    }

    /** Inserts an object into the stats */
    public void accept(){
      _accepted++;
    }

    /** Removes an object from the stats */
    public void remove(){
      _removed++;
    }

    /** Clears the WVM stats log */
    public void clearLog(){
      _wvmLog.clear();
    }
  }
}
// END: Management of WorkletJunctions ///////////////////////////////////////
