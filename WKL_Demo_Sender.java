package psl.worklets;

/**
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *  
 * Copyright (c) 2001: @author Gaurav S. Kc 
 * 
*/


import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class WKL_Demo_Sender extends JFrame implements Serializable {
  public static void main(String args[]) {
    new WKL_Demo_Sender();
  }

  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }

  private JFrame mainFrame;
  private JPanel mainPanel;
  private WKL_Demo_Sender() {
    super("Worklets Sender");
    mainFrame = this;
    setSize(450, 350);
    getContentPane().add(mainPanel = new JPanel(new BorderLayout(), true));
    setupSD();
    startWVM();
    setupGUI();
    sd.shutdown();
  }

  private StartupDialog sd;
  static PrintStream out;
  private void setupSD() {
    sd = new StartupDialog(this);
    out = sd.out;
  }

  private WVM wvm;
  private void startWVM() {
    out.print("Starting WVM");
    try {
      WVM.out = out;
      wvm = new WVM(this, InetAddress.getLocalHost().getHostAddress(), "WKL_Demo_Sender");
    } catch (UnknownHostException e) {
    }
  }
  
  private static JFrame _self;
  private final static JTextArea logArea = new JTextArea("Logging area", 20, 40);
	private final static JLabel lastLogLabel = new JLabel("  Last log");
  final static PrintStream outMain =
    new PrintStream(
      new OutputStream() {
        public void write(int b) { }
      }) {
      public void print(String s) {
        lastLogLabel.setText("   ... " + s + " ... ");
        logArea.append("\n" + s);
        if (_self != null) _self.repaint();
      }
      public void println(String s) {
        print(s);
      }
  };

	private Worklet wkl = new Worklet(null);
  private void setupGUI() {
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel ctrlPanel, logPanel;
    tabbedPane.addTab("", new ImageIcon("images/start.gif"), ctrlPanel = new JPanel(new BorderLayout()), "Control Panel");
    tabbedPane.addTab("", new ImageIcon("images/logs.gif"), logPanel = new JPanel(new BorderLayout()), "Logs");
    mainPanel.add(tabbedPane);

    /* -- Control panel -- */
		// Main - Control panel
		final JPanel mainPanel, controls, listings;
		ctrlPanel.add(mainPanel= new JPanel(new BorderLayout()), BorderLayout.CENTER);
		mainPanel.add(controls = new JPanel(new GridLayout(3, 1)), BorderLayout.WEST);
		mainPanel.add(new JScrollPane(listings = new JPanel(new GridLayout(10, 1))));

		// Controls - Main - Control panel
		final JButton addJunc = new JButton("Add junction");
		final JButton resetJunc  = new JButton("Reset");
		final JButton sendWorklet = new JButton("Dispatch Worklet");
		sendWorklet.setEnabled(false);
		controls.add(addJunc);
		controls.add(resetJunc);
		controls.add(sendWorklet);
		addJunc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				GridBagLayout gbl = new GridBagLayout();
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.BOTH;
				JPanel newPanel = new JPanel(gbl);
				listings.add(newPanel);

				// Information entry
				final JButton okButton = new JButton("OK");
				gbc.gridheight = 3; // Button has to span 3 rows in height
				gbc.weighty = 1.0;
				gbl.setConstraints(okButton, gbc);
				newPanel.add(okButton);
				gbc.gridheight = 1; // Reset to default
				gbc.weighty = 0.0;  // Reset to default

				final JTextField tfHost = new JTextField("host");
				final JTextField tfName = new JTextField("name");
				final JTextField tfPort = new JTextField("" + WVM_Host.PORT);

				tfHost.addFocusListener(new FocusAdapter() {
					public void focusGained(FocusEvent e) {
						tfHost.setSelectionStart(0);
						tfHost.setSelectionEnd(tfHost.getText().length());
					}
				});
				tfName.addFocusListener(new FocusAdapter() {
					public void focusGained(FocusEvent e) {
						tfName.setSelectionStart(0);
						tfName.setSelectionEnd(tfHost.getText().length());
					}
				});
				tfPort.addFocusListener(new FocusAdapter() {
					public void focusGained(FocusEvent e) {
						tfPort.setSelectionStart(0);
						tfPort.setSelectionEnd(tfHost.getText().length());
					}
				});


				gbl.setConstraints(tfHost, gbc);
				newPanel.add(new JLabel("Host:"));
				gbc.weightx = 3.0;
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gbl.setConstraints(tfHost, gbc);
				newPanel.add(tfHost);
				gbc.weightx = 0.0;
				gbc.gridwidth = 1; // Reset to default
				
				gbl.setConstraints(tfName, gbc);
				newPanel.add(new JLabel("Name:"));
				gbc.weightx = 3.0;
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gbl.setConstraints(tfName, gbc);
				newPanel.add(tfName);
				gbc.weightx = 0.0;
				gbc.gridwidth = 1; // Reset to default
				
				gbl.setConstraints(tfPort, gbc);
				newPanel.add(new JLabel("Port:"));
				gbc.weightx = 3.0;
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gbl.setConstraints(tfPort, gbc);
				newPanel.add(tfPort);
				gbc.weightx = 0.0;
				gbc.gridwidth = 1; // Reset to default
				
				out.print("Added a new Junction");

				// Disable other buttons: s.t. cannot add junctions or send WKL until done
				addJunc.setEnabled(false);
				sendWorklet.setEnabled(false);

				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						try {
							String wklHost = tfHost.getText();
							String wklName = tfName.getText();
							int wklPort = Integer.parseInt(tfPort.getText());
							if (wklPort<=0 || wklPort>=65536) {
								throw (new NumberFormatException("Invalid port number"));
							}
							InetAddress.getByName(wklHost);
							wkl.addJunction(new WKLDemo_WorkletJunction(wklHost, wklName, wklPort));
							okButton.setEnabled(false);
							addJunc.setEnabled(true);
							sendWorklet.setEnabled(true);
							tfHost.setEditable(false);
							tfName.setEditable(false);
							tfPort.setEditable(false);
						} catch (NumberFormatException e) {
							out.println("Must specify a valid number: " + e.getMessage());
						} catch (UnknownHostException e) {
							out.println("Invalid host specified: " + e.getMessage());
						}
					}
				});
			}
		});
		resetJunc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				listings.removeAll();
				addJunc.setEnabled(true);
				out.print("removed all Junctions");
			}
		});
		sendWorklet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				wkl.deployWorklet(wvm);
				wkl = new Worklet(null);
				listings.removeAll();
				addJunc.setEnabled(true);
				sendWorklet.setEnabled(false);
				out.print("Sent out worklet");
			}
		});

		// Listings - Main - Control panel

		// Bottom - Control panel
    JPanel bottomPanel = new JPanel(new BorderLayout());
    JButton quitButton = new JButton("Quit");
    quitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        System.exit(0);
      }
    });
    bottomPanel.add(quitButton, BorderLayout.WEST);
		bottomPanel.add(lastLogLabel, BorderLayout.CENTER);
    ctrlPanel.add(bottomPanel, BorderLayout.SOUTH);

    // Log panel
    logArea.setForeground(Color.white);
    logArea.setEditable(false);
    JScrollPane jsp;
    logArea.setBackground(new Color(22, 106, 175));
    JLabel labelLogs;
    logPanel.add(labelLogs = new JLabel("Logs"), BorderLayout.NORTH);
    labelLogs.setFont(new Font("Dialog", Font.BOLD, 16));
    logPanel.add(jsp = new JScrollPane(logArea), BorderLayout.CENTER);
    JButton clearLogs;
    logPanel.add(clearLogs = new JButton("Clear logs"), BorderLayout.SOUTH);
    clearLogs.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        logArea.setText("");
      }
    });
  }

}

