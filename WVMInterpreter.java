package psl.worklets;

import psl.worklets.*;

import java.io.*;

import org.python.core.*;
import org.python.modules.cStringIO;
import org.python.modules.cPickle;
import org.python.util.PythonInterpreter;


/**
 * The WVMInterpreter adds our functionality on top of the JPython interpreter
 */
public class WVMInterpreter extends PythonInterpreter implements Serializable, Runnable {

  // backreference to the WVM we're running in
  protected WVM wvm;

  // backreference to the Worklet object containing this WVMInterpreter
  protected Worklet enclosing_w = null;

  // have we been initialized once already?
  public boolean initted = false;

  // are we running in a console
  protected boolean interactive = false;

  // are we currently running in a thread?
  protected boolean running;

  // the console itself
  protected WVMConsole console = null;

  // the worklet object within the Interpreter
  protected PyObject worklet = null;

  // the resource name for the initscript loaded at startup
  public static String initScript = "WVMInit.wkl";

  // a name for the local environment
  protected String environmentName = "";

  // a description of this worklet
  protected String description = "";

  // a printstream directed to wherever we're supposed to write.
  protected PrintStream outstream = null;

  // an Interaction module if the interpreter is interactive
  public WVMInteract interact = null;

  // an event Queue for worklet events
  protected WorkletEventQueue weq = null;

  // Holds the Interpreter's sys module
  protected PyObject sysmodule = null;

  // getter for environmentName 
  public String getEnvironmentName() {
    return environmentName;
  }

