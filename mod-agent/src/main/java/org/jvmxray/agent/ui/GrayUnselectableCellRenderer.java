package org.jvmxray.agent.ui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

public class GrayUnselectableCellRenderer extends DefaultTableCellRenderer {

    public GrayUnselectableCellRenderer() {
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Object obj = table.getModel().getValueAt(row,column);
        if (obj instanceof JLabel) {
            ((JLabel)obj).setHorizontalAlignment(SwingConstants.LEFT);
        }

        String type = (String)table.getModel().getValueAt(row,0);
        if ( type.length()>0 ) {
            c.setBackground(Color.LIGHT_GRAY);
        } else {
            c.setBackground(table.getBackground());
        }

        return c;
    }

}
