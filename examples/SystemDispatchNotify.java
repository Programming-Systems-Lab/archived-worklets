/*
 * @(#)SystemDispatchNotify.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2002: @author Dan Phung (dp2041@cs.columbia.edu)
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
 * The <code>SystemDispatchNotify</code> class is a demo of the
 * Worklets system that show an example of how to send a Worklet to
 * notify certain WorkletJunctions with a certain label to wake-up.
 *
 * @version	$Revision$ $Date$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

public class SystemDispatchNotify implements Serializable {

  public static void main(String args[]) {
    SystemDispatchNotify sd = new SystemDispatchNotify(null, null, args);
  }

  /**
   * Creates a <code>SystemDispatchNotify</code> object that will send a Worklet to
   * notify certain WorkletJunctions with a certain <code>label</code> to wake-up.
   * 
   * You must specify at these <code>args</code>: 
   * - <code>args[0]</code>=<code>String rHost</code>: the remote host to send the Worklet to
   * - <code>args[1]</code>=<code>String rName</code>: the RMI name to try to send the Worklet to
   * - <code>args[2]</code>=<code>int rPort</code>: the socket port to try to send the Worklet to
   * - <code>args[3]</code>=<code>String label</code>: the label of the WorketJunction to wake-up
   * 
   * @param      wvm		WVM used to bootstrap the Worklet into the system
   * @param      originJunction	pointer to the originating junction
   * @param      args		array of parameters used to create the WorkletJunction
   */

  SystemDispatchNotify(WVM wvm, WorkletJunction originJunction, String args[]) {

	if (args.length < 4) {
	    WVM.out.println("usage: java SystemDispatchNotify rHost rName rPort label");
	    System.exit(0);
	}
	
	String rHost = args[0];
	String rName = args[1];
	int rPort = Integer.parseInt(args[2]);
	final String mylabel = args[3];

	try {
	    if (wvm == null) 
	      wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatchNotify");
	} catch (UnknownHostException e) {
	    WVM.out.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(0);
	}

	// add the WorkletJunction
	Worklet wkl = new Worklet(originJunction);
	WorkletID id = new WorkletID("notifier");
	wkl.addJunction(new WorkletJunction(rHost, rName, -1, rPort, false, id, null) {
	    public void init(Object _system, WVM _wvm) {}
	    public void execute() {
	      // _wvm is the remote WVM where this junction will execute.
	      Vector v = _wvm.getJunctions(new WorkletID(mylabel));
	      if (v != null) {
		Iterator itr = v.iterator();
		while (itr.hasNext()){
		  WorkletJunction wj = (WorkletJunction)itr.next();
		  wj.wakeUp();
		}
	      } else {
		System.out.println("no junctions found with worketID: "+ mylabel);
	      }
	    }
	  });

	// send the worklet
	wkl.deployWorklet(wvm);
	
	System.exit(0);
    }
}
