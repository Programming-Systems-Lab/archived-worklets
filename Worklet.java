package psl.worklets;

import psl.worklets.net.*;

import org.python.core.*;
import java.io.*;
import java.net.InetAddress;

/**
 * The base Worklet class
 * 
 * Each worklet is run inside its own interpreter.  Communication between 
 * worklets happens via event notifications.
 *
 * @author Steve Dossick (sdossick@cs.columbia.edu)
 */
public class Worklet {
  protected WVM wvm;
  protected WVMInterpreter wvmi;
  protected String code;
  //  protected String name = null;
  //protected String description = null;
  protected Thread interpthread;

  public static final int DEFAULT_INPUT_BUFSIZE = 2048;

  //  public PyObject ns = null;  // This is the Python namespace

  public Worklet( WVM parent, SerializedWorklet sw )
  {
    //    wvmi = new WVMInterpreter( WVMConsole.startConsole(), false );
    wvm = parent;
    wvmi = new WVMInterpreter(
		     wvm, sw.getName(),sw.getConsole(), sw.getInteractive() );
    wvmi.exec( sw.getCode() );
    wvmi.reconstitute( sw.getPickle() );
    wvmi.associateWorklet( this );
  }

  /**
   * CTOR: create a worklet from a wkl script
   */
  public Worklet( WVM parent, String script )
  {
    this( parent, new WVMInterpreter(parent), script );
  }

  public Worklet( WVM parent, WVMInterpreter i_param, String script )
  {
    wvm = parent;
    wvmi = i_param;
    wvmi.associateWorklet( this );
    wvmi.exec( script );
    wvmi.establishWorklet();
    try {
      String localHost = InetAddress.getLocalHost().getHostAddress();
      wvmi.bindVariable("_source",localHost);
    } catch (Exception e) {
      System.err.println( "Couldn't get local hostname!" );
    }
    code = script;
  }
  
  /**
   * CTOR: create a worklet from an InputStream
   */
  public Worklet( WVM parent, java.io.File f )
  {
    this( parent, new WVMInterpreter(parent), f );
  }  

  public Worklet( WVM parent, WVMInterpreter i_param, java.io.File f )
  {
    wvm = parent;
    wvmi = i_param;
    wvmi.associateWorklet( this );
    wvmi.execfile( f.getName() );
    wvmi.establishWorklet();
    try {
      String localHost = InetAddress.getLocalHost().getHostAddress();
      wvmi.bindVariable("_source",localHost);
    } catch (Exception e) {
      System.err.println( "Couldn't get local hostname!" );
    }
    try
      {
	code = getStreamText( new FileInputStream( f ) );
      }
    catch( IOException e )
      {
	System.err.println( "This should never happen:\n The interpreter "+
			    "was able to read file "+f.toString()+"\nbut it "+
			    "caused an IOException during read:" );
	e.printStackTrace();
	code = null;
      }
  }

  public Worklet(WVM parent, java.io.InputStream is )
  {
    this( parent, new WVMInterpreter(parent), is );
  }

  public Worklet(WVM parent, WVMInterpreter i_param, java.io.InputStream is)
  {
    wvm = parent;
    wvmi = i_param;
    wvmi.associateWorklet( this );
    try
      {
	code = getStreamText( is );
      }
    catch( IOException e )
      {
	System.err.println( "This should never happen:\n The interpreter "+
			    "was able to read input stream\nbut it "+
			    "caused an IOException during read:" );
	e.printStackTrace();
	code = null;
      }
    wvmi.exec( code );
    wvmi.establishWorklet();
    try {
      String localHost = InetAddress.getLocalHost().getHostAddress();
      wvmi.bindVariable("_source",localHost);
    } catch (Exception e) {
      System.err.println( "Couldn't get local hostname!" );
    }
  }

  private String getStreamText( InputStream is )
    throws IOException
  {
    char buf[] = new char[ DEFAULT_INPUT_BUFSIZE ];
    int sz;

    StringWriter sw = new StringWriter();
    BufferedReader r = new BufferedReader( new InputStreamReader( is ) );
    for ( sz = r.read( buf, 0, buf.length );
	  sz > -1;
	  sz = r.read( buf, 0, buf.length )) {
      sw.write( buf, 0, sz );
    }
    return(sw.toString());
  }

