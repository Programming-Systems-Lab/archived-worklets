package psl.worklets;

/**
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2001: @author Gaurav S. Kc
 * 
*/

// NOTES-TO-SELF: @ 4:38am, 15 November, 2000
//   - WKL will have 4 states: ready, waiting, running and terminated
//   - wjunc.execute: should revive the thread if it was already activated!

import java.util.*;

abstract class WJackCondition {
  WorkletJunction _wjunc;
  Worklet _wkl;
  public void admit() throws IllegalWJOperationException {
    if (_wjunc._state != WorkletJacket.STATE_READY) {
      throw (new IllegalWJOperationException("Can only ADMIT a READY WorkletJunction"));
    }
    _wjunc._state = WorkletJacket.STATE_WAITING;
  }

  public void start() throws IllegalWJOperationException {
    if (_wjunc._state != WorkletJacket.STATE_WAITING) {
      throw (new IllegalWJOperationException("Can only START a WAITING WorkletJunction"));
    }
    _wjunc._state = WorkletJacket.STATE_RUNNING;
    _wkl.execute();
  }
  abstract public void interrupt() throws IllegalWJOperationException;
  abstract public void terminate() throws IllegalWJOperationException;
}

final class TimerCondition extends WJackCondition {
  /* The rationale for this class is that specified timeouts can change the WKL's state */

  long _interval;
  Timer _timer;

  TimerTask _timerTask_awake = new TimerTask() {
    public void run() {
      _wjunc.execute();
    }
  };

  TimerTask _timerTask_cease = new TimerTask() {
    public void run() {
      // _wjunc.cease();
    }
  };

  TimerTask _timerTask_pause = new TimerTask() {
    public void run() {
      // _wjunc.pause();
    }
  };

  TimerCondition(WorkletJunction wj, int interval) {
    _wjunc = wj;
    _timer = new Timer();
    _interval = (long) interval;
  }

  public void admit() {
    _timer.schedule(_timerTask_awake, _interval);
  }

  public void start() {
    _timer.schedule(_timerTask_cease, _interval);
  }

  public void interrupt() {
    _timer.schedule(_timerTask_pause, _interval);
  }

  public void terminate() {
    // what happens here???
  }
}

class IllegalWJOperationException extends Exception {
  IllegalWJOperationException(String msg) {
    super(msg);
  }
  public String toString() {
    return (getMessage() + ": illegal control operation attempted on worklet");
  }
}

/* -- COMMENTED OUT so that i can get TimerCondition working first
class InvalidInducerException extends Exception {
  InvalidInducerException(String msg) {
    super(msg);
  }
  public String toString() {
    return (getMessage() + ": illegal attempt to induce worklet");
  }
}

final class WVM_Condition extends WJackCondition {
  /*
   * The rationale for this class is that certain specified worklets 
   * executing at the host can change the WKL's state
  * /
  Vector _inducers;
  WVM_Condition(WorkletJunction wj, Vector ind) {
    _wjunc = wj;
    _inducers = ind;
  }

  /* 
   * For this class, the _inducing_ [read invoking] worklet can 
   * be identified from the current thread 
  * /
  void verifyInducerWKL() throws psl.worklets2.InvalidInducerException {
    WVM_Thread current = (WVM_Thread) Thread.currentThread();
    Worklet inducerWKL = current.getWorklet();
    if (!_inducers.contains(current.getWorklet())) {
      throw (new InvalidInducerException(inducerWKL.toString()));
    }
  }
  public void admit() throws psl.worklets2.InvalidInducerException {
    verifyInducerWKL(); 
    _wjunc.execute();
  }
  public void start() throws psl.worklets2.InvalidInducerException {
    verifyInducerWKL(); 
    _wjunc.cease();
  }
  public void interrupt() throws psl.worklets2.InvalidInducerException {
    verifyInducerWKL(); 
    _wjunc.pause();
  }
}

final class Event_Condition extends WJackCondition {
  /*
   * The rationale for this class is that upon certain specified events 
   * occuring at the host, the local WVM can change the WKL's state
  * /
  Vector _events;
  Event_Condition(WorkletJunction wj, Vector evts) {
    _wjunc = wj;
    _events = evts;
  }

  /*
   * For this class, need some information about the local EVENT_SYSTEM, 
   * and how to access the event identifier from the calling THREAD
  * /
  public void admit() {
    _wjunc.execute();
  }
  public void start() {
    _wjunc.cease();
  }
  public void interrupt() {
    _wjunc.pause();
  }
}
*/
