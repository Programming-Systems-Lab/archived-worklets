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
 * edited by Dan Phung
 * NOTE: 
 * - i have the ctor's without the WorkletID in the general ctor because otherwise, 
 *  i'd have to pass in a new workletID with a reference to "this" (as the parent), 
 *  which can't be called before the super's ctor is called.
 *  
 *  
*/

import java.io.*;
import java.net.*;
import java.util.*;

public abstract class WorkletJunction implements Serializable, Runnable {

  // addressing info
  protected String _host;
  protected int _rmiport = -1;
  protected String _name;
  protected int _port;

  protected JunctionPlanner _junctionPlanner;
  protected WorkletID _id;
  private boolean _dropOff = false;             // tells the Worklet whether it needs to 
						// wait for the WorkletJunction to execute.

  // security related parameters
  private boolean _defaultSecurity = true;	// default to "parent" security
  private boolean _isSecure = false;		// is this junction secure?
  private String[] _transportMethods = null;	// the transport methods the Worklet Junction tries.
  static String[] secureDefault = {"secureRMI", "secureSocket"};
  static String[] plainDefault = {"plainRMI", "plainSocket"};
					        
  protected transient Object _system;
  protected transient WVM _wvm;

  protected URL _originClassServer = null; // THIS MUST BE SET @ CREATION TIME!
  protected Worklet _worklet;
  protected WorkletJunction _originJunction;
  protected Hashtable _payload;

  public static final int SUPER_PRIORITY = 9;
  public static final int HIGH_PRIORITY  = 7;
  public static final int MED_PRIORITY   = 5;
  public static final int LOW_PRIORITY   = 1;

  private int _priority = MED_PRIORITY;
  int _state = 0; // this is left here for backwards compatibility with WJackCondition

  // ---------------------------- Set of Constructors ----------------------------------- //
  public WorkletJunction(String host, String name, int rmiport, int port,
			 boolean dropOff, WorkletID id, JunctionPlanner jp) {
    _host = host;
    _name = name;
    _port = port;
    _payload = new Hashtable();
    _dropOff = dropOff;
    
    if (rmiport == -1) {
      try {
	_rmiport = Integer.parseInt(System.getProperty("WVM_RMI_PORT"));
      } catch (NumberFormatException nfe) {
	_rmiport = WVM_Host.PORT;
      }
    } else {
      _rmiport = rmiport;
    }

    _id = id;
    if (_id != null)
      _id.init(this);  // this gives the workletID the reference to this junction.

    _junctionPlanner = jp;
    if (_junctionPlanner != null)
      _junctionPlanner.init(this);  // this gives the junction planner the reference to this junction.
  }

  public WorkletJunction(String host, String name, int port) {
    this(host, name, WVM_Host.PORT, port, false, new WorkletID("default"), null);
  }


  // -------------- Functions to change/modify WorkletJunction inards --------------- //
  final void setOriginWorkletJunction(WorkletJunction origin) {
    _originJunction = origin;
  }

  final void sysInit(Object system, WVM wvm) {
    init(_system = system, _wvm = wvm);
  }

  // I should somehow check that whoever is changing my state has "permission"
  final void setState(int state){
    WVM.out.println("Changing STATE: " + _junctionPlanner.state() + " -> " + state);
    _junctionPlanner.setState(state);
  }

  final int getPriority() {
    return _priority;
  }

  final void setPriority(int priority) {
    _priority = priority;
  }

  final int state() {
    if (_junctionPlanner != null) return _junctionPlanner.state();
    else return -1;
  }

  final String sstate() {
    if (_junctionPlanner != null) return _junctionPlanner.sstate();
    else return "No JunctionPlanner";
  }

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

  protected abstract void init(Object system, WVM wvm);
  // Sub-classes must provide specific implementations

  protected abstract void execute();
  // Sub-classes must provide specific implementations

  final String getHost() {
    return (_host);
  }

  final int getRMIPort() {
    return (_rmiport);
  }

  final String getName() {
    return (_name);
  }

  final int getPort() {
    return (_port);
  }

  public String toString() {
    return (_name + "@" + _host + ":" + _port);
  }

  final WorkletID id() {
    return _id;
  }

  public final void dropOff(boolean dropOff){
    _dropOff = dropOff;
  }

  public final boolean dropOff(){
    return _dropOff;
  }

  public final void isSecure(boolean isSecure){
    _defaultSecurity = false;
    _isSecure = isSecure;
  }

  public final boolean isSecure(){
    if (_defaultSecurity)
      return _worklet.isSecure();
    else 
      return _isSecure;
  }

  public final void setTransportMethods(String[] tm){
    _transportMethods = tm;
  }

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

  final void wakeUp() {
    _junctionPlanner.wakeUp();
  }

  final void cancel() {
    _junctionPlanner.cancel();
  }

  final void addIterations(int iterations){
    _junctionPlanner.setIterations(_junctionPlanner.iterations() + iterations);
  }

  final void delIterations(int iterations){
    if (_junctionPlanner.iterations() - iterations > 0){
      _junctionPlanner.setIterations(_junctionPlanner.iterations() - iterations);

    } else {
      _junctionPlanner.setIterations(0);
    }
  }

  final void setIterations(int iterations){
    _junctionPlanner.setIterations(iterations);
  }

  final int iterations(){
    return _junctionPlanner.iterations();
  }

  final void setInterval(long interval){
    _junctionPlanner.setInterval(interval);
  }

  final long interval(){
    return _junctionPlanner.interval();
  }

  final Date date(){
    return _junctionPlanner.date();
  }

  final void setDate(Date date){
    _junctionPlanner.setDate(date);
  }
}
