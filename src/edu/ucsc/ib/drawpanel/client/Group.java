/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A grouping of drawable elements.
 */
public class Group extends ComplexPanel implements Drawable {
	private String id;
	private String transform;
	private double x;
	private double y;
	private double opacity;

	public Group() {
		setElement(DrawPanel.impl.createGroupElement());
		sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);
	}

	public void add(Widget w) {
		if (!(w instanceof Drawable)) {
			throw new IllegalArgumentException("Group can only add widgets "
					+ "of type DrawWidget");
		}
		super.add(w, getElement_drawable());
	}

	public double getWidth_drawable() {
		return DrawPanel.impl.getBBoxWidthForDP(this);
	}

	public double getHeight_drawable() {
		return DrawPanel.impl.getBBoxHeightForDP(this);
	}

	/**
	 * Get the x position of the underlying group element. This is the value of
	 * x that is used for the SVG group "translate" attribute.
	 */
	public double getX_drawable() {
		return this.x;
	}

	/**
	 * Get the y position of the underlying group element. This is the value of
	 * y that is used for the SVG group "translate" attribute.
	 */
	public double getY_drawable() {
		return this.y;
	}

	public void setID(String id) {
		this.id = id;
		DrawPanel.impl.setGroupID(this, this.id);
	}

	/**
	 * Set the position.
	 */
	public void setPosition_drawable(double x, double y) {
		this.x = x;
		this.y = y;
		DrawPanel.impl.setGroupPosition(this, x, y);
	}

	/**
	 * Set the transform.
	 * 
	 * @param transform
	 */
	public void setTransform(String transform) {
		this.transform = transform;
		DrawPanel.impl.setGroupTransform(this, this.transform);
	}

	/**
	 * Set the opacity.
	 * 
	 * @param opacity
	 */
	public void setOpacity(double opacity) {
		this.opacity = opacity;
		DrawPanel.impl.setGroupOpacity(this, this.opacity);
	}

	@Override
	public Element getElement_drawable() {
		return getElement();
	}
}
