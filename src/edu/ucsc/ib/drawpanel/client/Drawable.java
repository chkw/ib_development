/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.Element;

/**
 * Interface for Widgets that can be on a DrawPanel.
 */
public interface Drawable {
    public Element getElement_drawable();

    public double getX_drawable();

    public double getY_drawable();
    
    public double getHeight_drawable();
    
    public double getWidth_drawable();

	public void setPosition_drawable(double x, double y);
}
