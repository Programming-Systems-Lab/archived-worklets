/*
 * @(#)SystemDispatchStats.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Dan B. Phung
 * This file is a modified version of SystemDispatch.java
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
 * The <code>SystemDispatchStats</code> class is a demo example of the Worklets system that
 * shows how one would get useful stats from the WVM.  This file is modified from
 * SystemDispatchStats.java.
 *
 * @version	$Revision$ $Date: 2002/09/23 22:19:40
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

public class SystemDispatchStats implements Serializable {
  public static void main(String args[]) {
    WVM wvm = null;
    WorkletJunction originJunction = null;

    if (args.length < 3) {
      WVM.out.println("usage: java SystemDispatchStats rHost rName rPort");
      System.exit(0);
    }

    String rHost = args[0];
    String rName = args[1];
    int rPort = Integer.parseInt(args[2]);

    // create the local WVM that will bootstrap the Worklet into the system
    try {
      if (wvm == null)
	wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatchStats");
    } catch (UnknownHostException e) {
      WVM.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    }

    // Add a WorkletJunction
    Worklet wkl = new Worklet(originJunction);
    WorkletID id = new WorkletID("SystemDispatchStats");

    wkl.addJunction(new WorkletJunction(rHost, rName, -1, rPort, false, id, null) {
	public void init(Object _system, WVM _wvm) {}
	public void execute() {
	  // simply call the wvm's stats, and that will print the stats out.
	  _wvm.printStats();
	}
      });
    // send out the Worklet
    wkl.deployWorklet(wvm);

    System.exit(0);
  }
}
