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

public class WKL_Demo_Target extends JFrame {
  public static void main(String args[]) {
    new WKL_Demo_Target();
  }

  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      if (wvm != null) wvm.shutdown();
      System.exit(0);
    }
  }

  private JPanel panel;
  private JFrame _self = null;
  private String _name = "WKL_Demo_Target";
  private WKL_Demo_Target() {
    super("Worklets Target");
    _self = this;
    setSize(400, 500);

    getContentPane().add(panel = new JPanel(new BorderLayout(), true));
    panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
    logArea.setEditable(false);

    // Control panel @ bottom
    JPanel bottomPanel;
    panel.add(bottomPanel = new JPanel(new BorderLayout(), true), BorderLayout.SOUTH);
    JButton quitButton;
    bottomPanel.add(quitButton = new JButton("Quit"), BorderLayout.SOUTH);
    bottomPanel.add(logLabel, BorderLayout.NORTH);

    // Quit enabled
    quitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        System.exit(0);
      }
    });
    
    final JLabel wvmNameLabel = new JLabel("Set WVM name: ");
    final JTextField wvmNameText = new JTextField(_name);
    final JButton wvmNameButton  = new JButton("Set it!");
    bottomPanel.add(wvmNameLabel, BorderLayout.WEST);
    bottomPanel.add(wvmNameText, BorderLayout.CENTER);
    bottomPanel.add(wvmNameButton, BorderLayout.EAST);

    // WVM name changing
    wvmNameText.grabFocus();
    wvmNameText.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        wvmNameText.setSelectionStart(0);
        wvmNameText.setSelectionEnd(wvmNameText.getText().length());
      }
    });
    wvmNameButton.addActionListener(new ActionListener() {
      private boolean canClearLogs = false;
      public void actionPerformed(ActionEvent ae) {
        if (canClearLogs) {
          out.println("Cleared logs");
          logArea.setText("Cleared logs");
          return;
        }

        if (!wvmNameText.getText().equals("")) {
          canClearLogs = true;
          _name = wvmNameText.getText();
          startWVM();
          wvmNameLabel.setText("WVM name: ");
          wvmNameButton.setText("Clear Logs");
          wvmNameText.setEditable(false);
          _self.setTitle(wvm.toString());
        }
      }
    });

    WVM.out = out;
    show();
  }

  private WVM wvm = null;
  private void startWVM() {
    try {
      wvm = new WVM(this, InetAddress.getLocalHost().getHostAddress(), _name);
    } catch (UnknownHostException e) {
      out.println("UnknownHostException: " + e.getMessage());
      wvm = null;
    }
  }

  private final JTextArea logArea = new JTextArea("Logging Area", 20, 20);
  private final JLabel logLabel = new JLabel(" --- Logging Label --- ");
  final PrintStream out =
    new PrintStream(
      new OutputStream() {
        public void write(int b) { }
      }) {
      public void print(String s) {
        println(s);
      }
      public void println(String s) {
        logLabel.setText(" --- " + s + " --- ");
        logArea.append("\n" + s);
        _self.repaint();
      }
  };

}

