package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/**
 * Copyright (c) 2001: The Trustees of Columbia University in the City
 * of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Dan Phung
 *  
 */

/** 
 * OptionsParser:
 * Parse the options from the command line for the WVM.
 * 
 */

import java.io.*;

public class OptionsParser {
  private String _program;	// the name of the program running the parser
  private PrintStream out;
  private PrintStream err;

  // These are the variables used to create the WVM
  public String name = "WVM_Host";	// the rmi name to bind to.
  public int port = WVM_Host.PORT;	// the port to create/bind the rmi service on.
  public String keysfile = null;	// the keysfile containing public/private keys
  public String password = null;	// the password into the keysfile
  public String ctx = null;		// the context instance of the security manager
  public String kmf = null;		// the key manager factory instance type to use
  public String ks = null;		// the key store instance type to use
  public String rng = "SHA1PRNG";	// the random number generator algorithm to use
  public String WVMfile = null;		// the file holding all the WVM properties
  public int securityLevel = WVM.NO_SECURITY;	// the default security level of the WVM.

  public OptionsParser(){
    this("");
  }

  public OptionsParser(String program){
    _program = program;
    out = WVM.out;
    err = WVM.err;
  }

  /** 
   *
   */
  public int ParseOptions(String args[]) {

    // first we load the system environment variables.
    loadSystemProps();

    // then we see if the -wvmfile option was given.
    // the WVMfile could also have been set by a system property at startup
    if (args.length != 0) {
      int i=-1;
      String opt = null;
      while(++i < args.length) {
	opt = args[i];
	if (opt.equals("--wvmfile") || opt.equals("-wvmfile") ||
	    opt.equals("--file") || opt.equals("-file") || opt.equals("-f")) {
	  WVMfile = args[++i];
	  break;
	}
      }
    }

    // next we load the WVM file properties
    if (WVMfile != null) loadWVMFile(WVMfile);
    
    // then we load the arguements from the command line.
    int error= 0;
    if (args.length != 0) error = loadCommandLine(args);

    // set the system properties to the parameters we have so far.
    setWVMProperties();
    
    return error;
  }

  public void loadSystemProps() {
    String prop = "";

      prop = System.getProperty("WVM_RMI_NAME");
      if (prop != null) name = prop;

      prop = System.getProperty("WVM_RMI_PORT");
      if (prop != null) port = Integer.parseInt(prop);

      prop = System.getProperty("WVM_KEYSFILE");
      if (prop != null) keysfile = prop;

      prop = System.getProperty("WVM_PASSWORD");
      System.setProperty("WVM_PASSWORD", "");
      if (prop != null) password = prop;

      prop = System.getProperty("WVM_SSLCONTEXT");
      if (prop != null) ctx = prop;

      prop = System.getProperty("WVM_KEYMANAGER");
      if (prop != null) kmf = prop;

      prop = System.getProperty("WVM_KEYSTORE");
      if (prop != null) ks = prop;

      prop = System.getProperty("WVM_RNG");
      if (prop != null) rng = prop;

      prop = System.getProperty("WVM_FILE");
      if (prop != null) WVMfile = prop;

      prop = System.getProperty("WVM_SECURITY_LEVEL");
      if (prop != null) securityLevel = Integer.parseInt(prop);
  }

