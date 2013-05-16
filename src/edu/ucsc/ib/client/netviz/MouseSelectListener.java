/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.datapanels.MousePanelListener;
import edu.ucsc.ib.drawpanel.client.Shape;

/**
 * A child class of MouseListenerAdapter to detect dragging and clicking.
 */
public class MouseSelectListener implements MouseDownHandler, MouseMoveHandler,
		MouseUpHandler {

	private final MousePanelListener listener;

	/**
	 * define boundaries of selection
	 */
	private final Shape selectingBox;

	private boolean amDragging = false;
	private int x1;
	private int y1;
	private int x2;
	private int y2;
	private int originX;
	private int originY;

	public MouseSelectListener(MousePanelListener listener) {
		super();
		this.listener = listener;
		selectingBox = new Shape(Shape.RECT);
		selectingBox.setFill("LightGrey");
		selectingBox.setStroke("black");

		// this.selectingBox.setStrokeWidth(5);
	}

	/**
	 * log the selection coordinates. for debugging use.
	 */
	private void logSelectCoords() {
		LoggingDialogBox.log("(" + x1 + "," + y1 + ") (" + x2 + "," + y2
				+ ") coords within the select layer.");
	}

	/**
	 * Calculate and set the properties for visualization of the selecting box.
	 * 
	 */
	private void updateSelectingBox() {
		selectingBox.setSize(Math.abs(x2 - x1));
		selectingBox.setSizeY(Math.abs(y2 - y1));
		selectingBox.setPosition_drawable(originX + Math.rint((x2 - x1) / 2), originY
				+ Math.rint((y2 - y1) / 2));
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		// LoggingDialogBox.log("MouseSelectListener mouse down");

		UIObject object = (UIObject) event.getSource();
		DOM.setCapture(object.getElement());

		if (!amDragging) {
			amDragging = true;
			int x = event.getX();
			int y = event.getY();

			x1 = x;
			y1 = y;
			x2 = x;
			y2 = y;
			originX = x;
			originY = y;
			updateSelectingBox();
			listener.addSelectBox(selectingBox);
		} else {
			x2 = event.getX();
			y2 = event.getY();
			mouseUpAction();
		}
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {

		if (amDragging) {
			// LoggingDialogBox.log("MouseSelectListener mouse dragging");
			x2 = event.getX();
			y2 = event.getY();
			updateSelectingBox();
		} else {
			// do nothing
		}
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		// LoggingDialogBox.log("MouseSelectListener mouse up");

		x2 = event.getX();
		y2 = event.getY();

		UIObject object = (UIObject) event.getSource();
		DOM.releaseCapture(object.getElement());
		mouseUpAction();
	}

	/**
	 * Actions for mouse up event.
	 */
	private void mouseUpAction() {
		logSelectCoords();
		listener.selectInBox(x1, y1, x2, y2);
		selectingBox.removeFromParent();
		amDragging = false;
	}
}
