/*
 * @(#)WVM_Registry.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2002: @author Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;

import java.rmi.server.*;
import java.rmi.registry.*;
import java.rmi.*;
import java.util.*;

/**
 * The Worklets implemented RMI <code>Registry</code>
 *
 * We subclass the Sun implementation of the registry to add
 * registration features to the bind, rebind, and unbind methods, and
 * so that we can restrict who can rebind the names.
 */
class WVM_Registry extends sun.rmi.registry.RegistryImpl{

  /** Collection of WVM_URL/registrationKey values */
  private Map _regKeys;

  /** Collection of name/WVM_URL values */
  // we need to keep a name/WVM_URL association list
  // so that when a server unbinds, we can remove the
  // value up from the name
  private Map _names;

  /**
   * Creates a WVM_Registry on the given port
   *
   * @param port: Socket number to create the registry on
   * @throws RemoteException if the WVM_Registry could not be created
   */
  WVM_Registry(int port) throws RemoteException {
    super(port);
    _regKeys = Collections.synchronizedMap(new HashMap());
    _names = Collections.synchronizedMap(new HashMap());
    WVM.out.println("WVM RMI Registry running on port: " + port);
  }

  /**
   * Gets the available registration keys
   *
   * @return the collection of WVM_URL/registrationKey values.
   */
  Map getRegistrationKeys(){
    return _regKeys;
  }

  /**
   * Register an object with the RMI bound name
   *
   * @param name: RMI name that the object is binding with
   * @param obj: <code>Remote</code> object to register
   * @throws RemoteException if the name/obj cannot be bound
   */
  private void _registerHost(String name, Remote obj) throws RemoteException{
    _regKeys.put( ((RTU_Registrar) obj).getWVM_URL(),
		  ((RTU_Registrar) obj).getRegistrationKey());
    _names.put(name, ((RTU_Registrar) obj).getWVM_URL());
  }

  /**
   * Remove the RMI bound name from the registration.
   *
   * @param name: RMI name to remove
   */
  private void _removeHost(String name) {
    _regKeys.remove(name);
    _regKeys.remove(_names.get(name));
  }

  /**
   * Binds the <code>Remote</code> object to the given name
   *
   * @param name: name to bind with
   * @param obj: <code>Remote</code> object associated with the name
   * @throws AlreadyBoundException if the name is already used
   * @throws RemoteException if the object cannot be bound
   */
  public void bind(String name, Remote obj) throws AlreadyBoundException, RemoteException
  {
    try {
      Remote server = ((RTU_Registrar) obj).getServer();
      super.bind(name, server);
      _registerHost(name, obj);
    } catch (ClassCastException e) {
      super.bind(name, obj);
    }
  }

  /**
   * Rebinds the <code>Remote</code> object to the given name if the binding object is
   * a {@link WVM_RMI_Transporter}
   *
   * @param name: the name to rebind with
   * @param obj: the <code>Remote</code> object associated with the name
   * @throws RemoteException if the object cannot be bound
   * @throws AccessException if the object does not have permission to rebind with the name
   */
  public void rebind(String name, Remote obj) throws RemoteException, AccessException
  {
    String objName = "" + obj.getClass();
    Remote server = obj;

    if (objName.matches(".*RTU_Registrar.*")) {
      server = ((RTU_Registrar) obj).getServer();
      objName = "" + server.getClass();
    }

    if (objName.matches(".*psl[.]worklets[.]WVM_RMI_Transporter.*")) {
      try {
	super.rebind(name, server);
	_registerHost(name, obj);
      } catch (ClassCastException e) {
	super.rebind(name, obj);
      }
    }
    else
      throw new AccessException("WVM_Registry Error: You do not have access to rebind to this name: " + name);
  }

  /**
   * Unbinds the name from the registry.
   *
   * @param name: name to unbind
   * @throws RemoteException if the name could not be unbound
   * @throws NotBoundException if the name is not bound in the registry
   */
  public void unbind(String name) throws RemoteException, NotBoundException
  {
    super.unbind(name);
    _removeHost(name);
  }

  /**
   * Creates an external registry from a command line
   *
   * @param args[0]: the port to create the {@link WVM_Registry} on
   */
  public static void main (String args[]){
    int port = WVM_Host.PORT;
    if (args.length > 0)
      port = Integer.parseInt(args[0]);

    try {
      new WVM_Registry(port);
      new Thread() {
	  public void run() {
	    while (true){
		try {
		  sleep(1000);
		} catch (InterruptedException e) { }
	    }
	  }
	}.start();
    } catch (Exception e){
    }
  }
}
