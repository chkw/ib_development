/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.drawpanel.client.impl;

import com.google.gwt.user.client.Element;

import edu.ucsc.ib.drawpanel.client.DrawPanel;
import edu.ucsc.ib.drawpanel.client.Drawable;
import edu.ucsc.ib.drawpanel.client.Group;
import edu.ucsc.ib.drawpanel.client.Image;
import edu.ucsc.ib.drawpanel.client.Line;
import edu.ucsc.ib.drawpanel.client.Shape;
import edu.ucsc.ib.drawpanel.client.Text;

/**
 * The default DrawPanel implementation, which uses SVG.
 */
public class DrawPanelImpl {
	/**
	 * The namespace URI for SVG, <code>http://www.w3.org/2000/svg</code>.
	 */
	private static final String SVG_URI = "http://www.w3.org/2000/svg";

	/**
	 * The namespace URI for <code>xlink</code>,
	 * <code>http://www.w3.org/1999/xlink</code>.
	 * 
	 * This URI can be used to specify a namespace such as
	 * <code>xmlns:xlink="http://www.w3.org/1999/xlink"</code>.
	 */
	private static final String XLINK_URI = "http://www.w3.org/1999/xlink";

	private static final String kPointSep = ",";
	private static final String kCoordsSep = " ";

	/**
	 * Create and return SVG group element.
	 * 
	 * @return
	 */
	public Element createGroupElement() {
		return createElementNS(SVG_URI, "g");
	}

	/**
	 * Create and return SVG title element.
	 * 
	 * @return
	 */
	public Element createTitleElement() {
		return createElementNS(SVG_URI, "title");
	}

	/**
	 * Create and return SVG line element.
	 * 
	 * @return
	 */
	public Element createLineElement() {
		return createElementNS(SVG_URI, "line");
	}

	/**
	 * Create and return an <code>image</code> element in the SVG namespace.
	 * 
	 * @return
	 */
	public Element createImageElement() {
		return createElementNS(SVG_URI, "image");
	}

	/**
	 * Create and return an image element in the SVG namespace with the
	 * specified attributes.
	 * 
	 * @param url
	 *            value for <code>xlink:href</code> attribute
	 * @param height
	 *            value for <code>height</code> attribute
	 * @param width
	 *            value for <code>width</code> attribute
	 * @param x
	 *            value for <code>x</code> attribute
	 * @param y
	 *            value for <code>y</code> attribute
	 * @return
	 */
	public Element createImageElement(String url, double height, double width,
			double x, double y) {
		Element imageElement = createImageElement();
		this.setAttribute(imageElement, DrawPanelImpl.XLINK_URI, "xlink:href",
				url);
		this.setAttribute(imageElement, null, "x", String.valueOf(x));
		this.setAttribute(imageElement, null, "y", String.valueOf(y));

		if (height > 0) {
			this.setAttribute(imageElement, null, "height",
					String.valueOf(height));

		}
		if (width > 0) {
			this.setAttribute(imageElement, null, "width",
					String.valueOf(width));
		}

		return imageElement;
	}

