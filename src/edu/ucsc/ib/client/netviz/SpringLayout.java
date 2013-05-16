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
 * Class for implementing a basic spring-embedded layout algorithm as described
 * in: Eades, P. A heuristic for graph drawing Congressus Numerantium, 1984, 42,
 * 149-160.
 */
public class SpringLayout {

	/**
	 * When then total forces in the graph fall to this level, consider the
	 * graph relaxed.
	 */
	private static final int RELAXED_FORCE = 150;

	private static final int X_AXIS = 0;

	private static final int Y_AXIS = 1;

	/**
	 * c1, constant for spring forces.
	 */
	private static double c1 = 200.0;

	/**
	 * c2, natural length of the springs. originally c2 = 100.0
	 */
	private static double c2 = 120.0;

	/**
	 * c3, constant for node repulsive forces.
	 */
	private static double c3 = 50000.0;

	/**
	 * c4, factor of node repulsive force to apply.
	 */
	private static double c4 = 0.1;

	/**
	 * m, number of iterations.
	 */
	private static int m = 25;

	/**
	 * Performs a spring-embedded layout as described in: Eades, P. A heuristic
	 * for graph drawing Congressus Numerantium, 1984, 42, 149-160.
	 * 
	 * @param nodesMap
	 *            Map of BasicNode objects to layout (Each BasicNode has
	 *            information about its connected NetworkEdgeGroups.)
	 * @param pinSelected
	 *            if true, then selected nodes will not be moved
	 */
	public static void doLayout(Map<String, BasicNode> nodesMap,
			boolean pinSelected) {
		long start = (new Date()).getTime();

		double maxForce = 0;

		for (int i = 1; i <= m; i++) {

			maxForce = 0;

			for (String biode : nodesMap.keySet()) {

				BasicNode node = nodesMap.get(biode);

				// don't move pinned nodes
				if (pinSelected && node.isSelected()) {
					continue;
				}

				// calculate forces on node
				double[] forces = calcForces(node, nodesMap);

				// move node according to forces
				moveNode(node, forces[X_AXIS], forces[Y_AXIS]);

				// update maxForce
				double nodeForce = Math.abs(forces[X_AXIS])
						+ Math.abs(forces[Y_AXIS]);

				maxForce = Math.max(nodeForce, maxForce);

			}

			// stop layout when small change between iters
			if (maxForce <= RELAXED_FORCE) {
				LoggingDialogBox.log(i + " iterations to reach maxForce: "
						+ maxForce);
				break;
			}
		}
		LoggingDialogBox.log("maxForce of last iteration: " + maxForce);
	}

	/**
	 * Force exerted on a node by an edge (spring). This models a logarithmic
	 * strength spring.
	 * 
	 * @param d
	 *            length of spring
	 * @return spring force
	 */
	private static double springForce(double d) {
		return c1 * Math.log(d / c2);
	}

	/**
	 * Nonadjacent vertices should repel each other. This is an inverse square
	 * law force.
	 * 
	 * @param d
	 *            distance
	 * @return spring force
	 */
	private static double nodeRepulseForce(double d) {
		return c3 / (d * d);
	}

	/**
	 * Set a new position for a BasicNode.
	 * 
	 * @param node
	 *            BasicNode to re-position
	 * @param forceX
	 *            force exerted in the X-axis
	 * @param forceY
	 *            force exerted in the Y-axis
	 */
	private static void moveNode(BasicNode node, double forceX, double forceY) {
		double newX = node.getX_drawable() + (c4 * forceX);
		double newY = node.getY_drawable() + (c4 * forceY);

		node.setPositionInVpBounds(newX, newY);
	}

	/**
	 * Calculate forces on a BasicNode.
	 * 
	 * @param node
	 *            the BasicNode
	 * @param nodesMap
	 *            the Map of all BasicNodes in the graph
	 * @return the forces in a double[], with double[0] for X-axis and double[1]
	 *         for Y-axis
	 */
	private static double[] calcForces(BasicNode node,
			Map<String, BasicNode> nodesMap) {
		HashSet<String> nonadjBiodes = new HashSet<String>(nodesMap.keySet());

		// remove self from list
		nonadjBiodes.remove(node.getID());
		HashSet<String> adjBiodes = (HashSet<String>) node.getAdjacentNodes();

		nonadjBiodes.removeAll(adjBiodes);

		double deltaX;
		double deltaY;
		double dist;
		double unitForce;
		double forceX;
		double forceY;

		// calculate forces from nonadjacent NODES
		double[] nodeRepulseForces = { 0, 0 };

		for (String forceBiode : nonadjBiodes) {
			BasicNode forceNode = nodesMap.get(forceBiode);

			// find distance
			deltaX = node.getX_drawable() - forceNode.getX_drawable();
			deltaY = node.getY_drawable() - forceNode.getY_drawable();

			dist = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));

			// only care about nearby nodes ... originally dist < 50
			if (dist < 1) {
				unitForce = c3;
			} else if (dist < 80) {
				// find magnitude of force
				// unitForce = c3 / (dist * dist * dist);
				unitForce = nodeRepulseForce(dist) / dist;
			} else {
				unitForce = 0;
			}

			// breakdown force into X & Y components
			forceX = unitForce * deltaX;
			forceY = unitForce * deltaY;

			// add it to running total of force
			nodeRepulseForces[X_AXIS] += forceX;
			nodeRepulseForces[Y_AXIS] += forceY;
		}

		// calculate forces from EDGES (adjacent nodes)
		double[] edgeSpringForces = { 0, 0 };

		for (String forceBiode : adjBiodes) {
			BasicNode forceNode = nodesMap.get(forceBiode);

			if (!forceNode.getID().equalsIgnoreCase(node.getID())) {

				// find distance
				deltaX = node.getX_drawable() - forceNode.getX_drawable();
				deltaY = node.getY_drawable() - forceNode.getY_drawable();

				dist = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));

				// find magnitude of force
				// unitForce = c1/dist * Math.log(dist / c2);
				unitForce = springForce(dist) / dist;

				// break down force into X & Y components
				forceX = unitForce * -1 * deltaX;
				forceY = unitForce * -1 * deltaY;
			} else {
				forceX = 0;
				forceY = 0;
			}

			// add it to running total of force
			edgeSpringForces[X_AXIS] += forceX;
			edgeSpringForces[Y_AXIS] += forceY;
		}

		double[] totalForces = { 0, 0 };

		totalForces[X_AXIS] = nodeRepulseForces[X_AXIS]
				+ edgeSpringForces[X_AXIS];

		totalForces[Y_AXIS] = nodeRepulseForces[Y_AXIS]
				+ edgeSpringForces[Y_AXIS];

		return totalForces;
	}
}
