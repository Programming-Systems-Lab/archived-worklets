/*
 * @(#)WorkletJunction.java
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

/**
 * Execution nodes of the {@link Worklet}.  Each WorkletJunction
 * contains all the addressing information needed to reach its target
 * system.
 */
public abstract class WorkletJunction implements Serializable, Runnable {

  // addressing info
  /** hostname of target system */
  protected String _host;
  /** RMI port of target system */
  protected int _rmiport = -1;
  /** RMI bound name of target system */
  protected String _name;
  /** Socket port number of target system   */
  protected int _port;
    
    String appName = null;

  /** associated meta-information */
  protected JunctionPlanner _junctionPlanner;
  /** associated identification */
  protected WorkletID _id;
    protected String wid;

  /** tells the {@link Worklet} whether it needs to wait for the WorkletJunction to execute */
  private boolean _dropOff = false;

  // security related parameters
  /** default to "parent" security for Worklet transport methods*/
  private boolean _defaultSecurity = true;
  /** security of WorkletJunction*/
  private boolean _isSecure = false;
  /** the transport methods the WorkletJunction tries */
  private String[] _transportMethods = null;
  /** default transport methods for secure WorkletJunctions */
  static String[] secureDefault = {"secureRMI", "secureSocket"};
  /** default transport methods for plain WorkletJunctions */
  static String[] plainDefault = {"plainRMI", "plainSocket"};

  /** reference to the local system's host adaptor */
  protected transient Object _system;
  /** reference to the local system's {@link WVM} */
  protected transient WVM _wvm;

  /** <code>URL</code> of the class server at the origin.  MUST BE SET @ CREATION TIME! */
  protected URL _originClassServer = null;
  /** The parent {@link Worklet} that this junction is associated with */
  protected Worklet _worklet;
  /** reference to the WorkletJunction holding the addressing info of the origin */
  protected WorkletJunction _originJunction;
  /** Collected "data" of WorkletJunction */
  protected Hashtable _payload;

  /** highset possible priority level of WorkletJunction */
  public static final int SUPER_PRIORITY = 9;
  /** high priority level of WorkletJunction */
  public static final int HIGH_PRIORITY  = 7;
  /** normal priority level of WorkletJunction */
  public static final int MED_PRIORITY   = 5;
  /** lowest possible priority level of WorkletJunction */
  public static final int LOW_PRIORITY   = 1;

  /** current priority of the WorkletJunction */
  private int _priority = MED_PRIORITY;
  /** left here for backwards compatibility with WJackCondition */
  int _state = 0;

    //index into worklet's hash table where the byte array is
    public String index = null;

  // ---------------------------- Set of Constructors ----------------------------------- //
  /**
   * The complete constructor, creates a WorkletJunction with the
   * given parameters
   * <br>
   * defaults: <br>
   * rmiport=WVM_Host.PORT <br>
   * dropOff=false <br>
   * id="default" <br>
   * jp=null <br>
   *
   * @param host: hostname of target system
   * @param name: RMI bound name of target system
   * @param rmiport: RMI port of target system
   * @param port: port number of socket to try to send through
   * @param dropOff: designates whether the {@link Worklet} needs to wait for the WorkletJunction.
   * @param jp; associated {@link JunctionPlanner} that provides meta-information
   */
  public WorkletJunction(String host, String name, int rmiport, int port,
			 boolean dropOff, String id, JunctionPlanner jp) {
    _host = host;
    _name = name;
    _port = port;
    _payload = new Hashtable();
    _dropOff = dropOff;
    
    if(index == null)
	index =  new String((new Long(new Date().getTime())).toString());

    if (rmiport == -1) {
      try {
	_rmiport = Integer.parseInt(System.getProperty("WVM_RMI_PORT"));
      } catch (NumberFormatException nfe) {
	_rmiport = WVM_Host.PORT;
      }
    } else {
      _rmiport = rmiport;
    }

    wid = id;
    if (_id != null)
      _id.init(this);  // this gives the workletID the reference to this junction.

    _junctionPlanner = jp;
    if (_junctionPlanner != null)
      _junctionPlanner.init(this);  // this gives the junction planner the reference to this junction.
  }

