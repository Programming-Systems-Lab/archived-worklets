package psl.worklets;

import java.io.*;

public class WVMInteract {
  
  // Private constants
  private static final int BUFFER_SIZE = 128;
  private static final int COLON = (int)':';

  // Private member variables
  private InputStream in_str;
  private PrintStream out;
  private byte buffer[];
  private StringBuffer reserve;
  private int buf_len = 0;
  private int buf_ind = 0;
  private boolean atEOF = false;
  private String prompt = "WVM> ";

  // Public member variables
    //  public PrintStream out;

  public WVMInteract() {
    in_str = System.in;
    out    = System.out;
    init();
  }

  public WVMInteract( InputStream in_param ) {
    in_str = in_param;
    init();
  }

  public WVMInteract( InputStream in_param, PrintStream out_param ) {
    in_str = in_param;
    out    = out_param;
    init();
  }

  private void init() {
    buffer = new byte[BUFFER_SIZE];
  }

  public void setStreams( InputStream in_param, PrintStream out_param )
  {
    in_str = in_param;
    out    = out_param;
  }

  public void setIn( InputStream in_param )
  {
    in_str = in_param;
  }

  public void setOut( PrintStream out_param )
  {
    out = out_param;
  }

  public void prompt() {
    out.print( prompt );
  }

  public void setPrompt( String newprompt ) {
    prompt = newprompt;
  }

  public String getStatement() throws IOException {
    String line;
    int index, length;

    reserve = new StringBuffer();           // clear out old reserve string

    line = getLine();
    index = line.lastIndexOf( COLON );
    if ( index == -1 )
      return line;
    else {
      length = line.length();
      while ( ++index < length ) {
	if ( !Character.isWhitespace( line.charAt(index) ) )
	  return line;
      }
      reserve.append( line );
      getBlock();
      return reserve.toString();
    }
  }

  private void getBlock() throws IOException {
    String line, prefix;
    int index;
    
    line = getLine();
    for ( index = 0; index < line.length(); index++ ) {
      if ( !Character.isWhitespace( line.charAt( index ) )
	   || line.charAt( index ) == '\n' )
	break;
    }
    if ( index == 0 )
      return;
    prefix = line.substring( 0, index );
    while ( line.startsWith( prefix ) ) {
      reserve.append( line );
      line = getLine();
    }
    reserve.append( line );
    return;
  }

  private char nextChar() throws IOException {
    if ( buf_ind < buf_len )
      return (char)buffer[buf_ind++];
    else if ( atEOF )
      return (char)0;
    else {
      buf_len = in_str.read( buffer );
      if ( buf_len == -1 ) {
	atEOF = true;
	return (char)0;
      }
      buf_ind = 1;
      return (char)buffer[0];
    }    
  }


  private String getLine() throws IOException {
    char curr;
    StringBuffer buf = new StringBuffer();

    for ( curr = nextChar();
	  curr != '\n' && curr != (char)0;
	  curr = nextChar() ) {
      buf.append ( curr );
    }

    if ( curr == '\n' ) {
      buf.append( curr );
      if ( (buf.length() > 2) && buf.charAt( buf.length() - 3 ) == '\\' ) {
	buf.append( getLine() );
      }
    }
    return buf.toString();
  }

  /*
  public static void main( String[] args ) throws IOException {
    String s;

    WVMInteract i = new WVMInteract();
    while ( true ) {
      s = i.getStatement();
      if ( s.length() == 0 )
	break;
      System.out.print( s );
    }
  }
  */
}