	/**
	 * Create the panel element and return it. The list of unit identifiers in
	 * SVG matches the list of unit identifiers in CSS: em, ex, px, pt, pc, cm,
	 * mm, in and percentages.
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public Element createPanelElement(String width, String height) {
		// root SVG element (SVG tag)
		Element e = createElementNS(SVG_URI, "svg");

		e.setAttribute("xmlns", SVG_URI);
		e.setAttribute("xmlns:xlink", XLINK_URI);

		// TODO for some reason can't set namespace like this:
		// results in error:
		// An attempt was made to create or change an object
		// in a way which is incorrect with regard to namespaces" code: "14
		// this.setAttribute(e, null, "xmlns", SVG_URI);
		// this.setAttribute(e, null, "xmlns:xlink", XLINK_URI);

		// moving back to using setAttributeNS, the better way to do it because
		// the namespace is specified.
		this.setAttribute(e, null, "width", width);
		this.setAttribute(e, null, "height", height);

		// not using markers... keeping this here for record-keeping purpose
		// This is the path for the markers
		// Element path = createElementNS(SVG_URI, "path");
		// setAttribute(path, "d", "M 0 0 L 10 5 L 0 10 z");
		// setAttribute(path, "stroke", "brown");
		// setAttribute(path, "fill", "orange");
		//
		// Element path2 = createElementNS(SVG_URI, "path");
		// setAttribute(path2, "d", "M 0 5 L 10 10 L 10 0 z");
		// setAttribute(path2, "stroke", "orange");
		// setAttribute(path2, "fill", "brown");
		//
		// // triangle marker
		// Element marker1 = createElementNS(SVG_URI, "marker");
		// setAttribute(marker1, "id", "Triangle");
		// setAttribute(marker1, "viewBox", "0 0 10 10");
		// setAttribute(marker1, "refX", "0");
		// setAttribute(marker1, "refY", "5");
		// setAttribute(marker1, "markerUnits", "strokeWidth");
		// setAttribute(marker1, "markerWidth", "4");
		// setAttribute(marker1, "markerHeight", "3");
		// setAttribute(marker1, "orient", "auto");
		// DOM.appendChild(marker1, path);
		//
		// // reverse triangle marker
		// Element marker2 = createElementNS(SVG_URI, "marker");
		// setAttribute(marker2, "id", "reverseTriangle");
		// setAttribute(marker2, "viewBox", "0 0 10 10");
		// setAttribute(marker2, "refX", "10");
		// setAttribute(marker2, "refY", "5");
		// setAttribute(marker2, "markerUnits", "strokeWidth");
		// setAttribute(marker2, "markerWidth", "4");
		// setAttribute(marker2, "markerHeight", "3");
		// setAttribute(marker2, "orient", "auto");
		// DOM.appendChild(marker2, path2);
		//
		// Element defs = createElementNS(SVG_URI, "defs");
		// DOM.appendChild(defs, marker1);
		// DOM.appendChild(defs, marker2);
		//
		// DOM.appendChild(e, defs);

		return e;
	}

	/**
	 * Convert the element's width and height into pixel units.
	 * 
	 * @param e
	 */
	private native void convertWidthHeightToPixel(Element e) /*-{
																e.width.baseVal.convertToSpecifiedUnits(e.width.baseVal.SVG_LENGTHTYPE_PX);
																e.height.baseVal.convertToSpecifiedUnits(e.height.baseVal.SVG_LENGTHTYPE_PX);
																}-*/;

	public Element createShapeElement(int type) {
		switch (type) {
		case Shape.CIRCLE:
			return this.createElementNS(SVG_URI, "circle");
		case Shape.RECT:
			return this.createElementNS(SVG_URI, "rect");
		case Shape.ELLIPSE:
			return this.createElementNS(SVG_URI, "ellipse");
		case Shape.POLYLINE:
		case Shape.ARROW:
			return this.createElementNS(SVG_URI, "polyline");
		case Shape.POLYGON:
		case Shape.TRIANGLE:
		case Shape.DIAMOND:
		case Shape.PIPE:
			return this.createElementNS(SVG_URI, "polygon");
		default:
		}
		return null;
	}

	/**
	 * Create an SVG text element.
	 * 
	 * @return
	 */
	public Element createTextElement() {
		Element e = this.createElementNS(SVG_URI, "text");
		this.setAttribute(e, null, "text-anchor", "middle");

		// attributes for setting high-contrast text
		// seems to be more effective for larger size text
		// this.setAttribute(e, null, "fill", "black");
		// this.setAttribute(e, null, "stroke", "white");
		// this.setAttribute(e, null, "stroke-width", "0.5");
		return e;
	}

	public double getComputedTextLength(Text t) {
		return getComputedTextLength(t.getElement_drawable());
	}

	public void setFill(Shape s, String fill) {
		this.setAttribute(s.getElement_drawable(), null, "fill", fill);
	}

	public void setFillOpacity(Shape s, double opacity) {
		this.setAttribute(s.getElement_drawable(), null, "fill-opacity", ""
				+ (opacity / 100));
	}

	public void setStrokeOpacity(Shape s, double opacity) {
		this.setAttribute(s.getElement_drawable(), null, "stroke-opacity", ""
				+ (opacity / 100));
	}

	public void setGroupID(Group g, String id) {
		// g.getElement().setId(id);
		this.setAttribute(g.getElement_drawable(), null, "id", id);
	}

