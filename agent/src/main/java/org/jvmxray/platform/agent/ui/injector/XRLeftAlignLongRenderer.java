package org.jvmxray.platform.agent.ui.injector;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Default cell renderer for table String types is left justified text.
 * Unfortunate for us, the default renderer for Long types is right
 * justification.  This makes for a horrible presentation, text on
 * one side and numbers on the other.
 *
 * The XRLeftAlignLongRenderer fixes the previous by aligning Longs on
 * left like String types when presented in a table.
 * @author Milton Smith
 */
public class XRLeftAlignLongRenderer extends DefaultTableCellRenderer {
    public XRLeftAlignLongRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
    }
}
