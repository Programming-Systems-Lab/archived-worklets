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
  // addressing info
  protected String _name;
  // addressing info
  protected int _port;
  protected JunctionPlanner _junctionPlanner;
  protected WorkletID _id;
  private boolean _dropOff = false;            // tells the Worklet whether it needs to wait for the WorkletJunction to execute.

  protected transient Object _system;
  protected transient WVM _wvm;

  protected URL _originClassServer = null; // THIS MUST BE SET @ CREATION TIME!
  protected Worklet _worklet;
  protected WorkletJunction _originJunction;
  protected Hashtable _payload;

  int _priority;
  int _state = 0; // this is left here for backwards compatibility with WJackCondition

  // ------------------------------------------------------------------------------------ //
  // ---------------------------- SET OF CONSTRUCTORS ----------------------------------- //
  // ------------------------------------------------------------------------------------ //
  // This first one is the general ctor used by the others.
  public WorkletJunction(String host, String name, int port, boolean dropOff, WorkletID id, JunctionPlanner jp) {
    _host = host;
    _name = name;
    _port = port;
    _payload = new Hashtable();
    _dropOff = dropOff;

    _id = id;
    if (_id != null)
      _id.init(this);  // this gives the workletID the reference to this junction.

    _junctionPlanner = jp;
    if (_junctionPlanner != null)
      _junctionPlanner.init(this);  // this gives the junction planner the reference to this junction.
  }

  // --- These ctors NOT associated with the JunctionPlanner --- //
  public WorkletJunction(String host, String name) {
    this(host, name, WVM_Host.PORT, false, new WorkletID("default"), null);
  }

  public WorkletJunction(String host, int port) {
    this(host, "WVM", port, false, new WorkletID("default"), null);
  }

  public WorkletJunction(String host, String name, int port) {
    this(host, name, port, false, new WorkletID("default"), null);
  }

  public WorkletJunction(String host, String name, int port, boolean dropoff) {
    this(host, name, port, dropoff, new WorkletID("default"), null);
  }

  public WorkletJunction(String host, String name, int port, WorkletID id) {
    this(host, name, port, false, id, null);
  }

  public WorkletJunction(String host, String name, int port, boolean dropoff, WorkletID id) {
    this(host, name, port, dropoff, id, null);
  }

  // --- These ctors associated with the JunctionPlanner --- //
  public WorkletJunction(String host, String name, int port, JunctionPlanner jp) {
    this(host, name, port, false, new WorkletID("default"), jp);
  }

  public WorkletJunction(String host, String name, int port, boolean dropoff, JunctionPlanner jp) {
    this(host, name, port, dropoff, new WorkletID("default"), jp);
  }

  public WorkletJunction(String host, String name, int port, WorkletID id, JunctionPlanner jp) {
    this(host, name, port, false, id, jp);
  }

  // ------------------------------------------------------------------------------------ //



  // -------------------------------------------------------------------------------- //
  // -------------- Functions to change/modify WorkletJunction inards --------------- //
  // -------------------------------------------------------------------------------- //

  final void setOriginWorkletJunction(WorkletJunction origin) {
    _originJunction = origin;
  }

  final void sysInit(Object system, WVM wvm) {
    init(_system = system, _wvm = wvm);
  }

  // I should somehow check that whoever is changing my state has "permission"
  final void setState(int state){
    System.out.println("Changing STATE: " + _junctionPlanner.state() + " -> " + state);
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

  public final void dropOff(boolean dropOff){
    _dropOff = dropOff;
  }

  public final boolean dropOff(){
    return _dropOff;
  }
}
