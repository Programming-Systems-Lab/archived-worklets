package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/* JunctionPlanner: provides meta-information pertaining to a Worklet Junction.
 * author: Dan Phung dp2041@cs.columbia.edu, Gaurav S. Kc gksc@cs.columbia.edu
 * 
 * TODO
 * - must ensure that init(WorkletJunction) is called before the WorkletJunction
 * uses the JunctionPlanner
 * 
 * NOTES
 * - to run instantly, run with a delay of 0
 * - to run with an indefinite wait, run with a delay of -1 (or any negative number)
 * 
 */

import java.io.*;
import java.util.*;

/* WorkletJunction execution flow w/ JunctionPlanner
 * 1) The Worklet calls WorkletJunction.run(), which checks
 *    to see whether it has a JunctionPlanner.  
 *    - if it does then it calls JunctionPlanner.start()
 *    - if it doesn't, the worklet junction is simply executed.  
 * 2) In JunctionPlanner.start() it waits:
 *    - for a specified interval, then calls notify()
 *    - until a certain Date, then calls notify()
 *    - indefinitely (interval = 0), waiting for a notify from
 *      the nether regions.
* 3) After notify is called to wake up the thread, 
 *    WorkletJunction.execute() is called.
 */
public class JunctionPlanner implements Serializable{

  public static final int STATE_READY      = 0;
  public static final int STATE_WAITING    = 1;
  public static final int STATE_WAIT_INDEF = 3;
  public static final int STATE_RUNNING    = 4;
  public static final int STATE_TERMINATED = 5;

  private transient WVM _wvm;		// not used presently (2002/05/01: Dan Phung)
  private transient Object _system;	// not used presently (2002/05/01: Dan Phung)
  private WKL_XMLDesc _desc;		// not used presently (2002/05/01: Dan Phung)
  private WorkletJunction _parent;	// a reference to the worklet junction i'm associated with

  private int _state = STATE_READY;	// state of the worklet junction

  private int _iterations = 1;	// NOTE!!! _iterations+_interval and _date are mutually exclusive
  private long _interval = 0;		// time to wait, defaults to 0, which is: do not wait
  private Date _date = null;		// date/time to run at.  to set: Date(int year, int month, int date)

  // These members are related to the delay of WorkletJunction execution
  private Timer _timer;
  private TimerTask _timerTask;
  private boolean _timerTask_done;	// this variable used to check whether the Task has been canceled.


  // ------------------------------------------------------------------- //
  // ---------------------- CONTRUCTORS/INIT --------------------------- //

  // private general ctor used by other public ctors
  private JunctionPlanner(int iterations, long interval, Date date){
    _iterations = iterations;
    _interval = interval;
    _date = date;
  }

  // Set the interval
  public JunctionPlanner(int iterations, long interval){
    this(iterations, interval, null);
  }

  // Set the Date
  public JunctionPlanner(Date date){ 
    this(1, 0, date);  
  }

  // !!! This function must be called before the JunctionPlanner can be used.
  void init(WorkletJunction parent) {

    if (_parent == null){
      _parent = parent;

    } else {
      WVM.err.println("Error, parent already set, cannot reset");
    }
  }
  // ------------------------------------------------------------------- //



  
  // Start up the Junction Planner.  This is the main method called
  // by the WorkletJunction to use the JunctionPlanner.
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


  // void _wait()
  // my wait function that keeps track of the state of the planner, etc.
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

  synchronized void wakeUp(){
    this.notify();
    _state = STATE_READY;
  }

  synchronized void cancel(){
    if (!_timerTask_done){
      _timerTask.cancel();
      _timerTask_done = true;
    }
    _timer.cancel();
  }


  // ------ Methods dealing with Iterations ------ //
  synchronized void setIterations(int i){
    _iterations = i;
  }

  synchronized int iterations(){
    return _iterations;
  }


  // ------ Methods dealing with the timing of the Junction execution ------ //
  synchronized void setDate(Date date){
    if (_interval == 0)
      _date = date;
    else 
      WVM.err.println("JunctionPlanner Error, you can only change the date setting for this planner");
  }

  synchronized Date date(){
    return _date;
  }

  synchronized void setInterval(long interval){
    if (_date == null)
      _interval = interval;
    else 
      WVM.err.println("JunctionPlanner Error, you can only change the interval setting for this planner");
  }

  synchronized long interval(){
    return _interval;
  }


  // ------ Methods dealing with the state of the Junction ------ //
  synchronized void setState(int state){
    _state = state;
  }

  synchronized int state(){
    return _state;
  }

  // return the string meaning of the state.
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
