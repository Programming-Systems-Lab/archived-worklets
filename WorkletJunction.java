package psl.worklets;

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

public abstract class WorkletJunction implements Runnable, Serializable {

  // addressing info
  protected String _host;
  // addressing info
  protected String _name;
  // addressing info
  protected int _port;

  protected transient Object _system;
  protected transient WVM _wvm;

  protected URL _originClassServer = null; // THIS MUST BE SET @ CREATION TIME!
  protected Worklet _worklet;
  protected WorkletJunction _originJunction;
  protected Hashtable _payload;

  int _state;

  public WorkletJunction(String host, String name) {
    this(host, name, WVM_Host.PORT);
  }

  public WorkletJunction(String host, int port) {
    this(host, "WVM", port);
  }

  public WorkletJunction(String host, String name, int port) {
    _host = host;
    _name = name;
    _port = port;
    _state = WorkletJacket.STATE_READY;
    _payload = new Hashtable();
  }

  final void setOriginWorkletJunction(WorkletJunction origin) {
    _originJunction = origin;
  }

  final void sysInit(Object system, WVM wvm) {
    init(_system = system, _wvm = wvm);
  }

  final public void run() {
    while (_state == WorkletJacket.STATE_READY) {
      try {
        synchronized (this) {
          Thread.currentThread().wait();
          // Thread.currentThread().sleep(500);
          execute();
        }
      } catch (InterruptedException e) { }
    }

    while (_state != WorkletJacket.STATE_TERMINATED) {
      if (_state == WorkletJacket.STATE_WAITING) {
        // wait until state can be changed into running
      }
      execute();
    }
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

}
