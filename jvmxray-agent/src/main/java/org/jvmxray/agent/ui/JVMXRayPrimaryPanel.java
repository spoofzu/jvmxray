package org.jvmxray.agent.ui;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JButton cancelButton;
    private JRadioButton logbackRadioButton;
    private JRadioButton log4j2RadioButton;
    private JVMXRaySettingsListener jvmxRaySettingsListener;
    private static final String PROPERTIES_PATH = System.getProperty("user.home") +
            File.separator + "jvmxray-agent" +
            File.separator + "injector" +
            File.separator + "injector.properties";

    public JVMXRayPrimaryPanel() {
        setLayout(new GridBagLayout());
        initComponents();
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
        agentPayloadLbl.setToolTipText("Agent compatible jar file, see Java JAR File Specification");
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

        // Logback radio button (default)
        logbackRadioButton = new JRadioButton("logback");
        GridBagConstraints logbackButtonConstraints = new GridBagConstraints();
        logbackButtonConstraints.gridx = 0;
        logbackButtonConstraints.gridy = 1;
        logbackButtonConstraints.gridwidth = 1;
        logbackButtonConstraints.anchor = GridBagConstraints.WEST;
        logbackRadioButton.setSelected(true);
        logbackRadioButton.setToolTipText("Remember, include log dependencies in agent jar or target classpath.");
        add(logbackRadioButton, logbackButtonConstraints);

        // Log4j2 radio button
        log4j2RadioButton = new JRadioButton("log4j2");
        GridBagConstraints log4j2ButtonConstraints = new GridBagConstraints();
        log4j2ButtonConstraints.gridx = 1;
        log4j2ButtonConstraints.gridy = 1;
        log4j2ButtonConstraints.gridwidth = 1;
        log4j2ButtonConstraints.anchor = GridBagConstraints.WEST;
        log4j2RadioButton.setToolTipText("Remember, include log dependencies in agent jar or target classpath.");
        add(log4j2RadioButton, log4j2ButtonConstraints);

        // Mutually exclusive logging framework radio buttons
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(logbackRadioButton);
        buttonGroup.add(log4j2RadioButton);

        // Define the table model and setup a Long renderer so that PIDs left align like Strings.
        tableModel = new DefaultTableModel(new String[]{"Type", "PID", "Process Description"}, 0);
        table = new JTable(tableModel) {
            public Class getColumnClass(int column) {
                return ( column == 1 ) ? Long.class : String.class;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(new LeftAlignLongRenderer());

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

        // Cancel button
        cancelButton = new JButton("Cancel");
        buttonsPanel.add(cancelButton);

        // Resize "Target" columns.  Create a dummy label.  Use it's width
        // to set the target columns.  Not most accurate method but works.
        JLabel dummyLbl = new JLabel("XXXXXXXXXXXXX");
        Dimension widgetSize = dummyLbl.getPreferredSize();
        TableColumn col = table.getColumnModel().getColumn(0);
        col.setPreferredWidth(20+widgetSize.width);
        col.setMinWidth(20+widgetSize.width);
        col.setMaxWidth(40+widgetSize.width);
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
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        injectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                inject();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

    }

    public void setAgentPayload(String value) {
        agentPayload.setText(value);
        settingsValues.put("agentPayload",value);
    }

    public void setAgentLoggingFramework(String value) {
        if( value.equals("log4j2") ) {
            log4j2RadioButton.setSelected(true);
        } else if (value.equals("logback")) {
            logbackRadioButton.setSelected(true);
        } else {
            System.out.println("JVMXRayPrimaryPanel.setAgentLoggingFramework(): Unsupported agent logging framework.");
        }
        settingsValues.put("agentLoggingFramework",value);
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
        System.out.println("inject() called.  selectedRow="+selectedRow);
        if (selectedRow >= 0) {
            Long pid = (Long)tableModel.getValueAt(selectedRow, 1);
            if (jvmxRaySettingsListener != null) {
                System.out.println("Updated settings listener. pid="+pid);
                settingsValues.put("pid",pid);
                settingsValues.put("agentPayload",agentPayload.getText());
                settingsValues.put("agentLoggingFramework", logbackRadioButton.isSelected()?"logback":"log4j2");
                jvmxRaySettingsListener.onSettingsChange(settingsValues);
            }
        }
    }

    public void cancel() {
        System.out.println("cancel clicked");
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

    public void saveScreenLocation() throws IOException {
        try {
            // Create the folders if they don't exist
            System.out.println("JVMXRayPrimaryPanel.saveScreenLocation(): PROPERTIES_PATH="+PROPERTIES_PATH);
            Path directoryPath = Paths.get(PROPERTIES_PATH).getParent();
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            // Save the screen location to the properties file
            Properties properties = new Properties();
            properties.setProperty("x", String.valueOf(getTopLevelAncestor().getX()));
            properties.setProperty("y", String.valueOf(getTopLevelAncestor().getY()));
            try (FileOutputStream outputStream = new FileOutputStream(PROPERTIES_PATH)) {
                properties.store(outputStream, "Injector debug screen location");
            }
            System.out.println("Screen location saved.");
        } catch (IOException e) {
            System.err.println("Error saving screen location: " + e.getMessage());
        }
    }

    public void restoreScreenLocation() {
        System.out.println("JVMXRayPrimaryPanel.restoreScreenLocation(): PROPERTIES_PATH="+PROPERTIES_PATH);
        Dimension dimension = getSavedLocation();
        getTopLevelAncestor().setLocation(dimension.width, dimension.height);
    }

    public Dimension getSavedLocation() {
        System.out.println("JVMXRayPrimaryPanel.getSavedLocation(): PROPERTIES_PATH="+PROPERTIES_PATH);
        Dimension result = new Dimension(0,0);
        int x=0;
        int y=0;
        File propertiesFile = new File(PROPERTIES_PATH);
        if (propertiesFile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(propertiesFile)) {
                // Load the properties file
                Properties properties = new Properties();
                properties.load(inputStream);
                // Get the screen location and set the JFrame location
                x = Integer.parseInt(properties.getProperty("x", "-1"));
                y = Integer.parseInt(properties.getProperty("y", "-1"));
                // If out of bounds use 0,0 default.
                if (x != -1 && y != -1) {
                    result.setSize(x,y);
                }
            } catch (IOException e) {
                System.err.println("JVMXRayPrimaryPanel.getSavedLocation(): Error restoring screen location: " + e.getMessage());
            }
        }
        return result;
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
