package psl.worklets;

import psl.worklets.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;

public class WVMConsole extends GenericConsole {
  public Frame frame = null;
  //  public WVMConsole theConsole = null;

  /**
   * CTOR
   */
  public WVMConsole() {
    this(20,80);
  }

  /**
   * CTOR, with rows and columns
   */
  public WVMConsole(int rows, int cols) {
    super(rows, cols, null, null);
  }

  public boolean hasFrame()
  {
    if (frame == null)
      return false;
    return true;
  }

  /** 
   * create a new console with the specified colors, title, and size, and
   * environmentId for the interpreter
   */
  public static WVMConsole startConsole(Color fg, Color bg, String title, int rows, int cols ) {
    WVMConsole console = new WVMConsole(rows, cols);
    console.frame = new Frame(title);
    console.frame.setBackground(bg);
    console.frame.setForeground(fg);
    console.frame.add(console, "Center");
    console.frame.pack();
    console.frame.setVisible(true);
    console.frame.toFront();

    console.frame.addWindowListener( new WVMConsoleAdapter( console ) );
    
    return console;
  }

  /**
   * the lazy startConsole, use defaults for all settings
   */
  public static WVMConsole startConsole() {
    System.err.println( "Building new console" );
    return startConsole(Color.yellow, Color.black, "WVM Console", 20, 80 );
  }

  public static WVMConsole startConsole( String name ) {
    return startConsole(Color.yellow, Color.black, name, 20, 80 );
  }

  public static WVMConsole funkyConsole()
  {
    return startConsole(Color.orange, Color.green, "WVM Console", 20, 80 );
  }
  
  public String toString() {
    return "WVM console";
  }

  public Color getFG()
  {
    return frame.getForeground();
  }

  public Color getBG()
  {
    return frame.getBackground();
  }

  public void finalize()
  {
    System.err.println( "Finalizing WVMConsole" );
  }

}

class WVMConsoleAdapter extends WindowAdapter {
  private WVMConsole wvmc;

  WVMConsoleAdapter( WVMConsole console )
  {
    wvmc = console;
  }

  public void windowClosed ( WindowEvent e )
  {
    wvmc.frame.removeWindowListener( this );
    wvmc.frame = null;
  }

  public void windowClosing ( WindowEvent e )
  {
    // We're assuming that the window we're attached to belongs to our console
    wvmc.frame.removeAll();
    wvmc.frame.dispose();
    wvmc.frame.removeWindowListener( this );
    wvmc.frame = null;
    wvmc = null;
  }

}