class StartupDialog extends JDialog {
  static PrintStream out =
    new PrintStream(
      new OutputStream() {
        public void write(int b) { }
      }) {
      public void print(String s) {
        startupDialogLabel.setText(" ... " + s + " ... ");
        startupTextArea.append("\n" + s);
        if (_self != null) _self.repaint();
      }
      public void println(String s) {
        print(s);
      }
  };

  private static JDialog _self;
  private JFrame _owner;
  private static final JLabel startupDialogLabel = new JLabel("Starting up Worklets Sender", SwingConstants.CENTER);
  private static final JTextArea startupTextArea = new JTextArea("Starting up Worklets Sender", 8, 30);
  StartupDialog(JFrame owner) {
    super(owner, "Starting up Worklets Sender", true);
    _self = this;
    _owner = owner;
    out.println("Setting up GUI");
    (new Thread() {
      public void run() {
        out.println("Starting thread");
        show();
      }
    }).start();
  }

  private JButton closeBut;
  void shutdown() {
    out.print("Can close now");
    closeBut.setEnabled(true);
		out = WKL_Demo_Sender.outMain;
		WVM.out = WKL_Demo_Sender.outMain;
		WKL_Demo_Sender.out = WKL_Demo_Sender.outMain;
  }

  private static final Color bg = new Color(22, 106, 175);
  protected void dialogInit() {
    out.println("In dialogInit");
    startupDialogLabel.setFont(new Font("Dialog", Font.BOLD, 16));
    startupDialogLabel.setForeground(Color.white);
    startupTextArea.setForeground(Color.white);
    startupTextArea.setEditable(false);
    startupTextArea.setBackground(bg);
    setSize(400, 350);
    setResizable(false);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    // Adding components to the main panel
    JPanel p, topPanel, msgPanel;
    GridBagLayout gbl;
    GridBagConstraints c;
    add(p = new JPanel(gbl = new GridBagLayout(), true));
    p.setBackground(bg);
    topPanel = new JPanel(new GridBagLayout(), true);
    topPanel.setBackground(bg);
    msgPanel = new JPanel(new BorderLayout(), true);
    msgPanel.setBackground(bg);
    c = new GridBagConstraints();
    c.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(topPanel, c);
    p.add(topPanel);
    c.gridheight = GridBagConstraints.REMAINDER;
    c.weighty = 1.0;
    gbl.setConstraints(msgPanel, c);
    p.add(msgPanel);

    // Top level image
    JLabel topImgLabel = new JLabel(new ImageIcon("images/SD.gif"), SwingConstants.LEADING);
    JLabel topTxtLabel = new JLabel("Dispatchin' dem worklets", SwingConstants.CENTER);
    topTxtLabel.setFont(new Font("Dialog", Font.BOLD, 12));
    topTxtLabel.setForeground(Color.lightGray);
    gbl = (GridBagLayout) topPanel.getLayout();
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.gridheight = 2;
    gbl.setConstraints(topImgLabel, c);
    topPanel.add(topImgLabel);

    c.insets.top += 40;
    gbl.setConstraints(topTxtLabel, c);
    topPanel.add(topTxtLabel);

    // Middle level messages
    JScrollPane jsp = new JScrollPane(startupTextArea);
    jsp.getHorizontalScrollBar().setBackground(bg);
    jsp.getVerticalScrollBar().setBackground(bg);
    msgPanel.add(startupDialogLabel, BorderLayout.NORTH);
    msgPanel.add(jsp, BorderLayout.SOUTH);

    closeBut = new JButton("Start Application Now");
    closeBut.setBackground(Color.white);
    closeBut.setForeground(bg);
    closeBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _self.hide();
        _owner.show();
      }
    });
    closeBut.setEnabled(false);
    msgPanel.add(closeBut);
  }

}

class WKLDemo_WorkletJunction extends WorkletJunction {
	private Class appClass = null;
	WKLDemo_WorkletJunction(String host, String name, int port) {
		super(host, name, port);
		try {
		 appClass = Class.forName("gskc.Bingo");
		} catch (ClassNotFoundException e) {
			WVM.out.println("ClassNotFoundException: " + e.getMessage());
			e.printStackTrace();
	 	}
	}

	public void execute() {
		WVM.out.println("a worklet has arrived ... ");
		WVM.out.println(" and it has brought with it an application ...  ");
		try {
			appClass.newInstance();
		} catch (InstantiationException e) {
			WVM.out.println("InstantiationException: " + e.getMessage());
		} catch (IllegalAccessException e) {
			WVM.out.println("IllegalAccessException: " + e.getMessage());
		}
	}
}

