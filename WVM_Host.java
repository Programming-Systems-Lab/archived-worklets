/*
 * @(#)WVM_Host.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;

import java.rmi.*;
import java.util.*;
import java.net.URL;

/** Interface of the {@link WVM}'s RMI server (the RTU) */
public interface WVM_Host extends java.rmi.Remote {
  /** default port for the {@link WVM_Registry} */
  public final static int PORT = 9100;

  /**
   * Receives and installs a {@link Worklet}
   *
   * @param _worklet: {@link Worklet} to receive
   * @throws RemoteException if {@link Worklet} could no t be received
   */
  public void receiveWorklet(Worklet _worklet) throws RemoteException;
    public void receiveWorkletId(String id,String codeBase) throws RemoteException;
    public boolean receiveByteCode(String wid,String name, String u,byte []bc ) throws RemoteException;

  /**
   * Attemps to rejoin the {@link WVM_Registry}
   *
   * @throws RemoteException if RTU could not rejoin the {@link WVM_Registry}
   * @return {@link Date} of creation for the WVM_Host
   */
  public Date rejoinRegistry() throws RemoteException;

  /**
   * makes this WVM_Host create a local RMI <code>Registry</code>
   *
   * @throws RemoteException if <code>Registry</code> could not be created
   */
  public void createRegistry() throws RemoteException;

  /**
   * Pings a remote {@link WVM}
   *
   * @return success of ping
   * @throws RemoteException if ping could not be completed
   */
  public boolean ping() throws RemoteException;
  /**
   * Receives and handles the messages
   *
   * @param messageKey: Type of message that is being received, defined
   * in {@link WVM_Transporter}.
   * @param message: actual message being received
   * @return success of message reception
   * @throws RemoteException if there was a problem with receiving the message
   */
  public boolean receiveMessage(Object _messageKey, Object _message) throws RemoteException;
  /**
   * Handles message requests from clients
   *
   * @param messageKey: type of message being requested, defined
   * in {@link WVM_Transporter}
   * @return message that was received earlier of type <code>messageKey</code>
   * @throws RemoteException if there was a problem with the message request
   */
  public Object requestMessage(Object _messageKey) throws RemoteException;

  /**
   * Checks to see security of the WVM_Host
   *
   * @return true if WVM_Host is secure
   * @throws RemoteException if the security could not be checked
   */
  public boolean isSecure() throws RemoteException;
}
