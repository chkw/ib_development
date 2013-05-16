/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;

/**
 * Class for implementing a basic layout where nodes are arranged in a circle.
 */
public class CircleLayout {
	private static final int X_AXIS = 0;

	private static final int Y_AXIS = 1;

	/**
	 * Performs a circle layout.
	 * 
	 * @param nodesMap
	 *            Map of BasicNodes to layout (Each NetworkNode has information
	 *            about its connected NetworkEdgeGroups.)
	 * @param areaMaxX
	 *            width of area to draw as double
	 * @param areaMaxY
	 *            height of area to draw as double
	 */
	public static void doLayout(Map<String, BasicNode> nodesMap,
			double areaMaxX, double areaMaxY) {
		long start = (new Date()).getTime();
		Set<String> nodeSet = new HashSet<String>(nodesMap.keySet());

		// network will fill up only portion of drawable area
		// code to assign the smaller value
		double radius = (areaMaxX < areaMaxY) ? (areaMaxX * 0.5 - 20)
				: (areaMaxY * 0.5 - 20);

		double[] center = { areaMaxX * 0.5, areaMaxY * 0.5 };
		double degreesIncrement = 360 / new Integer(nodeSet.size())
				.doubleValue();
		double angle = 0;
		double[] coords = { 0, 0 };

		BasicNode node;
		for (String biode : nodeSet) {
			node = nodesMap.get(biode);

			coords = degreesToCoords(angle, radius, center[X_AXIS],
					center[Y_AXIS]);

			// node.setPositionInVpBounds(coords[X_AXIS], coords[Y_AXIS]);
			node.setPosition_drawable(coords[X_AXIS], coords[Y_AXIS]);
			angle += degreesIncrement;
		}

		LoggingDialogBox.log("CircleLayout: layout took "
				+ ((new Date()).getTime() - start) + " ms for "
				+ nodeSet.size() + " nodes");
	}

	/**
	 * Converts degrees into XY-coords on a circle.
	 * 
	 * @param degrees
	 * @param radius
	 * @param centerX
	 * @param centerY
	 * @return size 2 array of doubles with the coordinates
	 */
	private static double[] degreesToCoords(double degrees, double radius,
			double centerX, double centerY) {
		double[] coords = { 0, 0 };
		double angle = Math.toRadians(degrees);

		coords[X_AXIS] = centerX + radius * Math.cos(angle);
		coords[Y_AXIS] = centerY + radius * Math.sin(angle);

		return coords;
	}
}
