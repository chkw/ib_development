/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.drawpanel.client.Drawable;
import edu.ucsc.ib.drawpanel.client.Group;
import edu.ucsc.ib.drawpanel.client.TrackLine;

/**
 * A group of lines between two BasicNodes. Internally, the BasicNodes are
 * ordered by the Biode IDs, lexicographically, in order to distinguish the two.
 * It has a Group object called edgeGroup, which is the actually drawing
 * representation of the edge.
 * 
 */
public class NetworkEdgeGroup extends Composite implements Drawable {

	public static final int DEFAULT_GROUP_OPACITY = 100;

	private static final int LINE_SPACING = 1;

	private static final String BIODE_DELIMITER = NetworkVisualization.BIODE_DELIMITER;

	/**
	 * First node.
	 */
	protected BasicNode first;

	/**
	 * Second node
	 */
	protected BasicNode second;

	/**
	 * The name of the NetworkEdgeGroup should match up with what NetViz uses.
	 */
	private final String name;

	/**
	 * length
	 */
	protected double length;

	/**
	 * List of tracks.
	 */
	protected List<Track> tracks;

	/**
	 * List of TrackLines.
	 */
	protected List<TrackLine> trackLines;

	/**
	 * SVG group of TrackLine objects.
	 */
	protected Group edgeGroup;

	/**
	 * A width used to set offset of lines within the edgeGroup.
	 */
	protected double sumStrokeWidth;

	/**
	 * Constructor for NetworkEdgeGroup.
	 * 
	 * @param nodeA
	 * @param nodeB
	 */
	public NetworkEdgeGroup(BasicNode nodeA, BasicNode nodeB, int opacity) {
		this.first = nodeA;
		this.second = nodeB;

		orderNodes();

		// this name matches up with name that NetViz uses.
		this.name = getEdgeName(this.first.getID(), this.second.getID());

		this.length = calculateLength();
		this.tracks = new ArrayList<Track>();
		this.trackLines = new ArrayList<TrackLine>();

		// create a group
		this.edgeGroup = new Group();

		this.sumStrokeWidth = 0;

		setOpacity(opacity);

		initWidget(this.edgeGroup);
	}

	/**
	 * Get a name for the edge. No ordering is performed.
	 * 
	 * @param biode1
	 * @param biode2
	 * @return
	 */
	public static String getEdgeName(final String biode1, final String biode2) {
		return biode1 + BIODE_DELIMITER + biode2;
	}

	/**
	 * Separate the biodes in the edgeName. First biode is [0]. Seconde biode is
	 * [1].
	 * 
	 * @param edgeName
	 * @return
	 */
	public static String[] separateEdgeName(final String edgeName) {
		return edgeName.split(BIODE_DELIMITER);
	}

	/**
	 * Called by the Track when it has data between two nodes. List of tracks is
	 * updated and a line is added to the NetworkEdgeGroup.
	 * 
	 * @param t
	 *            Track that's being added
	 * @param firstMarker
	 *            firstMarker - Select from predefined marker types.
	 * @param secondMarker
	 *            secondMarker
	 * @param isDashed
	 * @param edgeWeight
	 *            Factor by which the line's default stroke width will be
	 *            multiplied for display. This is meant to indicate some kind of
	 *            score for the edge.
	 */
	public void addTrack(final Track t, final int firstMarker,
			final int secondMarker, boolean isDashed, double edgeWeight) {
		// add line for track

		// add the track if not already in the list
		if (!(tracks.contains(t))) {
			// add the track to the list of tracks
			tracks.add(t);
			TrackLine trackLine = makeTrackLine(t, firstMarker, secondMarker,
					isDashed);

			// set stroke width
			if (edgeWeight != Double.NaN) {
				trackLine.setStrokeWidth(edgeWeight);
			}

			trackLines.add(trackLine);
			// create a line and add it to the group
			edgeGroup.add(trackLine);
		}
		updateLineOffsets();
		// updatePosition();
	}

	public String getFirstBiode() {
		return first.getID();
	}

	public String getSecondBiode() {
		return second.getID();
	}

	public String getName() {
		return this.name;
	}

	public double getHeight_drawable() {
		return edgeGroup.getHeight_drawable();
	}

	public double getWidth_drawable() {
		return edgeGroup.getWidth_drawable();
	}

	// (Required by Drawable interface.)
	public double getX_drawable() {
		return first.getX_drawable();
	}

	public double getX2() {
		return second.getX_drawable();
	}

	// (Required by Drawable interface.)
	public double getY_drawable() {
		return first.getY_drawable();
	}

	public double getY2() {
		return second.getY_drawable();
	}

	/**
	 * Find the length of this edgeGroup.
	 * 
	 * @return
	 */
	private double calculateLength() {
		double diffX = this.second.getX_drawable() - this.first.getX_drawable();
		double diffY = this.second.getY_drawable() - this.first.getY_drawable();

		return this.length = Math.sqrt(diffX * diffX + diffY * diffY);
	}

	/**
	 * Calculate and return the length;
	 * 
	 * @return
	 */
	public double getLength() {
		this.calculateLength();
		return this.length;
	}