	/**
	 * Set the transform element to the x,y coords. Groups don't have x and y
	 * attributes. set position with transform.
	 * 
	 * @param g
	 * @param x
	 * @param y
	 */
	public void setGroupPosition(Group g, double x, double y) {
		this.setGroupTransform(g, "translate(" + x + ", " + y + ")");
	}

	/**
	 * Set the transform attribute for a Group.
	 * 
	 * @param g
	 * @param transform
	 */
	public void setGroupTransform(Group g, String transform) {
		this.setAttribute(g.getElement_drawable(), null, "transform", transform);
	}

	/**
	 * Set the opacity for the group.
	 * 
	 * @param g
	 * @param groupOpacity
	 */
	public void setGroupOpacity(Group g, double groupOpacity) {
		// set the group opacity
		this.setAttribute(g.getElement_drawable(), null, "opacity", ""
				+ (groupOpacity / 100));
	}

	public void setLineFrom(Line l, double x, double y) {
		// set the from location to x,y
		this.setAttribute(l.getElement_drawable(), null, "x1", "" + x);
		this.setAttribute(l.getElement_drawable(), null, "y1", "" + y);
	}

	public void setLineTo(Line l, double x, double y) {
		// set the to location to x,y
		this.setAttribute(l.getElement_drawable(), null, "x2", "" + x);
		this.setAttribute(l.getElement_drawable(), null, "y2", "" + y);
	}

	public void setLineToX(Line l, double x) {
		// set the to location to x
		this.setAttribute(l.getElement_drawable(), null, "x2", "" + x);
	}

	public void setLineToY(Line l, double y) {
		// set the to location to y
		this.setAttribute(l.getElement_drawable(), null, "y2", "" + y);
	}

	public void setLineTitle(Line l, String title) {
		// set the title. this is the tooltip for the line
		this.setAttribute(l.getElement_drawable(), null, "title", title);
	}

	public void setLineStrokeOpacity(Line l, double strokeOpacity) {
		// set the stroke opacity
		this.setAttribute(l.getElement_drawable(), null, "stroke-opacity", ""
				+ (strokeOpacity / 100));
	}

	/**
	 * Set the attribute for stroke-dasharray. Reference the <a href=
	 * 'http://www.w3.org/TR/SVG/painting.html#StrokeDasharrayProperty'>SVG
	 * spec</a>.
	 * 
	 * @param l
	 * @param strokeDashArray
	 */
	public void setStrokeDashArray(Line l, String strokeDashArray) {
		this.setAttribute(l.getElement_drawable(), null, "stroke-dasharray",
				strokeDashArray);
	}

	/**
	 * Use a marker at the end of the line. Sets the "marker-end".
	 * 
	 * @param l
	 */
	public void setMarkerEnd(Line l) {
		this.setAttribute(l.getElement_drawable(), null, "marker-end",
				"url(#Triangle)");
	}

	/**
	 * Use a marker at the beginning of the line. Sets the "marker-start".
	 * 
	 * @param l
	 */
	public void setMarkerStart(Line l) {
		this.setAttribute(l.getElement_drawable(), null, "marker-start",
				"url(#reverseTriangle)");
	}

	public void setShapePosition(Shape s, double x, double y) {
		switch (s.getType()) {
		case Shape.CIRCLE:
			this.setAttribute(s.getElement_drawable(), null, "cx", "" + x);
			this.setAttribute(s.getElement_drawable(), null, "cy", "" + y);
			break;
		case Shape.RECT:
			this.setAttribute(s.getElement_drawable(), null, "x",
					"" + (x - s.getWidth_drawable() / 2));
			this.setAttribute(s.getElement_drawable(), null, "y",
					"" + (y - s.getHeight_drawable() / 2));
			this.setAttribute(s.getElement_drawable(), null, "width",
					"" + s.getWidth_drawable());
			this.setAttribute(s.getElement_drawable(), null, "height",
					"" + s.getHeight_drawable());
			break;
		case Shape.TRIANGLE:
			this.setTrianglePoints(s, x, y, s.getWidth_drawable(),
					s.getHeight_drawable());
			break;
		case Shape.ARROW:
			this.setArrowPoints(s, x, y, s.getWidth_drawable(),
					s.getHeight_drawable());
			break;
		case Shape.DIAMOND:
			this.setDiamondPoints(s, x, y, s.getWidth_drawable(),
					s.getHeight_drawable());
			break;
		case Shape.PIPE:
			this.setPipePoints(s, x, y, s.getWidth_drawable(),
					s.getHeight_drawable());
			break;
		}
	}

