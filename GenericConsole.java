package psl.worklets;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;
/*
 *      Stolen from bsh source. We can improve/clean up this source, but
 *      for now we're just directly stealing it. -adam
 */

/*
	Notes: todo -
	clean up the watcher thread, set daemon status

*/

/*
	(trying to be a) Simple console for Bsh.

	I looked everwhere for one, and couldn't find anything that worked.
	I've tried to keep this as small as possible, no frills.
	(Well, one frill - a simple history with the up/down arrows)
	My hope is that this can be moved to a lightweight (portable) component
	with JFC soon... but Swing is still very slow and buggy.

	The big Hack:

	The heinous, disguisting hack in here is to keep the caret (cursor)
	at the bottom of the text (without the user having to constantly click
	at the bottom).  It wouldn't be so bad if the damned setCaretPostition()
	worked as expected.  But the AWT TextArea for some insane reason treats
	NLs as characters... oh, and it refuses to let you set a caret position
	greater than the text length - for which it counts NLs as *one* character.
	The glorious hack to fix this is to go the TextComponent peer.  I really
	hate this.

	Deprecation:

	This file uses two deprecate APIs.  We want to be a PrintStream so
	that we can redirect stdout to our console... I don't see a way around
	this.  Also we have to use getPeer() for the big hack above.
*/

