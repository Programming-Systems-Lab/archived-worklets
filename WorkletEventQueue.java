package psl.worklets;

import java.util.*;

public class WorkletEventQueue {
  private java.util.LinkedList list;
  private java.util.LinkedList subs;

  public WorkletEventQueue()
  {
    list = new java.util.LinkedList();
    subs = new java.util.LinkedList();
  }
			  
  public synchronized void enqueueEvent( psl.groupspace.GroupspaceEvent e )
  {
    ListIterator li = subs.listIterator(0);
    String eventName = e.getEventDescription();
    while( li.hasNext() ) {
      if (eventName.equals((String)li.next())) {
	list.addFirst( (java.lang.Object) e );
	if (list.size() == 1)
	  notify();
	return;
      }
    }
  }

  public synchronized psl.groupspace.GroupspaceEvent dequeueEvent()
    throws InterruptedException
  {
    if (list.size() == 0)
      wait();
    return (psl.groupspace.GroupspaceEvent) list.removeLast();
  }
  
  public synchronized void addSubscription( String eventName )
  {
    ListIterator li = subs.listIterator(0);
    while (li.hasNext())
      if (eventName.equals((String)li.next())) return;
    subs.add((java.lang.Object) eventName );
  }

  public synchronized void delSubscription( String eventName )
  {
    ListIterator li = subs.listIterator(0);
    while (li.hasNext()) {
      Object o = li.next();
      if (eventName.equals((String)o)) {
	subs.remove(o);
	return;
      }
    }
    System.err.println( "*****WARNING!*****\nTried to unsubscribe from "+
			"events named "+eventName+"\nBut NOT SUBSCRIBED\n" );

  }

}
