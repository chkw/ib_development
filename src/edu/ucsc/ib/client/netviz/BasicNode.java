/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseUpHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.drawpanel.client.DrawPanel;
import edu.ucsc.ib.drawpanel.client.Group;
import edu.ucsc.ib.drawpanel.client.Image;
import edu.ucsc.ib.drawpanel.client.Shape;
import edu.ucsc.ib.drawpanel.client.Text;
import edu.ucsc.ib.drawpanel.client.Title;

/**
 * A node in the network. Must have a unique identifier. This uses inheritance
 * rather than composition, as there is no clear way to efficiently have
 * Composite objects respond to events in their contained widget.
 * 
 * Upgrading from GWT 1.4.62 to GWT 2.0.3, it is its own mouse events handler!
 * 
 * There used to be just the NetworkNode class. This class was created, mostly
 * copied over from NetworkNode. Then, NetworkNode was changed to become a child
 * class of this BasicNode class. The change was intended to allow inheritance
 * to implement multiple kind of nodes.
 */
public class BasicNode extends Group implements HasMouseDownHandlers,
		HasMouseUpHandlers, HasMouseMoveHandlers, HasDoubleClickHandlers {

	// //////////// mouse handler vars ///////////////////

	protected boolean amDragging = false;

	/**
	 * The hasMoved variable is used to tell the difference between dragging
	 * events from clicking events. Originally, the clicking events were going
	 * to be handled by an implementation of ClickListener to be called
	 * NetworkNodeMouseSelector. The problem came up in testing where
	 * Firefox2.0.0.3 found clicking to be the same as dragging. Interestingly,
	 * testing on Opera9.10 indicated that dragging IS different from clicking.
	 * To work around, the hasMoved variable was introduced in this class, and
	 * NetworkNodeMouseSelector was not used.
	 */
	protected boolean hasMoved = false;

	/**
	 * previous mouse coordinate (used for dragging node)
	 */
	protected int xOrigin = 0;

	/**
	 * previous mouse coordinate (used for dragging node)
	 */
	protected int yOrigin = 0;

	/**
	 * set on mouseDown event
	 */
	protected double xOnMouseDown = 0;

	/**
	 * set on mouseDown event
	 */
	protected double yOnMouseDown = 0;

	// ////////////////////////////////
	private static final double MAXIMUM_NODE_SIZE = 200;

	protected String DESELECTED_STROKE = "gray";

	protected String DESELECTED_FILL = "silver";

	protected double DESELECTED_STROKEWIDTH = 1;

	protected final String SELECTED_STROKE = "lime";

	protected final String SELECTED_FILL = "cyan";

	protected final double SELECTED_STROKEWIDTH = DESELECTED_STROKEWIDTH + 2;

	/**
	 * Map an NCBI_tax_id to a color.
	 */
	protected static final HashMap<String, String> organismToColorMap = new HashMap<String, String>();
	static {
		organismToColorMap.put("4932", "lime");
		organismToColorMap.put("6239", "fuchsia");
		organismToColorMap.put("9606", "yellow");
		organismToColorMap.put("10090", "wheat");
		organismToColorMap.put("drug", "violet");
	}

	/**
	 * Map a biodespace to a shape code.
	 */
	protected static final HashMap<String, Integer> shapeMap = new HashMap<String, Integer>();

	static {
		shapeMap.put("gene", new Integer(Shape.CIRCLE));
		shapeMap.put("protein", new Integer(Shape.RECT));
		shapeMap.put("chemical", new Integer(Shape.TRIANGLE));
	}

	protected String id;

	protected List<NetworkEdgeGroup> edgeGroups;

	/**
	 * The Shape object for this node.
	 */
	protected Shape shapeObject;

	/**
	 * The Image object for this node.
	 */
	protected Image imageObject;

	/**
	 * The label for this node.
	 */
	protected Text label;

	/**
	 * The title for this node.
	 */
	protected Title title;

	protected int SIZE = Shape.DEFAULT_SIZE;

	protected int SHAPE_OPACITY = 75;

	/**
	 * Attribute for the selected status of this node.
	 */
	protected boolean selected;

	protected final NetworkVisualization nv;

	/**
	 * URL for an image.
	 */
	// protected String imageURL =
	// "http://sysbio.soe.ucsc.edu/projects/IBDev/ib_tutorial_images/slug-icon.gif";
	protected String imageURL = "";

	/**
	 * Attribute to find out if currently using an image.
	 */
	protected boolean usingImage = false;

	/**
	 * Keep track of NodeListeners.
	 */
	private Set<NodeListener> nodeListeners = new HashSet<NodeListener>();

	// TODO ///////////////////////////////////////////////////

	/**
	 * Constructor for a BasicNode.
	 * 
	 * @param id
	 * @param labeledName
	 * @param tooltipName
	 * @param organism
	 * @param biodeSpace
	 * @param netViz
	 * @param mouseEventsHandler
	 */
	public BasicNode(String id, String labeledName, String tooltipName,
			String organism, String biodeSpace, NetworkVisualization netViz,
			BasicNodeMouseEventsHandler mouseEventsHandler) {
		this.nv = netViz;
		this.id = id;
		setID(id);

		edgeGroups = new ArrayList<NetworkEdgeGroup>();

		shapeObject = new Shape(Shape.CIRCLE, Shape.DEFAULT_X, Shape.DEFAULT_Y,
				SIZE, SIZE);

		// setting the opacity seems to make the visualization sluggish
		// setOpacity(SHAPE_OPACITY);

		add(shapeObject);

		// This is for the tooltip.
		setTitle(tooltipName);

		label = new Text(labeledName);
		setAttachLabel(true);

		setStandardColor(organism);
		setStandardShape(biodeSpace);
		setDeselected();

		addBasicNodeMouseEventsHandler(mouseEventsHandler);
		// this.addMouseEventHandlers();
	}

	/**
	 * if label is attached, remove. If label is not attached and is not
	 * abstract, complex, or drug, add it. Attach a label if node is visualized
	 * as an image.
	 * 
	 * @param attach
	 */
	public void setAttachLabel(final boolean attach) {
		if ((attach) && (!label.isAttached() && (isUsingImage()))) {
			add(label);
		} else if ((attach)
				&& (!label.isAttached() && !(id.toLowerCase().endsWith(
						"(abstract)")
						|| id.toLowerCase().endsWith("(complex)") || id
						.toLowerCase().endsWith("(drug)")))) {
			add(label);
		} else if ((!attach) && (label.isAttached())) {
			label.removeFromParent();
		}
	}

	/**
	 * Switch to visualization using image.
	 * 
	 * @param useImage
	 */
	public void useImage(final boolean useImage) {
		if (useImage) {
			// switch from shape node to image node
			// also works for switching images
			switchToImage();
		} else if (!useImage && usingImage) {
			// switch from image node to shape node
			switchToShape();
		} else {
			// do nothing
		}
	}

	/**
	 * Use image for visualization instead of SVG image.
	 */
	private void switchToImage() {
		// remove shape stuff
		shapeObject.removeFromParent();
		setAttachLabel(false);

		// remove previous image stuff
		if (isUsingImage()) {
			imageObject.removeFromParent();
		}

		// add image stuff
		imageObject = new Image(imageURL);
		add(imageObject);

		usingImage = true;

		setAttachLabel(true);
	}

	/**
	 * Use SVG shape for visualization instead of image.
	 */
	private void switchToShape() {
		// remove image stuff
		imageObject.removeFromParent();
		setAttachLabel(false);

		// use shape stuff
		add(shapeObject);

		usingImage = false;

		setAttachLabel(true);
	}

	/**
	 * Add a listener.
	 * 
	 * @param listener
	 */
	public void addNodeListener(NodeListener listener) {
		nodeListeners.add(listener);
	}

	/**
	 * Remove a listener.
	 * 
	 * @param listener
	 */
	public void removeNodeListener(NodeListener listener) {
		nodeListeners.remove(listener);
	}

	/**
	 * Set the image url. Notify listeners.
	 * 
	 * @param url
	 */
	public void setImageURL(String url) {
		imageURL = url;

		// notify CircleMapUrlListeners
		for (NodeListener listener : nodeListeners) {
			listener.updatedCircleMapURL();
		}
	}

	/**
	 * Get the image url.
	 * 
	 * @return
	 */
	public String getImageURL() {
		return imageURL;
	}

	/**
	 * Get the imageObject's size
	 * 
	 * @return
	 */
	public double getImageSize() {
		return imageObject.getSize();
	}

	/**
	 * True if using an image. False if using SVG shape.
	 * 
	 * @return
	 */
	public boolean isUsingImage() {
		return usingImage;
	}

	/**
	 * 
	 * Reset the BasicNode parameters using this information.
	 * 
	 * @param id
	 * @param labeledName
	 * @param tooltipName
	 * @param organism
	 * @param biodeSpace
	 */
	public void redraw(final String id, final String labeledName,
			final String tooltipName, final String organism,
			final String biodeSpace) {
		this.id = id;

		if (!label.isAttached()) {
			label = new Text(labeledName);
		}

		setStroke("black");

		setStandardColor(organism);
		setStandardShape(biodeSpace);
		setDeselected();
	}

	/**
	 * Set the size of this object's, Shape.
	 * 
	 * @param size
	 */
	public void setSize(final double size) {
		// cap at maximum size
		if (size > MAXIMUM_NODE_SIZE) {
			shapeObject.setSize(MAXIMUM_NODE_SIZE);
			shapeObject.setSizeY(MAXIMUM_NODE_SIZE);
		} else {
			shapeObject.setSize(size);
			shapeObject.setSizeY(size);
		}
	}

	/**
	 * Set the shape to the standard mapped shape.
	 * 
	 * @param biodeSpace
	 */
	protected void setStandardShape(final String biodeSpace) {
		if (shapeMap.containsKey(biodeSpace)) {
			setShape(shapeMap.get(biodeSpace).intValue());
		} else {
			// do nothing... keep default shape
		}
	}

	/**
	 * Set the color to the standard mapped color.
	 * 
	 * @param systemSpace
	 */
	protected void setStandardColor(String systemSpace) {
		if (organismToColorMap.containsKey(systemSpace)) {
			setColor(organismToColorMap.get(systemSpace));
		} else {
			// do nothing... keep default color
			setColor("silver");
		}
	}

	/**
	 * Set the stroke
	 * 
	 * @param stroke
	 */
	protected void setStroke(String stroke) {
		// set stroke
		DESELECTED_STROKE = stroke;
		if (stroke.equalsIgnoreCase("black")) {
			DESELECTED_STROKEWIDTH = 1;
		} else {
			DESELECTED_STROKEWIDTH = 3;
		}

		// apply stroke
		if (isSelected()) {
			shapeObject.setStrokeWidth(SELECTED_STROKEWIDTH);
			shapeObject.setStroke(SELECTED_STROKE);
		} else {
			shapeObject.setStrokeWidth(DESELECTED_STROKEWIDTH);
			shapeObject.setStroke(DESELECTED_STROKE);
		}
	}

	protected void onLoad() {
		if (!label.isAttached()) {
			return;
		}
		label.setPosition_drawable(0, getTextHeight() / 3);
	}

	public void attachNetworkEdgeGroup(NetworkEdgeGroup neg) {
		edgeGroups.add(neg);
	}

	public String getID() {
		return id;
	}

	protected double getTextHeight() {
		if (!label.isAttached()) {
			return 0;
		}
		return DrawPanel.impl.getBBoxHeightForDP(label);
	}

	protected void updateEdgeGroupPosition() {
		for (int i = 0; i < edgeGroups.size(); i++) {
			edgeGroups.get(i).updatePosition();
		}
	}

	public void setPosition_drawable(double x, double y) {
		super.setPosition_drawable(x, y);
		updateEdgeGroupPosition();
	}

	/**
	 * Sets a new shape for this NetworkNode and apply the change.
	 * 
	 * @param shapeCode
	 *            (as defined in Shape.java.)
	 */
	public void setShape(final int shapeCode) {
		// swap in a new shape
		remove(shapeObject);
		shapeObject = new Shape(shapeCode);
		add(shapeObject);

		// put the label back on top
		setAttachLabel(false);
		setAttachLabel(true);

		// set colors
		if (isSelected()) {
			changeToSelectedColor();
		} else {
			changeToDeselectedColor();
		}
	}

	/**
	 * Change the color and apply it. This is the deselected color.
	 */
	public void setColor(final String colorCode) {
		DESELECTED_FILL = colorCode;
		// DESELECTED_STROKE = "black";
		// DESELECTED_STROKEWIDTH = 1;

		// switch to the new colors now
		if (isSelected()) {
			changeToSelectedColor();
		} else {
			changeToDeselectedColor();
		}
	}

	/**
	 * Get the color. This is the color for deselected fill.
	 * 
	 * @return
	 */
	public String getColor() {
		return getDeselectedFill();
	}

	/**
	 * Gets all NetworkEdgeGroups associated with this node.
	 * 
	 * @return the edge groups
	 */
	public List<NetworkEdgeGroup> getAllNetworkEdgeGroups() {

		// First, get rid of empty NEG's.
		// Next, return results.
		NetworkEdgeGroup NEG;
		for (Iterator<NetworkEdgeGroup> iter = edgeGroups.iterator(); iter
				.hasNext();) {
			NEG = iter.next();
			if (NEG.trackLines.size() > 0) {
			} else {
				NEG.removeFromNodes();
			}
		}
		return edgeGroups;
	}

	/**
	 * Gets all adjacent nodes.
	 * 
	 * @return the adjacent nodes
	 */
	public HashSet<String> getAdjacentNodes() {
		HashSet<String> adjNodes = new HashSet<String>();

		// First, remove empty NEG's.
		// Next, add them to a list to return.
		for (NetworkEdgeGroup NEG : edgeGroups) {
			if (NEG.trackLines.size() > 0) {
				if (!NEG.getFirstBiode().equalsIgnoreCase(id)) {
					adjNodes.add(NEG.getFirstBiode());
				} else {
					adjNodes.add(NEG.getSecondBiode());
				}
			} else {
				NEG.removeFromNodes();
			}
		}

		// in the case of self-links
		adjNodes.remove(id);

		return adjNodes;
	}

	/**
	 * Set the title for this node. It is the tooltip for the widget.
	 * 
	 * @param title
	 */
	public void setTitle(final String text) {
		title = new Title(text);
		this.add(title);
	}

	/**
	 * Set as selected and change color.
	 */
	public void setSelected() {
		selected = true;
		changeToSelectedColor();
	}

	/**
	 * Change to selected colors.
	 */
	protected void changeToSelectedColor() {
		shapeObject.setFill(SELECTED_FILL);
		shapeObject.setStroke(SELECTED_STROKE);
		shapeObject.setStrokeWidth(SELECTED_STROKEWIDTH);
	}

	/**
	 * Set as deselected and change color.
	 */
	public void setDeselected() {
		selected = false;
		changeToDeselectedColor();
	}

	/**
	 * Change to deselected colors.
	 */
	protected void changeToDeselectedColor() {
		shapeObject.setFill(DESELECTED_FILL);
		shapeObject.setStroke(DESELECTED_STROKE);
		shapeObject.setStrokeWidth(DESELECTED_STROKEWIDTH);
	}

	public boolean isSelected() {
		return selected;
	}

	/**
	 * Prevent selection of Network Node text when dragging
	 */
	public void onBrowserEvent(Event e) {
		DOM.eventPreventDefault(e);
		super.onBrowserEvent(e);
	}

	public boolean toggleSelected() {
		if (selected == true) {
			nv.deselectBiode(id);
		} else {
			nv.selectBiode(id);
		}
		return selected;
	}

	/**
	 * Provides visual cue to alert user to a node.
	 * 
	 */
	public void flash() {
		int delay = 1000;
		changeToSelectedColor();
		Timer t = new Timer() {
			public void run() {
				changeToDeselectedColor();
			}
		};

		// Schedule the timer to run once in ? ms.
		t.schedule(delay);
	}

	/**
	 * Make invisible. Also, make all this object's networkEdgeGroups invisible.
	 */
	public void hide() {
		this.setVisible(false);
		for (NetworkEdgeGroup neg : getAllNetworkEdgeGroups()) {
			neg.hide();
		}
	}

	/**
	 * Make visible. Also, make all this object's networkEdgeGroups visible and
	 * updates the position.
	 */
	public void show() {
		setVisible(true);
		for (NetworkEdgeGroup neg : getAllNetworkEdgeGroups()) {
			neg.show();
			neg.updatePosition();
		}
		flash();
	}

	/**
	 * Assigns new coords for the node, making sure it is within viewport
	 * bounds.
	 * 
	 * @param xCoord
	 *            the x coordinate
	 * @param yCoord
	 *            the y coordinate
	 */
	public void setPositionInVpBounds(double xCoord, double yCoord) {

		if (Double.toString(xCoord).equalsIgnoreCase("NaN")
				|| Double.toString(yCoord).equalsIgnoreCase("NaN")) {
			LoggingDialogBox.log("Not setting position for " + this.id
					+ " because NaN in new coordinates.");
			return;
		}

		double halfWidth = SIZE / 2;
		double halfHeight = SIZE / 2;

		double maxX = nv.getVportWidth() - halfWidth;
		double maxY = nv.getVportHeight() - halfHeight;

		double minX = 0 + halfWidth;
		double minY = 0 + halfHeight;

		if (xCoord < minX) {
			xCoord = minX;
		} else if (xCoord > maxX) {
			xCoord = maxX;
		}

		if (yCoord < minY) {
			yCoord = minY;
		} else if (yCoord > maxY) {
			yCoord = maxY;
		}

		setPosition_drawable(xCoord, yCoord);
	}

	/**
	 * Get the size of this BasicNode. If using images, return that size.
	 * 
	 * @return
	 */
	public int getSize() {
		if (this.isUsingImage()) {
			Double doubleSize = (new Double(this.getImageSize()).doubleValue());
			int intSize = doubleSize.intValue();
			return intSize;
		} else {
			return SIZE;
		}
	}

	/**
	 * Get the Shape object for this BasicNode.
	 * 
	 * @return
	 */
	public Shape getShapeObject() {
		return shapeObject;
	}

	/**
	 * Get the shape type in the form a shape code. Shape codes defined in
	 * Shape.java.
	 * 
	 * @return
	 */
	public int getShapeType() {
		return getShapeObject().getType();
	}

	public String getSelectedFill() {
		return SELECTED_FILL;
	}

	public String getDeselectedFill() {
		return DESELECTED_FILL;
	}

	/**
	 * Remove a NetworkEdgeGroup's name from this NetworkNode's list,
	 * edgeGroups.
	 * 
	 * @param negName
	 *            String
	 * @return boolean
	 */
	public boolean removeNetworkEdgeGroup(String negName) {
		return edgeGroups.remove(negName);
	}

	/**
	 * Reset <code>amDragging</code> and <code>hasMoved</code> to false.
	 */
	protected void resetToRestingCondition() {
		amDragging = false;
		hasMoved = false;
	}

	protected void addBasicNodeMouseEventsHandler(
			BasicNodeMouseEventsHandler handler) {
		addMouseDownHandler(handler);
		addMouseMoveHandler(handler);
		addMouseUpHandler(handler);
		addDoubleClickHandler(handler);
	}

	@Override
	public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
		return addDomHandler(handler, MouseDownEvent.getType());
	}

	@Override
	public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
		return addDomHandler(handler, MouseUpEvent.getType());
	}

	@Override
	public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
		return addDomHandler(handler, MouseMoveEvent.getType());
	}

	@Override
	public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
		return addDomHandler(handler, DoubleClickEvent.getType());
	}
}
