/*
 * @(#)effectGreeter.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * Copyright (c) 2002: @author Dan B. Phung
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import psl.worklets.*;

/**
 * effectGreeter is an demo example of the Worklets system that
 * shows how one would effect a system.
 */
public class effectGreeter implements Serializable{
  public static void main(String args[]) {

    WVM wvm = null;
    WorkletJunction originJunction = null;

    try {
      if (wvm == null)
	wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "effectGreeter");
    } catch (UnknownHostException e) {
      WVM.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    }

    // **************** ADDING THE WORKLET JUNCTION ********************** //

    // originJunction = new Effectlet(wvm.getWVMAddr(), wvm.getWVMName(), -1, wvm.getWVMPort(),
    // false, new WorkletID("origin"), null);

    Worklet wkl = new Worklet(originJunction);
    WorkletID id = new WorkletID("probeGreeter");

    wkl.addJunction(new Effectlet_Interlocute("localhost", "wvm1", -1, 9101, false, id, null));
    wkl.addJunction(new Effectlet_Interlocute("localhost", "wvm2", -1, 9103, false, id, null));
    wkl.addJunction(new Effectlet_Initiate("localhost", "wvm2", -1, 9103, false, id, null));
    wkl.deployWorklet(wvm);

    System.exit(0);
  }
}

class Effectlet_Interlocute extends WorkletJunction {
  Effectlet_Interlocute(String rHost, String rName, int rmiport, int rPort,
	   boolean dropOff, WorkletID id, JunctionPlanner jp){

    super(rHost, rName, rmiport, rPort, dropOff, id, jp);
  }

  public void init(Object _system, WVM _wvm) {;}

  public void execute() {
    ((Greeter)_system).startInterlocuting();
  }
}

class Effectlet_Initiate extends WorkletJunction {
  Effectlet_Initiate(String rHost, String rName, int rmiport, int rPort,
	   boolean dropOff, WorkletID id, JunctionPlanner jp){

    super(rHost, rName, rmiport, rPort, dropOff, id, jp);
  }

  public void init(Object _system, WVM _wvm) {;}

  public void execute() {
    // System.out.println("i'm here!");
    ((Greeter)_system).initiateGreeting();
  }
}