  public void loadWVMFile(String wvmfile) {
    
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(wvmfile));      
    } catch (FileNotFoundException e){
      err.println("Error with file " + wvmfile + ": " + e);
    }

    String[] buffer;
    try {
      while(reader.ready()){
	String nextline = reader.readLine().trim();

	// add the ability to put commments in the wvm file
	if (nextline.matches(".*[#].*")) {
	    buffer = nextline.split("#");
	    nextline = buffer[0].trim();

	}

	if (nextline.matches("\\s*"))
	  continue;

	buffer = nextline.split("=");
	if (buffer.length == 1)
	  continue;
	
	if (buffer[0].equals("WVM_RMI_PORT"))
	  port = Integer.parseInt(buffer[1]);
	else if (buffer[0].equals("WVM_RMI_NAME")) 
	  name = buffer[1];
	else if (buffer[0].equals("WVM_KEYSFILE")) 
	  keysfile = buffer[1];
	else if (buffer[0].equals("WVM_PASSWORD"))
	  password = buffer[1];
	else if (buffer[0].equals("WVM_SSLCONTEXT"))
	  ctx = buffer[1];
	else if (buffer[0].equals("WVM_KEYMANAGER"))
	  kmf = buffer[1];
	else if (buffer[0].equals("WVM_KEYSTORE"))
	  ks = buffer[1];
	else if (buffer[0].equals("WVM_RNG"))
	  rng = buffer[1];
	else if (buffer[0].equals("WVM_SECURITY_LEVEL"))
	  securityLevel = Integer.parseInt(buffer[1]);
	else if (buffer[0].equals("WVM_FILE"))
	  WVMfile = buffer[1];
	else 
	  System.setProperty(buffer[0], buffer[1]);
      }
      reader.close();

    } catch (IOException e) {
      err.println("Error with file " + wvmfile + ": " + e);
    }
  }

  private int loadCommandLine(String[] args) {
    int i=-1;
    while(++i < args.length) {
      String opt = args[i];
      if (opt.equals("--help") || opt.equals("-help") || opt.equals("-h")) 
	{
	  printHelp();
	  return 1;
	}
      else if (opt.equals("--name") || opt.equals("-name") || opt.equals("-n")) 
	name = args[++i];
      else if (opt.equals("--port") || opt.equals("-port") || opt.equals("-p")) 
	port = Integer.parseInt(args[++i]);
      else if (opt.equals("--keysfile") || opt.equals("-keysfile") || opt.equals("-k")) 
	keysfile = args[++i];
      else if (opt.equals("--password") || opt.equals("-password") || opt.equals("-P"))
	password = args[++i];
      else if (opt.equals("--context") || opt.equals("-context") || opt.equals("-c"))
	ctx = args[++i];
      else if (opt.equals("--keymanager") || opt.equals("-keymanager") || opt.equals("-m"))
	kmf = args[++i];
      else if (opt.equals("--keystore") || opt.equals("-keystore") || opt.equals("-s"))
	ks = args[++i];
      else if (opt.equals("--rng") || opt.equals("-rng") || opt.equals("-r"))
	rng = args[++i];
      else if (opt.equals("--securityLevel") || opt.equals("-securityLevel") || opt.equals("-S"))
	securityLevel = Integer.parseInt(args[++i]);
      else if (opt.equals("--hostsAllow") || opt.equals("-hostsAllow") || opt.equals("-a"))
	{
	  // append these to the list already in the system
	  String allowed = System.getProperty("WVM_HOSTS_ALLOW");
	  if (allowed != null) System.setProperty("WVM_HOSTS_ALLOW", allowed + "," + args[++i]);
	  else System.setProperty("WVM_HOSTS_ALLOW", args[++i]);
	}
      else if (opt.equals("--hostsDeny") || opt.equals("-hostDeny") || opt.equals("-d"))
	{
	  // append these to the list already in the system
	  String denied = System.getProperty("WVM_HOSTS_DENY");
	  if (denied != null) System.setProperty("WVM_HOSTS_DENY", denied + "," + args[++i]);
	  else System.setProperty("WVM_HOSTS_ALLOW", args[++i]);
	}
      else if (opt.equals("--wvmfile") || opt.equals("-wvmfile") || 
	       opt.equals("--file") || opt.equals("-file") || opt.equals("-f"))
	WVMfile = args[++i];
      else 
	err.println("WVM Error -- unknown arguements: " + opt);
    }
    return 0;
  }

  public void setWVMProperties() {
    if (name != null) System.setProperty("WVM_RMI_NAME", name);
    if (port != -1) System.setProperty("WVM_RMI_PORT", "" + port);
    if (keysfile != null) System.setProperty("WVM_KEYSFILE", keysfile);
    if (ctx != null) System.setProperty("WVM_SSLCONTEXT", ctx);
    if (kmf != null) System.setProperty("WVM_KEYMANAGER", kmf);
    if (ks != null) System.setProperty("WVM_KEYSTORE", ks);
    if (rng != null) System.setProperty("WVM_RNG", rng);
    System.setProperty("WVM_SECURITY_LEVEL", ""+securityLevel);
    if (WVMfile != null) System.setProperty("WVM_FILE", WVMfile);
  }

  private void printHelp() {
    out.println("Usage: java " + _program + " <options>");
    out.println("");
    out.println("<Options>: ");
    out.println("-help, -h:\tPrint this message.");
    out.println("-name, -n:\tThe name to be bound to in the rmi registry (default=WVM_Host).");
    out.println("-port, -p:\tThe port used to for the RMI service (default=9100).");
    out.println("-keysfile, -k:\tLocation and name of keys file holding the");
    out.println("\t\t public/private keys (required for secure transporter).");
    out.println("-password, -P:\tPassword for keysfile (req'd for secure transporter).");
    out.println("-context, -c:\tThe instance of the context to be implemented");
    out.println("\t\t (optional, default = \"TLS\").");
    out.println("-keymanager, -m: The algorithm of the Key Manager Factory to be ");
    out.println("\t\t implemented (optional, default = \"SunX509\").");
    out.println("-keystore, -s:\tThe algorithm used in the keystore of the keys file");
    out.println("\t\t (optional, default = \"JKS\").");
    out.println("-rng, -R:\tThe random number generator algorithm to use");
    out.println("\t\t (optional, default = \"SHA1PRNG\").");
    out.println("-securityLevel, -S:Set the security level (see below.)");
    out.println("-hostsAllow, -a: Specify the allowed hosts.");
    out.println("-hostsDeny, -d:\tSpecify the hosts that should be denied access.");
    out.println("-wvmfile, -f:\tThe filename containing the WVM properties (see below).");
    out.println("");
    out.println(" DESCRIPTION");    
    out.println("  To create a plain WVM host you do not need to specify any parameters");
    out.println("  and the RMI server name/port will default to WVM_Host/9100.  To create");
    out.println("  a secure WVM host you will need to specify at least the keysfile, ");
    out.println("  password, and the WVM Properties file.  This can be done either by setting");
    out.println("  the environment variables with -D or by using the switches as described");
    out.println("  above.  The WVM parameters in the WVM file are the same as you would set");
    out.println("  them using these options facility described above.  The file contains ");
    out.println("  property=value pairs separated by white space.  Note that the security of");
    out.println("  this file is at the operating systems level, so you must be aware of the");
    out.println("  permissions set for the file (as there might be passwords in the file.)");
    out.println("  The possible WVM systems properties are: WVM_RMI_NAME, WVM_RMI_PORT, ");
    out.println("  WVM_KEYSFILE, WVM_PASSWORD, WVM_SSLCONTEXT, WVM_KEYMANAGER, WVM_KEYSTORE,");
    out.println("  WVM_RNG, WVM_SECURITY_LEVEL, WVM_HOSTS_ALLOW, WVM_HOSTS_DENY and WVM_FILE");
    out.println("  which all correspond to the settings as described above.  Note that the");
    out.println("  WVM_PASSWORD setting is never explicitly set in the Systems.Property.");
    out.println("  Also, if you set the WVM_PASSWORD with the -D facility the property will");
    out.println("  be flushed from the system properties after it is read by the program.");
    out.println("  IMPORTANT: There is also an internally needed parameter that needs to be");
    out.println("  set in the WVM file, namely: ");
    out.println("  java.rmi.server.RMIClassLoaderSpi=psl.worklets.WVM_RMIClassLoaderSpi");
    out.println("  ");
    out.println("  The precedence of loaded settings (from lowest to highest) are:");
    out.println("  1) ENVIRONMENT/SYSTEM settings");
    out.println("  2) WVM file settings");
    out.println("  3) command-line settings");
    out.println("  Only the WVM_HOST_ALLOW and DENY properties are concantenated.");
    out.println("  ");
    out.println("Examples:");
    out.println("Plain WVM:  java psl.worklets.WVM -name target");
    out.println("Secure WVM: java -DWVM_FILE=wvm_properties psl.worklets.WVM");
    out.println("");
    out.println("For more information please see the Worklets documentation.");
  }
}
