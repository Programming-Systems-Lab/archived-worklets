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

public interface RTU_Registrar extends java.rmi.Remote {
  public Remote getServer() throws RemoteException;
  public String getRegistrationKey() throws RemoteException;
  public String getWVM_URL() throws RemoteException;  

}
