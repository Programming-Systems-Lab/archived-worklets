/*
 * @(#)probeGreeter.java
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
import psl.probelets.*;
import psl.worklets.*;

/**
 * <code>probeGreeter</code> is an demo example of the Worklets system that
 * shows how one would probe the {@link WVM}.
 */
public class probeGreeter implements Serializable {

  public static void main(String args[]) {

    WVM wvm = null;
    WorkletJunction originJunction = null;

    if (args.length < 3) {
      WVM.out.println("usage: java probeGreeter <rHost> <rName> <rPort>");
      System.exit(0);
    }

    String rHost = args[0];
    String rName = args[1];
    int rPort = Integer.parseInt(args[2]);

    try {
      if (wvm == null)
	wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "probeGreeter");	} catch (UnknownHostException e) {
	  WVM.out.println("Exception: " + e.getMessage());
	  e.printStackTrace();
	  System.exit(0);
	}

    // **************** ADDING THE WORKLET JUNCTION ********************** //

    originJunction = new Probelet(wvm.getWVMAddr(), wvm.getWVMPort(), -1, wvm.getWVMPort(),
				  false, new WorkletID("origin"), null);

    Worklet wkl = new Worklet(originJunction);
    WorkletID id = new WorkletID("probeGreeter");

    wkl.addJunction(new Probelet(rHost, rName, -1, rPort, false, id, null));
    wkl.deployWorklet(wvm);

    System.exit(0);
  }
}

class Probelet extends WorkletJunction {
  Probelet(String rHost, String rName, int rmiport, int rPort,
	   boolean dropOff, WorkletID id, JunctionPlanner jp){

    super(rHost, rName, rmiport, rPort, dropOff, id, jp);
  }

  public void init(Object _system, WVM _wvm) {
    ((Greeter)_system).initiateGreeting();
  }

  public void execute() {
    String greeting = ((Greeter)_system).getGreeting();
    System.out.println("found the greeting to be: " + greeting);
  }
}
