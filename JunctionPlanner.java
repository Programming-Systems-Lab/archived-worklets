/*
 * @(#)JunctionPlanner.java
 *
 * Copyright (c) 2002: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2002: @author Dan Phung (dp2041@cs.columbia.edu)
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


/**
 * Provides meta-information pertaining to a Worklet Junction.  Make
 * sure you call <code>init(WorkletJunction)</code> before the
 * {@link WorkletJunction} uses the JunctionPlanner.
 *
 * <p>Notes
 * <li>to run instantly, run with a delay of 0</li>
 * <li>to run with an indefinite wait, run with a delay of -1 (or any negative number)</li>
 * <li> _interval and _date are mutually exclusive</li></p>
 */
/*
  WorkletJunction execution flow w/ JunctionPlanner
  1) The Worklet calls WorkletJunction.run(), which checks
  to see whether it has a JunctionPlanner.
  - if it does then it calls JunctionPlanner.start()
  - if it doesn't, the worklet junction is simply executed.
  2) In JunctionPlanner.start() it waits:
  - for a specified interval, then calls notify()
  - until a certain Date, then calls notify()
  - indefinitely (interval = 0), waiting for a notify from
    the nether regions.
  3) After notify is called to wake up the thread,
     WorkletJunction.execute() is called.

  TODO
  - check permissions before using the setState (or just get rid of
    the setState value.
*/
public class JunctionPlanner implements Serializable{

  /** {@link WorkletJunction} is ready to be run */
  public static final int STATE_READY      = 0;
  /** {@link WorkletJunction} is waiting for a predetermined period of time */
  public static final int STATE_WAITING    = 1;
  /** {@link WorkletJunction} is waiting for an indefinite period of time */
  public static final int STATE_WAIT_INDEF = 2;
  /** {@link WorkletJunction} is running */
  public static final int STATE_RUNNING    = 3;
  /** {@link WorkletJunction} is terminated */
  public static final int STATE_TERMINATED = 4;

  /** The local {@link WVM}. */
  private transient WVM _wvm;
  /** The host adapter that acts as a reference to the target system */
  private transient Object _system;
  /** Reference to the {@link WorkletJunction} associated with the JunctionPlanner */

  private WorkletJunction _parent;
  /** Current state of the {@link WorkletJunction} */
  private int _state = STATE_READY;
  /** Number of times to execute (default = 1) */
  private int _iterations = 1;
  /** Time to wait between iterations (in milliseconds, default = 0). Set to -1 to wait indefinetly */
  private long _interval = 0;
  /** <code>Date</code> to execute at. */
  private Date _date = null;

  /** <code>Timer</code> associated with keeping track of the delay between iterations. */
  private Timer _timer;
  /** <code>TimerTask</code> associated with the <code>Timer</code> */
  private TimerTask _timerTask;
  /** Checks whether the <code>TimerTask</code> has been canceled. */
  private boolean _timerTask_done;


  // ------------------------------------------------------------------- //
  // ---------------------- CONTRUCTORS/INIT --------------------------- //

  /**
   * Private general constructor used by other public constructors
   *
   * @param iterations: number of times to execute
   * @param interval: delay (in milliseconds) between iterations
   * @param date: <code>Date</code> at which to execute
   */
  private JunctionPlanner(int iterations, long interval, Date date){
    _iterations = iterations;
    _interval = interval;
    _date = date;
  }

  /**
   * Creates a JunctionPlanner to execute the given number of iterations
   * at every interval
   *
   * @param iterations: number of times to execute
   * @param interval: delay (in milliseconds) between iterations
   */
  // Set the interval
  public JunctionPlanner(int iterations, long interval){
    this(iterations, interval, null);
  }

  /**
   * dp2041 Creates a JunctionPlanner to execute the given number of iterations
   * at every <code>Date</code>
   *
   * @param iterations: number of times to execute
   * @param date: <code>Date</code> at which to execute
   */
  // Set the Date
  public JunctionPlanner(Date date){
    this(1, 0, date);
  }

  /**
   * Initialize the JunctionPlanner by giving it a reference to the
   * corresponding {@link WorkletJunction}
   *
   * @param parent: reference to the {@link WorkletJunction} associated
   */
  void init(WorkletJunction parent) {

    if (_parent == null){
      _parent = parent;

    } else {
      WVM.err.println("Error, parent already set, cannot reset");
    }
  }
  // ------------------------------------------------------------------- //


