/*
 * @(#)SystemDispatchDate.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
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
 * The <code>SystemDispatchDate</code> class is a demo of the Worklets
 * system that shows an example of how to create a WorkletJunction
 * that executes at a certain date.
 *
 * @version	$Revision$ $Date$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
public class SystemDispatchDate implements Serializable {

  public static void main(String args[]) {
    new SystemDispatchDate(null, null, args);    
  }

  /**
   * Creates a <code>SystemDispatchDate</code> object that will send a
   * Worklet with a WorkletJunction that will execute on the specified
   * date as set through the <code>args</code>.  The Worklet will be
   * sent through the specified <code>wvm</code> with the nodal point
   * beginning at <code>originJunction</code>.
   * 
   * You must specify these <code>args</code>: 
   * - <code>args[0]</code>=<code>String rHost</code>: the remote host to send the Worklet to.
   * - <code>args[1]</code>=<code>String rName</code>: the RMI name to try to send the Worklet to.
   * - <code>args[2]</code>=<code>int rPort</code>: the socket port to try to send the Worklet to 
   * - <code>args[3]</code>=<code>int year</code>
   * - <code>args[4]</code>=<code>int month</code>
   * - <code>args[5]</code>=<code>int day</code>
   * - <code>args[6]</code>=<code>int hour</code>
   * - <code>args[7]</code>=<code>int minute</code>
   * 
   * @param      wvm		WVM used to bootstrap the Worklet into the system.
   * @param      originJunction	pointer to the originating junction.
   * @param      args		array of parameters that holds the dated information
   */
  SystemDispatchDate(WVM wvm, WorkletJunction originJunction, String args[]) {
    if (args.length < 8) {
      WVM.out.println("usage: java SystemDispatchDate rHost rName rPort year month day hour minute");
      System.exit(0);
    }
    
    String rHost = args[0];
    String rName = args[1];
    int rPort = Integer.parseInt(args[2]);

    int year = Integer.parseInt(args[3]);
    int month = Integer.parseInt(args[4]);
    int day = Integer.parseInt(args[5]);
    int hour = Integer.parseInt(args[6]);
    int minute = Integer.parseInt(args[7]);

    // create the local WVM that will bootstrap the Worklet into the system
    try {
      if (wvm == null) {
	wvm = new WVM(new Object(), InetAddress.getLocalHost().getHostAddress(), "SystemDispatchDate");
      }
    } catch (UnknownHostException e) {
      WVM.out.println("Exception: " + e);
      System.exit(0);
    } 

    // add a JunctionPlanner
    Calendar cal = Calendar.getInstance();
    cal.set(1900+year, month, day, hour, minute);
    JunctionPlanner jp = new JunctionPlanner(cal.getTime());
    WorkletID id = new WorkletID("DatedJunction");
  
    // add a WorkletJunction
    Worklet wkl = new Worklet(originJunction);
    wkl.addJunction(new WorkletJunction(rHost, rName, -1, rPort, false, id, jp) {
	private final String methodName = "main";
	public void init(Object _system, WVM _wvm) {}
	public void execute() {
	  System.out.println(" <----- WorkletJunction.execute() (implemented in SystemDispatchDate) ----->");
	  System.out.println("Hello!  The date is: " + new Date());
	}
      });
    // send out the Worklet
    wkl.deployWorklet(wvm);
    
    System.exit(0);
  }
}
