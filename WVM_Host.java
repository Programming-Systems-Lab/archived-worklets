package psl.worklets;


/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *  
 * Copyright (c) 2001: @author Gaurav S. Kc 
 * 
*/

import java.rmi.*;
import java.util.*;

public interface WVM_Host extends java.rmi.Remote {
  public final static int PORT = 9100;
  public void receiveWorklet(Worklet _worklet) throws RemoteException;
  public Date rejoinRegistry() throws RemoteException;
  // returns the Date of creation for this WVM_Host

  public void createRegistry() throws RemoteException;
  // makes this WVM_Host create a local RMI Registry
  
  public boolean ping() throws RemoteException;
  // used to, well, ping the remote WVM

  public boolean receiveMessage(Object _messageKey, Object _message) throws RemoteException;
  public Object requestMessage(Object _messageKey) throws RemoteException;

  public boolean isSecure() throws RemoteException;
}
