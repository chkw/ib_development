package edu.ucsc.ib.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.fileupload.FileItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.rpi.cs.xgmml.Att;
import edu.rpi.cs.xgmml.GraphicEdge;
import edu.rpi.cs.xgmml.GraphicGraph;
import edu.rpi.cs.xgmml.GraphicNode;
import edu.rpi.cs.xgmml.Graphics;
import edu.rpi.cs.xgmml.ObjectFactory;
import edu.rpi.cs.xgmml.ObjectType;

/**
 * This is a servlet for bouncing a pathway back to the client as JSON. It can
 * also take a pathway in JSON and convert it to some pathway file format. For
 * example, it can take a pathway in xgmml and convert it to JSON for the
 * client.
 * 
 * @author Chris
 * 
 */
public class PathwayBounceService extends HttpServlet {

	/**
	 * Compiler generated serial version ID
	 */
	private static final long serialVersionUID = 919583527157779430L;

	/**
	 * Path suffix for decoding xgmml file.
	 */
	private static final String DECODER = "decoder";

	/**
	 * Constructor for this service
	 */
	public PathwayBounceService() {
		super();
	}

	/**
	 * Initialize this service.
	 */
	public void init() {

	}

	/**
	 * Handle GET request.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		this.doPost(req, resp);
	}

	/**
	 * Handle POST request.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		int id = 0;

		String path = req.getPathInfo();

		if (path.endsWith(DECODER)) {
			// test with:
			// http://localhost:8080/ib/pathwayBounce/decoder

			List<FileItem> fileItemList = DatabaseService
					.parseMultipartRequest(req);
			Map<String, String> parameterMap = DatabaseService
					.getParamaterMapping(fileItemList);

			String fileName = parameterMap.get("fileName");
			String fileContents = parameterMap.get("uploadFormElement");
			String fileFormat = parameterMap.get("fileFormat");

			JSONObject resultJO = null;

			try {
				if (fileFormat.equalsIgnoreCase("xgmml")) {
					resultJO = xgmmlToJson(fileName, fileContents);
				} else if (fileFormat.equalsIgnoreCase("ucsc_pathway")) {
					resultJO = new JSONObject();
					resultJO.put("fileFormat", fileFormat);
				} else if (fileFormat.equalsIgnoreCase("sif")) {
					resultJO = new JSONObject();
					resultJO.put("fileFormat", fileFormat);
				}

				DatabaseService.writeJSONRPCTextResponse(resultJO, null, id,
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// test with:
			// http://localhost:8080/ib/pathwayBounce/test
			try {
				DatabaseService.writeTextResponse("pathway bounce service",
						resp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// TODO ///////////////////////////////////

	/**
	 * The JSON string contains nodes and edges of a pathway, and is used to
	 * build an xgmml file that is written to the HttpServletResponse.
	 * 
	 * @param resp
	 * @param json_string
	 *            This is a JSON string that contains 2 JSONArrays, called
	 *            "nodes" and "edges".
	 * @param fileName
	 */
	protected static void doJsonPathwayToXgmml(HttpServletResponse resp,
			String json_string, String fileName) {

		// write response
		resp.setHeader("Content-disposition", "attachment; filename=\""
				+ fileName + "\"");
		resp.setContentType("text/xml");

		try {
			String marshalled = jsonToXgmml(json_string, fileName);
			PrintWriter respWriter = resp.getWriter();
			String newString = marshalled.replaceAll("__NEW_LINE__", "\n");
			respWriter.println(newString);
			respWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The JSON string contains nodes and edges of a pathway, and is used to
	 * build an xgmml file that is returned is a String.
	 * 
	 * @param json_string
	 *            This is a JSON string that contains 2 JSONArrays, called
	 *            "nodes" and "edges".
	 * @param graphLabel
	 * @throws JSONException
	 */
	private static String jsonToXgmml(String json_string, String graphLabel)
			throws JSONException {
		JSONObject pathwayJO = new JSONObject(json_string);

		ObjectFactory factory = new ObjectFactory();

		GraphicGraph graph = factory.createGraphicGraph();
		graph.setLabel(graphLabel);

		List<Object> nodeOrEdgeList = graph.getNodeOrEdge();

		// handle nodes
		JSONArray nodesJA = pathwayJO.getJSONArray("nodes");
		for (int i = 0; i < nodesJA.length(); i++) {
			JSONObject nodeJO = nodesJA.getJSONObject(i);

			GraphicNode node = factory.createGraphicNode();
			node.setLabel(nodeJO.getString("label"));
			node.setId(nodeJO.getString("id"));

			// if (nodeJO.has("desc")) {
			// Att commonNameAtt = new Att();
			// commonNameAtt.setName("commonName");
			// commonNameAtt.setType(ObjectType.STRING);
			// commonNameAtt.setValue(nodeJO.getString("desc"));
			// node.getAtt().add(commonNameAtt);
			// }

			if (nodeJO.has("x") && nodeJO.has("y")) {
				Graphics graphics = factory.createGraphics();
				node.setGraphics(graphics);

				graphics.setX(nodeJO.getDouble("x"));
				graphics.setY(nodeJO.getDouble("y"));
			}

			nodeOrEdgeList.add(node);
		}

		// handle edges
		JSONArray edgesJA = pathwayJO.getJSONArray("edges");
		for (int i = 0; i < edgesJA.length(); i++) {
			JSONObject edgeJO = edgesJA.getJSONObject(i);

			GraphicEdge edge = factory.createGraphicEdge();
			edge.setLabel(edgeJO.getString("label"));
			edge.setSource(edgeJO.getString("source"));
			edge.setTarget(edgeJO.getString("target"));

			// track name will be used as the xgmml canonicalName
			Att canonicalNameAtt = new Att();
			canonicalNameAtt.setName("canonicalName");
			canonicalNameAtt.setType(ObjectType.STRING);
			canonicalNameAtt.setValue(edgeJO.getString("label"));
			edge.getAtt().add(canonicalNameAtt);

			nodeOrEdgeList.add(edge);
		}

		// marshal using JAXB
		StringWriter writer = new StringWriter();

		String contextPath = "edu.rpi.cs.xgmml";
		JAXBElement<GraphicGraph> graphElement = factory.createGraph(graph);

		try {
			JaxbConverter converter = new JaxbConverter(contextPath);
			converter.marshal(graphElement, writer);
		} catch (JAXBException e) {
			e.printStackTrace();
		}

		String marshalled = writer.toString();
		return marshalled;
	}

	/**
	 * Encode the xgmml into a JSONObject that can be used by
	 * client.PathwayData.
	 * 
	 * @param fileName
	 * @param xgmml
	 * @return
	 */
	private static JSONObject xgmmlToJson(String fileName, String xgmml) {
		JSONObject resultJO = new JSONObject();

		String contextPath = "edu.rpi.cs.xgmml";

		try {

			// get GraphicGraph from xgmml
			JaxbConverter xgmmlConverter = new JaxbConverter(contextPath);

			StringReader sr = new StringReader(xgmml);

			JAXBElement<?> element = xgmmlConverter.unmarshal(sr);
			GraphicGraph graph = (GraphicGraph) element.getValue();

			// get annotations
			Set<String> definedNodes = getAllNodes(graph);
			String jsonRcpResp = AnnoDbService.aliasToAnnotation2(definedNodes
					.toArray(new String[0]));

			JSONArray annotationsJA = new JSONObject(jsonRcpResp)
					.getJSONObject("result").getJSONArray("annotations");

			/**
			 * annotationsJO keyed by ID. These are annotations for IDs that may
			 * be HUGO or some other keyspace.
			 */
			HashMap<String, JSONObject> annotationsHashMap = new HashMap<String, JSONObject>();
			for (int i = 0; i < annotationsJA.length(); i++) {
				JSONObject annotationJO = annotationsJA.getJSONObject(i);
				String id = annotationJO.getString("ID");
				annotationsHashMap.put(id, annotationJO);
			}

			// get xgmml nodes and edges
			JSONArray nodesJA = new JSONArray();
			JSONArray edgesJA = new JSONArray();
			JSONObject metadataJO = new JSONObject();

			resultJO.put("concepts", nodesJA);
			resultJO.put("relations", edgesJA);
			resultJO.put("metadata", metadataJO);

			metadataJO.put("NCBI_species", "9606");
			metadataJO.put("name", fileName);
			metadataJO.put("source", "user");

			HashMap<String, String> xgmmlNodeIdToBiodeHashMap = new HashMap<String, String>();

			for (GraphicNode node : getXgmmlNodes(graph)) {
				JSONObject nodeJO = xgmmlNodeToJsonObject(annotationsHashMap,
						node);
				nodesJA.put(nodeJO);

				// xgmml node id
				String nodeXId = node.getId();

				String biode = nodeJO.getString("ID");

				xgmmlNodeIdToBiodeHashMap.put(nodeXId, biode);
			}

			for (GraphicEdge edge : getXgmmlEdges(graph)) {
				JSONObject edgeJO = new JSONObject();
				edgesJA.put(edgeJO);

				String sourceXId = edge.getSource();
				String targetXId = edge.getTarget();

				// get the biode that matches up with the xgmml node ID
				String sourceBiode = xgmmlNodeIdToBiodeHashMap.get(sourceXId);
				String targetBiode = xgmmlNodeIdToBiodeHashMap.get(targetXId);

				double weight = 1;
				try {
					weight = Double.valueOf(edge.getWeight());
				} catch (NullPointerException ne) {
					// do nothing... use the default weight of 1
				}

				edgeJO.put("1", sourceBiode);
				edgeJO.put("2", targetBiode);
				edgeJO.put("weight", weight);
			}

		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		return resultJO;
	}

	/**
	 * Convert the data in an xgmml.GraphicNode to a JSONObject. If no
	 * annotation was found, then create a generic nodeJO.
	 * 
	 * @param annotationsHashMap
	 *            annotationsJO keyed by ID
	 * @param node
	 *            an xgmml.GraphicNode
	 * @return JSONObject with keys: ID, common, desc, x, y.
	 * @throws JSONException
	 */
	private static JSONObject xgmmlNodeToJsonObject(
			HashMap<String, JSONObject> annotationsHashMap, GraphicNode node)
			throws JSONException {
		String label = node.getLabel();

		String canonicalName = null;
		for (Att att : node.getAtt()) {
			if (att.getName().equalsIgnoreCase("canonicalName")) {
				canonicalName = att.getValue();
			}
			break;
		}

		// check if there is annotation
		JSONObject nodeJO = null;
		if ((canonicalName != null)
				& (annotationsHashMap.containsKey(canonicalName))) {
			nodeJO = annotationsHashMap.get(canonicalName);
		} else if ((label != null) & (annotationsHashMap.containsKey(label))) {
			nodeJO = annotationsHashMap.get(label);
		} else {
			nodeJO = createGenericNodeJO(label, label, null);
		}

		// check if there are coordinates
		Graphics graphics = node.getGraphics();
		if (graphics != null && graphics.getX() != null
				&& graphics.getY() != null) {
			nodeJO.put("x", graphics.getX());
			nodeJO.put("y", graphics.getY());
		}

		return nodeJO;
	}

	/**
	 * Create a generic JSONObject for a node.
	 * 
	 * @param label
	 *            Used to set the value of "common".
	 * @param canonicalName
	 *            Used to set the value of "ID".
	 * @param description
	 *            Used to set the value of "desc".
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject createGenericNodeJO(String label,
			String canonicalName, String description) throws JSONException {
		JSONObject nodeJO;
		nodeJO = new JSONObject();
		nodeJO.put("common", label);
		nodeJO.put("ID", canonicalName);
		if (description != null) {
			nodeJO.put("desc", description);
		} else {
			nodeJO.put("desc", "no description");
		}
		return nodeJO;
	}

	/**
	 * Get the node canonicalName/label from the nodes in xgmml.GraphicGraph.
	 * 
	 * @param graph
	 * @return
	 */
	private static Set<String> getAllNodes(GraphicGraph graph) {
		Set<String> definedNodes = new HashSet<String>();

		for (GraphicNode node : getXgmmlNodes(graph)) {

			String label = node.getLabel();

			String canonicalName = null;
			List<Att> attList = node.getAtt();
			for (Att att : attList) {
				if (att.getName().equalsIgnoreCase("canonicalName")) {
					canonicalName = att.getValue();
				}
				break;
			}

			if (label != null) {
				definedNodes.add(label);
			}

			if (canonicalName != null) {
				definedNodes.add(canonicalName);
			}
		}

		return definedNodes;
	}

	/**
	 * Get node set from an xgmml graph.
	 * 
	 * @param graph
	 * @return
	 */
	private static Set<GraphicNode> getXgmmlNodes(GraphicGraph graph) {
		List<Object> nodesAndEdgesList = graph.getNodeOrEdge();
		Set<GraphicNode> nodeSet = new HashSet<GraphicNode>();
		for (Object obj : nodesAndEdgesList) {
			if (obj instanceof GraphicNode) {
				nodeSet.add((GraphicNode) obj);
			}
		}
		return nodeSet;
	}

	/**
	 * Get edge set from an xgmml graph.
	 * 
	 * @param graph
	 * @return
	 */
	private static Set<GraphicEdge> getXgmmlEdges(GraphicGraph graph) {
		List<Object> nodesAndEdgesList = graph.getNodeOrEdge();
		Set<GraphicEdge> edgeSet = new HashSet<GraphicEdge>();
		for (Object obj : nodesAndEdgesList) {
			if (obj instanceof GraphicEdge) {
				edgeSet.add((GraphicEdge) obj);
			}
		}
		return edgeSet;
	}
}