  // setter for environmentName
  public void setEnvironmentName(String str) {
    environmentName = str;
    if (console != null) {
      outstream.println( "Local Environment Name: " + str );
      if (console.hasFrame())
	console.frame.setTitle( str );
    }
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription( String str )
  {
    description = str;
  }

  public WVMInterpreter( WVM parent )
  {
    this( parent, "WVM Default Environment", null, false );
  }

  public WVMInterpreter( WVM parent, String id )
  {
    this( parent, id, null, false );
  }

  public WVMInterpreter( WVM parent, WVMConsole wvmc )
  {
    this( parent, "WVM Default Environment", wvmc, false );
  }

  public WVMInterpreter( WVM parent, WVMConsole wvmc, boolean inter ) {
    this( parent, "WVM Default Environment", wvmc, inter );
  }

  public WVMInterpreter( WVM parent, String id, WVMConsole wvmc )
  {
    this( parent, id, wvmc, false );
  }

  public WVMInterpreter( WVM parent,
			 String id,
			 WVMConsole wvmc,
			 boolean inter )
  {
    super();
    wvm = parent;
    setEnvironmentName( id );
    sysmodule = __builtin__.__import__( new PyString("sys") );
    if (inter)
      makeInteractive();
    associateConsole( wvmc );
    init();
  }

  public void setIn( InputStream is ) {
    sysmodule.__setattr__("stdin", new PyFile( is ));
    if (interactive)
      interact.setIn( is );
  }

  public void setIn( InputStream is, String name ) {
    sysmodule.__setattr__("stdin", new PyFile( is, name ));
    if (interactive)
      interact.setIn( is );
  }

  public void setOut( PrintStream ps ) {
    outstream = ps;
    sysmodule.__setattr__("stdout", new PyFile( ps ));
    if (interactive)
      interact.setOut( ps );
  }

  public void setOut( PrintStream ps, String name ) {
    outstream = ps;
    sysmodule.__setattr__("stdout", new PyFile( ps, name ));
    if (interactive)
      interact.setOut( ps );
  }

  public void setErr( PrintStream ps ) {
    sysmodule.__setattr__("stderr", new PyFile( ps ));
  }

  public void setErr( PrintStream ps, String name ) {
    sysmodule.__setattr__("stderr", new PyFile( ps, name ));
  }

  public boolean associateWorklet( Worklet w )
  {
    System.err.println( "In associateWorklet" );
    if (enclosing_w != null) 
      return false;
    enclosing_w = w;
    System.err.println( "worklet successfully associated with interp" );
    return true;
  }

  public void associateConsole( WVMConsole c ) {
    console = c;
    if ( c == null ) {
      setIn ( WVM.nullIn );
      setOut( WVM.nullOut );
    } else {
      setIn ( c.in );
      setOut( c.out );
      if (c.hasFrame())
	c.frame.setTitle( environmentName );
    }
  }

  public boolean isConsoleMode()
  {
    if ( console == null )
      return false;
    return true;
  }

  public WVMConsole getConsole()
  {
    return console;
  }

  public void makeInteractive() {
    if (!interactive) {
      interactive = true;
      interact = new WVMInteract();
    }
  }

  public boolean isInteractive()
  {
    return interactive;
  }

  /**
   * init this WVMInterpreter by loading our initialization code in
   */
  protected void init() {

    running = false;

    weq = new WorkletEventQueue();

    if ( sysmodule == null )
	sysmodule = __builtin__.__import__( new PyString("sys") );

    exec( "def raw_input (p=''):\n\tprint p,\n\tl=sys.stdin.readline()\n\tif l=='':\n\t\traise EOFError\n\telse:\n\t\treturn l[:-1]\n\n" );

    //    set( "interp", this );
    //    DEBUG=false;  // true for lots of error messages

    try {
      execfile( this.getClass().getResourceAsStream(initScript) );
    } catch (Exception e) {
      System.err.println("Tried to open " + initScript +
			 " and got a " + e.getClass() + 
			 "\n\twith message: " + e.getMessage() +
			 "\n\tand string\n" + e.toString() );
    }

    initted = true;

    set("interp", this);

  }

  public void establishWorklet()
  {
    worklet = get("w");
  }

  /**
   * bind an object from this JVM to a variable name in the interpreter
   */
  public void bindVariable(String name, Object value) {
    worklet.__setattr__(name, Py.java2py(value));
  }

  PyObject packUp()
  {
    return eval( "w.packUp()" );
  }

  public void reconstitute( PyObject s )
  {
    if ( running )
      return;
    System.err.println( "In WVMInterpreter::reconstitute" );
    worklet = get("w");
    worklet.__setattr__( "__dict__", s );
    System.err.println( "Leaving WVMInterpreter::reconstitute" );
  }

  SerializedWorklet getSerializedVersion()
  {
    if (enclosing_w == null)
      return new SerializedWorklet( packUp() );
    else
      return new SerializedWorklet( enclosing_w );
  }

  public void sendCopy( String host )
    throws psl.worklets.net.WVMTransportException
  {
    wvm.send( getSerializedVersion(), host );
  }

  protected void sendCopy( String host, int port )
    throws psl.worklets.net.WVMTransportException
  {
    wvm.send( getSerializedVersion(), host, port );
  }

  public boolean isRunning()
  {
    return running;
  }

  public void stop()
  {
    running = false;
  }

  public void sendEvent( String desc, Object oldval, Object newval )
  {
    psl.groupspace.GroupspaceEvent e =
      new psl.groupspace.GroupspaceEvent(
	  wvm, "WVM."+getEnvironmentName()+"."+desc, oldval, newval, false );
    wvm.dispatchEvent( e );
  }

  public void sendEvent( String desc )
  {
    sendEvent( desc, null, null );
  }

  public void enqueueEvent( psl.groupspace.GroupspaceEvent e )
  {
    if (weq != null) {
      weq.enqueueEvent( e );
    }
  }

  public void addSubscription( String s )
  {
    if (weq != null) 
      weq.addSubscription( s );
  }

  public void delSubscription( String s )
  {
    if (weq != null)
      weq.delSubscription( s );
  }

  public void enterEventLoop()
  {
    while (isRunning()) {
      try
	{
	  psl.groupspace.GroupspaceEvent e = weq.dequeueEvent();
	  set( "__currentEvent", e );
	  exec( "w.handleEvent()" );
	} 
      catch (InterruptedException e)
	{
	}
    }
  }

  public void run() {
    String statement;
    
    running = true;

    exec( "w.activate()" );

    if (interactive) {
      while (running) {
	interact.prompt();
	try {
	  statement = interact.getStatement();
	} catch ( IOException e ) {
	  outstream.println( "Caught IOException:\n" + e.getMessage() );
	  return;
	} catch ( Exception e ) {
	  outstream.println( "Caught " + e.getClass() + "\nwith message"
				+ e.getMessage() );
	  return;
	}
	if ( statement.length() == 0 ) {
	  outstream.close();
	  break;
	}
	try {
	  exec( statement );
	} catch ( PyException e ) {
	  outstream.println( "Caught PyException:\n" + e.getMessage() +
				"\n  " + e.getLocalizedMessage() + "\n  "
				+ e.toString() );
	}
      }
      System.err.println( "Exit successful" );
    }
    running = false;
    weq = null;
    if (console != null) {
      console = null;
      System.err.println( "Dis-associated console" );
    }
    wvm.removeWorklet( getEnvironmentName() );
  }

}



