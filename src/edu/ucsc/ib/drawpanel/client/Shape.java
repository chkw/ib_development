/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * A shape, that is centered on the specified coordinate.
 */
public class Shape extends Widget implements Drawable {
	public static final String[] COLOR_LIST = { "aqua", "black", "blue",
			"fuchsia", "gray", "green", "lime", "maroon", "navy", "olive",
			"purple", "red", "silver", "teal", "white", "yellow" };

	// these shapes come from the vars below
	public static final String[] SHAPE_LIST = { "circle", "rectangle",
			"ellipse", "polyline", "polygon", "triangle", "arrow", "diamond",
			"pipe" };

	public static final int CIRCLE = 0;
	public static final int RECT = 1;
	public static final int ELLIPSE = 2;
	public static final int POLYLINE = 3;
	public static final int POLYGON = 4;
	public static final int TRIANGLE = 5;
	public static final int ARROW = 6;
	public static final int DIAMOND = 7;
	public static final int PIPE = 8;

	public static final int OVER = 1;
	public static final int ABOVE = 2;
	public static final int BELOW = 3;
	public static final int LEFT = 4;
	public static final int RIGHT = 5;

	// originally, public static final int DEFAULT_SIZE = 20;
	public static final int DEFAULT_SIZE = 20;

	public static final int DEFAULT_X = 0;

	public static final int DEFAULT_Y = 0;

	private String fill;

	/**
	 * For circles, 1/2 of this value is used for radius.
	 */
	private double size;

	private double sizeY;

	private static final double FILL_OPACITY = 75;

	private String stroke;

	private double strokeWidth;

	private int type;

	private double x;

	private double y;

	private String title;

	// Ignored by Copy Constructor
	private Text label;
	private int labelLocation;

	// TODO /////////////////////////////////////////////////

	/**
	 * Constructor for using default dimensions.
	 * 
	 * @param type
	 */
	public Shape(int type) {
		this(type, DEFAULT_X, DEFAULT_Y, DEFAULT_SIZE);
	}

	/**
	 * Constructor where only one size is specified... such as for circles.
	 * 
	 * @param type
	 * @param x
	 * @param y
	 * @param size
	 */
	public Shape(int type, double x, double y, double size) {
		this(type, x, y, size, size);
	}

	/**
	 * Constructor where two sizes are specified, height and width.
	 * 
	 * @param type
	 * @param x
	 * @param y
	 * @param xSize
	 * @param ySize
	 */
	public Shape(int type, double x, double y, double xSize, double ySize) {
		setType(type);
		setDims(x, y, xSize, ySize);

		DrawPanel.impl.setFillOpacity(this, FILL_OPACITY);

		DrawPanel.impl.setStrokeOpacity(this, FILL_OPACITY);
	}

	public String getFill() {
		return fill;
	}

	public double getHeight_drawable() {
		return sizeY;
	}

	/**
	 * For circles, this is the diameter.
	 * 
	 * @return
	 */
	public double getSize() {
		return size;
	}

	public int getType() {
		return type;
	}

	/**
	 * For circles, this is the diameter
	 */
	public double getWidth_drawable() {
		return size;
	}

	public double getX_drawable() {
		return x;
	}

	public double getY_drawable() {
		return y;
	}

	public void setFill(String fill) {
		this.fill = fill;
		DrawPanel.impl.setFill(this, fill);
	}

	/**
	 * For circles, this sets the coordinates for the center.
	 */
	public void setPosition_drawable(double x, double y) {
		this.x = x;
		this.y = y;
		DrawPanel.impl.setShapePosition(this, this.x, this.y);
		setLabelPos();
	}

	/**
	 * For circles, this sets the diameter. For those with 2 size parameters,
	 * sets the "x" size.
	 * 
	 * @param size
	 */
	public void setSize(double size) {
		this.size = size;
		if (type == CIRCLE)
			sizeY = size; // Keep both updated
		DrawPanel.impl.setShapeSize(this, this.size);
		setLabelPos();
	}

	public void setSizeY(double sizeY) {
		this.sizeY = sizeY;
		DrawPanel.impl.setShapeHeight(this, this.sizeY);
		setLabelPos();
	}

	public void setDims(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		size = width;
		sizeY = height;
		DrawPanel.impl.setShapeDims(this, x, y, size, sizeY);
		setLabelPos();
	}

