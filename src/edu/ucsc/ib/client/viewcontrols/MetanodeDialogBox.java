package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeInfoCenter;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.MetanodeInfo;
import edu.ucsc.ib.client.MetanodeInfoCenter;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.BasicNode;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

/**
 * A dialog box for working with metanodes.
 * 
 * @author cw
 * 
 */
public class MetanodeDialogBox extends IbDialogBox {
	private final NetworkVisualization nv;
	private final MetanodeInfoCenter mic;
	private final BiodeInfoCenter bic;

	/**
	 * For displaying metanode members.
	 */
	private final Tree metanodeTree;

	/**
	 * For displaying selected nodes.
	 */
	private final FlexTable selectedNodesFT;

	/**
	 * Panel for displaying metanodes and members.
	 */
	private final VerticalPanel currentMetaNodesPanel;

	/**
	 * Panel for controls.
	 */
	private final VerticalPanel metaNodeControlsPanel;

	/**
	 * Button to make a metanode from the selected nodes.
	 */
	private final Button createMetanodeButton = new Button("create metanode",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					String name = metanodeNameTextBox.getText();
					String desc = metanodeDescTextArea.getText();
					BiodeSet bs = nv.getSelectedNodeIds();

					if (bs.size() == 0) {
						return;
					}

					MetanodeInfo mi = mic.addMetanode(name, bs);
					if (desc.length() > 0) {
						mi.setDescription(desc);
					}

					// TODO hide the appropriate NetworkNodes
					// TODO maybe need to do more than just set the member nodes
					// to be invisible. even though they are invisible, they are
					// still present in the netviz. evidence of them can be seen
					// when a circle layout is performed after having formed a
					// metanode.
					for (String biode : bs) {
						BiodeInfo bi = bic.getBiodeInfo(biode);
						BasicNode nn = nv.getNode(biode);
						if (bi.getAllMemberships().size() >= 1) {
							nn.hide();
							// TODO need a way to transfer the edges between
							// metanode and individual nodes.
						}
					}

					// TODO the section below is a hack for setting systemspace
					// and biodespace of metanode
					HashMap<String, Integer> organismHashMap = new HashMap<String, Integer>();
					HashMap<String, Integer> biodespaceHashMap = new HashMap<String, Integer>();
					for (String biode : bs) {
						BiodeInfo bi = bic.getBiodeInfo(biode);
						String organism = bi.getSystemSpace();
						String biodeSpace = bi.getBiodeSpace();

						if (organismHashMap.containsKey(organism)) {
							organismHashMap.put(organism,
									organismHashMap.get(organism) + 1);
						} else {
							organismHashMap.put(organism, 1);
						}

						if (biodespaceHashMap.containsKey(biodeSpace)) {
							biodespaceHashMap.put(biodeSpace,
									biodespaceHashMap.get(biodeSpace) + 1);
						} else {
							biodespaceHashMap.put(biodeSpace, 1);
						}
					}

					String topOrganismName = "";
					int topOrganismCount = 0;

					for (String organismName : organismHashMap.keySet()) {
						if (organismHashMap.get(organismName) > topOrganismCount) {
							topOrganismName = organismName;
							topOrganismCount = organismHashMap
									.get(organismName);
						}
					}

					String topBiodespaceName = "";
					int topBiodespaceCount = 0;

					for (String biodespaceName : biodespaceHashMap.keySet()) {
						if (biodespaceHashMap.get(biodespaceName) > topBiodespaceCount) {
							topBiodespaceName = biodespaceName;
							topBiodespaceCount = biodespaceHashMap
									.get(biodespaceName);
						}
					}

					mi.setSystemSpace(topOrganismName);
					mi.setBiodeSpace(topBiodespaceName);

					nv.addMetanode(mi);
					nv.deselectBiodeSet(bs);

					updateDisplays();
				}
			});

	/**
	 * Text Box for specifying the name of a metanode.
	 */
	private final TextBox metanodeNameTextBox = new TextBox();

	/**
	 * Text Area for a short description of a metanode.
	 */
	private final TextArea metanodeDescTextArea = new TextArea();

	/**
	 * Construct a dialog box for working with metanodes.
	 * 
	 * @param nv
	 */
	public MetanodeDialogBox(NetworkVisualization nv) {
		super("metanode controls");

		this.nv = nv;
		this.mic = this.nv.MIC;
		this.bic = this.nv.BIC;
		this.metanodeTree = new Tree();

		this.selectedNodesFT = new FlexTable();
		this.selectedNodesFT.setTitle("selected nodes");
		this.selectedNodesFT.setWidth("30em");
		this.selectedNodesFT.setBorderWidth(1);

		this.currentMetaNodesPanel = new VerticalPanel();
		this.currentMetaNodesPanel.add(this.metanodeTree);

		this.metaNodeControlsPanel = new VerticalPanel();
		this.metaNodeControlsPanel.add(this.selectedNodesFT);
		this.metaNodeControlsPanel.add(this.metanodeNameTextBox);
		this.metaNodeControlsPanel.add(this.metanodeDescTextArea);
		this.metaNodeControlsPanel.add(this.createMetanodeButton);

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(this.currentMetaNodesPanel);
		hp.add(this.metaNodeControlsPanel);

		this.setWidget(hp);

		this.updateDisplays();
	}

	/**
	 * Setup the controls for new metanode.
	 */
	private void presetMetanodeControls() {
		// control for metanode name
		final String defaultMetanodeName = "myMetanode";

		Set<String> metanodeNames = this.mic.getMetaNodeNames();

		int index = 0;
		String possibleName = defaultMetanodeName + index;
		if (metanodeNames.size() > 0) {
			while (metanodeNames.contains(possibleName)) {
				index++;
				possibleName = defaultMetanodeName + index;
			}
		}
		this.metanodeNameTextBox.setText(defaultMetanodeName + index);

		this.metanodeNameTextBox.setName("metanodeName");
		this.metanodeNameTextBox.setTitle("Give your metanode a name.");

		// control for metanode description
		this.metanodeDescTextArea.setName("metanodeDesc");
		this.metanodeDescTextArea
				.setTitle("Give your metanode a short description.");
	}

	/**
	 * Update the information displays and controls.
	 */
	private void updateDisplays() {
		this.updateSelectedNodeList();
		this.updateMetanodeTree();
		this.presetMetanodeControls();
	}

	/**
	 * Update the display of selected nodes.
	 */
	private void updateSelectedNodeList() {
		BiodeSet bs = this.nv.getSelectedNodeIds();

		this.selectedNodesFT.removeAllRows();

		int count = 0;
		if (bs.size() > 0) {
			for (String biode : bs) {
				int row = count / 5;
				int column = count % 5;

				String displayName = bic.getBiodeInfo(biode).getCommonName();
				if (displayName.equalsIgnoreCase("no alias")) {
					this.selectedNodesFT.setText(row, column, biode);
				} else {
					this.selectedNodesFT.setText(row, column, displayName);
				}
				count++;
			}
		} else {
			this.selectedNodesFT.setText(0, 0, "no selected nodes");
		}
	}

	/**
	 * Update the metanodeTree widget to display the current metanode
	 * information.
	 */
	private void updateMetanodeTree() {
		this.metanodeTree.clear();

		Set<String> metanodeNames = this.mic.getMetaNodeNames();

		LoggingDialogBox.log("updateMetanodeTree got " + metanodeNames.size()
				+ " metanodes.");

		if (metanodeNames.size() > 0) {
			for (String metanodeName : metanodeNames) {
				MetanodeInfo mi = this.mic.getMetanodeInfo(metanodeName);
				BiodeSet bs = mi.getMembers();

				Grid metanodeGrid = new Grid(1, 4);
				metanodeGrid.setText(0, 0, mi.getSystematicName());
				metanodeGrid.setText(0, 1, bs.size() + " members");
				metanodeGrid.setText(0, 2, mi.getDescription());
				metanodeGrid.setWidget(0, 3, createRemoveMetanodeButton(mi));

				// show members of metanode in flextable
				// biode ID and button to get biode info
				FlexTable membersFT = new FlexTable();
				membersFT.setWidth("30em");
				membersFT.setBorderWidth(1);
				membersFT.setTitle("members of " + metanodeName);
				int row = 0;
				for (String biode : bs) {
					int column = 0;
					BiodeInfo bi = this.bic.getBiodeInfo(biode);
					membersFT.setText(row, column++, bi.getCommonName());
					membersFT.setText(row, column++, bi.getAllMemberships()
							.size() + " metanode memberships");
					row++;
				}

				TreeItem metanodeTI = new TreeItem(metanodeGrid);
				metanodeTI.addItem(membersFT);

				this.metanodeTree.addItem(metanodeTI);
			}
		} else {
			this.metanodeTree.addItem("There are no metanodes to display.");
		}
	}

	/**
	 * Create a button for removing a metanode.
	 * 
	 * @param mi
	 * @return
	 */
	private Button createRemoveMetanodeButton(final MetanodeInfo mi) {
		Button b = new Button("ungroup", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {

				// remove the metanode from netviz
				nv.removeMetanode(nv.getMetanode(mi.getSystematicName()));

				// remove record of metanode
				mic.removeMetanode(mi.getSystematicName());

				// show ungrouped network nodes
				for (String biode : mi.getMembers()) {
					BiodeInfo bi = bic.getBiodeInfo(biode);
					if (bi.getAllMemberships().size() == 0) {
						BasicNode nn = nv.getNode(biode);
						nn.show();
					}
				}

				updateMetanodeTree();
			}
		});
		b.setTitle("ungroup " + mi.getSystematicName());
		return b;
	}
}
