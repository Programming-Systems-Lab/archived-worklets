package psl.worklets;

import psl.worklets.net.*;
import psl.groupspace.*;

import org.python.modules.cPickle;

import java.io.*;
import java.util.*;
//import java.net.InetAddress;

/**
 * The Worklet Virtual Machine is responsible for maintaining an execution
 * environment for the worklets in a given system. This includes maintaining
 * worklet interpreters and the threads they run in, as well as an extensible
 * network layer for transporting worklets between WVMs and serialization
 * capabilities.
 *
 * For now, our scripting language is JPython from www.jpython.org
 */

/**
 * NullOutputStream is a utility class for use when we need an output stream
 * that points nowhere. It actually may not be used anywhere anymore.
 */
class NullOutputStream extends OutputStream {
    public void close() {}
    public void flush() {}
    public void write(byte[] b) {}
    public void write(byte[] b, int off, int len) {}
    public void write(int b) {}
}

/**
 * NullInputStream is a utility class for use when we need an input stream
 * that contains no data. It actually may not be used anywhere anymore.
 */
class NullInputStream extends InputStream {
    public int available() { return 0; }
    public void close() {}
    public void mark(int r) {}
    public boolean markSupported() { return false; }
    public int read() { return -1; }
    public int read(byte[] b) { return -1; }
    public int read(byte[] b, int off, int len) { return -1; }
    public void reset() {}
    public long skip(long n) { return 0; }
}

public class WVM implements GroupspaceService {

  public static final String version = "0.5";
  public static final PrintStream nullOut =
                           new PrintStream( new NullOutputStream() );
  public static final InputStream nullIn  =
                           new NullInputStream();

  private final GroupspaceCallback cb = new WVMCallback( this );

  private WVMTransport wvmt;
  private GroupspaceController gc = null;
  private Set worklets = Collections.synchronizedSet(new HashSet());

  /** 
   * if CONSOLE is true, we're running in a frame console
   */
  public boolean CONSOLE=false;

  /**
   * should we start a networking layer for sending/receiving worklets?
   */
  public boolean sender = false;

  /**
   * should we be verbose?
   */
  public boolean verbose = true;

  /** 
   * for testing purposes, a main routine which just starts up a console
   * so we can do things by hand
   */
  public static void main(String args[]) {
    System.err.println("Worklet Virtual Machine v"+version);
    System.err.println("Programming Systems Lab");
    System.err.println("Columbia University");
    System.err.println(
		     "Copyright 1998-1999 by Trustees of Columbia University");
    System.err.println("All Rights Reserved");
    System.err.println("");

    WVM wvm = new WVM();
    wvm.localMain(args);
  }
    
  public void localMain(String args[]) {
    for (int i = 0 ; i < args.length ; i++ ) {
      if (args[i].compareTo("-c") == 0)
	CONSOLE=true;
      else if (args[i].compareTo("-init") == 0) {
	WVMInterpreter.initScript = args[i+1];
	i++;
      } else if (args[i].compareTo("-v") == 0) {
	verbose = true;
      } else if (args[i].compareTo("-sender") == 0) {
	sender = true;
      } else {
	System.err.println("Usage: WVM [-c] [-init] [-rmi]\n" + 
			   "-c:\t\t\tStart a console frame\n" +
			   "-init <script path>:\tSpecify an init script\n" +
			   "-net:\t\t\tStart network support for worklets\n" +
			   "-v:\t\t\tVerbose error/debug messages");
	System.exit(0);
      }
    }

    if (CONSOLE) {
      WVMInterpreter i = new WVMInterpreter( this, WVMConsole.startConsole(),
					     true );
      Worklet w = new Worklet( this, i, "w=Worklet()" );
      w.setName( "Main Console" );
      w.setDescription( "Interactive Console--NULL Worklet" );
      addWorklet( w );
    }

    if (!sender) {
      File f;
      try {
	f = new File( "event.wkl" );
      } catch( Exception e ) {
	System.err.println( "Exception trying to open event.wkl:\n" );
	e.printStackTrace();
	return;
      }
      WVMInterpreter i = new WVMInterpreter( this, WVMConsole.startConsole(),
					     false );
      Worklet w = new Worklet( this, i, f );
      w.setName( "Event listener" );
      addWorklet( w );
    }

    if (sender) {
      File f;
      try {
	f = new File( "sender.wkl" );
      } catch( Exception e ) {
	System.err.println( "Exception trying to open sender.wkl:\n" );
	e.printStackTrace();
	return;
      }
      WVMInterpreter i = new WVMInterpreter( this, WVMConsole.startConsole(),
      				     false );
      Worklet w = new Worklet( this, i, f );
      w.setName("eventGenerator");
      addWorklet( w );
    }
  }

