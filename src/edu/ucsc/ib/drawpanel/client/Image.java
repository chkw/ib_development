/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.drawpanel.client.impl.DrawPanelImpl;

/**
 * A shape, that is centered on the specified coordinate.
 */
public class Image extends Widget implements Drawable {

	public static final int OVER = 1;
	public static final int ABOVE = 2;
	public static final int BELOW = 3;
	public static final int LEFT = 4;
	public static final int RIGHT = 5;

	// originally, public static final int DEFAULT_SIZE = 20;
	public static final int DEFAULT_SIZE = 50;

	public static final int DEFAULT_X = 0;

	public static final int DEFAULT_Y = 0;

	private double size;

	private double sizeY;

	private double x;

	private double y;

	private String title;

	private String url;

	/**
	 * Constructor for using default dimensions.
	 * 
	 * @param type
	 */
	public Image(String url) {
		this(url, DEFAULT_X, DEFAULT_Y, DEFAULT_SIZE);
	}

	/**
	 * Constructor where only one size is specified... such as for circles.
	 * 
	 * @param type
	 * @param x
	 * @param y
	 * @param size
	 */
	public Image(String url, double x, double y, double size) {
		this(url, x, y, size, size);
	}

	/**
	 * Constructor where two sizes are specified, height and width. Other
	 * constructors call this one.
	 * 
	 * @param type
	 * @param x
	 * @param y
	 * @param xSize
	 * @param ySize
	 */
	public Image(String url, double x, double y, double xSize, double ySize) {
		this.url = url;
		this.setImage(this.url);
		setDims(x, y, xSize, ySize);
	}

	/**
	 * This is the height of this image within its SVG group.
	 */
	public double getHeight_drawable() {
		return DrawPanel.impl.getBBoxHeightForDP(this);
	}

	/**
	 * This is the value used for the width and height of the image.
	 * 
	 * @return
	 */
	public double getSize() {
		return size;
	}

	/**
	 * This is the width of this image within its SVG group.
	 */
	public double getWidth_drawable() {
		return DrawPanel.impl.getBBoxWidthForDP(this);
	}

	/**
	 * This is the x position of the center of the image within its SVG group.
	 */
	public double getX_drawable() {
		return x;
	}

	/**
	 * This is the y position of the center of the image within its SVG group
	 */
	public double getY_drawable() {
		return y;
	}

	/**
	 * This is the SVG image's x attribute. Note this is not for the SVG group.
	 * 
	 * @return
	 */
	public double getX_svg() {
		String dpx = DrawPanelImpl
				.getAttribute(this.getElement_drawable(), "x");

		Double.valueOf(dpx);

		return Double.valueOf(dpx);
	}

	/**
	 * This is the SVG image's y attribute. Note this is not for the SVG group.
	 * 
	 * @return
	 */
	public double getY_svg() {
		String dpy = DrawPanelImpl
				.getAttribute(this.getElement_drawable(), "y");

		Double.valueOf(dpy);

		return Double.valueOf(dpy);
	}

	/**
	 * For circles, this sets the coordinates for the center.
	 */
	public void setPosition_drawable(double x, double y) {
		this.x = x;
		this.y = y;
		// TODO
		DrawPanel.impl.setImagePosition(this, this.x, this.y);
	}

	/**
	 * Create an SVG image element and set it as this object's element.
	 * 
	 * @param url
	 */
	private void setImage(final String url) {
		this.url = url;
		setElement(DrawPanel.impl.createImageElement(this.url, this.sizeY,
				this.size, DEFAULT_X, DEFAULT_Y));
	}

	/**
	 * For circles, this sets the diameter. For those with 2 size parameters,
	 * sets the "x" size.
	 * 
	 * @param size
	 */
	public void setSize(double size) {
		this.size = size;
		// TODO
		DrawPanel.impl.setImageSize(this, this.size);
	}

	public void setSizeY(double sizeY) {
		this.sizeY = sizeY;
		// TODO
		DrawPanel.impl.setImageHeight(this, this.sizeY);
	}

	public void setDims(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		size = width;
		sizeY = height;
		// TODO
		DrawPanel.impl.setImageDims(this, x, y, size, sizeY);
	}

	/**
	 * Test if the entire shape, or any part of the shape, is in the box defined
	 * by (x1, y1) (x2, y2). Will always return false if x1 >= x2 or y1 >= y2.
	 * This is a quick and dirty verion, that just looks at the bounding
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

		return rectInBox(x1, y1, x2, y2, completely);
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

	public void setTitle(String title) {
		this.title = title;
		// TODO
		DrawPanel.impl.setImageTitle(this, this.title);
	}

	public String getTitle() {
		return this.title;
	}

	@Override
	public Element getElement_drawable() {
		return getElement();
	}
}
