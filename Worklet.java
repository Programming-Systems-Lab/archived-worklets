/*
 * @(#)Worklet.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.worklets;

import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * The mobile agent that is transported to and from {@link WVM}'s
 * in order to carry out certain tasks, as designated through the
 * {@link WorkletJunction} execution points.
 */
public final class Worklet implements Serializable {

  /** local {@link WVM} that the Worklet is currently at */
  private transient WVM _wvm;
  /** The host adapter that acts as a reference to the target system */
  private transient Object _system;
  /** local hostname */
  private String _lHost;
  /** local name of the RMI service */
  private String _lName;
  /** local port of the RMI service */
  private int _lPort;

  /** local hash of the class set */
  transient HashSet classHashSet;

  /** true if the Worklet is at its the origin point */
  private boolean _atOrigin;

  /** reference to the {@link WorkletJunction} at the origin point */
  private WorkletJunction _originJunction;

  /** reference to the current {@link WorkletJunction} */
  private WorkletJunction _currentJunction;

  /** _hashCode used by ClassLoader to index within WVM's _activeWorklets */
  String _hashCode = null;

  /** Collected "data" of WorkletJunction */
  protected Hashtable _payload;
    public String wid = "";

  /** route that the Worket is to take */
  private final Vector _route = new Vector();
  /** {@link WorkletJunction}s of the Worklet */
  private final Vector _junctions = new Vector();
  /** set of classes associated with the {@link WorkletJunction}s of the Worklet */
  private final Vector _wjClasses = new Vector();

  /** default to transporter security */
  private boolean _defaultSecurity = true;
  /** default security */
  private boolean _isSecure = false;

    //class loader to load junctions reconstructed from byte array
    private transient WorkletClassLoader _ldr = null;
    //junctions kept as byte arrays
    public Hashtable byteArrays = new Hashtable();

  /**
   * Creates a Worklet with the given {@link WorkletJunction} as the
   * originJunction
   *
   * @param _oj: execution point to set as original junction.
   */
  public Worklet(WorkletJunction _oj) {
    _originJunction = _oj;
    if (_oj != null) {
      _wjClasses.add(_oj.getClass());
      _oj._worklet = this;
    }

    // create s String worklet Id
    wid = new String((new Long(new Date().getTime())).toString()+WVM.wvm_id);
  }

  /**
   * Adds the {@link WorkletJunction} to this Worklet
   *
   * @param _wj: {@link WorkletJunction} to add
   */
  public void addJunction(WorkletJunction _wj) {
    if (_wj == null) return;
    synchronized(_junctions) {
      _junctions.addElement(_wj);
      _wjClasses.add(_wj.getClass());
      _wj.setOriginWorkletJunction(_originJunction);
      _wj._worklet = this;
    }
  }

    
    /*Add a dummy junction, with actual junction being supplied as byte code*/
    /*code base for actual junction must be supplied also*/
    public void addJunction(WorkletJunction _wj, byte []bc,URL url){
	_wj._originClassServer = url;
	//add dummy junction
	addJunction(_wj);
	//store byte array with actual junction
	byteArrays.put(_wj.getIndex(),bc);

	// TO DO:
	//encrypt the byte array
    }

  /**
   * Gets the {@link WorkletJunction} at the origin
   *
   * @return {@link WorkletJunction} at the origin
   */
  public WorkletJunction getOriginJunction() {
    return (_originJunction);
  }

  /**
   * Initializes the Worklet with the local system and {@link WVM}
   *
   * @param system: reference to local system (host adapter)
   * @param wvm: reference to local {@link WVM}
   */
  void init(Object system, WVM wvm) {
    _wvm = wvm;
    _system = system;
    if (_atOrigin) {
      _originJunction.sysInit(system, wvm);
     
    } else {
      classHashSet = new HashSet();
      _wjClasses.removeElement(_currentJunction.getClass());
      _currentJunction.sysInit(system, wvm);
     
    }
  }