	/**
	 * Similar to setShapePosition, except for use with Image elements.
	 * 
	 * @param i
	 * @param x
	 * @param y
	 */
	public void setImagePosition(Image i, double x, double y) {
		this.setAttribute(i.getElement_drawable(), null, "x",
				"" + (x - i.getWidth_drawable() / 2));
		this.setAttribute(i.getElement_drawable(), null, "y",
				"" + (y - i.getHeight_drawable() / 2));
		this.setAttribute(i.getElement_drawable(), null, "width",
				"" + i.getWidth_drawable());
		this.setAttribute(i.getElement_drawable(), null, "height",
				"" + i.getHeight_drawable());
	}

	/**
	 * Create a "point down" triangle. The upper two points are the "inputs",
	 * the lower point will be the "output"
	 * 
	 * @param triangle
	 *            the shape object
	 * @param x
	 *            x coordinate of the center of the triangle
	 * @param y
	 *            y coordinate of the center of the triangle
	 * @param width
	 *            width of the triangle
	 * @param height
	 *            height of the triangle
	 */
	private void setTrianglePoints(Shape triangle, double x, double y,
			double width, double height) {
		StringBuffer points = new StringBuffer();

		width = width / 2;
		height = height / 2;

		// Bottom point
		points.append(x);
		points.append(kPointSep);
		points.append(y + height);
		points.append(kCoordsSep);

		double upper = y - height;
		// Upper left point
		points.append(x - width);
		points.append(kPointSep);
		points.append(upper);
		points.append(kCoordsSep);

		// Upper right point
		points.append(x + width);
		points.append(kPointSep);
		points.append(upper);

		this.setAttribute(triangle.getElement_drawable(), null, "points",
				points.toString());
	}

	/**
	 * Create a "point down" arrow.
	 * 
	 * @param arrow
	 *            the shape object
	 * @param x
	 *            x coordinate of the start of the arrow
	 * @param y
	 *            y coordinate of the start of the arrow
	 * @param width
	 *            width of the box that contains the arrow. Negative to send the
	 *            arrow to the left
	 * @param height
	 *            height of the box that contains the arrow. < 0 to have the
	 *            arrow point up
	 */
	private void setArrowPoints(Shape arrow, double x, double y, double width,
			double height) {
		StringBuffer points = new StringBuffer();

		// Start point
		points.append(x);
		points.append(kPointSep);
		points.append(y);
		points.append(kCoordsSep);

		double endX = x + width;
		double endY = y + height;

		// End point
		points.append(endX);
		points.append(kPointSep);
		points.append(endY);
		points.append(kCoordsSep);

		double[][] heads = calcArrowHeads(endX, endY, width, height);

		// +30 leg of arrow head: Out
		points.append(heads[0][0]);
		points.append(kPointSep);
		points.append(heads[0][1]);
		points.append(kCoordsSep);

		// Cross bar of arrow head to -30
		points.append(heads[1][0]);
		points.append(kPointSep);
		points.append(heads[1][1]);
		points.append(kCoordsSep);

		// Close arrow head
		points.append(endX);
		points.append(kPointSep);
		points.append(endY);

		this.setAttribute(arrow.getElement_drawable(), null, "points",
				points.toString());
	}

	private double[][] calcArrowHeads(double x, double y, double width,
			double height) {
		double[][] heads = new double[2][];
		for (int i = 0; i < 2; ++i)
			heads[i] = new double[2];

		double arrowLen = 10.0;

		double angle, offset, curAngle;

		angle = Math.atan(height / width);
		offset = Math.PI / 6.0; // 30 degrees
		if (width >= 0.0)
			angle -= Math.PI;

		curAngle = angle + offset;
		heads[0][0] = (arrowLen * Math.cos(curAngle)) + x;
		heads[0][1] = (arrowLen * Math.sin(curAngle)) + y;

		curAngle = angle - offset;
		heads[1][0] = (arrowLen * Math.cos(curAngle)) + x;
		heads[1][1] = (arrowLen * Math.sin(curAngle)) + y;

		return heads;
	}