public class GenericConsole extends TextArea 
	implements Runnable, KeyListener, java.io.Serializable {

	transient private OutputStream outPipe;
	transient private InputStream inPipe;
	transient public InputStream in;
	transient public PrintStream out;

	private StringBuffer line = new StringBuffer();
	private String startedLine;
	private int textLength = 0;
	private Vector history = new Vector();
	private int histLine = 0;

	public GenericConsole( int rows, int cols, InputStream cin, OutputStream cout ) {
	        super(rows,cols);
		setFont( new Font("Monospaced",Font.PLAIN,14) );
		setEditable(false);
		addKeyListener ( this );

		outPipe = cout;
		if ( outPipe == null ) {
			outPipe = new PipedOutputStream();
			try {
				in = new PipedInputStream((PipedOutputStream)outPipe);
			} catch ( IOException e ) {
				print("Console internal error...");
			}
		}

		// start the inpipe watcher
		inPipe = cin;
		Thread t = new Thread( this );
		t.setName( "Console I/O Thread" );
		t.start();
		requestFocus();
	}

	public void keyPressed( KeyEvent e ) {
		type( e.getKeyCode(), e.getKeyChar(), e.getModifiers() );
		e.consume();
	}

	public GenericConsole() {
		this(12, 80, null, null);
	}
	public GenericConsole( InputStream in, OutputStream out ) {
		this(12, 80, in, out);
	}

        public void destroy()
        {
	  closePipe();
	  removeKeyListener(this);
        } 

	public void type(int code, char ch, int modifiers ) {
	      if ( modifiers == InputEvent.CTRL_MASK ) {
		if ( code == KeyEvent.VK_C
		     || code == KeyEvent.VK_D
		     || code == KeyEvent.VK_Z )
		  closePipe();
	      } else {
		switch ( code ) {
			case ( KeyEvent.VK_BACK_SPACE ):	
				if (line.length() > 0) {
					line.setLength( line.length() - 1 );
					replaceRange( "", textLength-1, textLength );
					textLength--;
				}
				break;
			case ( KeyEvent.VK_ENTER ):	
			        line.append( '\n' );
				enter();
				break;
			case ( KeyEvent.VK_U ):
				if ( (modifiers & InputEvent.CTRL_MASK) > 0 ) {
					int len = line.length();
					replaceRange( "", textLength-len, textLength );
					line.setLength( 0 );
					histLine = 0;
					textLength = getText().length(); 
				} else
					doChar( ch );
				break;
			case ( KeyEvent.VK_UP ):
				historyUp();
				break;
			case ( KeyEvent.VK_DOWN ):
				historyDown();
				break;
			case ( KeyEvent.VK_TAB ):	
				line.append("    ");
				append("    ");
				textLength +=4;
				break;
/*
			case ( KeyEvent.VK_LEFT ):	
				if (line.length() > 0) {
				break;
*/
			default:
				doChar( ch );
		}
	      }
	}

        private void closePipe () {
	  //enter();
	  try {
	    in.close();
	  } catch (IOException e) {
	    println("Problem closing pipe to interpreter: ");
	    e.printStackTrace();
	  }
	  try {
	    outPipe.close();
	  } catch (IOException e) {
	    println("Internal Console Exception while closing outPipe: ");
	    e.printStackTrace();
	  }
        }

	private void doChar( char ch ) {
		if ( (ch >= ' ') && (ch <= '~') ) {
			line.append( ch );
			append( String.valueOf(ch) );
			textLength++;
		}
	}

	private void enter() {
		String s;
		if ( line.length() == 0 )  // special hack for empty return!
		  s = String.valueOf( (char)0 );
		else {
		  //			s = line +"\n";
		  s = line.toString();
		  history.addElement( line.toString() );
		}
		line.setLength( 0 );
		histLine = 0;
		append("\n");
		textLength = getText().length(); // sync for safety
		acceptLine( s );

		setCaretPosition( textLength );
	}

	/* 
		Here's the really disguisting hack.
		We have to got to the peer because TextComponent will refuse to
		let us set us set a caret position greater than the text length.
		Great.  What a piece of crap.
	*/
	public void setCaretPosition( int pos ) {
		((java.awt.peer.TextComponentPeer)getPeer()).setCaretPosition( 
			pos + countNLs() );
	}

	/*
		This is part of a hack to fix the setCaretPosition() bug
		Count the newlines in the text
	*/
	private int countNLs() { 
		String s = getText();
		int c = 0;
		for(int i=0; i< s.length(); i++)
			if ( s.charAt(i) == '\n' )
				c++;
		return c;
	}

	private void historyUp() {
		if ( history.size() == 0 )
			return;
		if ( histLine == 0 )  // save current line
			startedLine = line.toString();
		if ( histLine < history.size() ) {
			histLine++;
			showHistoryLine();
		}
	}
	private void historyDown() {
		if ( histLine == 0 ) 
			return;

		histLine--;
		showHistoryLine();
	}

	private void showHistoryLine() {
		String showline;
		if ( histLine == 0 )
			showline = startedLine;
		else
			showline = (String)history.elementAt( history.size() - histLine );

		replaceRange( showline, textLength-line.length(), textLength );
		line = new StringBuffer(showline);
		textLength = getText().length();
	}

	private void acceptLine( String line ) {
		if (outPipe == null )
			print("Console internal error...");
		else
			try {
				outPipe.write( line.getBytes() );
				outPipe.flush();
			} catch ( IOException e ) {
				outPipe = null;
				throw new RuntimeException("Console pipe broken...");
			}
	}

	public void println( String s ) {
		print( s+"\n" );
	}
	synchronized public void print( String s ) {
		append(s);
		textLength = getText().length(); // sync for safety
	}

	private void inPipeWatcher() throws IOException {
		if ( inPipe == null ) {
			PipedOutputStream pout = new PipedOutputStream();
			out = new PrintStream( pout );
			inPipe = new PipedInputStream(pout);
		}
		byte [] ba = new byte [256]; // arbitrary blocking factor
		int read;
		try {
		  while ( (read = inPipe.read(ba)) != -1 ) {
		    print( new String(ba, 0, read) );
		  }
		  println("Console: Input closed...");
		  inPipe.close();
		} catch (IOException e) {
		  println("Console: Caught IOException while closing Input");
		  println(e.getMessage());
		}
		//		Runtime.getRuntime().exit(0);
	}

	public void run() {
		try {
			inPipeWatcher();
		} catch ( IOException e ) {
			println("Console: I/O Error...");
		}
		System.err.println( "Closing Console Thread: "+
				    Thread.currentThread().getName());
	}

  /*	public static void main( String args[] ) {
		GenericConsole console = new GenericConsole();
		final Frame f = new Frame("Generic Console");
		f.add(console, "Center");
		f.pack();
		f.show();
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				f.dispose();
			}
		} );
			
		Interpreter interpreter = new Interpreter( console );
		interpreter.setVariable("__console", console);
		interpreter.run();
	}
  */
	public String toString() {
		return "Generic console";
	}

	// unused
	public void keyTyped(KeyEvent e) { }
    public void keyReleased(KeyEvent e) { }


}