  /**
   * get the name of this worklet
   */
  public String getName() { 
    return wvmi.getEnvironmentName();
  }

  public void setName( String s )
  {
    System.err.println( "Got environment name: "+s );
    wvmi.setEnvironmentName( s );
  }

  /**
   * get a text description of what this worklet does, hopefully including
   * a list of parameters it requires
   */
  public String getDescription() { 
    return wvmi.getDescription();
  }

  public void setDescription( String s )
  {
    wvmi.setDescription(s);
  }

  public boolean hasConsole()
  {
    return wvmi.isConsoleMode();
  }

  public WVMConsole getConsole()
  {
    return wvmi.getConsole();
  }

  public boolean isInteractive()
  {
    return wvmi.isInteractive();
  }
    
  /**
   * initWorklet is called when the worklet is initially loaded into a WVM.
   * here, it can check to make sure its required providers actually exist,
   * etc.
   *
   * @return a boolean, false on error or true if the worklet is ready
   */
  public boolean initWorklet() { 
    return makeBoolMethodCall("init()");
  }

  /**
   * reconnect is called when we're woken up at a new WVM.
   *
   * @return a boolean, true if worklet is ready, false on error
   */
  public boolean reconnect() {
      //    wvmi = new WVMInterpreter(name+"Interpreter", ns);
    return makeBoolMethodCall("reconnect()");
  }

  /**
   * disconnect is called when we're about to leave this WVM
   *
   * @return a boolean, true if worklet is ready, false on error
   */
  public boolean disconnect() {
    //    ns = wvmi.getGlobalNameSpace();
    return makeBoolMethodCall("disconnect()");
  }

  /**
   * activate is called to activate the worklet.  Any parameters are given
   * in the String parameter
   */
  public void activate(String params) { 
    //    ns = wvmi.getGlobalNameSpace();
    try {
      FileOutputStream ostream = new FileOutputStream("/tmp/t.tmp");
      ObjectOutputStream p = new ObjectOutputStream(ostream);
      //      p.writeObject(ns);
      p.flush();
      ostream.close();
    } catch (Exception e) { e.printStackTrace(); }    
  }

  public void run()
  {
    interpthread = new Thread( wvmi );
    interpthread.setName( "Worklet Thread" );
    interpthread.start();
  }

  public void stop()
  {
    wvmi.stop();
    interpthread.interrupt();
  }

  public SerializedWorklet getSerializedVersion()
  {
    boolean current_run_status = wvmi.isRunning();

    if (current_run_status)
      interpthread.suspend();
    SerializedWorklet ret = new SerializedWorklet( this );
    if (current_run_status)
      interpthread.resume();
    return ret;
  }

  public PyObject getNamespace()
  {
    boolean current_run_status = wvmi.isRunning();

    PyObject ret = wvmi.packUp();
    return ret;
  }

  public String getDefinitions()
  {
    return code;
  }

  public void sendCopy( String host )
    throws psl.worklets.net.WVMTransportException
  {
    wvm.send( getSerializedVersion(), host );
  }

  public void sendCopy( String host, int port )
    throws psl.worklets.net.WVMTransportException
  {
    wvm.send( getSerializedVersion(), host, port );
  }

  public void send( String host )
    throws psl.worklets.net.WVMTransportException
  {
    SerializedWorklet sw = getSerializedVersion();
    stop();
    //    wvm.remove( this );
    wvm.send( sw, host );
  }

  public void send( String host, int port )
    throws psl.worklets.net.WVMTransportException
  {
    SerializedWorklet sw = getSerializedVersion();
    stop();
    //    WVM.remove( this );
    wvm.send( sw, host, port );
  }

  // preconditions, postconditions, strategies

  /**
   * make a method call which returns a boolean and return the result.
   */
  protected boolean makeBoolMethodCall(String methodName) {
    System.err.println("makeBoolMethodCall(" + methodName + ")");

    PyObject o = wvmi.eval( "w." + methodName );
    if (o == null)
      return false;

    if (o instanceof PyInteger)
      return (((PyInteger) o).getValue() == 1);
    else
      return false;
  }

  public void receiveEvent( psl.groupspace.GroupspaceEvent e )
  {
    wvmi.enqueueEvent( e );
  }

}