  /**
   * Creates a WorkletJunction with the given parameters.
   *
   * @param host: hostname of target system
   * @param name: RMI bound name of target system
   * @param port: port number of socket to try to send through
   */
  public WorkletJunction(String host, String name, int port) {
    this(host, name, WVM_Host.PORT, port, false, new String("default"), null);
  }
   
    public WorkletJunction(String host, String name, int port, URL u) {
	this(host, name, WVM_Host.PORT, port, false, new String("default"), null);
	_originClassServer = u;
	
    } 

    // return this junction as byte array for serialized transportation
    // and storage
    public byte[] getBytes(){
	try{
	    ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
	    ObjectOutputStream ooStream = new ObjectOutputStream(baoStream);
	    
	    //create byte array out of actual junction to use first
	    ooStream.writeObject(this);
	    byte []wjbytes = baoStream.toByteArray();
	    baoStream.close();
	    return wjbytes;
	} catch (IOException ioe){
	    ioe.printStackTrace();
	    return null;
	}
    }

    public String getIndex(){
	return index;
    }


  /** Entry point function for Threads */
  synchronized final public void run() {
    // register the workletjunction in the WVM.
      
      _wvm.registerJunction(this);

    if (_junctionPlanner == null) {
      execute(); 
    } else {
      _junctionPlanner.start();
    }
    // remove the worket junction from the WVM.
    
    _wvm.removeJunction(this.id());
  }

  /** Sub-classes must provide specific implementations */
  protected abstract void init(Object system, WVM wvm);

  /** Sub-classes must provide specific implementations */
  protected abstract void execute();

  // -------------- Functions to change/modify WorkletJunction inards --------------- //
  /**
   * Set the origin point to be at the given WorkletJunction
   *
   * @param origin: WorkletJunction holding addressing info of the origin
   */
  final void setOriginWorkletJunction(WorkletJunction origin) {
    _originJunction = origin;
  }

  /**
   * Initialize the WorkletJunction.  Must be called if the
   * WorkletJunction is to access the local system.
   *
   * @param system: local system
   * @param wvm: local {@link WVM}
   */
  final void sysInit(Object system, WVM wvm) {
    init(_system = system, _wvm = wvm);
  }

  /**
   * Sets the state of the WorkletJunction
   *
   * @param state: new state
   */
  final void setState(int state){
    if (_junctionPlanner != null) {
      WVM.out.println("Changing STATE: " + _junctionPlanner.state() + " -> " + state);
      _junctionPlanner.setState(state);
    } else
      new Exception().printStackTrace();
      WVM.err.println("Error, no JunctionPlanner so there is no state information");
  }

  /**
   * Gets the priority level
   *
   * @return priority level
   */
  final int getPriority() {
    return _priority;
  }

  /**
   * Sets the priority level
   *
   * @param priority: level of priority
   */
  final void setPriority(int priority) {
    _priority = priority;
  }

  /**
   * Get the current state of the WorkletJunction
   *
   * @return current state
   */
  final int state() {
    if (_junctionPlanner != null) return _junctionPlanner.state();
    else {
      WVM.err.println("Error, no JunctionPlanner so there is no state information");
      return -1;
    }
  }

  /**
   * Get the current state of the WorkletJunction in String format
   *
   * @return current state
   */
  final String sstate() {
    if (_junctionPlanner != null) return _junctionPlanner.sstate();
    else {
      WVM.err.println("Error, no JunctionPlanner so there is no state information");
      return "";
    }
  }

  /** @return target hostname */
  final String getHost() {
    return (_host);
  }

  /** @return target RMI port number */
  final int getRMIPort() {
    return (_rmiport);
  }

  /** @return target RMI server name */
  final String getName() {
    return (_name);
  }

  /** @return target socket number */
  final int getPort() {
    return (_port);
  }