  /**
   * Start the executes thread for the Worklet.  Used by the
   * {@link WVM} after Worklet has been received.
   */
  void execute() {
      //  System.out.println("Worklet: execute");
    (new Thread() {
      // create a new thread regardless
      public void run() {
        // 2-do: create _priority variable in WJ:
        // 2-do: existing super-priority thread to inherit higher priorities from ...
        WorkletJunction _wj = _atOrigin ? _originJunction : _currentJunction;
	if(byteArrays.containsKey(_wj.getIndex())){
	    try{
		 _junctions.removeElement(_wj);
		 //TO DO
		 // decrypt the byte array if encrypted
		byte []bytecode = (byte[])byteArrays.get(_wj.getIndex());
		ByteArrayInputStream baiStream = new ByteArrayInputStream(bytecode);
		if(WVM.wkltRepository.containsKey(wid)){
		    _ldr = (WorkletClassLoader)WVM.wkltRepository.get(wid);		    
		} else {
		    if (!_atOrigin) {
			if (_junctions.isEmpty()) returnToOrigin();
			else {
			    WVM.err.println("FAILED TO LOAD WJ from BYTES ARRAY: LOADER NOT FOUND!");
			    WVM.err.println("MOVING ON TO THE NEXT JUNCTION");
			    moveToNextJunction();
			    return;
			}
		    }
		}
		//this must be present
		_ldr.addTopCodebase(_wj._originClassServer);
		_ldr.dontSendWorkletId();
		ObjectInputStream ois = new ObjectInputStream(baiStream) {
			protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
			    String name = v.getName();
			    Class c = Class.forName(name, true, _ldr);
			    return ( (c == null) ? super.resolveClass(v) : c );
			}
		    };
		_wj = (WorkletJunction) ois.readObject();
		_wj.sysInit(_system, _wvm);
		baiStream.close();
	    } catch(Exception e){
		WVM.err.println("FAILED TO LOAD WJ from BYTES ARRAY:");
		e.printStackTrace();
		WVM.err.println("MOVING ON TO THE NEXT JUNCTION");
		if (!_atOrigin) {
		    if (_junctions.isEmpty()) returnToOrigin();
		    else{ 
			moveToNextJunction();
			return;
		    }
		}
	    }
	}
        Thread t = new Thread(_wj, _hashCode);
	t.setPriority(_wj.getPriority());
        t.start();

        if (_atOrigin || !_currentJunction.dropOff()) {
          try {
            t.join();
          } catch (InterruptedException ie) { }
        }

        if (!_atOrigin) {
          if (_junctions.isEmpty()) returnToOrigin();
          else{ 
	      moveToNextJunction();
	      return;
	  }
        }
      }
    }).start();
    // immediately return control to the WVM
  }

  /**
   * After all the {@link WorkletJunction}s have been added, deploy the
   * Worklet to the given {@link WVM}
   *
   * @param wvm: {@link WVM} to send the Worklet to
   */
  public void deployWorklet(WVM wvm) {
      //  System.out.println("Worklet: deployWorklet");
    synchronized (_junctions) {
      _currentJunction = (WorkletJunction) _junctions.firstElement();
      _junctions.removeElement(_currentJunction);
      _junctions.trimToSize();
    }
    //  System.out.println(wid + " " + _junctions.size());
    try{
	wvm.regJunctions(wid,_junctions);
    }catch(Exception e){
	e.printStackTrace();
    }
    _wvm = wvm;
    _lHost = wvm.transporter._host;
    _lName = wvm.transporter._name;
    _lPort = wvm.transporter._port;

    wvm.transporter.sendWorklet(this, _currentJunction);
  }

  /** Moves the Worklet from one {@link WorkletJunction} to the next */
  void moveToNextJunction() {
      //   System.out.println("Worklet: moveToNextJunction");
    WVM _tmpWVM = _wvm;
    _ldr = null;
    synchronized (_junctions) {
      _currentJunction = (WorkletJunction) _junctions.firstElement();
      _junctions.removeElement(_currentJunction);
      _junctions.trimToSize();
    }

    _lHost = _wvm.transporter._host;
    _lName = _wvm.transporter._name;
    _lPort = _wvm.transporter._port;

    // todo: locate and use predefined WVM URL comparison function.
    // note: hopefully that predefined WVM function resolves the difference
    // between 'localhost' and 127.0.0.1, and the computer's actual ip!!!
    if (_lHost == _currentJunction._host &&
	(_lName == _currentJunction._name || _lPort == _currentJunction._port)) {

      _wvm.installWorklet(this);

    } else {
      // Use local WVM to catapult to next junction
      _tmpWVM.transporter.sendWorklet(this, _currentJunction);
    }
  }

  /** Moves the Worklet to the {@link WorkletJunction} at the origin */
  void returnToOrigin() {
      //   System.out.println("Worklet: returnToOrigin");
    WVM _tmpWVM = _wvm;
    _atOrigin = true;
    _currentJunction = null;

    _lHost = _wvm.transporter._host;
    _lName = _wvm.transporter._name;
    _lPort = _wvm.transporter._port;

    if (_originJunction != null) {
      // todo: locate and use predefined WVM URL comparison function.
      if (_lHost == _originJunction._host &&
	  (_lName == _originJunction._name || _lPort == _originJunction._port)) {

	_wvm.installWorklet(this);

      } else {
	// Use local system to catapult self back to home
	_tmpWVM.transporter.sendWorklet(this, _originJunction);
      }
    }
  }

  /** Gets the number of classes associated with the {@link WorkletJunction}s */
  int getNumClasses() {
    return (_wjClasses.size());
  }

  /** Gets an enumeration of the classes associated with the {@link WorkletJunction}s */
  Enumeration getClasses() {
    return (_wjClasses.elements());
  }

  /**
   * Sets the security of the Worklet
   *
   * @param isSecure: true if the Worklet is to be secure
   */
  public final void isSecure(boolean isSecure){
    _defaultSecurity = false;
    _isSecure = isSecure;
  }

  /**
   * Checks whether this Worklet is secure, which could default to
   * security level of the {@link WVM}
   *
   * @return whether the Worklet is to be secure
   */
  public final boolean isSecure(){
    if (_defaultSecurity && _wvm != null)
      return _wvm.isSecure();
    else
      return _isSecure;
  }
}
