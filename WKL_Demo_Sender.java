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

public class WKL_Demo_Sender extends JFrame {
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
	private PrintStream out;
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
	
	private void setupGUI() {
  	JTabbedPane tabbedPane = new JTabbedPane();
		JPanel ctrlPanel, logPanel;
		tabbedPane.addTab("", new ImageIcon("images/start.gif"), ctrlPanel = new JPanel(new BorderLayout()), "Control Panel");
		tabbedPane.addTab("", new ImageIcon("images/logs.gif"), logPanel = new JPanel(new BorderLayout()), "Logs");
		mainPanel.add(tabbedPane);

		// Control panel
		ctrlPanel.add(new JButton("a button in the control panel"));

		// Log panel
		final JTextArea logArea = new JTextArea("Logging area", 20, 40);
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
