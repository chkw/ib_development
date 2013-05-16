/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * A line on the draw panel.
 */
public class Line extends Widget implements Drawable {
	private double fromX;
	private double fromY;
	private String stroke;
	private double strokeWidth;
	private double strokeOpacity;
	private double toX;
	private double toY;
	private int type;
	private String title;

	/**
	 * Constructor creates an SVG line element and sets its beginning and end
	 * positions.
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public Line(double x1, double y1, double x2, double y2) {
		setElement(DrawPanel.impl.createLineElement());
		this.setPosition_drawable(x1, y1);
		this.setToPosition(x2, y2);
	}

	/**
	 * starting x coord
	 */
	public double getX_drawable() {
		return fromX;
	}

	/**
	 * starting y coord
	 */
	public double getY_drawable() {
		return fromY;
	}

	/**
	 * ending x coord
	 * 
	 * @return
	 */
	public double getToX() {
		return toX;
	}

	/**
	 * ending y coord
	 * 
	 * @return
	 */
	public double getToY() {
		return toY;
	}

	public int getType() {
		return type;
	}

	public double getWidth_drawable() {
		return DrawPanel.impl.getBBoxWidthForDP(this);
	}

	public double getHeight_drawable() {
		return DrawPanel.impl.getBBoxHeightForDP(this);
	}

	public double getStrokeWidth() {
		return this.strokeWidth;
	}

	/**
	 * Put a marker at the end.
	 */
	public void setMarkerEnd() {
		DrawPanel.impl.setMarkerEnd(this);
	}

	/**
	 * Put a marker at the start.
	 */
	public void setMarkerStart() {
		DrawPanel.impl.setMarkerStart(this);
	}

	/**
	 * Set the beginning x,y coords
	 */
	public void setPosition_drawable(double x, double y) {
		this.fromX = x;
		this.fromY = y;
		// set the position in the impl
		DrawPanel.impl.setLineFrom(this, this.fromX, this.fromY);
	}

	/**
	 * set the beginning x coord
	 * 
	 * @param x
	 */
	public void setFromX(double x) {
		this.fromX = x;
		// set the position in the impl
		DrawPanel.impl.setLineFrom(this, this.fromX, this.fromY);
	}

	/**
	 * set the beginning y coord
	 * 
	 * @param y
	 */
	public void setFromY(double y) {
		this.fromY = y;
		// set the position in the impl
		DrawPanel.impl.setLineFrom(this, this.fromX, this.fromY);
	}

	public void setStroke(String stroke) {
		this.stroke = stroke;
		// set the position in the impl
		DrawPanel.impl.setStroke(this, this.stroke);
	}

	public void setStrokeWidth(double strokeWidth) {
		this.strokeWidth = strokeWidth;
		// set the position in the impl
		DrawPanel.impl.setStrokeWidth(this, this.strokeWidth);
	}

	/**
	 * Set the ending x,y coords
	 * 
	 * @param x
	 * @param y
	 */
	public void setToPosition(double x, double y) {
		this.toX = x;
		this.toY = y;
		// set the position in the impl
		DrawPanel.impl.setLineTo(this, this.toX, this.toY);
	}

	/**
	 * set the ending x coord
	 * 
	 * @param x
	 */
	public void setToX(double x) {
		this.toX = x;
		// set the position in the impl
		DrawPanel.impl.setLineToX(this, this.toX);
	}

	/**
	 * set the ending y coord
	 * 
	 * @param y
	 */
	public void setToY(double y) {
		this.toY = y;
		// set the position in the impl
		DrawPanel.impl.setLineToY(this, this.toY);
	}

	public void setType(int type) {
		this.type = type;
		// TODO set the line type in the impl
	}

	public void setStrokeOpacity(double strokeOpacity) {
		this.strokeOpacity = strokeOpacity;
		DrawPanel.impl.setLineStrokeOpacity(this, this.strokeOpacity);
	}

	public void setTitle(String title) {
		this.title = title;
		DrawPanel.impl.setLineTitle(this, this.title);
	}

	public String getTitle() {
		return this.title;
	}

	@Override
	public Element getElement_drawable() {
		return getElement();
	}

	/**
	 * Set the stroke-dasharray property. Refer to <a href=
	 * 'http://www.w3.org/TR/SVG/painting.html#StrokeDasharrayProperty'>SVG
	 * spec</a> for correct value.
	 * 
	 * @param value
	 */
	public void setStrokeDashArray(String value) {
		DrawPanel.impl.setStrokeDashArray(this, value);
	}
}