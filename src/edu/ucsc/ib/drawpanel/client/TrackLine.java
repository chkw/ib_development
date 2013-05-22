/**
 * 
 */
package edu.ucsc.ib.drawpanel.client;

import edu.ucsc.ib.client.netviz.Track;

/**
 * This is an object that draws a track's line. It consists of a line and
 * possibly 0, 1, or 2 shapes capping the ends for the purpose of illustrating
 * direction. SVG markers are not used because those can't be changed
 * individually.
 * 
 * @author Chris
 * 
 */
public class TrackLine extends Group {
	public static final String STROKE_DASH_ARRAY_VALUE = "3,3";
	public static final double DEFAULT_STROKE_WIDTH = 2;
	public static final int LINE_STROKE_OPACITY = 50;
	public static final int PLAIN = 0;
	public static final int ARROW = 1;
	public static final int BAR = 2;

	/**
	 * This is the line that is drawn for this object.
	 */
	private final Line line;

	// need to use SVG group in order to rotate and otherwise reposition
	private final Group headGroup;
	private final Group tailGroup;

	private final Shape headCap;
	private final Shape tailCap;

	// TODO /////////////////////////////////////////////////

	/**
	 * Constructor.
	 * 
	 * @param titleText
	 * @param color
	 * @param length
	 * @param vOffset
	 * @param head
	 * @param tail
	 * @param isDashed
	 * 
	 */
	public TrackLine(String titleText, String color, double length,
			double vOffset, int head, int tail, boolean isDashed) {
		// draw a line
		this.line = new Line(0, vOffset, length, vOffset);
		this.line.setStroke(color);
		this.line.setStrokeWidth(DEFAULT_STROKE_WIDTH);

		// this is for the tooltip
		this.add(new Title(titleText));

		this.line.setStrokeOpacity(LINE_STROKE_OPACITY);

		if (isDashed) {
			this.line.setStrokeDashArray(STROKE_DASH_ARRAY_VALUE);
		}

		this.add(line);

		// head direction
		switch (head) {
		case TrackLine.ARROW:
			this.headGroup = new Group();
			this.headCap = new Shape(Shape.TRIANGLE, 0, vOffset, 5);
			this.headCap.setStrokeWidth(1);
			this.headCap.setStroke(color);
			this.headCap.setFill(color);
			this.headGroup.add(this.headCap);
			this.add(this.headGroup);
			break;
		case TrackLine.BAR:
			this.headGroup = new Group();
			this.headCap = new Shape(Shape.RECT, 0, vOffset, 5, 1);
			this.headCap.setStrokeWidth(1);
			this.headCap.setStroke(color);
			this.headCap.setFill(color);
			this.headGroup.add(this.headCap);
			this.add(this.headGroup);
			break;
		default: // no head
			this.headGroup = null;
			this.headCap = null;
			break;
		}

		// tail direction
		switch (tail) {
		case TrackLine.ARROW:
			this.tailGroup = new Group();
			this.tailCap = new Shape(Shape.TRIANGLE, 0, vOffset, 5);
			this.tailCap.setStrokeWidth(1);
			this.tailCap.setStroke(color);
			this.tailCap.setFill(color);
			this.tailGroup.add(this.tailCap);
			this.add(this.tailGroup);
			break;
		case TrackLine.BAR:
			this.tailGroup = new Group();
			this.tailCap = new Shape(Shape.RECT, 0, vOffset, 5, 1);
			this.tailCap.setStrokeWidth(1);
			this.tailCap.setStroke(color);
			this.tailCap.setFill(color);
			this.tailGroup.add(this.tailCap);
			this.add(this.tailGroup);
			break;
		default: // no tail
			this.tailGroup = null;
			this.tailCap = null;
			break;
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param t
	 * @param length
	 * @param vOffset
	 * @param head
	 * @param tail
	 * @param isDashed
	 */
	public TrackLine(Track t, double length, double vOffset, int head,
			int tail, boolean isDashed) {
		this(t.getName(), t.getColor(), length, vOffset, head, tail, isDashed);
	}

	/**
	 * Get the stroke width for the Line object.
	 * 
	 * @return
	 */
	public double getStrokeWidth() {
		return this.line.getStrokeWidth();
	}

	/**
	 * Set the vertical offset. This is to account for having many tracks in the
	 * NetworkEdgeGroup.
	 * 
	 * @param vOffset
	 */
	public void setVertOffset(double vOffset) {
		this.line.setFromY(vOffset);
		this.line.setToY(vOffset);
	}

	/**
	 * Get the vertical offset.
	 * 
	 * @return
	 */
	public double getVertOffset() {
		// line.getToY() would return the same result
		return this.line.getY_drawable();
	}

	/**
	 * Reposition the elements in this TrackLine group. Intended to keep the
	 * TrackLine from drawing past the NetworkNode boundary.
	 * 
	 * @param shortenHead
	 * @param drawingLength
	 */
	public void changeLength(double shortenHead, double drawingLength) {
		// change position of end of line
		this.line.setFromX(shortenHead + 5);
		this.line.setToX(drawingLength - 5);

		// change the position of headCap and/or tailCap
		StringBuffer strBuf;
		if (this.headCap != null) {
			strBuf = new StringBuffer();
			strBuf.append("rotate(90 " + this.headCap.getX_drawable() + " "
					+ this.headCap.getY_drawable() + ")");
			strBuf.append(" translate(0 "
					+ (-1 * ((-0.5 * this.line.getStrokeWidth()) + this.line
							.getX_drawable())) + ")");
			this.headGroup.setTransform(strBuf.toString());
		}

		if (this.tailCap != null) {
			strBuf = new StringBuffer();
			strBuf.append("rotate(-90 " + this.tailCap.getX_drawable() + " "
					+ this.tailCap.getY_drawable() + ")");
			strBuf.append(" translate(0 " + this.line.getToX() + ")");
			this.tailGroup.setTransform(strBuf.toString());
		}
	}

	/**
	 * Set a new stroke width based on the specified factor. The new stroke
	 * width will be the product of the factor and the default stroke width. The
	 * factor is capped at 9.
	 * 
	 * @param strokeWidthFactor
	 */
	public void setStrokeWidth(double strokeWidthFactor) {
		double factorCap = 9;

		strokeWidthFactor = Math.abs(strokeWidthFactor);

		strokeWidthFactor = (strokeWidthFactor > factorCap) ? factorCap
				: strokeWidthFactor;

		double newStrokeWidth = strokeWidthFactor * DEFAULT_STROKE_WIDTH;

		this.line.setStrokeWidth(newStrokeWidth);
	}

}
