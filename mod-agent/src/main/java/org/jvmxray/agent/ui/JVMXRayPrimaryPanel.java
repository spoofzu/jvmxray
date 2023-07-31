package org.jvmxray.agent.ui;

import org.jvmxray.agent.util.SimpleLogging;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.table.TableColumn;

/**
 * A simple GUI for use with jvmxrayinjector during
 * configuration/debug sessions.
 * @author Milton Smith
 */
public class JVMXRayPrimaryPanel extends JPanel {
    //TODO: Better to redesign this as a JFrame.
    private Map<String,Object> settingsValues = new HashMap<String,Object>();
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField agentPayload;
    private JButton refreshButton;
    private JButton injectButton;
    private JButton infoButton;
    private JButton cancelButton;
    private JVMXRaySettingsListener jvmxRaySettingsListener;
    private String agentBasePath = "";
    private SimpleLogging simplelogger;

    public JVMXRayPrimaryPanel(SimpleLogging simmplelogger) {
        setLayout(new GridBagLayout());
        initComponents();
        this.simplelogger = simmplelogger;
        setupEventListeners();
        refresh();
    }

    private void initComponents() {
        // Fully qualified agent payload file.
        JLabel agentPayloadLbl = new JLabel("Agent Payload: ");
        GridBagConstraints agentPayloadLblConstraints = new GridBagConstraints();
        agentPayloadLblConstraints.gridx = 0;
        agentPayloadLblConstraints.gridy = 0;
        agentPayloadLblConstraints.gridwidth = 1;
        agentPayloadLblConstraints.anchor = GridBagConstraints.WEST;
        add(agentPayloadLbl, agentPayloadLblConstraints);

        // Agent payload.
        agentPayload = new JTextField();
        GridBagConstraints agentPayloadConstraints = new GridBagConstraints();
        agentPayloadConstraints.gridx = 1;
        agentPayloadConstraints.gridy = 0;
        agentPayloadConstraints.gridwidth = GridBagConstraints.REMAINDER;
        agentPayloadConstraints.fill = GridBagConstraints.BOTH;
        agentPayloadConstraints.anchor = GridBagConstraints.WEST;
        add(agentPayload, agentPayloadConstraints);

        // Define the table model and setup a Long renderer so that PIDs left align like Strings.
        tableModel = new DefaultTableModel(new String[]{"Type", "PID", "Process Description"}, 0);
        table = new JTable(tableModel) {
            public Class getColumnClass(int column) {
                return ( column == 1 ) ? Long.class : String.class;
            }
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(new LeftAlignLongRenderer() );

        JScrollPane scrollPane = new JScrollPane(table);
        GridBagConstraints scrollPaneConstraints = new GridBagConstraints();
        scrollPaneConstraints.gridx = 0;
        scrollPaneConstraints.gridy = 2;
        scrollPaneConstraints.gridwidth = 3;
        scrollPaneConstraints.fill = GridBagConstraints.BOTH;
        scrollPaneConstraints.weightx = 1.0;
        scrollPaneConstraints.weighty = 1.0;
        add(scrollPane, scrollPaneConstraints);

        // Buttons panel with FlowLayout
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); // 10 pixels horizontal gap, 0 pixels vertical gap
        GridBagConstraints buttonsPanelConstraints = new GridBagConstraints();
        buttonsPanelConstraints.gridx = 0;
        buttonsPanelConstraints.gridy = 3;
        buttonsPanelConstraints.gridwidth = 3;
        buttonsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        buttonsPanelConstraints.anchor = GridBagConstraints.CENTER;
        add(buttonsPanel, buttonsPanelConstraints);

        // Refresh button
        refreshButton = new JButton("Refresh");
        refreshButton.isDefaultButton();
        buttonsPanel.add(refreshButton);

        // Inject button
        injectButton = new JButton("Inject");
        buttonsPanel.add(injectButton);

        // Info button
        infoButton = new JButton("Info");
        buttonsPanel.add(infoButton);

        // Cancel button
        cancelButton = new JButton("Cancel");
        buttonsPanel.add(cancelButton);

        // Resize "Target" columns.  Create a dummy label.  Use it's width
        // to set the target columns.  Not most accurate method but works.
        JLabel dummyLbl = new JLabel("XXXXXXXXXXXXX");
        Dimension widgetSize = dummyLbl.getPreferredSize();
        TableColumn col = table.getColumnModel().getColumn(0);
        col.setPreferredWidth(40+widgetSize.width);
        col.setMinWidth(40+widgetSize.width);
        col.setMaxWidth(80+widgetSize.width);
        // Resize "PID" column
        col = table.getColumnModel().getColumn(1);
        col.setPreferredWidth(20+widgetSize.width);
        col.setMinWidth(20+widgetSize.width);
        col.setMaxWidth(40+widgetSize.width);

    }

