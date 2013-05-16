/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;

/**
 * Class for implementing a layout where nodes are arranged in concentric
 * circles. This can be possibly improved by reducing edge crossings:
 * http://www.springerlink.com/content/fepu2a3hd195ffjg/
 */
public class RingLayout {
	private static final int X_AXIS = 0;

	private static final int Y_AXIS = 1;

	/**
	 * Perform a ring layout with concentric circles. Selected nodes will be in
	 * the innermost ring. Selected nodes' neighbors in the in-between ring. All
	 * other, more distant nodes in outermost ring.
	 * 
	 * @param selectedSet
	 *            set of biodes whose nodes will be in the innermost ring
	 * @param nodesMap
	 *            mapping of biodes to their nodes
	 * @param areaMaxX
	 *            maximum X coord
	 * @param areaMaxY
	 *            maximum Y coord
	 */
	public static void doLayout(Set<String> selectedSet,
			Map<String, BasicNode> nodesMap, double areaMaxX, double areaMaxY) {
		long start = (new Date()).getTime();

		// figure out which ones are selected, neighbors, and distant
		// copy the nodesMap.keySet without the backing nodesMap Map
		Set<String> distantSet = new HashSet<String>(nodesMap.keySet());
		Set<String> neighborSet = new HashSet<String>();

		String biode;
		BasicNode node;
		for (Iterator<String> iter = selectedSet.iterator(); iter.hasNext();) {
			biode = iter.next();
			node = nodesMap.get(biode);
			neighborSet.addAll(node.getAdjacentNodes());
		}
		neighborSet.removeAll(selectedSet);
		distantSet.removeAll(selectedSet);
		distantSet.removeAll(neighborSet);

		// network will fill up only portion of drawable area
		// code to assign the smaller value
		double maxRadius = (areaMaxX < areaMaxY) ? (areaMaxX * 0.5 - 20)
				: (areaMaxY * 0.5 - 20);

		double[] center = { areaMaxX * 0.5, areaMaxY * 0.5 };

		// layout innermost ring with 1/3 * r (selected nodes, if there are any)
		if (selectedSet.size() != 0) {
			layoutOneRing(selectedSet, nodesMap, center[X_AXIS],
					center[Y_AXIS], (maxRadius / 3));
		}
		// layout in-between ring with 2/3 * r (neighbors of selected nodes, if
		// any)
		if (neighborSet.size() != 0) {
			layoutOneRing(neighborSet, nodesMap, center[X_AXIS],
					center[Y_AXIS], (2 * maxRadius / 3));
		}
		// layout outermost ring with 3/3 * r (the rest of them)
		if (distantSet.size() != 0) {
			layoutOneRing(distantSet, nodesMap, center[X_AXIS], center[Y_AXIS],
					maxRadius);
		}

		LoggingDialogBox.log("RingLayout: layout took "
				+ ((new Date()).getTime() - start) + " ms for "
				+ nodesMap.keySet().size() + " nodes");
	}

	/**
	 * Lay out a ring of nodes.
	 * 
	 * @param nodeSet
	 * @param nodesMap
	 * @param centerX
	 * @param centerY
	 * @param radius
	 */
	private static void layoutOneRing(Set<String> nodeSet,
			Map<String, BasicNode> nodesMap, double centerX, double centerY,
			double radius) {
		double degreesIncrement = 360 / new Integer(nodeSet.size())
				.doubleValue();
		// attempt to have an offset rotation between rings
		double angle = 10 * nodeSet.size();
		double[] coords = { 0, 0 };

		String biode;
		for (Iterator<String> iter = nodeSet.iterator(); iter.hasNext();) {
			biode = iter.next();
			coords = degreesToCoords(angle, radius, centerX, centerY);
			nodesMap.get(biode).setPosition_drawable(coords[X_AXIS], coords[Y_AXIS]);
			angle += degreesIncrement;
		}
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
