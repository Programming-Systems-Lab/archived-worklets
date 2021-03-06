/*
 * @(#)testPrio.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Dan Phung (dp2041@cs.columbia.edu)
 * Modified from SystemDispatch.java
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;  // because we need to manipulate the thread priorities.

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * The <code>testPrio</code> is a program to test the priority
 * setting of Worklets.  Found that for Linux systems there was
 * no effect, and for Windows systems priority setting works
 * correctly.
 *
 * @version	$Revision$ $Date$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
public class testPrio extends Thread implements Serializable {
  public static void main(String args[]) {
    WVM wvm = null;
    WorkletJunction originJunction = null;

    if (args.length < 3) {
      WVM.out.println("usage: java testPrio <rHost> <rName> <rPort>");
      System.exit(0);
    }

    String rHost = args[0];
    String rName = args[1];
    int rPort = Integer.parseInt(args[2]);

    try {
      if (wvm == null)
	wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "testPrio");
    } catch (UnknownHostException e) {
      WVM.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      System.exit(0);
    }

    WorkletID id = new WorkletID("testPrio");
    Worklet wkl = new Worklet(originJunction);
    WorkletJunction highprio = new WorkletJunction(rHost, rName, -1, rPort, false, id, null) {
	public void init(Object _system, WVM _wvm) {}
	public void execute() {
	  for (int i = 0; i < 5; i++){
	    WVM.out.println(" <----- WorkletJunction.execute() (implemented in testPrio) ----->");
	    WVM.out.println(" <----- I am VERY IMPORTANT: HIGH PRIOIRTY ----->");
	    WVM.out.println("  !!!!!!!!!");
	    WVM.out.println("   /     \\ ");
	    WVM.out.println("  (.)   (.) ");
	    WVM.out.println("      ^     ");
	    WVM.out.println();
	    WVM.out.println("    \\___/   ");
	    try {
	      sleep(5000);
	    } catch (Exception e){
	      WVM.out.println("caught exception in sleep: " + e);
	    }
	  }
	}
      };


    WorkletJunction lowprio = new WorkletJunction(rHost, rName, -1, rPort, false, id, null) {
	public void init(Object _system, WVM _wvm) {}
	public void execute() {
	  for (int i = 0; i < 5; i++){
	    WVM.out.println(" <----- WorkletJunction.execute() (implemented in testPrio) ----->");
	    WVM.out.println(" <----- I am not important: LOW PRIOIRTY ----->");
	    WVM.out.println("  !!!!!!!!!");
	    WVM.out.println("   /     \\ ");
	    WVM.out.println("  (.)   (.) ");
	    WVM.out.println("      ^     ");
	    WVM.out.println();
	    WVM.out.println("    ------   ");
	    try {
	      sleep(5000);
	    } catch (Exception e){
	      WVM.out.println("caught exception in sleep: " + e);
	    }
	  }
	}
      };

    int prio1 = highprio.getPriority();
    int prio2 = lowprio.getPriority();

    prio1++;
    prio2--;

    highprio.setPriority(prio1);
    lowprio.setPriority(prio2);

    wkl.addJunction(highprio);
    wkl.addJunction(lowprio);
    wkl.deployWorklet(wvm);

    System.exit(0);
  }
}

