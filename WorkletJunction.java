package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *  
 * Copyright (c) 2001: @author Gaurav S. Kc 
 * 
*/

import java.io.*;
import java.util.*;

public abstract class WorkletJunction implements Serializable {

  protected String _host;
  protected String _name;
  protected int _port;

  protected Object _system;
  protected WVM _wvm;

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

  void setOriginWorkletJunction(WorkletJunction origin) {
    _originJunction = origin;
  }

  protected void init(Object system, WVM wvm) {
    _system = system;
    _wvm = wvm;
  }

  void run() {
    while (_state == WorkletJacket.STATE_READY) {
      try {
        synchronized (this) {
          Thread.currentThread().wait();
          // Thread.currentThread().sleep(500);
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

  protected abstract void execute();
  // Sub-classes must provide specific implementations

  String getHost() {
    return (_host);
  }

  String getName() {
    return (_name);
  }

  int getPort() {
    return (_port);
  }

  public String toString() {
    return (_name + "@" + _host + ":" + _port);
  }

}
