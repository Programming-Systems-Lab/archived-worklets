package psl.worklets;

import java.rmi.server.*;
import java.rmi.registry.*;
import java.rmi.*;
import java.util.*;

// The WVM Registry:
// We subclass the sun implementation of the registry to add
// registration features to the bind, rebind, and unbind methods, and
// restrict who can rebind the names.

class WVM_Registry extends sun.rmi.registry.RegistryImpl{
  private Map _regKeys;  // key/value = WVM_URL/registrationKey
  private Map _names;    // key/value = name/WVM_URL

  WVM_Registry(int port) throws RemoteException {
    super(port);
    _regKeys = Collections.synchronizedMap(new HashMap());
    _names = Collections.synchronizedMap(new HashMap());
    WVM.out.println("WVM RMI Registry running");
  }

  Map getRegistrationKeys(){
    return _regKeys;
  }

  private void _registerHost(String name, Remote obj) throws RemoteException{
    _regKeys.put( ((RTU_Registrar) obj).getWVM_URL(), 
		  ((RTU_Registrar) obj).getRegistrationKey());
    _names.put(name, ((RTU_Registrar) obj).getWVM_URL());
  }

  private void _removeHost(String name) {
    _regKeys.remove(name);
    _regKeys.remove(_names.get(name));
  }

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

  public void unbind(String name) throws RemoteException, NotBoundException
  {
    super.unbind(name);
    _removeHost(name);
  }

  public static void main (String args[]){
    int port = 0;
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
