package psl.worklets;

/* CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

/* JunctionPlanner.java: implement the notion of a JunctionPlanner
 * author: Dan Phung dp2041@cs.columbia.edu, Gaurav S. Kc gksc@cs.columbia.edu
 * 
 * JunctionPlanner: provides meta-information pertaining to a Worklet Junction.
 * - provides conditional functionality
 * - provides control flow functionality
 * - 
 * TODO
 * 
 * NOTES
 * - "parent" concept only applied to WorkletJunction at this point.
 */

import java.io.*;
import java.util.*;

public class JunctionPlanner implements Serializable{
    
    public static final int PRIORITY_LOW    = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH   = 2;
    public static final int PRIORITY_ULTRA  = 3;

    public static final int STATE_READY      = 0;
    public static final int STATE_WAITING    = 1;
    public static final int STATE_RUNNING    = 2;
    public static final int STATE_TERMINATED = 3;

    private transient WVM _wvm;
    private transient Object _system;
    private WKL_XMLDesc _desc;
    private String _name;
    private WorkletJunction _parent; // a reference to the worklet junction i'm associated with

    private int _priority;
    private int _iterations;
    private boolean _dropOff;   // this was here from gaurav, i should ask him whatfer...
    private int _waitTime;	// waitTime, defaults to 0, which is: do not wait
    private int _state;		// state of the worklet junction

    private TimerCondition _timerCondition;

    JunctionPlanner() {
	init();
    }

    void setParent(WorkletJunction parent) {
	if (_parent == null){
	    _parent = parent;
	} else {
	    System.err.println("Error, parent already set, cannot reset");
	}
    }

    public JunctionPlanner(int itr){
	init();
	_iterations = itr;
    }

    protected void init() {
	_wvm = null;
	_desc = null;
	_name = null;
	_parent = null;

	_priority = PRIORITY_MEDIUM;
	_iterations = 1;
	_waitTime = 0;
    }

    void run(){ 
    }

    void execute(){
	for (int i=0; i<_iterations; i++){
	    if (_waitTime > 0){
		synchronized (this) {
		    TimerCondition t = new TimerCondition(_parent, _waitTime);
		    t.admit();
		}

	    } else {
		_parent.execute();
	    }
	}
    }
    
    void setIterations(int i){
	_iterations = i;
    }

    void setWaitTime(int w){
	_waitTime = w;
    }

    int iterations(){
	return _iterations;
    }

    int waitTime(){
	return _waitTime;
    }

    void setState(int state){
	_state = state;
    }

    int state(){
	return _state;
    }
}