	/**
	 * Create a diamond. The upper point will be the "input", the lower point
	 * will be the "output"
	 * 
	 * @param diamond
	 *            the shape object
	 * @param x
	 *            x coordinate of the center of the diamond
	 * @param y
	 *            y coordinate of the center of the diamond
	 * @param width
	 *            width of the diamond
	 * @param height
	 *            height of the diamond
	 */
	private void setDiamondPoints(Shape diamond, double x, double y,
			double width, double height) {
		StringBuffer points = new StringBuffer();

		width = width / 2;
		height = height / 2;

		// Bottom point
		points.append(x);
		points.append(kPointSep);
		points.append(y + height);
		points.append(kCoordsSep);

		// Left side point
		points.append(x - width);
		points.append(kPointSep);
		points.append(y);
		points.append(kCoordsSep);

		// Top point
		points.append(x);
		points.append(kPointSep);
		points.append(y - height);
		points.append(kCoordsSep);

		// Right side point
		points.append(x + width);
		points.append(kPointSep);
		points.append(y);

		this.setAttribute(diamond.getElement_drawable(), null, "points",
				points.toString());
	}

	/**
	 * Create a pipe. The upper point will be the "input", the lower point will
	 * be the "output" Has not actually been done. Will do if need a pipe that
	 * isn't vertical Currently this makes a diamond
	 * 
	 * @param pipe
	 *            the shape object
	 * @param x
	 *            x coordinate of the center of the pipe
	 * @param y
	 *            y coordinate of the center of the pipe
	 * @param width
	 *            width of the pipe's bounding box
	 * @param height
	 *            height of the pipe's bounding box
	 */
	private void setPipePoints(Shape pipe, double x, double y, double width,
			double height) {
		StringBuffer points = new StringBuffer();

		width = width / 2;
		height = height / 2;

		// Bottom point
		points.append(x);
		points.append(kPointSep);
		points.append(y + height);
		points.append(kCoordsSep);

		// Left side point
		points.append(x - width);
		points.append(kPointSep);
		points.append(y);
		points.append(kCoordsSep);

		// Top point
		points.append(x);
		points.append(kPointSep);
		points.append(y - height);
		points.append(kCoordsSep);

		// Right side point
		points.append(x + width);
		points.append(kPointSep);
		points.append(y);

		this.setAttribute(pipe.getElement_drawable(), null, "points",
				points.toString());
	}

	/**
	 * Note, should be called setShapeWidth, since that's what it does
	 * 
	 * @param s
	 *            The shape to change
	 * @param size
	 *            new width
	 */
	public void setShapeSize(Shape s, double size) {
		switch (s.getType()) {
		case Shape.CIRCLE:
			this.setAttribute(s.getElement_drawable(), null, "r", ""
					+ (size / 2));
			break; // GTD 7/26/07 Circles don't have widths, so added break;
		case Shape.RECT:
			this.setAttribute(s.getElement_drawable(), null, "width", "" + size);
			break;
		case Shape.TRIANGLE:
			this.setTrianglePoints(s, s.getX_drawable(), s.getY_drawable(),
					size, s.getHeight_drawable());
			break;
		case Shape.ARROW:
			this.setArrowPoints(s, s.getX_drawable(), s.getY_drawable(), size,
					s.getHeight_drawable());
			break;
		case Shape.DIAMOND:
			this.setDiamondPoints(s, s.getX_drawable(), s.getY_drawable(),
					size, s.getHeight_drawable());
			break;
		case Shape.PIPE:
			this.setPipePoints(s, s.getX_drawable(), s.getY_drawable(), size,
					s.getHeight_drawable());
			break;
		}
	}

	/**
	 * Similar to setShapeSize, except with Image instead.
	 * 
	 * @param i
	 *            The shape to change
	 * @param size
	 *            new width
	 */
	public void setImageSize(Image i, double size) {
		this.setAttribute(i.getElement_drawable(), null, "width", "" + size);
	}

