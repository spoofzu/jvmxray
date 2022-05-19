package org.jvmxray.collector.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class CommandProcessor extends Thread {

    /** Get logger instance. */
    private static final Logger logger = LoggerFactory.getLogger("org.jvmxray.collector.cli.CommandProcessor");
    private static final String EOL = System.lineSeparator();
    private static final boolean NIX = EOL.length()>1;
    private ArrayList<ICommandListener> listeners = new ArrayList<>();
    private boolean bShutdown = false;
    private BufferedInputStream in;

    public CommandProcessor( InputStream in ) {
        super();
        this.in = new BufferedInputStream(in);

    }

    public void shutdownNow() {
        bShutdown = true;
    }

    public void run() {
        try {
            // Initialize
            StringBuffer buff = new StringBuffer(50);
            int ch;
            // Scan for commands until shutdown requested.
            while((ch=in.read())>-1 && !bShutdown) {
                // Add chars to line buffer as they are typed by operator.
                buff.append((char)ch);
                // Platform independent method to Determine if this ch ends the line.
                boolean bIsEOL = false;
                if( buff.length()>=EOL.length() ) {
                    bIsEOL = buff.toString().endsWith(EOL);
                }
                // Fire the key pressed to any listeners
                final char fch = (char)ch;
                listeners.stream().forEach( k -> {
                    k.cliKeyListener(fch);
                });
                // Fire the line (cmds w/args) entered to any listeners.
                String sLine = buff.toString();
                if( bIsEOL && sLine.length()>EOL.length()) {
                    ArrayList<String> alArgs = new ArrayList<>();
                    Scanner scScanner = new Scanner(sLine);
                    while (scScanner.hasNext() ) {
                        alArgs.add(scScanner.next());
                    }
                    final boolean fbIsEOL = bIsEOL;
                    // sArgs[0] is the cmd, sArgs[1+n] are the cmd arguments.
                    String cmd = alArgs.get(0);
                    alArgs.remove(0); // Remove the cmd, leaving only args.
                    final String[] sArgs = alArgs.toArray(new String[alArgs.size()]);
                    listeners.stream().forEach( k -> {
                        k.cliCommandListener(cmd, sArgs);
                    });
                    // Clear buff once cmd and parameters sent to listeners.
                    buff.setLength(0);
                }
                Thread.yield();
                Thread.sleep(50);
             }
        }catch(Throwable t) {
            logger.error("Unhandled exception. msg="+t.getMessage(), t);
        }
    }

    public void addCommandListener(ICommandListener listener) {
        listeners.add(listener);
    }

    public void removeCommandListener(ICommandListener listener) {
        listeners.remove(listener);
    }

}
