/*
 * @(#)RTU_Registrar.java
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

import java.rmi.*;

/**
 * RTU (RMI Transportation Unit) Registrar is an interface between the
 * {@link WVM_Registry} and the RTU, which is the WVM's RMI server.
 */
public interface RTU_Registrar extends java.rmi.Remote {
  /**
   * Gets the RTU/RMI server
   *
   * @throws RemoteException if the server cannot be reached
   */
  public Remote getServer() throws RemoteException;
  /**
   * Gets the registration key of the RTU/RMI server
   *
   * @throws RemoteException if we cannot get the registration key
   */
  public String getRegistrationKey() throws RemoteException;
  /**
   * Gets the WVM_URL of the RTU/RMI server.  The WVM_URL is a URL
   * that represents a {@link WVM} location in the form of :
   * remote_hostname@RMI_name:remote_port where the RMI_name is
   * optional.
   *
   * @throws RemoteException if we cannot get the WVM_URL
   */
  public String getWVM_URL() throws RemoteException;

}
