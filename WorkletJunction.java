package psl.worklets;

import java.io.*;


/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *  
 * Copyright (c) 2001: @author Gaurav S. Kc 
 * 
*/

public abstract class WorkletJunction implements Serializable {

  protected String _host;
  protected String _name;
  protected int _port;

  protected Object _system;
  protected WVM _wvm;

  protected WorkletJunction _originJunction;

  public WorkletJunction(String host, String name) {
    this(host, name, WVM_Host.PORT);
  }

  public WorkletJunction(String host, int port) {
    this(host, "", port);
  }

  public WorkletJunction(String host, String name, int port) {
    _host = host;
    _name = name;
    _port = port;
  }

  public void setOriginWorkletJunction(WorkletJunction origin) {
    _originJunction = origin;
  }

  public void init(Object system, WVM wvm) {
    _system = system;
    _wvm = wvm;
  }

  public abstract void execute();
  // Sub-classes must provide specific implementations

  public String getHost() {
    return (_host);
  }

  public String getName() {
    return (_name);
  }

  public int getPort() {
    return (_port);
  }

  public String toString() {
    return (_name + "@" + _host + ":" + _port);
  }

}
