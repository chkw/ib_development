package edu.ucsc.ib.server;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Find the shortest path between vertices in a graph. This code was written
 * following the example at <a
 * href="http://renaud.waldura.com/doc/java/dijkstra/"
 * >http://renaud.waldura.com/doc/java/dijkstra/</a>.
 * 
 * @author Chris
 * 
 */
public class DijkstrasShortestPath {

	private static final Double INFINITE_DISTANCE = Double.POSITIVE_INFINITY;

	private static final int INITIAL_CAPACITY = 3;

	/**
	 * predecessor of each vertex on the shortest path from the source
	 */
	private Map<String, String> predecessorMap = new HashMap<String, String>();

	/**
	 * vertices whose shortest distances from the source have been found
	 */
	private Set<String> settledVertices = new HashSet<String>();

	/**
	 * Comparator to determine which vertex is closer to the origin. In case of
	 * tie, String compare is used.
	 */
	private final Comparator<String> shortestDistanceComparator = new Comparator<String>() {
		public int compare(String left, String right) {
			double shortestDistanceLeft = getShortestDistance(left);
			double shortestDistanceRight = getShortestDistance(right);

			if (shortestDistanceLeft > shortestDistanceRight) {
				return +1;
			} else if (shortestDistanceLeft < shortestDistanceRight) {
				return -1;
			} else // equal
			{
				return left.compareTo(right);
			}
		}
	};

	/**
	 * vertices whose shortest distances from the source have not been found
	 */
	private PriorityQueue<String> unsettledVertices = new PriorityQueue<String>(
			INITIAL_CAPACITY, shortestDistanceComparator);

	/**
	 * store each vertex's shortest distance to the origin.
	 */
	private Map<String, Double> shortestDistancesMap = new HashMap<String, Double>();

	/**
	 * Data structure containing the edges of the network.
	 */
	private final NetworkLinkTreeMap links;

	// TODO ////////////////////////////////////////////////////

	/**
	 * Constructor sets the network edges to use for algorithm. To compute a
	 * shortest path, use {@link #returnVerticesOnShortestPath(String,String)}.
	 * 
	 * @param links
	 */
	public DijkstrasShortestPath(final NetworkLinkTreeMap links) {
		this.links = links;
	}

	/**
	 * Set the starting conditions for the algorithm. **DOES NOT LOAD THE
	 * NETWORK DATA**
	 */
	private void initialize() {
		predecessorMap = new HashMap<String, String>();
		settledVertices = new HashSet<String>();
		unsettledVertices = new PriorityQueue<String>(INITIAL_CAPACITY,
				shortestDistanceComparator);
		shortestDistancesMap = new HashMap<String, Double>();
	}

	/**
	 * Compute the shortest path from the origin to the destination.
	 * 
	 * @param origin
	 *            starting vertex of the shortest path
	 * @param destination
	 *            ending vertex of the shortest path
	 * 
	 * @return length of shortest path
	 */
	private double findPath(final String origin, final String destination) {
		initialize();

		// check origin and destination are in the network
		if (!links.containsKey(origin) || !links.containsKey(destination)) {
			return INFINITE_DISTANCE;
		}

		// initialization steps
		shortestDistancesMap.put(origin, (double) 0);
		unsettledVertices.add(origin);

		// get the node with shortest distance
		String vertex;
		while ((vertex = extractMin()) != null) {

			// destination reached, stop
			if (vertex == destination) {
				break;
			}

			settledVertices.add(vertex);

			relaxNeighbors(vertex);
		}

		return getShortestDistance(destination);
	}

	/**
	 * Return the vertices on the shortest path. This is the main method of the
	 * class.
	 * 
	 * @param origin
	 *            starting vertex of the shortest path
	 * @param destination
	 *            ending vertex of the shortest path
	 * @return JSONObject with 2 parts: "path" JSONArray with vertices in the
	 *         path, and "distance" JSONValue with length of the path
	 * @throws JSONException
	 */
	public JSONObject returnVerticesOnShortestPath(final String origin,
			final String destination) throws JSONException {
		JSONObject pathJO = new JSONObject();
		JSONArray pathJA = new JSONArray();
		pathJO.put("path", pathJA);

		// find shortest path
		double shortestDistance = findPath(origin, destination);

		// JSON does not allow non-finite numbers.
		if (shortestDistance == INFINITE_DISTANCE) {
			shortestDistance = Double.MAX_VALUE;
		}

		pathJO.put("distance", shortestDistance);

		// origin or destination not in network. no path to backtrack
		if (!links.containsKey(origin) || !links.containsKey(destination)) {
			return pathJO;
		}

		// work backwards from destination to origin
		for (String vertex = destination; vertex != null; vertex = getPredecessor(vertex)) {
			pathJA.put(vertex);
		}

		return pathJO;
	}

	/**
	 * This is the "relaxation" step in the algorithm. It uses the absolute
	 * value of the edge score to determine identify alternate paths that are
	 * shorter than the current shortest path to the vertex from the origin.
	 * 
	 * @param vertex
	 *            vertex whose neighbors will be processed
	 */
	private void relaxNeighbors(final String vertex) {
		// check for end of path
		if (!links.containsKey(vertex)) {
			return;
		}

		for (String neighbor : links.getNeighbors(vertex)) {
			if (isSettled(neighbor)) {
				// do nothing with settled neighbor
			} else {
				// settle the neighbor
				double candidateDistance = getShortestDistance(vertex)
						+ Math.abs(links.getScore(vertex, neighbor));
				if (getShortestDistance(neighbor) > candidateDistance) {
					// shorter path exists from neighbor to origin, and it goes
					// through the vertex
					setShortestDistance(neighbor, candidateDistance);
					setPredecessor(neighbor, vertex);
				} else {
					// no shorter path
				}
			}
		}
	}

	/**
	 * Check if a shortest path from the origin to the vertex has been found.
	 * 
	 * @param vertex
	 * @return
	 */
	private boolean isSettled(final String vertex) {
		return settledVertices.contains(vertex);
	}

	/**
	 * Set the shortest distance from the specified vertex to the origin. Extra
	 * steps are taken to ensure that the intended ordering of unsettledVertices
	 * is maintained.
	 * 
	 * @param vertex
	 * @param distance
	 */
	private void setShortestDistance(final String vertex, final double distance) {
		unsettledVertices.remove(vertex);
		shortestDistancesMap.put(vertex, distance);
		unsettledVertices.add(vertex);
	}

	/**
	 * Get the current shortest distance from the specified vertex to the
	 * origin.
	 * 
	 * @param vertex
	 * @return
	 */
	private double getShortestDistance(final String vertex) {
		Double distance = shortestDistancesMap.get(vertex);

		return (distance == null) ? INFINITE_DISTANCE : distance;
	}

	/**
	 * Set the predecessor of a vertex in the shortest path to the origin.
	 * 
	 * @param vertex
	 * @param predecessor
	 */
	private void setPredecessor(final String vertex, final String predecessor) {
		predecessorMap.put(vertex, predecessor);
	}

	/**
	 * Get the predecessor of a vertex in the shortest path to the origin.
	 * 
	 * @param vertex
	 * @return
	 */
	private String getPredecessor(String vertex) {
		return predecessorMap.get(vertex);
	}

	/**
	 * get the vertex with the shortest distance
	 * 
	 * @return
	 */
	private String extractMin() {
		return unsettledVertices.poll();
	}
}