    private GridBagConstraints copyGridBagConstraintsWithInsets(GridBagConstraints source, int top, int left, int bottom, int right) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = source.gridx;
        gridBagConstraints.gridy = source.gridy;
        gridBagConstraints.gridwidth = source.gridwidth;
        gridBagConstraints.fill = source.fill;
        gridBagConstraints.weightx = source.weightx;
        gridBagConstraints.anchor = source.anchor;
        gridBagConstraints.insets = new Insets(top, left, bottom, right);
        return gridBagConstraints;
    }

    private void setupEventListeners() {
        refreshButton.addActionListener((ActionEvent e) -> {
            refresh();
        });
        injectButton.addActionListener((ActionEvent e) -> {
            inject();
        });
        infoButton.addActionListener((ActionEvent e) -> {
            info();
        });
        cancelButton.addActionListener((ActionEvent e) -> {
            cancel();
        });
        agentPayload.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                payloadVerifier();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                payloadVerifier();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                payloadVerifier();
            }
        });
    }

    public void setAgentPayload(String value) {
        agentPayload.setText(value);
        settingsValues.put("agentPayload",value);
        // Unlock the injectButton if payload is verfied.
        payloadVerifier();
    }

    public void setAgentBasePath(String value) {
        agentBasePath = value;
    }

    private void refresh() {
        // Retrieve updated process information
        List<Object[]> updatedProcessInformation = getProcessInformation();
        if (tableModel == null || updatedProcessInformation == null) {
            return;
        }
        // Clear existing table data
        tableModel.setRowCount(0);
        // Add updated process information to the table model
        for (Object[] rowData : updatedProcessInformation) {
            tableModel.addRow(rowData);
        }
    }

    public List<Object[]> getProcessInformation() {
        List<Object[]> processInformation = new ArrayList<>();
        return processInformation;
    }


    private void inject() {
        int selectedRow = table.getSelectedRow();
        simplelogger.log("JVMXRayPrimaryPanel: inject() called.  selectedRow="+selectedRow);
        if (selectedRow >= 0) {
            Long pid = (Long)tableModel.getValueAt(selectedRow, 1);
            if (jvmxRaySettingsListener != null) {
                simplelogger.log("JVMXRayPrimaryPanel: Updated settings listener. pid="+pid);
                settingsValues.put("pid",pid);
                settingsValues.put("agentPayload",agentPayload.getText());
                jvmxRaySettingsListener.onSettingsChange(settingsValues);
            }
        }
    }

    private void info() {
        int selectedRow = table.getSelectedRow();
        simplelogger.log("JVMXRayPrimaryPanel: info() clicked.  selectedRow="+selectedRow);
        if (selectedRow >= 0) {
            Long pid = (Long)tableModel.getValueAt(selectedRow, 1);
            List<ProcessHandle> processes = ProcessHandle.allProcesses().toList();
            for (ProcessHandle process : processes) {
                String commandLine = process.info().commandLine().orElse("");
                if( process.pid() == pid.longValue() ) {
                    String[] argsArray = commandLine.split(" ");
                    StringListDialog dialog = new StringListDialog((JFrame)SwingUtilities.getWindowAncestor(this), "Cmd Details", argsArray);
                    dialog.setVisible(true);
                }
            }
        }
    }

    public void cancel() {
        simplelogger.log("JVMXRayPrimaryPanel: Cancel clicked.");
        JFrame parentFrame = (JFrame)SwingUtilities.getWindowAncestor(this);
        parentFrame.dispatchEvent(new WindowEvent(parentFrame, WindowEvent.WINDOW_CLOSING));
    }

    public void exit() {
        try {
            saveScreenLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the Inject button only if the payload is verified.  A verified
     * payload is file that exists, is readable, is a jar type.  Anything else,
     * is unverified which disables the Inject button.
     */
    private void payloadVerifier() {
        boolean bInjectEnabled = false;
        String fqpayload = agentPayload.getText();
        if(fqpayload!=null && fqpayload.length()>0) {
            fqpayload = fqpayload.trim();
            File payloadFile = new File(fqpayload);
            if (payloadFile.exists() && payloadFile.canRead() ) {
                bInjectEnabled = true;
            }
        }
        simplelogger.log("JVMXRayPrimaryPanel: payloadVerifer() called. injectButton.setEnabled("+bInjectEnabled+")");
        String tipText = !bInjectEnabled ? "Inject enabled if payload jar is valid." : "";
        injectButton.setToolTipText(tipText);
        injectButton.setEnabled(bInjectEnabled);
    }

    public void saveScreenLocation() throws IOException {
        try {
            // Create the folders if they don't exist
            String child = "injector" + File.separator +
                           "injector.properties";
            File propertiesfile = new File(agentBasePath, child);

            simplelogger.log("JVMXRayPrimaryPanel.saveScreenLocation(): propertiesfile="+propertiesfile);
            Path directoryPath = Paths.get(propertiesfile.getAbsolutePath()).getParent();
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            // Save the screen location to the properties file
            Properties properties = new Properties();
            properties.setProperty("x", String.valueOf(getTopLevelAncestor().getX()));
            properties.setProperty("y", String.valueOf(getTopLevelAncestor().getY()));
            properties.setProperty("height", String.valueOf(getTopLevelAncestor().getHeight()));
            properties.setProperty("width", String.valueOf(getTopLevelAncestor().getWidth()));
            try (FileOutputStream outputStream = new FileOutputStream(propertiesfile)) {
                properties.store(outputStream, "JVMXRay injector debug screen location");
            }
            simplelogger.log("Screen location saved.");
        } catch (IOException e) {
            simplelogger.log("Error saving screen location: " + e.getMessage());
        }
    }

    public void restoreScreenLocation() {
        ScreenLocator locator = getSavedLocation();
        getTopLevelAncestor().setLocation(locator.getX(), locator.getY());
        getTopLevelAncestor().setSize(locator.getWidth(),locator.getHeight());
    }

    public ScreenLocator getSavedLocation() {
        String child = "injector" + File.separator +
                       "injector.properties";
        File propertiesfile = new File(agentBasePath, child);
        simplelogger.log("JVMXRayPrimaryPanel.getSavedLocation(): propertiesfile="+propertiesfile);
        int x=0;
        int y=0;
        int height=100;
        int width=100;
        if (propertiesfile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(propertiesfile)) {
                // Load the properties file
                Properties properties = new Properties();
                properties.load(inputStream);
                // Get the screen location and set the JFrame location
                x = Integer.parseInt(properties.getProperty("x", "-1"));
                y = Integer.parseInt(properties.getProperty("y", "-1"));
                width = Integer.parseInt(properties.getProperty("width", "-1"));
                height = Integer.parseInt(properties.getProperty("height", "-1"));
                // Basic bounds check in event file manually adjusted.
                x = (x > -1 && x < Integer.MAX_VALUE) ? x : 0;
                y = (y > -1 && y < Integer.MAX_VALUE) ? y : 0;
                width = (width > -1 && width < Integer.MAX_VALUE) ? width : 0;
                height = (height > -1 && height < Integer.MAX_VALUE) ? height : 0;
            } catch (IOException e) {
                simplelogger.log("JVMXRayPrimaryPanel.getSavedLocation(): Error restoring screen location: " + e.getMessage());
            }
        }
        return new ScreenLocator(x,y,width,height);
    }

    public void setJvmxRaySettingsListener(JVMXRaySettingsListener listener) {
        jvmxRaySettingsListener = listener;
    }

    public void attachWindowListener(JFrame frame) {
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
    }

}