  /** @return name@host:port */
  public String toString() {
    return (_name + "@" + _host + ":" + _port);
  }

  /** @return {@link WorkletID} */
  final String id() {
    return wid;
  }

  /**
   * Sets the value of dropOff, the parameter informing the
   * {@link Worklet} that this WorkletJunction does not have to
   * be waited upon
   *
   * @param dropOff new value of dropOff
   */
  public final void dropOff(boolean dropOff){
    _dropOff = dropOff;
  }

  /**
   * Gets the value of dropOff, the parameter informing the
   * {@link Worklet} that this WorkletJunction does not have to
   * be waited upon
   *
   * @return value of dropOff
   */
  public final boolean dropOff(){
    return _dropOff;
  }

  /**
   * Sets the security of this WorkletJunction
   *
   * @param new security of WorkletJunction
   */
  public final void isSecure(boolean isSecure){
    _defaultSecurity = false;
    _isSecure = isSecure;
  }

  /**
   * Gets the security of this WorkletJunction
   *
   * @return security of WorkletJunction
   */
  public final boolean isSecure(){
    if (_defaultSecurity)
      return _worklet.isSecure();
    else
      return _isSecure;
  }

  /**
   * Sets the transport methods to try.  Possible values are:
   * plainSocket, secureSocket, plainRMI and secureRMI.
   *
   * @param tm: array of methods
   */
  public final void setTransportMethods(String[] tm){
    _transportMethods = tm;
  }

  /**
   * Sets the currently set transport methods
   *
   * @return current transport methods
   */
  public final String[] getTransportMethods(){
    if (_transportMethods == null){
      if (isSecure())
	_transportMethods = secureDefault;
      else
	_transportMethods = plainDefault;
    }

    return _transportMethods;
  }

  // -------------- Functions to change modify WorkletJunction status --------------- //

  // The following methods are almost all completely transparent, in that it just calls
  // the JunctionPlanner's methods.
  // --------------------------------------------------------------------------------- //

  /** Wake up a suspended WorkletJunction */
  public final void wakeUp() {
    _junctionPlanner.wakeUp();
  }

  /** Cancel a WorkletJunction after current iteration */
  public final void cancel() {
    _junctionPlanner.cancel();
  }

  /**
   * Add iterations to the executing WorkletJunction
   *
   * @param iterations: number to add
   */

  final void addIterations(int iterations){
    _junctionPlanner.setIterations(_junctionPlanner.iterations() + iterations);
  }

  /**
   * Delete iterations to the executing WorkletJunction
   *
   * @param iterations: number to delete
   */
  final void delIterations(int iterations){
    if (_junctionPlanner.iterations() - iterations > 0){
      _junctionPlanner.setIterations(_junctionPlanner.iterations() - iterations);

    } else {
      _junctionPlanner.setIterations(0);
    }
  }

  /**
   * Sets the number of iterations for the WorkletJunction
   *
   * @param iterations: new iteration number
   */
  final void setIterations(int iterations){
    _junctionPlanner.setIterations(iterations);
  }

  /**
   * Gets the number of iterations for the WorkletJunction
   *
   * @param number of iterations
   */
  final int iterations(){
    return _junctionPlanner.iterations();
  }

  /**
   * Sets the interval bewteen iterations
   *
   * @param interval: new inter-iteration delay
   */
  final void setInterval(long interval){
    _junctionPlanner.setInterval(interval);
  }

  /**
   * Gets the interval bewteen iterations
   *
   * @return interval: inter-iteration delay
   */
  final long interval(){
    return _junctionPlanner.interval();
  }

  /**
   * Sets the <code>Date</code> of execution
   *
   * @param date: <code>Date</code> of execution
   */
  final void setDate(Date date){
    _junctionPlanner.setDate(date);
  }

  /**
   * Gets the current <code>Date</code> of execution
   *
   * @return <code>Date</code> of execution
   */
  final Date date(){
    return _junctionPlanner.date();
  }
}
