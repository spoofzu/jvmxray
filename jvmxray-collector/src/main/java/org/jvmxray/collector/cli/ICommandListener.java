package org.jvmxray.collector.cli;

import java.awt.event.KeyEvent;

public interface ICommandListener {

    public void cliKeyListener(char keytyped);
    public void cliCommandListener(String cmd, String[] args);

}
