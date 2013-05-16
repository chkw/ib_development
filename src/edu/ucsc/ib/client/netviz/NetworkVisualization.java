/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.netviz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeInfoCenter;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.MetanodeInfo;
import edu.ucsc.ib.client.MetanodeInfoCenter;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.datapanels.MousePanelListener;
import edu.ucsc.ib.drawpanel.client.DrawPanel;
import edu.ucsc.ib.drawpanel.client.Group;
import edu.ucsc.ib.drawpanel.client.Shape;
import edu.ucsc.ib.drawpanel.client.impl.DrawPanelImpl;

/**
 * The container class for visualising the network. This class supports adding
 * and removing tracks of edges, moving nodes, <strike>zooming and
 * scrolling</strike> the view.
 */
public class NetworkVisualization extends Composite implements
		MousePanelListener {
	private static final String CSS_CLASS = "ib-netVizPanel";

	protected static final String BIODE_DELIMITER = "\t";

	private final BiodeSet currentBiodes = new BiodeSet();

	private final BiodeSet selectedBiodes = new BiodeSet();

	/**
	 * The underlying DrawPanel.
	 */
	private DrawPanel dp;

	private Group selectLayer;

	private Group edgeLayer;

	private Group nodeLayer;

	/**
	 * Map of edgeGroups. Key is "biodeA<BIODE_DELIMITER>biodeB" Value is
	 * NetworkEdgeGroup(biodeA, biodeB).
	 * 
	 */
	private Map<String, NetworkEdgeGroup> edges;

	/**
	 * Hashmap from Biodes to BasicNode objects.
	 */
	private Map<String, BasicNode> nodes;

	/**
	 * Hashmap from metanode ID to Metanode.
	 */
	private Map<String, Metanode> metanodes;

	/**
	 * List of all tracks currently displayed.
	 */
	private List<Track> tracks;

	private List<WorkingSetListener> workingSetListeners;

	private List<SelectedSetListener> selectedSetListeners;

	private List<TrackSetListener> trackSetListeners;

	/**
	 * Store information about current biodes in NetViz.
	 */
	public BiodeInfoCenter BIC;

	public MetanodeInfoCenter MIC;

	public final MouseSelectListener msl = new MouseSelectListener(this);

	/**
	 * Value 0-100 to use for opacity.
	 */
	private int networkEdgeGroupOpacity = 100;

	public static final BasicNodeMouseEventsHandler basicNodeMouseEventsHandler = new BasicNodeMouseEventsHandler();

	/**
	 * Constructor for creating a NetworkVisualization with specialized
	 * NodeFactorys or EdgeFactorys.
	 * 
	 * @param width
	 *            valid HTML/CSS String width
	 * @param height
	 *            valid HTML/CSS String width
	 * 
	 */
	public NetworkVisualization(String width, String height) {

		this.dp = new DrawPanel(width, height);
		initWidget(this.dp);

		this.tracks = new ArrayList<Track>();
		this.workingSetListeners = new ArrayList<WorkingSetListener>();
		this.selectedSetListeners = new ArrayList<SelectedSetListener>();
		this.trackSetListeners = new ArrayList<TrackSetListener>();
		this.nodes = new HashMap<String, BasicNode>();
		this.metanodes = new HashMap<String, Metanode>();
		this.edges = new HashMap<String, NetworkEdgeGroup>();

		this.selectLayer = makeSelectionLayer();
		this.selectLayer.getElement_drawable().setId("selectLayer");

		this.edgeLayer = new Group();
		this.edgeLayer.getElement_drawable().setId("edgeLayer");

		this.nodeLayer = new Group();
		this.nodeLayer.getElement_drawable().setId("nodeLayer");

		this.dp.add(this.selectLayer);
		this.dp.add(edgeLayer);
		this.dp.add(nodeLayer);

		this.BIC = new BiodeInfoCenter();
		this.addWorkingSetListener(this.BIC);

		this.MIC = new MetanodeInfoCenter(this.BIC);
	}

	/**
	 * Deselects all currently selected biodes.
	 * 
	 */
	public void deselectAllBiodes() {
		if ((selectedBiodes != null) && (selectedBiodes.size() > 0)) {
			this.deselectBiodeSet(selectedBiodes);
		} else {
			LoggingDialogBox.log("nv: nothing to deselect");
		}
	}

	/**
	 * Sets all biodes to selected = true.
	 * 
	 * @return
	 */
	public BiodeSet selectAllBiodes() {
		this.selectBiodeSet(currentBiodes);
		return this.selectedBiodes;
	}

	/**
	 * Sets the status of a biode to selected = false. Uses
	 * deselecteBiodeSet(BiodeSet).
	 * 
	 * @param biode
	 *            the biode to deselect
	 * @return the deselected biode
	 */
	public BasicNode deselectBiode(String biode) {
		BiodeSet b = new BiodeSet();
		b.add(biode);
		this.deselectBiodeSet(b);
		return this.nodes.get(biode);
	}

	/**
	 * Sets the status of the biodes in a BiodeSet to be selected = false.
	 * 
	 * @param b
	 */
	public void deselectBiodeSet(BiodeSet b) {
		// keep only currently selected nodes

		if ((this.selectedBiodes != null) && (this.selectedBiodes.size() > 0)) {
			b.retainAll(this.selectedBiodes);
		}

		if (b.isEmpty()) {
			LoggingDialogBox.log("nv: nothing to deselect");
			return;
		}

		// iterate through each biode and set them to be deselected
		for (String biode : b) {
			this.nodes.get(biode).setDeselected();
		}

		// notify SelectedSetListeners
		for (SelectedSetListener ssl : selectedSetListeners) {
			ssl.deselectedBiodes(b);
		}

		// update list of selected biodes.
		this.selectedBiodes.removeAll(b);

	}

	/**
	 * Sets the status of a biode to selected = true. Uses
	 * {@link #selectBiodeSet(BiodeSet)}.
	 * 
	 * @param biode
	 * @return the BasicNode that was selected
	 */
	public BasicNode selectBiode(String biode) {
		BiodeSet b = new BiodeSet();
		b.add(biode);
		this.selectBiodeSet(b);
		return this.nodes.get(biode);
	}

	/**
	 * Sets the status of the biodes in a BiodeSet to be selected = true. Calls
	 * SelectedSetListener.selectedBiodes(BiodeSet b).
	 * 
	 * @param b
	 */
	public void selectBiodeSet(BiodeSet b) {
		// remove biodes that are already selected
		if ((this.selectedBiodes != null) && (this.selectedBiodes.size() > 0)) {
			b.removeAll(this.selectedBiodes);
		}

		if (b.isEmpty()) {
			LoggingDialogBox.log("NetworkVisualization: nothing to select");
			return;
		}

		// iterate through each biode and set them to be selected
		for (String biode : b) {
			this.nodes.get(biode).setSelected();
		}

		// notify SelectedSetListeners
		for (SelectedSetListener ssl : selectedSetListeners) {
			ssl.selectedBiodes(b);
		}

		// append list of selected biodes.
		this.selectedBiodes.addAll(b);
	}

	/**
	 * Add a BiodeSet of Biodes' NetworkNodes to the NetViz, if they don't
	 * already exist. The BiodeInfo for creating the NetworkNodes is retrieved
	 * from the BiodeInfoCenter, so they must be added to the BIC *before*
	 * calling this method. Calls WorkingSetListener.addedBiodes(BiodeSet b).
	 * 
	 * @param bs
	 */
	public void addBiode(BiodeSet bs) {
		if (bs.isEmpty()) {
			return;
		}

		// for each biode ...
		for (String biode : bs) {
			if (biode.isEmpty()) {
				bs.remove(biode);
				LoggingDialogBox.log("not adding biode with no name: " + biode);
				continue;
			}

			// get BiodeInfo
			BiodeInfo bi = this.BIC.getBiodeInfo(biode);

			if (bi == null) {
				// bs.remove(biode);
				LoggingDialogBox.log("not adding because of null info: "
						+ biode);
				continue;
			}

			// create NetworkNode if doesn't exist
			if (this.containsNode(biode)) {
				// bs.remove(biode);
				LoggingDialogBox.log("not adding because already exists: "
						+ biode);
				continue;
			}

			// add a node for the biode
			this.addNetworkNode(bi);
		}
		// append list of current biodes
		this.currentBiodes.addAll(bs);

		// workingSetListeners to detect added and removed biodes.
		for (WorkingSetListener wsl : this.workingSetListeners) {
			wsl.addedBiodes(bs);
		}

		// TODO layout
		// SpringLayout
		// .doLayout(new HashMap<String, BasicNode>(this.nodes), false);

		performSpringLayout(false);
	}

	/**
	 * Add the BiodeInfo to the netViz's BiodeInfoCenter.
	 * 
	 * @param bi
	 */
	public void addBiodeInfoToBIC(BiodeInfo bi) {
		if ((bi != null) && (!bi.getSystematicName().isEmpty())) {
			this.BIC.addBiodeInfo(bi);
		}
	}

	/**
	 * Add the BiodeInfo objects from a HashMap of BiodeInfo objects to the
	 * netViz's BiodeInfoCenter.
	 * 
	 * @param biodeInfoHash
	 */
	public void addBiodeInfoToBIC(HashMap<String, BiodeInfo> biodeInfoHash) {
		if (!biodeInfoHash.isEmpty()) {
			for (String biode : biodeInfoHash.keySet()) {
				this.BIC.addBiodeInfo(biodeInfoHash.get(biode));
			}
		}
	}

	/**
	 * Adds the edgeGroup to netViz.
	 * 
	 * @param edgeGroup
	 * 
	 */
	protected void addEdgeGroup(NetworkEdgeGroup edgeGroup) {
		// edges.put(edgeGroup.getFirstBiode() + BIODE_DELIMITER
		// + edgeGroup.getSecondBiode(), edgeGroup);
		this.edges.put(edgeGroup.getName(), edgeGroup);
		this.edgeLayer.add(edgeGroup);
	}

	/**
	 * Create and add a NetworkNode to the NetViz's nodeLayer. Uses the node
	 * attributes from the BiodeInfo object, if available.
	 * 
	 * @param bi
	 */
	private void addNetworkNode(BiodeInfo bi) {
		NetworkNode n = new NetworkNode(bi, this);

		double biXpos = bi.getXPosition();
		double biYpos = bi.getYPosition();

		// position in BiodeInfo is set to (0,0) by default
		if (biXpos == 0 && biYpos == 0) {
			// use positions in BiodeInfo, if available
			double padding = 40;
			double nvHeight = this.getVportHeight();
			double nvWidth = this.getVportWidth();

			nvHeight = (nvHeight <= padding) ? 500 : nvHeight - padding;
			nvWidth = (nvWidth <= padding) ? 500 : nvWidth - padding;

			// set an initial position
			n.setPositionInVpBounds(Math.random() * nvWidth + padding / 2,
					Math.random() * nvHeight + padding / 2);
		} else {
			n.setPositionInVpBounds(biXpos, biYpos);
		}

		if (bi.getColor() == null || bi.getColor() == "") {
			// do nothing. use default.
		} else {
			n.setColor(bi.getColor());
		}

		if (bi.getShapeCode() < 0) {
			// do nothing... use default
		} else {
			n.setShape(bi.getShapeCode());
		}

		// add to nv's list of nodes
		this.nodes.put(n.getID(), n);

		// add to the nodeLayer
		this.nodeLayer.add(n);

		n.flash();
	}

	/**
	 * Add a WorkingSetListener to respond to changes in the working set of
	 * biodes. Checks if already in list before adding.
	 * 
	 * @param wsl
	 */
	public void addWorkingSetListener(final WorkingSetListener wsl) {
		if (!workingSetListeners.contains(wsl)) {
			workingSetListeners.add(wsl);
		}
		LoggingDialogBox.log("num wsl: " + workingSetListeners.size());
	}

	/**
	 * Remove a WorkingSetListener if it is in the list.
	 * 
	 * @param wsl
	 */
	public void removeWorkingSetListener(final WorkingSetListener wsl) {
		if (workingSetListeners.contains(wsl)) {
			workingSetListeners.remove(wsl);
		}
		LoggingDialogBox.log("num wsl: " + workingSetListeners.size());
	}

	/**
	 * Get the list of WorkingSetListener objects.
	 * 
	 * @return
	 */
	public List<WorkingSetListener> getWorkingSetListeners() {
		return workingSetListeners;
	}

	/**
	 * Add a SelectedSetListener to respond to changes in the selected set of
	 * biodes.
	 * 
	 * @param ssl
	 */
	public void addSelectedSetListener(SelectedSetListener ssl) {
		if (!selectedSetListeners.contains(ssl)) {
			selectedSetListeners.add(ssl);
		}
		LoggingDialogBox.log("num ssl: " + selectedSetListeners.size());
	}

	/**
	 * Remove a SelectedSetListener.
	 * 
	 * @param ssl
	 */
	public void removeSelectedSetListener(SelectedSetListener ssl) {
		if (!selectedSetListeners.contains(ssl)) {
			selectedSetListeners.remove(ssl);
		}
		LoggingDialogBox.log("num ssl: " + selectedSetListeners.size());
	}

	/**
	 * Add a TrackSetListener to respond to changes in the set of Tracks.
	 * 
	 * @param tsl
	 */
	public void addTrackSetListener(TrackSetListener tsl) {
		this.trackSetListeners.add(tsl);
	}

	/**
	 * Remove a TrackSetListener.
	 * 
	 * @param tsl
	 */
	public void removeTrackSetListener(TrackSetListener tsl) {
		this.trackSetListeners.remove(tsl);
	}

	/**
	 * Get all the NetworkEdgeGroups.
	 * 
	 * @return
	 */
	public HashSet<NetworkEdgeGroup> getEdgeGroups() {
		return (HashSet<NetworkEdgeGroup>) this.edges.values();
	}

	/**
	 * Get all NetworkEdgeGroups that have a biode in the specified BiodeSet.
	 * 
	 * @param bs
	 * @return
	 */
	public HashSet<NetworkEdgeGroup> getEdgeGroups(BiodeSet bs) {
		HashSet<NetworkEdgeGroup> resultHashSet = new HashSet<NetworkEdgeGroup>();

		for (NetworkEdgeGroup networkEdgeGroup : this.edges.values()) {
			if (bs.contains(networkEdgeGroup.getFirstBiode())
					|| bs.contains(networkEdgeGroup.getSecondBiode())) {
				resultHashSet.add(networkEdgeGroup);
			}
		}

		return resultHashSet;
	}

	/**
	 * Retrieves the edgeGroup connecting specified biodes. If no edge group
	 * exists, a new one will be created.
	 * 
	 * @param biodeA
	 * @param biodeB
	 * @return the edge group
	 */
	public NetworkEdgeGroup getEdgeGroup(String biodeA, String biodeB) {

		// set the order of biodes
		if (biodeA.compareTo(biodeB) > 0) { // wrong order need to swap
			String swap = biodeB;
			biodeB = biodeA;
			biodeA = swap;
		}

		// return an edgeGroup
		if (this.edges.containsKey(biodeA
				+ NetworkVisualization.BIODE_DELIMITER + biodeB)) {
			// existing edgeGroup

			return this.edges.get(biodeA + NetworkVisualization.BIODE_DELIMITER
					+ biodeB);
		} else {
			// new edgeGroup between existing nodes
			BasicNode nodeA = this.nodes.get(biodeA);
			BasicNode nodeB = this.nodes.get(biodeB);

			// LoggingDialogBox.log("got nodes");

			NetworkEdgeGroup newEdge = new NetworkEdgeGroup(nodeA, nodeB,
					networkEdgeGroupOpacity);

			// LoggingDialogBox.log("created edgeGroup");

			nodeA.attachNetworkEdgeGroup(newEdge);
			nodeB.attachNetworkEdgeGroup(newEdge);

			// LoggingDialogBox.log("attached edgeGroup to nodes");

			this.addEdgeGroup(newEdge);

			// LoggingDialogBox.log("added edgeGroup to netviz");

			return newEdge;
		}
	}

	/**
	 * Set the network edge opacity. This affects all current and future network
	 * edges.
	 * 
	 * @param opacity
	 *            some value 0-100
	 */
	public void setNetworkEdgeOpacity(int opacity) {
		networkEdgeGroupOpacity = opacity;
		for (NetworkEdgeGroup neg : this.edges.values()) {
			neg.setOpacity(networkEdgeGroupOpacity);
		}
	}

	/**
	 * Update the position of specified NetworkEdgeGroups.
	 * 
	 * @param networkEdgeGroupHashSet
	 */
	public void updateEdgePositions(
			HashSet<NetworkEdgeGroup> networkEdgeGroupHashSet) {
		for (NetworkEdgeGroup networkEdgeGroup : networkEdgeGroupHashSet) {
			networkEdgeGroup.updatePosition();
		}
	}

	/**
	 * Get all the BasicNodes in the graph.
	 * 
	 * @return Collection consisting of BasicNodes
	 */
	public Collection<BasicNode> getBasicNodes() {
		return this.nodes.values();
	}

	/**
	 * Get the BiodeSet of current node IDs.
	 * 
	 * @return
	 */
	public BiodeSet getCurrentNodeIds() {
		return this.currentBiodes;
	}

	/**
	 * Get the current Node Ids in the specified systemSpace.
	 * 
	 * @param systemSpace
	 * @return
	 */
	public BiodeSet getCurrentNodeIds(String systemSpace) {
		BiodeSet bs = new BiodeSet();

		for (String biode : this.currentBiodes) {
			BiodeInfo bi = this.BIC.getBiodeInfo(biode);
			if (bi.getSystemSpace().equalsIgnoreCase(systemSpace)) {
				bs.add(biode);
			}
		}

		return bs;
	}

	/**
	 * 
	 * @return BiodeSet of selected node IDs
	 */
	public BiodeSet getSelectedNodeIds() {
		return this.selectedBiodes;
	}

	/**
	 * Get biodes for nodes in the specified organism.
	 * 
	 * @param selectedOrganismID
	 * @param onlySelected
	 *            if true, only consider selected nodes
	 * @return
	 */
	public BiodeSet getNodeIdsInOrganism(final String selectedOrganismID,
			final boolean onlySelected) {
		BiodeSet resultBS = new BiodeSet();

		BiodeSet poolBS;
		if (onlySelected) {
			poolBS = this.getSelectedNodeIds();
		} else {
			poolBS = this.getCurrentNodeIds();
		}

		for (String biode : poolBS) {
			if (this.BIC.getBiodeInfo(biode).getSystemSpace()
					.equalsIgnoreCase(selectedOrganismID)) {
				resultBS.add(biode);
			}
		}

		return resultBS;
	}

	/**
	 * 
	 * @return List of track objects
	 */
	public List<Track> getTracks() {
		return this.tracks;
	}

	/**
	 * Get track names in a HashSet.
	 * 
	 * @param includeStaticTracks
	 * 
	 * @return
	 */
	public HashSet<String> getTrackNamesHash(final boolean includeStaticTracks) {
		HashSet<String> trackSet = new HashSet<String>();
		for (Track track : this.tracks) {
			if (!includeStaticTracks && (track instanceof StaticTrack)) {
				continue;
			}
			trackSet.add(track.getName());
		}
		return trackSet;
	}

	/**
	 * Get ALL track names in HashSet, including StaticTracks.
	 */
	public HashSet<String> getTrackNamesHash() {
		return getTrackNamesHash(true);
	}

	/**
	 * 
	 * @return Set of NetworkEdgeGroup names
	 */
	public Set<String> getEdges() {
		return new HashSet<String>(this.edges.keySet());
	}

	/**
	 * Remove an edge from netViz's list of edge groups. Should only be called
	 * when a biode is being removed or if the last track in the edge has been
	 * turned off. Also, need to remove the edgeGroup form each of the biode's
	 * list of edgeGroups.
	 * 
	 * @param biodeA
	 *            String
	 * @param biodeB
	 *            String
	 * @return NetworkEdgeGroup
	 */
	public NetworkEdgeGroup removeNetworkEdgeGroup(String biodeA, String biodeB) {
		String edgeGroupName = biodeA + NetworkVisualization.BIODE_DELIMITER
				+ biodeB;
		this.edges.get(edgeGroupName).removeFromNodes();
		return this.edges.remove(edgeGroupName);
	}

	/**
	 * Remove the NetworkEdgeGroup. Wrapper for removeNetworkEdgeGroup(String
	 * biodeA, String biodeB).
	 * 
	 * @param negName
	 *            String
	 * @return NetworkEdgeGroup
	 */
	public NetworkEdgeGroup removeNetworkEdgeGroup(String negName) {
		String[] biodes = negName
				.split(NetworkVisualization.BIODE_DELIMITER, 2);
		return this.removeNetworkEdgeGroup(biodes[0], biodes[1]);
	}

	/**
	 * Remove empty NetworkEdgeGroups from the NetViz's map of edge groups. If
	 * the NetworkEdgeGroup's tracks.size() < 1, it is removed from the map.
	 * Also, remove the edgeGroup from the 2 nodes' lists of edgeGroups.
	 */
	private void removeEmptyEdgeGroups() {
		String negName;
		NetworkEdgeGroup neg;
		Set<String> edgeNames = new HashSet<String>(this.edges.keySet());
		for (Iterator<String> i = edgeNames.iterator(); i.hasNext();) {
			negName = i.next();
			neg = this.edges.get(negName);
			if (neg.tracks.size() < 1) {
				this.removeNetworkEdgeGroup(negName);
			}
		}
	}

	/**
	 * Create a BiodeSet of one biode and call removeBiodeSet on it.
	 * 
	 * @param biode
	 */
	public void removeBiode(String biode) {
		BiodeSet b = new BiodeSet();
		b.add(biode);
		removeBiodeSet(b);
	}

	/**
	 * Remove a BiodeSet of NetworkNodes. Removes only biodes that have no
	 * metanode memberships.
	 * 
	 * @param b
	 */
	public void removeBiodeSet(BiodeSet b) {
		// only remove biodes that have no metanode memberships
		for (String biode : b) {
			BiodeInfo bi = this.BIC.getBiodeInfo(biode);
			if (bi.getAllMemberships().size() > 0) {
				b.remove(biode);
			}
		}

		if (b.size() == 0) {
			return;
		}

		// notify WorkingSetListeners
		for (WorkingSetListener wsl : this.workingSetListeners) {
			wsl.removedBiodes(b);
		}

		// remove NetworkNode objects
		for (String biode : b) {
			LoggingDialogBox.log("trying to remove node for: " + biode);
			BasicNode n = this.getNode(biode);
			this.removeNetworkNode(n);
			LoggingDialogBox.log("done removing node for: " + biode);
		}

		// remove biode from NetViz's data structures
		this.currentBiodes.removeAll(b);
		this.selectedBiodes.removeAll(b);
	}

	/**
	 * Remove the NetworkNode from the NetworkVisualization.
	 * 
	 * @param n
	 *            the NetworkNode to remove
	 */
	private void removeNetworkNode(BasicNode n) {
		List<NetworkEdgeGroup> negs = n.getAllNetworkEdgeGroups();

		for (NetworkEdgeGroup neg : negs) {
			BasicNode other;
			if (neg.getFirstBiode().equals(n.getID())) {
				other = this.getNode(neg.getSecondBiode());
			} else {
				other = this.getNode(neg.getFirstBiode());
			}
			if (!other.getAllNetworkEdgeGroups().remove(neg)) {
				LoggingDialogBox
						.log("Couldn't remove NEG from other network node's list: "
								+ neg.getFirstBiode()
								+ " "
								+ neg.getSecondBiode());
			} else {
				LoggingDialogBox
						.log("Removed NEG from other network node's list: "
								+ neg.getFirstBiode() + " "
								+ neg.getSecondBiode());
			}
			Object result = this.edges.remove(neg.getFirstBiode()
					+ NetworkVisualization.BIODE_DELIMITER
					+ neg.getSecondBiode());
			if (result == null) {
				LoggingDialogBox.log("couldn't remove NEG: "
						+ neg.getFirstBiode() + " " + neg.getSecondBiode());
			} else {
				LoggingDialogBox.log("Removed NEG: " + neg.getFirstBiode()
						+ " " + neg.getSecondBiode());
			}
			this.edgeLayer.remove(neg);
		}

		this.nodes.remove(n.getID());
		n.removeFromParent();
		n.clear();
	}

	/**
	 * User "turning on" a track.
	 * 
	 * @param t
	 *            Track
	 */
	public void addTrack(Track t) {
		LoggingDialogBox.log("nv adding track: " + t.getName());
		tracks.add(t);
		workingSetListeners.add(t);
		t.addedBiodes(currentBiodes);
		TrackSetListener tsl;
		for (int i = 0; i < trackSetListeners.size(); ++i) {
			tsl = trackSetListeners.get(i);
			tsl.trackAdded(t);
		}
	}

	/**
	 * User "turning off" a track.
	 * 
	 * @param t
	 *            Track
	 */
	public void removeTrack(Track t) {
		LoggingDialogBox.log("nv removing track: " + t.getName());
		// remove the WorkingSetListener, t
		this.removeWorkingSetListener(t);
		// remove edges from the track
		t.removeAllEdges();
		// remove the track from netViz's list of tracks
		this.tracks.remove(t);
		TrackSetListener tsl;
		// notify trackSetListeners of removal
		for (int i = 0; i < this.trackSetListeners.size(); ++i) {
			tsl = this.trackSetListeners.get(i);
			tsl.trackRemoved(t);
		}
		this.removeEmptyEdgeGroups();
	}

	/**
	 * Retrieve a BasicNode by its biode.
	 * 
	 * @param biode
	 *            String Biode to retrieve
	 * @return the biode
	 */
	public BasicNode getNode(String biode) {
		return nodes.get(biode);
	}

	/**
	 * Retrieve a Metanode by its metanodeId.
	 * 
	 * @param biode
	 *            String metanodeId to retrieve
	 * @return the Metanode
	 */
	public Metanode getMetanode(String metanodeId) {
		return metanodes.get(metanodeId);
	}

	/**
	 * Find out if this biode exists as a key in nodes mapping.
	 * 
	 * @param biode
	 * @return
	 */
	public boolean containsNode(String biode) {
		return this.nodes.containsKey(biode);
	}

	/**
	 * Get width of viewport.
	 * 
	 * @return width in pixels
	 */
	public double getVportWidth() {
		return this.dp.getSVGWidth();
	}

	/**
	 * Get height of viewport.
	 * 
	 * @return height in pixels
	 */
	public double getVportHeight() {
		return this.dp.getSVGHeight();
	}

	/**
	 * Get width of Bounding Box. Note: This method uses e.getBBox(), which is
	 * problematic in Firefox2.0.0.3
	 * 
	 * @return width in pixels
	 */
	public double getBBoxWidth() {
		return this.dp.getWidth();
	}

	/**
	 * Get height of Bounding Box. Note: This method uses e.getBBox(), which is
	 * problematic in Firefox2.0.0.3
	 * 
	 * @return height in pixels
	 */
	public double getBBoxHeight() {
		return this.dp.getHeight();
	}

	/**
	 * Toggles nodeLayer visible status between true and false.
	 * 
	 */
	public void toggleNodeLayerVisible() {
		if (this.nodeLayer.isVisible()) {
			this.nodeLayer.setVisible(false);
		} else {
			this.nodeLayer.setVisible(true);
		}
	}

	/**
	 * Toggles edgeLayer visible status between true and false.
	 * 
	 */
	public void toggleEdgeLayerVisible() {
		if (this.edgeLayer.isVisible()) {
			this.edgeLayer.setVisible(false);
		} else {
			this.edgeLayer.setVisible(true);
		}
	}

	/**
	 * Public method for doing basic spring embedded layout. If there are many
	 * NetworkEdgeGroups, the edgeLayer is turned off while doing layout.
	 * 
	 * @param pinSelected
	 *            if true, selected nodes will not move
	 * 
	 */
	public void doSpringLayout(final boolean pinSelected) {
		// turn off edgeLayer if lots of edges - can improve performance
		int maxEdgeCount = Integer.MAX_VALUE;

		if (this.edges.size() > maxEdgeCount) {
			this.edgeLayer.setVisible(false);
			Timer t = new Timer() {
				public void run() {
					performSpringLayout(pinSelected);
					toggleEdgeLayerVisible();
				}
			};
			// Schedule the timer to run once in ? ms.
			t.schedule(100);
		} else {
			this.performSpringLayout(pinSelected);
		}
	}

	/**
	 * Perform random layout
	 */
	public void doRandomLayout() {
		final double nvHeight = this.getVportHeight();
		final double nvWidth = this.getVportWidth();

		for (BasicNode node : this.nodes.values()) {
			if (node.isSelected()) {
				continue;
			}
			node.setPositionInVpBounds(Math.random() * nvWidth, Math.random()
					* nvHeight);
		}
	}

	/**
	 * Private method for doing ring layout.
	 * 
	 */
	public void doRingLayout() {
		RingLayout.doLayout(this.getSelectedNodeIds(), this.nodes,
				this.getVportWidth(), this.getVportHeight());
	}

	/**
	 * Private method for doing basic spring embedded layout.
	 * 
	 * @param pinSelected
	 *            if true, selected nodes will not move
	 * 
	 */
	private void performSpringLayout(boolean pinSelected) {
		SpringLayout.doLayout(new HashMap<String, BasicNode>(this.nodes),
				pinSelected);
	}

	/**
	 * Basic circle layout.
	 * 
	 */
	public void doCircleLayout() {
		CircleLayout.doLayout(this.nodes, this.getVportWidth(),
				this.getVportHeight());
	}

	/**
	 * Create a Group element that can be used to select nodes in the NetViz.
	 * The selectionBox picks up the mouse events.
	 * 
	 * @return Group element
	 */
	private Group makeSelectionLayer() {

		// make selectionBox

		SelectionLayer sl = new SelectionLayer(Shape.RECT);

		// attach mouse listeners
		sl.addMouseDownHandler(msl);
		sl.addMouseMoveHandler(msl);
		sl.addMouseUpHandler(msl);

		Group layer = new Group();
		layer.add(sl);

		return layer;
	}

	/**
	 * This is meant to be a rectangle(layer) to detect mouse clicks and
	 * draggings that are not captured by nodes/edges. The dragging should add a
	 * rectangle to the layer. Nodes contained within should be set "selected".
	 * 
	 * @author cw
	 * 
	 */
	private class SelectionLayer extends Shape implements HasMouseDownHandlers,
			HasMouseMoveHandlers, HasMouseUpHandlers {

		protected SelectionLayer(int shapeCode) {
			super(shapeCode);
			// this.setFill("#E0FFFF");
			this.setFill("white");
			this.setStroke("black");

			// making it bigger than the netViz size
			// TODO should probably not be hard-coded
			int xSize = 3000;
			int ySize = 3000;

			this.setSize(xSize);
			this.setSizeY(ySize);
			this.setPosition_drawable(Math.rint(xSize / 2),
					Math.rint(ySize / 2));
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

	}

	/**
	 * Get the DrawPanel's SVG dimensions.
	 * 
	 * @return array of dimensions for x and y
	 */
	public int[] getDPdimensions() {
		String temp = this.dp.getSVGWidth() + "";
		int w = Integer.parseInt(temp);
		temp = this.dp.getSVGHeight() + "";
		int h = Integer.parseInt(temp);
		int[] dimensions = { w, h };
		return dimensions;
	}

	public void addSelectBox(Shape b) {
		this.selectLayer.add(b);
	}

	/**
	 * Get a BiodeSet that identifies all BasicNodes within box defined in
	 * parameters.
	 * 
	 * @param x1
	 *            double
	 * @param y1
	 *            double
	 * @param x2
	 *            double
	 * @param y2
	 *            double
	 * @return BiodeSet
	 */
	public BiodeSet inSelectionBox(double x1, double y1, double x2, double y2) {
		// first check order of coords
		if (x1 > x2) {
			double temp = x1;
			x1 = x2;
			x2 = temp;
		}
		if (y1 > y2) {
			double temp = y1;
			y1 = y2;
			y2 = temp;
		}
		BiodeSet b = new BiodeSet();
		Set<String> biodes = new HashSet<String>(this.nodes.keySet());
		BasicNode node;
		double nodeX;
		double nodeY;
		for (String candidate : biodes) {
			node = this.nodes.get(candidate);
			nodeX = node.getX_drawable();
			nodeY = node.getY_drawable();
			if ((x1 <= nodeX && nodeX <= x2) && (y1 <= nodeY && nodeY <= y2)) {
				b.add(candidate);
				LoggingDialogBox.log(candidate);
			}
		}
		return b;
	}

	/**
	 * Select the nodes in the box defined by coordinates. Used with
	 * MouseSelectListener.
	 */
	public void selectInBox(double x1, double y1, double x2, double y2) {
		BiodeSet b = inSelectionBox(x1, y1, x2, y2);
		if (b.size() > 0) {
			selectBiodeSet(b);
		} else {
			deselectAllBiodes();
		}
	}

	/**
	 * Invert the selection of BasicNodes. That is, currently selected nodes
	 * will be deselected. Currently deselected nodes will be selected.
	 * 
	 */
	public void invertSelection() {
		for (String biode : this.getCurrentNodeIds()) {
			this.getNode(biode).toggleSelected();
		}
	}

	/**
	 * Get the unconnected nodes in currentBiodes.
	 * 
	 * @return BiodeSet
	 */
	public BiodeSet getUnconnectedBiodes() {
		BiodeSet unconnectedBiodes = new BiodeSet();
		for (String biode : this.getCurrentNodeIds()) {
			BasicNode node = this.getNode(biode);
			if (node.getAdjacentNodes().size() < 1) {
				unconnectedBiodes.add(biode);
			}
		}
		return unconnectedBiodes;
	}

	/**
	 * Remove unconnected NetworkNodes from the netViz. Returns the BiodeSet for
	 * unconnected nodes.
	 * 
	 * @return BiodeSet
	 */
	public BiodeSet removeUnconnectedNodes() {
		LoggingDialogBox.log("removeUnconnectedNodes");
		BiodeSet unconnectedBiodes = this.getUnconnectedBiodes();
		this.removeBiodeSet(unconnectedBiodes);
		return unconnectedBiodes;
	}

	/**
	 * Select unconnected NetworkNodes from the netViz. Returns the BiodeSet for
	 * unconnected nodes.
	 * 
	 * @return BiodeSet
	 */
	public BiodeSet selectUnconnectedNodes() {
		LoggingDialogBox.log("selectUnconnectedNodes");
		BiodeSet unconnectedBiodes = this.getUnconnectedBiodes();
		this.selectBiodeSet(unconnectedBiodes);
		return unconnectedBiodes;
	}

	/**
	 * Remove the biodes from the input BiodeSet that are also in the current
	 * BiodeSet for this netViz, and then return a BiodeSet containing the
	 * remaining biodes.
	 * 
	 * @param bs
	 * @return
	 */
	public BiodeSet getNewBiodes(BiodeSet bs) {
		bs.removeAll(this.currentBiodes);
		return bs;
	}

	/**
	 * Change the shape of the BasicNode for each biode in the BiodeSet.
	 * 
	 * @param bs
	 * @param shapeCode
	 *            (as defined in Shape.java)
	 */
	public void changeBiodeSetShape(BiodeSet bs, int shapeCode) {
		for (String biode : bs) {
			BasicNode node = this.nodes.get(biode);
			node.setShape(shapeCode);
		}
	}

	/**
	 * Change the color of the BasicNode for each biode in the BiodeSet.
	 * 
	 * @param bs
	 * @param colorCode
	 */
	public void changeBiodeSetColor(BiodeSet bs, String colorCode) {
		for (String biode : bs) {
			BasicNode node = this.nodes.get(biode);
			node.setColor(colorCode);
		}
	}

	/**
	 * Select a BiodeSet's neighbors only, deselecting the original biodes.
	 * 
	 * @param keepQuery
	 * @param bs
	 */
	public void selectCurrentNeighbors(boolean keepQuery) {
		BiodeSet originallySelected = new BiodeSet(this.selectedBiodes);
		if (!keepQuery) {
			this.deselectBiodeSet(originallySelected);
		}
		BiodeSet neighbors = new BiodeSet();
		for (String biode : originallySelected) {
			BasicNode node = this.getNode(biode);
			neighbors.addAll(new BiodeSet(node.getAdjacentNodes()));
		}
		neighbors.removeAll(originallySelected);
		this.selectBiodeSet(neighbors);
	}

	/**
	 * Get the drawPanel's SVG as a String that can be saved as an SVG file.
	 * 
	 * @return
	 */
	public String getDrawPanelSVG() {
		return this.dp.toString();
	}

	/**
	 * Get a JSONObject that has nodes and edges that can be used to build up an
	 * xgmml file.
	 * 
	 * @return
	 */
	public JSONObject getJsonForXgmml() {

		JSONObject resultJO = new JSONObject();

		JSONArray edgesJA = new JSONArray();

		// go through each edge group
		for (String edgeName : this.edges.keySet()) {
			NetworkEdgeGroup neg = this.edges.get(edgeName);
			JSONString source = new JSONString(neg.getFirstBiode());
			JSONString target = new JSONString(neg.getSecondBiode());

			for (String trackName : neg.getTrackNames()) {
				JSONObject edgeJO = new JSONObject();
				edgesJA.set(edgesJA.size(), edgeJO);

				edgeJO.put("label", new JSONString(trackName));
				edgeJO.put("source", source);
				edgeJO.put("target", target);
			}
		}

		JSONArray nodesJA = new JSONArray();

		// go through each node
		for (String biode : this.getCurrentNodeIds()) {
			BiodeInfo bi = this.BIC.getBiodeInfo(biode);
			JSONString id = new JSONString(bi.getSystematicName());
			JSONString label = new JSONString(bi.getCommonName());

			BasicNode node = this.getNode(biode);
			JSONNumber x = new JSONNumber(Math.round(node.getX_drawable()));
			JSONNumber y = new JSONNumber(Math.round(node.getY_drawable()));

			JSONObject nodeJO = new JSONObject();
			nodesJA.set(nodesJA.size(), nodeJO);

			nodeJO.put("id", id);
			nodeJO.put("label", label);
			nodeJO.put("x", x);
			nodeJO.put("y", y);
		}

		resultJO.put("nodes", nodesJA);
		resultJO.put("edges", edgesJA);

		return resultJO;
	}

	/**
	 * Get the links for this network in links.tab format.
	 * 
	 * @return
	 */
	public String getLinksFileString() {

		StringBuffer strBuf = new StringBuffer();
		for (String edgeName : this.edges.keySet()) {
			strBuf.append(edgeName + "\t");
			NetworkEdgeGroup neg = this.edges.get(edgeName);

			for (String trackName : neg.getTrackNames()) {
				strBuf.append(trackName + " ");
			}

			int index = strBuf.lastIndexOf(" ");
			strBuf = strBuf.replace(index, index + 1, "");

			strBuf.append("__NEW_LINE__");
		}
		return strBuf.toString();
	}

	// for debugging
	public void allNodeCoords() {
		for (String biode : this.nodes.keySet()) {
			BasicNode node = this.nodes.get(biode);
			LoggingDialogBox.log(node.getID() + " at " + node.getX_drawable()
					+ "," + node.getY_drawable());
		}
	}

	// for debugging
	public String getDPsize() {
		StringBuffer sb = new StringBuffer();
		sb.append("w:");
		sb.append(DrawPanelImpl.getAttribute(this.dp.getElement(), "width"));
		sb.append("\n");
		sb.append("h:");
		sb.append(DrawPanelImpl.getAttribute(this.dp.getElement(), "height"));
		return sb.toString();
	}

	/**
	 * Create and add a metanode based on the metanodeInfo.
	 * 
	 * @param mi
	 */

	/**
	 * Create and add a NetworkNode to the NetViz's nodeLayer. Uses the node
	 * attributes from the BiodeInfo object, if available.
	 * 
	 * @param mi
	 */
	public void addMetanode(MetanodeInfo mi) {
		Metanode mn = new Metanode(mi, this);

		double miXpos = mi.getXPosition();
		double miYpos = mi.getYPosition();

		if (miXpos * miYpos == 0) {
			// use positions in MetanodeInfo, if available
			double padding = 40;
			double nvHeight = this.getOffsetHeight();
			double nvWidth = this.getOffsetWidth();

			nvHeight = (nvHeight <= padding) ? 500 : nvHeight - padding;
			nvWidth = (nvWidth <= padding) ? 500 : nvWidth - padding;

			// set an initial position
			mn.setPosition_drawable(Math.random() * nvWidth + padding / 2,
					Math.random() * nvHeight + padding / 2);
		} else {
			mn.setPositionInVpBounds(miXpos, miYpos);
		}

		if (mi.getColor() == null || mi.getColor() == "") {
			// do nothing. use default.
		} else {
			mn.setColor(mi.getColor());
		}

		if (mi.getShapeCode() < 0) {
			// do nothing... use default
		} else {
			mn.setShape(mi.getShapeCode());
		}

		// add to nv's list of metanodes
		this.metanodes.put(mn.getID(), mn);

		// add to the nodeLayer
		this.nodeLayer.add(mn);

		mn.flash();
	}

	/**
	 * Remove the metanode.
	 * 
	 * @param mn
	 */
	public void removeMetanode(Metanode mn) {
		this.metanodes.remove(mn.getID());
		mn.removeFromParent();
		mn.clear();
	}

	/**
	 * Use images for visualization instead of SVG shapes.
	 * 
	 * @param bs
	 */
	public void switchNodeToImage(boolean useImage, BiodeSet bs) {
		for (String biode : bs) {
			BasicNode nn = this.nodes.get(biode);
			if (!nn.getImageURL().equalsIgnoreCase("")) {
				nn.useImage(useImage);
			}
		}
	}

	/**
	 * Find the nodes with the most neighbors. Only returns the node(s) with the
	 * most neighbors.
	 * 
	 * @return
	 */
	public HashSet<BasicNode> getHubNodes() {
		HashSet<BasicNode> hubsHashSet = new HashSet<BasicNode>();

		int maxNeighbors = 0;
		for (BasicNode node : this.nodes.values()) {
			int currentNeighborhoodSize = node.getAdjacentNodes().size();
			if (currentNeighborhoodSize == maxNeighbors) {
				// add to set
				hubsHashSet.add(node);
			} else if (currentNeighborhoodSize > maxNeighbors) {
				// create new set
				hubsHashSet = new HashSet<BasicNode>();
				hubsHashSet.add(node);
				maxNeighbors = currentNeighborhoodSize;
			} else {
				// ignore it
			}
		}

		return hubsHashSet;
	}
}
