package psl.worklets;

import org.python.core.*;
import org.python.modules.cStringIO;
import org.python.modules.cPickle;
import java.io.*;
import java.awt.Color;

public class SerializedWorklet implements Serializable {
  String pickle;
  String code;
  boolean hasConsole;
  boolean isInteractive;
  String consoleText;
  String name;
  int consoleRows; int consoleCols;
  int consoleBG; int consoleFG;

  public SerializedWorklet( Worklet w )
  {
    StringWriter sw = new StringWriter();
    cPickle.dump( w.getNamespace(), new PyFile( sw ) );
    pickle = sw.toString();
    code = w.getDefinitions();
    hasConsole = w.hasConsole();
    if (hasConsole) {
      serializeConsole( w.getConsole() );
    }
    isInteractive = w.isInteractive();
    name = w.getName();
    //    System.err.println( "Here's my code:\n"+code );
    //System.err.println( "Here's my pickle:\n"+pickle );
  }

  public SerializedWorklet( PyObject dict )
  {
    cStringIO.StringIO s = cStringIO.StringIO();

    cPickle.dump( dict, s );
    pickle = s.read();
    code = null;
    hasConsole = false;
    isInteractive = false;
  }

  public void serializeConsole( WVMConsole wvmc )
  {
    hasConsole = true;
    consoleText = wvmc.getText();
    consoleRows = wvmc.getRows();
    consoleCols = wvmc.getColumns();
    consoleFG = wvmc.getFG().getRGB();
    consoleBG = wvmc.getBG().getRGB();
  }

  public WVMConsole getConsole()
  {
    if (hasConsole) {
      WVMConsole wvmc = WVMConsole.startConsole( new Color(consoleFG),
						 new Color(consoleBG), "",
						 consoleRows, consoleCols );
      wvmc.append( consoleText );
      return wvmc;
    }
    return null;
  }

  public String getName()
  {
    return name;
  }

  public void setInteractive( boolean t )
  {
    isInteractive = t;
  }

  public boolean getInteractive()
  {
    return isInteractive;
  }

  public PyObject getPickle()
  {
    return Py.java2py(cPickle.load(new PyFile(
	   new ByteArrayInputStream(pickle.getBytes()))));
  }

  public String getCode()
  {
    return code;
  }

}
