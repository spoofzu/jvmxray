package org.jvmxray.platform.agent.ui.injector;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;

public class XRStringListDialog extends JDialog {
    private JTextArea textArea;

    public XRStringListDialog(Frame owner, String title, String[] strings) {
        super(owner, title, true);

        // Create a JTextArea with line and word wrapping enabled
        textArea = new JTextArea();
        textArea.setLineWrap(false);
        //textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Add the strings to the text area
        int element = 0;
        for (String string : strings) {
            textArea.append("["+element+"]  "+ string + "\n");
            element++;
        }

        // Create a JScrollPane to contain the text area
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        // Add the scroll pane to the dialog
        getContentPane().add(scrollPane);

        // Set the dialog properties
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(owner);
    }
}

