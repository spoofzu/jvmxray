package org.jvmxray.platform.agent.ui.injector;

import java.awt.Dimension;
import java.awt.Point;

/**
 * A convenience class to store screen coordinates, width
 * and height data, associated with moving and resizing user
 * interfaces and persisting the information between debug
 * sessions.
 *
 * @author Milton Smith
 */
public class XRScreenLocator {

    private Dimension dimension = new Dimension();
    private Point point = new Point();

    public XRScreenLocator(int pointx, int pointy, int width, int height) {
        point.x = pointx;
        point.y = pointy;
        dimension.setSize(width,height);
    }

    public int getX() {
        return point.x;
    }

    public int getY() {
        return point.y;
    }

    public int getWidth() {
        return (int)dimension.getWidth();
    }

    public int getHeight() {
        return (int)dimension.getHeight();
    }

}
