package psl.worklets;
  /**
   * This class acts as an intermediary between the registry and the RTU
   * for RMI server registration purposes
   */ 

import java.rmi.*;
import java.rmi.server.*;
import java.security.*;
import java.util.*;
import java.net.MalformedURLException;

class RTU_RegistrarImpl extends UnicastRemoteObject implements RTU_Registrar{
  private final String _name;
  private final String _host;
  private final int _port;
  private final String _registrationKey;
  private final String _WVM_URL;
  private WVM_RMI_Transporter.RTU _rtu;	// the RTU that this registrar will handle

  RTU_RegistrarImpl(String name, String host, int port, String rng, Date seed) throws RemoteException{
    super(0);
    _name = name;
    _host = host;
    _port = port;

    SecureRandom r = null;
    if (rng == null) rng = "";
    try {
      r = SecureRandom.getInstance(rng);
    } catch (NoSuchAlgorithmException nsae){
      // WVM.err.println("Exception try to get instance of: " + rng + ":" + nsae);
      r = new SecureRandom();
      // WVM.out.println("Default to highest-priority installed provider: " + r.getProvider());
    }
    r.setSeed(seed.getTime());
    _registrationKey = "" + r.nextLong();
    _WVM_URL = _name + "@" + _host + ":" + _port;
  }
    
  void bind(String rmibind, WVM_RMI_Transporter.RTU rtu) throws RemoteException, AlreadyBoundException, MalformedURLException{
    _rtu = rtu;
    Naming.bind(rmibind, this);
  }
    
  void rebind(String rmibind, WVM_RMI_Transporter.RTU rtu) throws RemoteException, MalformedURLException{
    _rtu = rtu;
    Naming.rebind(rmibind, this);
  }
    
  public Remote getServer() throws RemoteException{
    return _rtu;
  }
    
  public String getRegistrationKey() throws RemoteException{
    return _registrationKey;
  }
    
  public String getWVM_URL() throws RemoteException{
    return _WVM_URL;
  }
}
  
