package edu.ucsc.ib.client.netviz;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.user.client.DOM;

import edu.ucsc.ib.client.Stats;
import edu.ucsc.ib.client.datapanels.BiodeInfoDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.viewcontrols.CircleMapDialogBox;
import edu.ucsc.ib.drawpanel.client.Image;

/**
 * Handler for various mouse events on BasicNode objects.
 * 
 * @author cw
 * 
 */
public class BasicNodeMouseEventsHandler implements MouseDownHandler,
		MouseMoveHandler, MouseUpHandler, DoubleClickHandler {

	private static final int CORRECTION_Y = 4;
	private static final int CORRECTION_X = 4;

	public static Request request = null;

	@Override
	public void onMouseDown(MouseDownEvent event) {
		BasicNode node = (BasicNode) event.getSource();

		DOM.setCapture(node.getElement_drawable());

		node.xOnMouseDown = node.getX_drawable();
		node.yOnMouseDown = node.getY_drawable();

		node.xOrigin = event.getClientX();
		node.yOrigin = event.getClientY();

		node.hasMoved = false;
		node.amDragging = true;
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		BasicNode node = (BasicNode) event.getSource();

		if (node.amDragging) {

			double dx = event.getClientX() - node.xOrigin;
			double dy = event.getClientY() - node.yOrigin;

			// update node position
			node.setPosition_drawable(node.getX_drawable() + dx,
					node.getY_drawable() + dy);

			node.xOrigin = event.getClientX();
			node.yOrigin = event.getClientY();
			node.hasMoved = true;
		}
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		BasicNode node = (BasicNode) event.getSource();

		DOM.releaseCapture(node.getElement_drawable());

		node.amDragging = false;

		double dx = event.getClientX() - node.xOrigin;
		double dy = event.getClientY() - node.yOrigin;

		if (!node.hasMoved) {
			if (node.usingImage && !node.getImageURL().equalsIgnoreCase("")) {
				// node is in image mode (may be circle image)
				Image image = node.imageObject;

				double nodex = node.getX_drawable();
				double nodey = node.getY_drawable();

				// double imgw = image.getWidth_drawable();
				// double imgh = image.getHeight_drawable();
				//
				// double imgx = image.getX_svg();
				double imgy = image.getY_svg();

				double clickx = event.getClientX();
				double clicky = event.getClientY();

				// dx = clickx - nodex - 3;
				// dy = clicky - nodey + imgy - 3;

				double clickedDistance = Stats.euclideanDist(
						(nodex + CORRECTION_X), (nodey - imgy + CORRECTION_Y),
						clickx, clicky);

				LoggingDialogBox.log("clickedDistance:" + clickedDistance);

				// DYNAMIC CIRCLE MAP IMAGE - get new circle images with
				// new orderFeature and with sorting prioritized on the clicked
				// ring

				int numRings = CircleMapDialogBox.getNumDisplayedRings();

				if (numRings == 0) {
					// no rings. also, avoid division by 0
					node.resetToRestingCondition();
					return;
				}

				double fullRadius = node.getSize() * 0.5;

				// get radius for each displayed ring
				ArrayList<Double> ringRadii = CircleMapDialogBox
						.getListOfRingRadii(1, numRings, fullRadius);

				// determine clicked ring
				int clickedRingNumber = determineClickedRing(ringRadii,
						clickedDistance);

				handleRingClick(node, clickedRingNumber, true);

			} else {
				// toggle select state of node
				node.toggleSelected();
			}
		} else {
			// compute new position - within viewport boundary
			double newX = node.getX_drawable() + dx;
			double newY = node.getY_drawable() + dy;

			node.setPositionInVpBounds(newX, newY);

			// if this is a selected node, set the position of other selected
			// nodes
			if (node.isSelected()) {
				LoggingDialogBox
						.log("begin checking for other selected nodes to reposition");

				// compute movement since mouseDown event
				dx = node.getX_drawable() - node.xOnMouseDown;
				dy = node.getY_drawable() - node.yOnMouseDown;

				LoggingDialogBox.log("move: " + dx + "," + dy);
				for (String biode : node.nv.getSelectedNodeIds()) {
					BasicNode selectedNode = node.nv.getNode(biode);
					// don't move this one again
					if (selectedNode != node) {
						selectedNode.setPositionInVpBounds(
								selectedNode.getX_drawable() + dx,
								selectedNode.getY_drawable() + dy);
					}
				}
			}
		}

		node.resetToRestingCondition();
	}

	/**
	 * Handle ring click by requesting new, resorted CircleMap images.
	 * 
	 * @param node
	 * @param clickedRingNumber
	 * @param toggleSelected
	 *            TODO
	 */
	public static void handleRingClick(BasicNode node, int clickedRingNumber,
			boolean toggleSelected) {
		// what to do with clicked ring
		switch (clickedRingNumber) {
		case Integer.MAX_VALUE:
			LoggingDialogBox.log("click out of range");
			break;
		case 0:
			LoggingDialogBox.log("clicked center");
			if (toggleSelected) {
				node.toggleSelected();
			}
			break;
		default:
			LoggingDialogBox.log("click ring number: " + clickedRingNumber);

			// get new circle images

			CircleMapDialogBox.reSortCircleMaps(node.getID(),
					clickedRingNumber, request);
			break;
		}
	}

	/**
	 * Figure out which CircleMap ring was clicked. If some error occurs,
	 * Integer.MAX_VALUE will be returned.
	 * 
	 * @param ringRadii
	 *            list of ring radii, including the center.
	 * @param clickedDistance
	 *            distance from the center of CircleMap where click occurred
	 * @return
	 */
	public static int determineClickedRing(ArrayList<Double> ringRadii,
			double clickedDistance) {
		// edge case: clickedDistance is negative
		if (clickedDistance <= 0) {
			return Integer.MAX_VALUE;
		}

		// edge case: clickedDistance is beyond outermost radius
		if (clickedDistance > ringRadii.get(ringRadii.size() - 1)) {
			return Integer.MAX_VALUE;
		}

		// start checking from center
		// find 1st radius that is larger than clickDistance
		int clickedRing = 0;
		for (double ringRadius : ringRadii) {
			if (ringRadius < clickedDistance) {
				// click occurred within this radius
				// test next bigger ring
				clickedRing++;
				continue;
			} else {
				// click occurred outside this radius
				// code is reachable with clickedRing==0;
				break;
			}
		}

		return clickedRing;
	}

	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		BasicNode node = (BasicNode) event.getSource();

		// bring up the information for the source
		final BiodeInfoDialogBox dialogBox = new BiodeInfoDialogBox(
				node.nv.BIC.getBiodeInfo(node.getID()), node.nv);

		dialogBox.center();

		node.resetToRestingCondition();
	}
}
