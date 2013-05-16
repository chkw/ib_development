/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.client.Stats;
import edu.ucsc.ib.drawpanel.client.impl.DrawPanelImpl;

/**
 * Panel for drawing shapes, lines, and text.
 */
public class DrawPanel extends ComplexPanel {

	public static DrawPanelImpl impl = new DrawPanelImpl();

	public DrawPanel(String width, String height) {
		setElement(impl.createPanelElement(width, height));
	}

	/**
	 * Add a new DrawWidget to the panel. Only DrawWidgets are allowed, attempts
	 * to add other widgets results in an IllegalArgumentExcetption
	 * 
	 * @param w
	 *            the Widget to add, which must be a DrawWidget
	 * @throws IllegalArgumentException
	 *             when the Widget is not a DrawWidget
	 */
	public void add(Widget w) {
		if (!(w instanceof Drawable)) {
			throw new IllegalArgumentException("Can only add DrawPanel "
					+ "widgets to a DrawPanel");
		} else {
			super.add(w, getElement());
		}
	}

	/**
	 * Add a new DrawWidget to the panel. Only DrawWidgets are allowed, attempts
	 * to add other widgets results in an IllegalArgumentExcetption
	 * 
	 * @param w
	 *            the Widget to add, which must be a DrawWidget
	 * @param x
	 *            the double x coordinate at which to place the widget
	 * @param y
	 *            the double y coordinate at which to place the widget
	 * @throws IllegalArgumentException
	 *             when the Widget is not a DrawWidget
	 */
	public void add(Widget w, double x, double y) {
		add(w);
		Drawable dw = (Drawable) w;
		dw.setPosition_drawable(x, y);
	}

	/**
	 * Get the DrawPanel's width.
	 * 
	 * @return
	 */
	public double getWidth() {
		return impl.getDrawPanelWidth(this);
	}

	/**
	 * Get the DrawPanel's height.
	 * 
	 * @return
	 */
	public double getHeight() {
		return impl.getDrawPanelHeight(this);
	}

	/**
	 * Get the DrawPanel's SVG width.
	 * 
	 * @return
	 */
	public double getSVGWidth() {
		return impl.getSVGWidthForDP(this);
	}

	/**
	 * Get the DrawPanel's SVG height.
	 * 
	 * @return
	 */
	public double getSVGHeight() {
		return impl.getSVGHeightForDP(this);
	}

	public boolean remove(Widget widget) {
		if (super.remove(widget)) {
			return true;
		}

		return false;
	}

	/**
	 * Get a color from a gradient. The RGB values of the color code are forced
	 * to be between the min and max.
	 * 
	 * @param percent
	 * @param minR
	 * @param minG
	 * @param minB
	 * @param maxR
	 * @param maxG
	 * @param maxB
	 * @return An RGB color code that can be used as SVG color - "rgb(r, g, b)"
	 */
	public static String gradientLevel(double percent, int minR, int minG,
			int minB, int maxR, int maxG, int maxB) {

		int r;

		if (percent < 0) {
			r = minR;
		} else if (percent > 1) {
			r = maxR;
		} else {
			r = (int) Stats.linearInterpolation(percent,
					new Double(minR).doubleValue(),
					new Double(maxR).doubleValue());
		}

		int g;

		if (percent < 0) {
			g = minG;
		} else if (percent > 1) {
			g = maxG;
		} else {
			g = (int) Stats.linearInterpolation(percent,
					new Double(minG).doubleValue(),
					new Double(maxG).doubleValue());
		}

		int b;

		if (percent < 0) {
			b = minB;
		} else if (percent > 1) {
			b = maxB;
		} else {
			b = (int) Stats.linearInterpolation(percent,
					new Double(minB).doubleValue(),
					new Double(maxB).doubleValue());
		}

		return "rgb(" + r + ", " + g + ", " + b + ")";
	}
}
