package psl.worklets;

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

  private boolean _atOrigin;
  private WorkletJunction _originJunction;
  private WorkletJunction _currentJunction;

  private final Vector _junctions = new Vector();
  private final Vector _wjClasses = new Vector();

  public Worklet(WorkletJunction _oj) {
    _originJunction = _oj;
    if (_oj != null) _wjClasses.add(_oj.getClass());
  }

  public void addJunction(WorkletJunction _wj) {
    synchronized(_junctions) {
      _junctions.addElement(_wj);
      _wjClasses.add(_wj.getClass());
      _wj.setOriginWorkletJunction(_originJunction);
    }
  }
  
  void init(Object system, WVM wvm) {
    _wvm = wvm;
    _system = system;    
    if (_atOrigin) {
      _originJunction.init(system, wvm);
    } else {
      _currentJunction.init(system, wvm);
    }
  }
  
  void execute() {
    if (_atOrigin) {
      _originJunction.execute();
    } else {
      _currentJunction.execute();
      if (_junctions.isEmpty()) {
        returnToOrigin();
      } else {
        moveToNextJunction();
      }
    }
  }

  public void deployWorklet(WVM wvm) {
    synchronized (_junctions) {
      _currentJunction = (WorkletJunction) _junctions.firstElement();
      _junctions.removeElement(_currentJunction);
    }
    wvm.transporter.sendWorklet(this, _currentJunction);
  }
  
  void moveToNextJunction() {
    WVM _tmpWVM = _wvm;
    synchronized (_junctions) {
      _currentJunction = (WorkletJunction) _junctions.firstElement();
      _junctions.removeElement(_currentJunction);
    }
    // Use local WVM to catapult to next junction
    _tmpWVM.transporter.sendWorklet(this, _currentJunction);
  }

  void returnToOrigin() {
    WVM _tmpWVM = _wvm;
    _atOrigin = true;
    _currentJunction = null;
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
