/*
 * @(#)RTU_RegistrarImpl.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;

import java.rmi.*;
import java.rmi.server.*;
import java.security.*;
import java.util.*;
import java.net.MalformedURLException;

/**
 * Acts as an intermediary between the registry and the RTU for RMI
 * server registration purposes.
 */
/*
  The reasoning for the existence of this object inbetween the RTU and
  the registry is because if the RTU is secure and tries to make a
  Naming call, it will get an exception because the RTU is expecting a
  secure protocol.  This object acts as an intermediary by calling the
  Naming "binding" with itself, but in the WVM_Registry the server
  that this class contains is the server that is actually bound.
 */
class RTU_RegistrarImpl extends UnicastRemoteObject implements RTU_Registrar{
  /** RMI name of the associated RMI server */
  private final String _name;
  /** Hostname of the system with the associated RMI server */
  private final String _host;
  /** Port that the RMI server handle is on, usually the port of the {@link WVM_Registry} */
  private final int _port;
  /** Unique registration key to be associated with the RMI server */
  private final String _registrationKey;
  /** Unique WVM_URL of the RMI server */
  private final String _WVM_URL;
  /** RTU that this registrar will handle */
  private WVM_RMI_Transporter.RTU _rtu;

  /**
   * Creates a RTU_Registrar
   *
   * @param name: RMI name of the RMI server
   * @param host: hostname of the system
   * @param port: port that the RMI server handle is on, usually the port of the {@link WVM_Registry}
   * @param rng: <code>SecureRandom</code> (random number generator algorithm) to use
   * @param seed: seed for the rng
   * @throws RemoteException if RTU_Registrar cannot be created
   */
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

  /**
   * Binds the given RTU to the {@link WVM_Registry}
   *
   * @param rmibind: the RMI URL to bind, usually "rmi://" + hostname + ":" + port + "/" + RMI_name
   * @param rtu: the RMI server to bind
   * @throws RemoteException if the RTU could not be bound
   * @throws AlreadyBoundException if the given rmibind URL is already used
   * @throws MalformedURLException if the rmibind URL is malformed
   */
  void bind(String rmibind, WVM_RMI_Transporter.RTU rtu) throws RemoteException, AlreadyBoundException, MalformedURLException{
    _rtu = rtu;
    Naming.bind(rmibind, this);
  }

  /**
   * Rebinds the given RTU to the {@link WVM_Registry}
   *
   * @param rmibind: the RMI URL to rebind, usually "rmi://" + hostname + ":" + port + "/" + RMI_name
   * @param rtu: the RMI server to rebind
   * @throws RemoteException if the RTU could not be rebound
   * @throws MalformedURLException if the rmibind URL is malformed
   */
  void rebind(String rmibind, WVM_RMI_Transporter.RTU rtu) throws RemoteException, MalformedURLException{
    _rtu = rtu;
    Naming.rebind(rmibind, this);
  }

  /** Gets the RTU/RMI server */
  public Remote getServer() throws RemoteException{
    return _rtu;
  }

  /** Gets the registration key of the RTU/RMI server */
  public String getRegistrationKey() throws RemoteException{
    return _registrationKey;
  }

  /**
   * Gets the WVM_URL of the RTU/RMI server.  The WVM_URL is a URL
   * that represents a {@link WVM} location in the form of :
   * remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional.
   */
  public String getWVM_URL() throws RemoteException{
    return _WVM_URL;
  }
}