  /**
   * Starts up the Junction Planner.  This is the main method called
   * by the {@link WorkletJunction} to use the JunctionPlanner.
   */
  synchronized void start(){
    if (_parent == null){
      WVM.err.println(" Error, Parent not set.  You must call ");
      WVM.err.println(" JunctionPlanner.init(WorketJunction) before using the JunctionPlanner.");

    } else {
      _timer = new Timer();

      while (_iterations != 0) {
        if (_interval != 0 || _date != null){
          _wait();
        }

        if (_state == STATE_READY){
          _state = STATE_RUNNING;
          _parent.execute();
          _state = STATE_READY;

        } else if (_state == STATE_TERMINATED){
          break;
        }

        _iterations--;
      }
      _state = STATE_TERMINATED;
    }
  }


  /**
   * My implementation of a wait function that also keeps track of the
   * state of the {@link WorkletJunction}
   */
  synchronized private void _wait(){

    _timerTask = new TimerTask(){
      public void run(){
      wakeUp();
      }};
    _timerTask_done = false;

    if (_interval > 0)
      _timer.schedule(_timerTask, _interval);

    else if (_date != null)
      _timer.schedule(_timerTask, _date);

    synchronized (this)
    {
      try {
        if (_interval > 0 || _date != null){
          _state = STATE_WAITING;
        } else {
          _state = STATE_WAIT_INDEF;
        }

        wait();
        if (!_timerTask_done){
          _timerTask.cancel();
          _timerTask_done = true;
        }
      } catch (Exception e) {
        WVM.out.println("Exception with JunctionPlanner wait(): " + e);
      }
    }
    _state = STATE_READY;
  }



  // ------------------------------------------------------------------------- //
  // ------------------------- AUXILLIARY FUNCTIONS -------------------------- //
  // ------------------------------------------------------------------------- //
  // The following functions modify or return the value of parameters of the
  // JunctionPlanner
  // ------------------------------------------------------------------------- //

  /**
   * Wakup a JunctionPlanner in the waiting state
   * (<code>STATE_WAITING</code> or <code>STATE_WAIT_INDEF</code>)
   */
  synchronized void wakeUp(){
    this.notify();
    _state = STATE_READY;
  }

  /** Cancel a {@link WorkletJunction} from further execution */
  synchronized void cancel(){
    if (!_timerTask_done){
      _timerTask.cancel();
      _timerTask_done = true;
    }
    _timer.cancel();
  }


  // ------ Methods dealing with Iterations ------ //
  /**
   * Resets the number of iterations to execute
   *
   * @param i: new value of iterations
   */
  synchronized void setIterations(int i){
    _iterations = i;
  }

  /**
   * Gets the current number of iterations left to execute
   *
   * @return current number of iterations left to execute
   */
  synchronized int iterations(){
    return _iterations;
  }

  // ------ Methods dealing with the timing of the Junction execution ------ //
  /**
   * Resets the <code>Date</code> at which to execute
   *
   * @param date: new value <code>Date</code> at which to execute
   */
  synchronized void setDate(Date date){
    if (_interval == 0)
      _date = date;
    else
      WVM.err.println("JunctionPlanner Error, you can only change the date setting for this planner");
  }

  /**
   * Gets the current <code>Date</code> at which to execute
   *
   * @return current <code>Date</code> at which to execute
   */
  synchronized Date date(){
    return _date;
  }

  /**
   * Resets the interval value
   *
   * @param interval: new value of interval.
   */
  synchronized void setInterval(long interval){
    if (_date == null)
      _interval = interval;
    else
      WVM.err.println("JunctionPlanner Error, you can only change the interval setting for this planner");
  }

  /**
   * Gets the current interval value
   *
   * @return current interval value
   */
  synchronized long interval(){
    return _interval;
  }

  /**
   * Sets the state of the WorkletJunction
   *
   * @param state
   */
  synchronized void setState(int state){
    _state = state;
  }

  /**
   * return the current state of the {@link WorkletJunction}
   *
   * @return current state of the {@link WorkletJunction}
   */
  synchronized int state(){
    return _state;
  }

  /**
   * Gets the string meaning of the state
   *
   * @return string meaning of the state
   */
  String sstate() {
    String msg = "";
    if (_state == STATE_READY)
      msg = "WorkletJunction Ready";
    else if (_state == STATE_WAITING)
      msg = "WorkletJunction Waiting with TimerTask";
    else if (_state == STATE_WAIT_INDEF)
      msg = "WorkletJunction Waiting indefitely";
    else if (_state == STATE_RUNNING)
      msg = "WorkletJunction Running";
    else if (_state == STATE_TERMINATED)
      msg = "WorkletJunction Terminated";
    return msg;
  }
}