  public WVM()
  {
    new cPickle(); // Initialize new pickle module

    try
      {
	wvmt = new WVMTransport(this);
      }
    catch (WVMTransportException e)
      {
	System.err.println( "Error starting net layer:\n" );
	e.printStackTrace();
	return;
      }
  }

  public void send( SerializedWorklet sw, String host )
    throws WVMTransportException
  {
    wvmt.send( sw, host );
  }

  public void send( SerializedWorklet sw, String host, int port )
    throws WVMTransportException
  {
    wvmt.send( sw, host, port );
  }

  public void send( Worklet w, String host )
    throws WVMTransportException
  {
    send( w.getSerializedVersion(), host );
  }

  public void send( Worklet w, String host, int port )
    throws WVMTransportException
  {
    send( w.getSerializedVersion(), host, port );
  }

  public void receive(SerializedWorklet sw)
  {
    //System.err.println( "Received worklet in central WVM" );
    Worklet w = new Worklet( this, sw );
    addWorklet( w );
  }

  /**
   * add a worklet to the system
   */
  public boolean addWorklet(Worklet w) {
    //System.err.println( "In addWorklet" );
    if (!w.initWorklet()) {
      System.err.println( "Failure to initialize worklet!" );
      return false;
    }
    while (worklets.add( w ) == false) {
      w.setName( w.getName() + "!" );
    }
    w.run();
    return true;
  }

  /**
   * add a worklet from a file 
   */
  public boolean addWorkletFromFile(String filename) {
    File f;
    try {
      f = new File(filename);
    } catch (Exception e) { return false; }
    Worklet w = new Worklet(this, f);
    return addWorklet(w);
  }

  /**
   * add a worklet from a script already in a String buffer
   */
  public boolean addWorkletFromScript(String script) {
    Worklet w = new Worklet(this, script);
    return addWorklet(w);
  }

  /**
   * get an enumeration of the names of installed Worklets
   */
  public Enumeration listWorklets() {
    Iterator i = worklets.iterator();
    Vector v = new Vector();

    while( i.hasNext() ) {
      v.add( ((Worklet)i.next()).getName() );
    }
    return v.elements();
  }

  /**
   * get a String array of worklets installed here
   */
  public String[] listWorkletsArray() {
    Iterator iter = worklets.iterator();
    String [] ret = new String[worklets.size()];
    int i = 0;
    while (iter.hasNext()) {
      ret[i] = ((Worklet)iter.next()).getName();
    }
    return ret;
  }

  /**
   * how many worklets are installed here?
   */
  public int countWorklets() {
    return worklets.size();
  }

  /**
   * get a handle to an installed worklet
   */
  public Worklet getWorklet(String name) {
    Iterator i = worklets.iterator();
    
    while(i.hasNext()) {
      Worklet w = (Worklet)i.next();
      if (name.equals(w.getName()))
	return w;
    }
    return null;
  }

  /**
   * remove a worklet
   */
  public void removeWorklet(String name) {
    Worklet w = getWorklet(name);
    removeWorklet(w);
  }

  public void removeWorklet(Worklet w) 
  {
    worklets.remove(w);
  }

  /**
   * activate a worklet
   */
  /*
  public void activateWorklet(String name, String params) {
    Worklet w = (Worklet) worklets.get(name);

    if (w == null)
      return;

    w.activate(params);
  }
  */

  /*
  public String getLocalHost()
  {
    String name=null;
    try {
      name =  InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      System.err.println( "Big error! Unknown localhost!" );
      System.exit(1);
    }
    return name;
  }
  */

  public void dispatchEvent( psl.groupspace.GroupspaceEvent e )
  {
    //System.err.println( "Dispatching event "+ e.getEventDescription() );
    if (gc == null)
      receiveEvent( e );
    else
      gc.sendEvent( e );
  }

  public void receiveEvent( psl.groupspace.GroupspaceEvent e )
  {
    Iterator i = worklets.iterator();

    while( i.hasNext() ) {
      ((Worklet)i.next()).receiveEvent( e );
    }
  }

  public boolean gsInit( GroupspaceController gc_arg )
  {
    gc = gc_arg;

    gc.registerRole("WVM", this);
    gc.subscribeAllEvents( cb );
    return true;
  }

  public void gsUnload()
  {
    gc = null;
  }

  public void run()
  {
    ;
  }

}

class WVMCallback implements GroupspaceCallback {
  WVM wvm;
  
  public WVMCallback( WVM wvm_arg )
  {
    wvm = wvm_arg;
  }
  
  public int callback( GroupspaceEvent ge )
  {
    wvm.receiveEvent( ge );
    return CONTINUE;
  }
  
  public String roleName()
  {
    return "WVM";
  }

}

