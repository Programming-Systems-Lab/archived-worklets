package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Gaurav S. Kc
 *
*/

import java.io.*;
import java.util.*;

public final class Worklet implements Serializable {

  private transient WVM _wvm;
  private transient Object _system;

  private String _lHost;
  private String _lName;
  private int _lPort;
  transient HashSet classHashSet;
  transient boolean retrieveBytecode;
  // this tells the local WVM whether to send BytecodeRetrieval or not

  private boolean _atOrigin;
  private WorkletJunction _originJunction;
  private WorkletJunction _currentJunction;

  /** _hashCode used by ClassLoader to index within WVM's _activeWorklets */
  String _hashCode = null;

  private final Vector _route = new Vector();
  private final Vector _junctions = new Vector();
  private final Vector _wjClasses = new Vector();

  public Worklet(WorkletJunction _oj) {
    _originJunction = _oj;
    if (_oj != null) {
      _wjClasses.add(_oj.getClass());
      _oj._worklet = this;
    }
  }

  public void addJunction(WorkletJunction _wj) {
    if (_wj == null) return;
    synchronized(_junctions) {
      _junctions.addElement(_wj);
      _wjClasses.add(_wj.getClass());
      _wj.setOriginWorkletJunction(_originJunction);
      _wj._worklet = this;
    }
  }

  public WorkletJunction getOriginJunction() {
    return (_originJunction);
  }

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

  void execute() {
    (new Thread() {
      // create a new thread regardless
      public void run() {
        // 2-do: create _priority variable in WJ: 
        // 2-do: existing super-priority thread to inherit higher priorities from ... 
        WorkletJunction _wj = _atOrigin ? _originJunction : _currentJunction;
        Thread t = new Thread(_wj, _hashCode);
        t.setPriority(_originJunction._priority);
        t.start();
        
        if (_atOrigin || !_currentJunction.dropOff()) {
          try {
            t.join();
          } catch (InterruptedException ie) { }
        }

          /*
           if (!WVM.NO_BYTECODE_RETRIEVAL_WORKLET && retrieveBytecode && !(_currentJunction instanceof psl.worklets.TargetWJ)) {
            // new BytecodeRetrieval(classHashSet, _wvm, _wvm.transporter._host, _wvm.transporter._name, _wvm.transporter._port, _lHost, _lName, _lPort);
          }
          */

        if (!_atOrigin) {
          if (_junctions.isEmpty()) returnToOrigin();
          else moveToNextJunction();
        }
      }
    }).start();    
    // immediately return control to the WVM 
  }

  public void deployWorklet(WVM wvm) {
    synchronized (_junctions) {
      _currentJunction = (WorkletJunction) _junctions.firstElement();
      _junctions.removeElement(_currentJunction);
      _junctions.trimToSize();
    }

    _lHost = wvm.transporter._host;
    _lName = wvm.transporter._name;
    _lPort = wvm.transporter._port;

    wvm.transporter.sendWorklet(this, _currentJunction);
  }

  void moveToNextJunction() {
    WVM _tmpWVM = _wvm;
    synchronized (_junctions) {
      _currentJunction = (WorkletJunction) _junctions.firstElement();
      _junctions.removeElement(_currentJunction);
      _junctions.trimToSize();
    }

    _lHost = _wvm.transporter._host;
    _lName = _wvm.transporter._name;
    _lPort = _wvm.transporter._port;

    // Use local WVM to catapult to next junction
    _tmpWVM.transporter.sendWorklet(this, _currentJunction);
  }

  void returnToOrigin() {
    WVM _tmpWVM = _wvm;
    _atOrigin = true;
    _currentJunction = null;
    
    _lHost = _wvm.transporter._host;
    _lName = _wvm.transporter._name;
    _lPort = _wvm.transporter._port;

    if (_originJunction != null) {
      // Use local system to catapult self back to home
      _tmpWVM.transporter.sendWorklet(this, _originJunction);
    }
  }

  int getNumClasses() {
    return (_wjClasses.size());
  }

  Enumeration getClasses() {
    return (_wjClasses.elements());
  }

}