	/**
	 * Test if the entire shape, or any part of the shape, is in the box defined
	 * by (x1, y1) (x2, y2). Will always return false if x1 >= x2 or y1 >= y2.
	 * This is a quick and dirty version, that just looks at the bounding
	 * rectangle, rather than the actual shape's points.
	 * 
	 * @param x1
	 *            left edge of bounding box
	 * @param y1
	 *            top edge of bounding box
	 * @param x2
	 *            right edge of bounding box
	 * @param y2
	 *            bottom edge of bounding box
	 * @param completely
	 *            if true shape must be completely encolsed by the bounding box.
	 *            Otherwise just any part of the shape must be within the box.
	 * @return true if in, else false.
	 */
	public boolean inBox(double x1, double y1, double x2, double y2,
			boolean completely) {
		switch (type) {
		case CIRCLE:
		case RECT:
		case TRIANGLE:
		case ARROW:
		case DIAMOND:
		case PIPE:
			return rectInBox(x1, y1, x2, y2, completely);
		}

		return false;
	}

	/**
	 * Test if the entire Rect, or any part of the Rect, is in the box defined
	 * by (x1, y1) (x2, y2). Will always return false if x1 >= x2 or y1 >= y2.
	 * 
	 * @param x1
	 *            left edge of bounding box
	 * @param y1
	 *            top edge of bounding box
	 * @param x2
	 *            right edge of bounding box
	 * @param y2
	 *            bottom edge of bounding box
	 * @param completely
	 *            if true shape must be completely encolsed by the bounding box.
	 *            Otherwise just any part of the shape must be within the box.
	 * @return true if in, else false.
	 */
	private boolean rectInBox(double x1, double y1, double x2, double y2,
			boolean completely) {
		double upper, lower, left, right, vOffset, hOffset;
		boolean inside;

		hOffset = size / 2.0;
		vOffset = sizeY / 2.0;
		if (completely) {
			upper = y - vOffset;
			lower = y + vOffset;
			left = x - hOffset;
			right = x + hOffset;
		} else {
			lower = y - vOffset;
			upper = y + vOffset;
			right = x - hOffset;
			left = x + hOffset;
		}

		inside = (x1 <= left) && (y1 <= upper) && (x2 >= right)
				&& (y2 >= lower);
		return inside;
	}

	public void setStroke(String stroke) {
		this.stroke = stroke;
		DrawPanel.impl.setShapeStroke(this, this.stroke);
	}

	public String getStroke() {
		return this.stroke;
	}

	public void setStrokeWidth(double strokeWidth) {
		this.strokeWidth = strokeWidth;
		DrawPanel.impl.setShapeStrokeWidth(this, this.strokeWidth);
	}

	public double getStrokeWidth() {
		return this.strokeWidth;
	}

	public void setType(int type) {
		this.type = type;
		setElement(DrawPanel.impl.createShapeElement(type));
	}

	public void setTitle(String title) {
		this.title = title;
		DrawPanel.impl.setShapeTitle(this, this.title);
	}

	public String getTitle() {
		return this.title;
	}

	public void removeFromParent() {
		super.removeFromParent();
		if (label != null)
			label.removeFromParent();
	}

	public void changeLabel(String text) {
		if (label == null)
			return;

		if (text == null)
			text = "";

		label.setText(text);
	}

	public String getLabel() {
		if (label == null)
			return null;

		return label.getText();
	}

	public Text setLabel(String text, int location, DrawPanel panel) {
		if (text == null)
			text = "";

		labelLocation = location;

		label = new Text(text);
		setLabelPos();
		panel.add(label);

		return label;
	}

	/**
	 * Routine to update the correct position for label text when an object
	 * moves or is resized.
	 */
	private void setLabelPos() {
		if (label == null)
			return;

		double textX, textY, height;

		textX = textY = 0.0;
		height = 12.0;
		switch (labelLocation) {
		case OVER:
			textX = x;
			textY = y;
			break;

		case ABOVE:
			textX = x;
			// textY = y - (sizeY / 2);
			textY = y - ((sizeY / 2) + height);
			break;

		case BELOW:
			textX = x;
			// textY = y + (sizeY / 2);
			textY = y + ((sizeY / 2) + height);
			break;

		case LEFT:
			textX = x - (size / 2);
			textY = y;
			break;

		case RIGHT:
			textX = x + (size / 2);
			textY = y;
			break;
		}

		label.setPosition_drawable(textX, textY);
	}

	@Override
	public Element getElement_drawable() {
		return getElement();
	}
}