	public void setShapeDims(Shape shape, double x, double y, double width,
			double height) {
		switch (shape.getType()) {
		case Shape.CIRCLE:
			this.setAttribute(shape.getElement_drawable(), null, "cx", "" + x);
			this.setAttribute(shape.getElement_drawable(), null, "cy", "" + y);
			this.setAttribute(shape.getElement_drawable(), null, "r", ""
					+ (width / 2));
			break;

		case Shape.RECT:
			this.setAttribute(shape.getElement_drawable(), null, "x", ""
					+ (x - (width / 2)));
			this.setAttribute(shape.getElement_drawable(), null, "y", ""
					+ (y - (height / 2)));
			this.setAttribute(shape.getElement_drawable(), null, "width", ""
					+ width);
			this.setAttribute(shape.getElement_drawable(), null, "height", ""
					+ height);
			break;

		case Shape.TRIANGLE:
			this.setTrianglePoints(shape, x, y, width, height);
			break;

		case Shape.ARROW:
			this.setArrowPoints(shape, x, y, width, height);
			break;

		case Shape.DIAMOND:
			this.setDiamondPoints(shape, x, y, width, height);
			break;

		case Shape.PIPE:
			this.setPipePoints(shape, x, y, shape.getWidth_drawable(),
					shape.getHeight_drawable());
			break;
		}
	}

	/**
	 * Similar to setShapeDims, except with Image instead.
	 * 
	 * @param i
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void setImageDims(Image i, double x, double y, double width,
			double height) {
		this.setAttribute(i.getElement_drawable(), null, "x", ""
				+ (x - (width / 2)));
		this.setAttribute(i.getElement_drawable(), null, "y", ""
				+ (y - (height / 2)));
		this.setAttribute(i.getElement_drawable(), null, "width", "" + width);
		this.setAttribute(i.getElement_drawable(), null, "height", "" + height);
	}

	public void setShapeHeight(Shape s, double height) {
		switch (s.getType()) {
		case Shape.CIRCLE:
			break; // GTD 7/26/07 Circles don't have heights

		case Shape.RECT:
			this.setAttribute(s.getElement_drawable(), null, "height", ""
					+ height);
			break;

		case Shape.TRIANGLE:
			this.setTrianglePoints(s, s.getX_drawable(), s.getY_drawable(),
					s.getWidth_drawable(), height);
			break;

		case Shape.ARROW:
			this.setArrowPoints(s, s.getX_drawable(), s.getY_drawable(),
					s.getWidth_drawable(), height);
			break;

		case Shape.DIAMOND:
			this.setDiamondPoints(s, s.getX_drawable(), s.getY_drawable(),
					s.getWidth_drawable(), height);
			break;

		case Shape.PIPE:
			this.setPipePoints(s, s.getX_drawable(), s.getY_drawable(),
					s.getWidth_drawable(), height);
			break;
		}
	}

	/**
	 * Similar to setShapeHeight, except with Image instead.
	 * 
	 * @param i
	 * @param height
	 */
	public void setImageHeight(Image i, double height) {
		this.setAttribute(i.getElement_drawable(), null, "height", "" + height);
	}

	public void setShapeStroke(Shape s, String stroke) {
		this.setAttribute(s.getElement_drawable(), null, "stroke", stroke);
	}

	public void setShapeStrokeWidth(Shape s, double strokeWidth) {
		this.setAttribute(s.getElement_drawable(), null, "stroke-width", ""
				+ strokeWidth);
	}

	public void setShapeTitle(Shape s, String title) {
		this.setAttribute(s.getElement_drawable(), null, "title", title);
	}

	/**
	 * Same as setShapeTitle, except for Image instead of SVG shape.
	 * 
	 * @param i
	 * @param title
	 */
	public void setImageTitle(Image i, String title) {
		this.setAttribute(i.getElement_drawable(), null, "title", title);
	}

	public void setStroke(Line l, String stroke) {
		// set the to location to x,y
		this.setAttribute(l.getElement_drawable(), null, "stroke", stroke);
	}

	public void setStrokeWidth(Line l, double strokeWidth) {
		// set the to location to x,y
		this.setAttribute(l.getElement_drawable(), null, "stroke-width", ""
				+ strokeWidth);
	}

	public void setTextPosition(Text t, double x, double y) {
		Element e = t.getElement_drawable();
		this.setAttribute(e, null, "x", x + "px");
		this.setAttribute(e, null, "y", y + "px");
	}

	public double getBBoxWidthForDP(Drawable d) {
		return getBBoxWidth(d.getElement_drawable());
	}