	/**
	 * Create and return a TrackLine object representing a link.
	 * 
	 * @param t
	 * @param headType
	 * @param tailType
	 * @param isDashed
	 * @return
	 */
	private TrackLine makeTrackLine(Track t, int headType, int tailType,
			boolean isDashed) {
		double offset = this.sumStrokeWidth
				+ (0.5 * TrackLine.DEFAULT_STROKE_WIDTH) + LINE_SPACING;

		this.sumStrokeWidth += TrackLine.DEFAULT_STROKE_WIDTH + LINE_SPACING;

		TrackLine tLine = new TrackLine(t, this.length, offset, headType,
				tailType, isDashed);

		return tLine;
	}

	/**
	 * Remove a track from the NetworkEdgeGroup.
	 * 
	 * @param t
	 *            The track to remove.
	 * @return Number of tracks remaining.
	 */
	public int removeTrack(Track t) {
		int index = this.tracks.indexOf(t);
		if (index >= 0) {
			this.tracks.remove(index);
			this.edgeGroup.remove((TrackLine) this.trackLines.get(index));
			this.trackLines.remove(index);
			updateLineOffsets();
		}
		return this.tracks.size();
	}

	/**
	 * Remove this NetworkEdgeGroup from its NetworkNodes.
	 */
	public void removeFromNodes() {
		this.first.removeNetworkEdgeGroup(this.name);
		this.second.removeNetworkEdgeGroup(this.name);
	}

	/**
	 * Set the x,y position of the edgeGroup. (Required by Drawable interface.)
	 * 
	 * @param x
	 * @param y
	 */
	public void setPosition_drawable(double x, double y) {
		edgeGroup.setPosition_drawable(x, y);
	}

	/**
	 * set the opacity of the group
	 * 
	 * @param opacity
	 */
	public void setOpacity(int opacity) {
		if (opacity < 0) {
			opacity = 0;
		} else if (opacity > 100) {
			opacity = 100;
		}
		edgeGroup.setOpacity(opacity);
	}

	/**
	 * Transformation matrix to rotate and keep the center of the edge group:
	 * 
	 * <pre>
	 *  [ 1  0  tx ]   [ cos(a)  -sin(a)  0 ]   [ 1  0    0  ]   [ cos(a) -sin(a)   sin(a)*w/2 + tx ]
	 *  [ 0  1  ty ] * [ sin(a)   cos(a)  0 ] * [ 0  1  -w/2 ] = [ sin(a)  cos(a)  -cos(a)*w/2 + ty ]
	 *  [ 0  0   1 ]   [   0        0     1 ]   [ 0  0    1  ]   [   0       0             1        ]
	 * </pre>
	 */
	public void updatePosition() {
		double tx = this.first.getX_drawable();
		double ty = this.first.getY_drawable();
		double deltaX = this.second.getX_drawable() - tx;
		double deltaY = this.second.getY_drawable() - ty;

		this.length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		double cosA = deltaX / this.length;
		double sinA = deltaY / this.length;

		this.edgeGroup.setTransform("matrix( " + cosA + " " + sinA + " "
				+ (-sinA) + " " + cosA + " "
				+ ((0.5 * sinA * this.sumStrokeWidth) + tx) + " "
				+ ((-0.5 * cosA * this.sumStrokeWidth) + ty) + ")");

		// get the larger of the shapes
		double largerShapeSize = (this.first.getSize() > this.second.getSize()) ? this.first
				.getSize() : this.second.getSize();

		// draw lines up to edge of node
		changeLineLengths(this.length, 0.5 * largerShapeSize);

	}

	/**
	 * Modify the length of all the lines in the edgeGroup, chopping off
	 * portions at ends to account for node size. We want to draw the line up to
	 * the boundary of the node.
	 * 
	 * @param length
	 * @param shortenHead
	 */
	private void changeLineLengths(double length, double shortenHead) {
		double drawingLength = length - shortenHead;
		TrackLine tLine;
		for (Iterator<Widget> iLines = this.edgeGroup.iterator(); iLines
				.hasNext();) {
			tLine = (TrackLine) iLines.next();
			// tLine.setFromX(shortenHead + 5);
			// tLine.setToX(drawingLength - 5);
			tLine.changeLength(shortenHead, drawingLength);
		}
	}

	/**
	 * Change the offsets for the lines in this edgeGroup. This is to keep the
	 * edgeGroup centered when there are multiple edges.
	 */
	private void updateLineOffsets() {
		this.sumStrokeWidth = -LINE_SPACING;
		for (TrackLine tLine : trackLines) {

			double offset = sumStrokeWidth + 0.5 * tLine.getStrokeWidth()
					+ LINE_SPACING;
			sumStrokeWidth += tLine.getStrokeWidth() + LINE_SPACING;
			tLine.setVertOffset(offset);
		}
		updatePosition();
	}

	/**
	 * Make sure the nodes for this edgeGroup are ordered. That is, keep them in
	 * alphabetical order.
	 */
	private void orderNodes() {
		if (this.first.getID().compareTo(this.second.getID()) > 0) {
			final BasicNode swap = this.first;
			this.first = this.second;
			this.second = swap;
		}
	}

	/**
	 * Return the set of track names.
	 * 
	 * @return
	 */
	public Set<String> getTrackNames() {
		Set<String> trackNames = new HashSet<String>();
		for (Track track : this.tracks) {
			String trackName = track.getName();
			trackNames.add(trackName);
		}

		return trackNames;
	}

	/**
	 * Set invisible.
	 */
	public void hide() {
		this.setVisible(false);
	}

	/**
	 * Set visible.
	 */
	public void show() {
		this.setVisible(true);
	}

	@Override
	public Element getElement_drawable() {
		return getElement();
	}
}