	public double getBBoxHeightForDP(Drawable d) {
		return getBBoxHeight(d.getElement_drawable());
	}

	private native double getBBoxWidth(Element e) /*-{
													return e.getBBox().width;
													}-*/;

	private native double getBBoxHeight(Element e) /*-{
													return e.getBBox().height;
													}-*/;

	public double getDrawPanelWidth(DrawPanel d) {
		return getBBoxWidth(d.getElement());
	}

	public double getDrawPanelHeight(DrawPanel d) {
		return getBBoxHeight(d.getElement());
	}

	private native double getSVGWidth(Element e) /*-{
													return e.width.baseVal.value;
													}-*/;

	private native double getSVGHeight(Element e) /*-{
													return e.height.baseVal.value;
													}-*/;

	public double getSVGWidthForDP(DrawPanel d) {
		return getSVGWidth(d.getElement());
	}

	public double getSVGHeightForDP(DrawPanel d) {
		return getSVGHeight(d.getElement());
	}

	/**
	 * Create an element with createElementNS(ns, tag)
	 * 
	 * @see <a
	 *      href="https://developer.mozilla.org/en/DOM/document.createElementNS">https://developer.mozilla.org/en/DOM/document.createElementNS</a>
	 * @param ns
	 *            namespaceURI
	 * @param tag
	 *            qualifiedName
	 * @return
	 */
	private native Element createElementNS(String ns, String tag)/*-{
																	return $doc.createElementNS(ns, tag);
																	}-*/;

	private native double getComputedTextLength(Element elem) /*-{
																return elem.getComputedTextLength();
																}-*/;

	/**
	 * Get the offset height for an Element such as a div.
	 * 
	 * @param elem
	 * @return
	 */
	private native double getOffsetHeight(Element elem) /*-{
														return elem.offsetHeight;
														}-*/;

	/**
	 * Get the offset width for an Element such as a div.
	 * 
	 * @param elem
	 * @return
	 */
	private native double getOffsetWidth(Element elem) /*-{
														return elem.offsetWidth;
														}-*/;

	/**
	 * The setAttributeNS() method adds a new attribute (with a namespace). If
	 * an attribute with that name or namespace already exists in the element,
	 * its value is changed to be that of the prefix and value parameter.
	 * 
	 * @see <a
	 *      href="http://www.w3schools.com/dom/met_element_setattributens.asp">http://www.w3schools.com/dom/met_element_setattributens.asp</a>
	 *      and <a
	 *      href="https://developer.mozilla.org/en/SVG/Namespaces_Crash_Course"
	 *      >https://developer.mozilla.org/en/SVG/Namespaces_Crash_Course</a>
	 * 
	 * @param elem
	 * @param namespaceURI
	 * @param attr
	 *            name of attribute
	 * @param value
	 *            value of attribute
	 */
	private native void setAttribute(Element elem, String namespaceURI,
			String attr, String value) /*-{
										elem.setAttributeNS(namespaceURI, attr, value);
										}-*/;

	/**
	 * Set an attribute with the namespace, null.
	 * 
	 * @see <a
	 *      href="http://www.w3schools.com/dom/met_element_setattributens.asp">http://www.w3schools.com/dom/met_element_setattributens.asp</a>
	 *      and <a
	 *      href="https://developer.mozilla.org/en/SVG/Namespaces_Crash_Course"
	 *      >https://developer.mozilla.org/en/SVG/Namespaces_Crash_Course</a>
	 * 
	 * @param elem
	 * @param attr
	 *            name of attribute
	 * @param value
	 *            value of attribute
	 */
	private void setAttribute(Element elem, String attr, String value) {
		this.setAttribute(elem, null, attr, value);
	}

	/**
	 * Get the attribute of an element.
	 * 
	 * @param elem
	 * @param attr
	 * @return
	 */
	public static native String getAttribute(Element elem, String attr) /*-{
																		return elem.getAttribute(attr);
																		}-*/;

	/**
	 * Get the client user agent.
	 * 
	 * @return A String that looks something like: Mozilla/5.0 (Windows; U;
	 *         Windows NT 5.1; en-US; rv:1.9.0.1) Gecko/2008070208 Firefox/3.0.1
	 */
	public static native String getUserAgent() /*-{
												return navigator.userAgent;
												}-*/;
}
