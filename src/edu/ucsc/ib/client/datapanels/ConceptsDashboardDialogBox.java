package edu.ucsc.ib.client.datapanels;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.netviz.BasicNode;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;
import edu.ucsc.ib.client.viewcontrols.MetanodeDialogBox;
import edu.ucsc.ib.drawpanel.client.Shape;

/**
 * Contains controls for selecting biodes and getting annotation about them.
 * Also contains controls for changing the visualization (shape & color) of
 * selected nodes.
 * 
 * @author Chris
 * 
 */
public class ConceptsDashboardDialogBox extends IbDialogBox implements
		DataPanel {

	private static final String DEFAULT_SEARCH_TEXT = "search for a concept in the graph";

	private final NetworkVisualization nv;

	/**
	 * Each displayed node has a row, which contains widgets for performing
	 * actions on the node.
	 */
	private final FlexTable currentlyDisplayedFlexTable = new FlexTable();

	/**
	 * Contains currentlyDisplayedFlexTable.
	 */
	private final ScrollPanel biodeTableScrollPanel = new ScrollPanel();
	{
		biodeTableScrollPanel.setWidth("30em");
		biodeTableScrollPanel.setHeight("25em");
		biodeTableScrollPanel.add(currentlyDisplayedFlexTable);
	}

	/**
	 * Selects all nodes.
	 */
	private final Button selectAllButton = new Button("select all",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					nv.selectAllBiodes();
				}
			});

	/**
	 * Deselects all nodes.
	 */
	private final Button deselectAllButton = new Button("deselect all",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					nv.deselectAllBiodes();
				}
			});

	/**
	 * Remove selected nodes.
	 */
	private final Button removeButton = new Button("remove selected",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					BiodeSet bs = nv.getSelectedNodeIds();
					nv.removeBiodeSet(bs);
				}
			});

	/**
	 * Make selected nodes deselected. Make deselected nodes selected.
	 */
	private final Button invertSelectionButton = new Button("invert selection",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					nv.invertSelection();
				}
			});

	/**
	 * Bring up metanodeDialogBox.
	 */
	private final Button metanodeButton = new Button("metanode",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					final MetanodeDialogBox dialogBox = new MetanodeDialogBox(
							nv);

					dialogBox.show();
					dialogBox.center();
				}
			});

	/**
	 * Control for changing color of selected nodes.
	 */
	private final ListBox colorListBox = ConceptsDashboardDialogBox
			.makeColorListBox();
	{
		colorListBox.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				nv.changeBiodeSetColor(nv.getSelectedNodeIds(),
						colorListBox.getValue(colorListBox.getSelectedIndex()));
			}
		});
	}

	/**
	 * Control for changing shape of selected nodes.
	 */
	private final ListBox shapeListBox = new ListBox();
	{
		shapeListBox.setTitle("shapeListBox");

		shapeListBox.addItem("circle", "0");
		shapeListBox.addItem("rectangle", "1");
		shapeListBox.addItem("triangle", "5");
		shapeListBox.addItem("diamond", "7");

		shapeListBox.setItemSelected(0, true);

		shapeListBox.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				nv.changeBiodeSetShape(
						nv.getSelectedNodeIds(),
						Integer.valueOf(
								shapeListBox.getValue(shapeListBox
										.getSelectedIndex())).intValue());
			}
		});
	}

	/**
	 * TextBox for live filtering loaded concepts.
	 */
	private final TextBox filterTextBox = new TextBox();
	{
		filterTextBox.setTitle(DEFAULT_SEARCH_TEXT);
		filterTextBox.setText(DEFAULT_SEARCH_TEXT);

		filterTextBox.addKeyUpHandler(new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				event.stopPropagation();

				TextBox source = (TextBox) event.getSource();
				String value = source.getValue();

				filterConceptsList(value);
			}

		});
	}

	/**
	 * Contains various controls for node selection, color, shape, etc.
	 */
	private VerticalPanel controlPanel = new VerticalPanel();

	/**
	 * Outermost panel to be set as the DialogBox object's widget.
	 */
	private final FlexTable outermostFlexTable = new FlexTable();

	private boolean developerMode;

	// TODO ///////////////////////////////////////////////////////

	public ConceptsDashboardDialogBox(NetworkVisualization netViz,
			boolean developerMode) {
		super("Concepts Dashboard");

		this.developerMode = developerMode;

		nv = netViz;

		// register with nv as a SelectedSetListener
		nv.addSelectedSetListener(this);

		// register with nv as WorkingSetListener
		nv.addWorkingSetListener(this);

		controlPanel.add(this.removeButton);
		controlPanel.add(this.selectAllButton);
		controlPanel.add(this.deselectAllButton);
		controlPanel.add(this.invertSelectionButton);
		controlPanel.add(this.colorListBox);
		controlPanel.add(this.shapeListBox);

		if (this.developerMode) {
			controlPanel.add(this.metanodeButton);
		}

		controlPanel.add(this.filterTextBox);

		int row = 0;
		int col = 0;

		outermostFlexTable.setWidget(row, col, controlPanel);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		col++;

		outermostFlexTable.setWidget(row, col, biodeTableScrollPanel);
		outermostFlexTable.getCellFormatter().setAlignment(row, col,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);

		row++;

		this.setWidget(outermostFlexTable);
	}

	/**
	 * Reset the concepts filter.
	 */
	protected void resetConceptsFilter() {
		filterTextBox.setValue(DEFAULT_SEARCH_TEXT, false);
		filterConceptsList(DEFAULT_SEARCH_TEXT);
	}

	/**
	 * Filter the concepts list. If the filter value is null, show all concepts.
	 * 
	 * @param value
	 */
	protected void filterConceptsList(String value) {

		if (value.equalsIgnoreCase(DEFAULT_SEARCH_TEXT)
				|| value.equalsIgnoreCase("")) {

			currentlyDisplayedFlexTable.removeAllRows();

			// no filter - display all
			for (String biode : this.nv.BIC.getAllSystematicNames()) {
				BiodeInfo bi = this.nv.BIC.getBiodeInfo(biode);
				this.addBiodeControlToFlexTable(bi);
			}
		} else {

			currentlyDisplayedFlexTable.removeAllRows();

			String subStr = value.toLowerCase();

			for (String biode : this.nv.BIC.getAllSystematicNames()) {
				BiodeInfo bi = this.nv.BIC.getBiodeInfo(biode);

				// check for match in name or description
				if ((biode.toLowerCase().indexOf(subStr) != -1)
						|| (bi.getDescription().toLowerCase().indexOf(subStr) != -1)) {
					this.addBiodeControlToFlexTable(bi);
				}
			}
		}
	}

	/**
	 * Make a colorListBox and fill it with color options.
	 * 
	 * @return
	 */
	public static ListBox makeColorListBox() {
		ListBox colorListBox = new ListBox();
		colorListBox.setTitle("colorListBox");
		for (String color : Shape.COLOR_LIST) {
			colorListBox.addItem(color, color);
		}
		colorListBox.setItemSelected(3, true);
		return colorListBox;
	}

	/**
	 * Required by SelectedSetListener interface. Selects the biodes in the
	 * specified BiodeSet to the ListBox.
	 * 
	 * @param b
	 *            The BiodeSet of biodes to select.
	 */
	public void selectedBiodes(BiodeSet b) {
		for (String biode : b) {
			this.setSelect(biode, true);
		}
	}

	/**
	 * Required by SelectedSetListener interface. Deselects the biodes in the
	 * specified BiodeSet to the ListBox.
	 * 
	 * @param b
	 *            The BiodeSet of biodes to deselect.
	 */
	public void deselectedBiodes(BiodeSet b) {
		for (String biode : b) {
			setSelect(biode, false);
		}
	}

	/**
	 * Sets the biode's row in the FlexTable to reflect the selected status.
	 * 
	 * @param biode
	 *            String
	 * @param selected
	 *            boolean to set selected status to
	 */
	private void setSelect(String biode, boolean selected) {
		// reset concepts filter
		resetConceptsFilter();

		int row = findRow(biode);
		AbsolutePanel swatch = ((AbsolutePanel) currentlyDisplayedFlexTable
				.getWidget(row, 0));
		String fill;
		if (selected) {
			((CheckBox) currentlyDisplayedFlexTable.getWidget(row, 1))
					.setValue(true);
			fill = nv.getNode(biode).getSelectedFill();
		} else {
			((CheckBox) currentlyDisplayedFlexTable.getWidget(row, 1))
					.setValue(false);
			fill = nv.getNode(biode).getDeselectedFill();
		}
		DOM.setStyleAttribute(swatch.getElement(), "background", fill);
	}

	/**
	 * Required by WorkingSetListener interface. Adds the biodes in the
	 * specified BiodeSet to the ListBox. Looks up BiodeInfo from BIC.
	 * 
	 * @param b
	 *            The BiodeSet of biodes to add.
	 */
	public void addedBiodes(BiodeSet b) {
		for (String biode : b) {
			BiodeInfo bi = this.nv.BIC.getBiodeInfo(biode);

			addBiodeControlToFlexTable(bi);
		}
	}

	/**
	 * Add a biode control to the FlexTable.
	 * 
	 * @param BiodeInfo
	 */
	private void addBiodeControlToFlexTable(BiodeInfo bi) {

		BasicNode node = nv.getNode(bi.getSystematicName());

		// add row to the FlexTable
		int row = currentlyDisplayedFlexTable.getRowCount();

		// Set up color icon
		AbsolutePanel swatch = new AbsolutePanel();
		if (node.isSelected()) {
			setSwatchColor(swatch, node.getSelectedFill());
		} else {
			setSwatchColor(swatch, node.getDeselectedFill());
		}
		swatch.setPixelSize(9, 9);
		swatch.setTitle(bi.getSystematicName());
		currentlyDisplayedFlexTable.setWidget(row, 0, swatch);

		// Set up checkbox with short name
		CheckBox cb = makeSelectCheckBox(bi.getSystematicName());
		if (node.isSelected()) {
			cb.setValue(true, true);
		} else {
			cb.setValue(false, true);
		}

		currentlyDisplayedFlexTable.setWidget(row, 1, cb);

		// Set up common name
		currentlyDisplayedFlexTable.setText(row, 2, bi.getCommonName());

		// Set up description
		// currentlyDisplayed.setText(row, 3, bi.getDescription());

		// Set up info button
		Button infoButton = makeInfoButton(bi.getSystematicName());
		currentlyDisplayedFlexTable.setWidget(row, 4, infoButton);

		// Set up remove button
		Button aRemoveButton = makeRemoveButton(bi.getSystematicName());
		currentlyDisplayedFlexTable.setWidget(row, 5, aRemoveButton);
	}

	/**
	 * Required by WorkingSetListener interface. Removes the biodes in the
	 * specified BiodeSet from currentlyDisplayed.
	 * 
	 * @param b
	 *            The BiodeSet of biodes to remove.
	 */
	public void removedBiodes(BiodeSet b) {
		LoggingDialogBox.log("BTP.removedBiodes.. trying to remove rows");

		resetConceptsFilter();

		for (String biode : b) {
			int row = findRow(biode);
			if (row != -1) {
				currentlyDisplayedFlexTable.removeRow(row);
			}
		}
	}

	/**
	 * Makes a CheckBox for selecting with a ClickListener.
	 * 
	 * @param text
	 *            String to use for text and title
	 * @return a CheckBox
	 */
	private CheckBox makeSelectCheckBox(String text) {
		CheckBox cb = new CheckBox();
		cb.setTitle(text);
		cb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				CheckBox sender = (CheckBox) event.getSource();
				String s = sender.getTitle();
				if (sender.getValue()) {
					nv.selectBiode(s);
				} else {
					nv.deselectBiode(s);
				}
			}
		});
		return cb;
	}

	/**
	 * Makes a Button for showing biode information.
	 * 
	 * @param biode
	 * 
	 * @return a button
	 */
	private Button makeInfoButton(String biode) {

		Button b = new Button("i", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final Widget sender = (Widget) event.getSource();
				final String s = sender.getTitle();
				final BiodeInfoDialogBox dialogBox = new BiodeInfoDialogBox(
						nv.BIC.getBiodeInfo(s), nv);

				dialogBox.center();

				// set position before showing using
				// setPopupPositionAndShow or setPopupPosition

				// dialogBox
				// .setPopupPositionAndShow(new PopupPanel.PositionCallback() {
				//
				// @Override
				// public void setPosition(int offsetWidth,
				// int offsetHeight) {
				//
				// int sender_left_pos = sender.getAbsoluteLeft();
				// int sender_top_pos = sender.getAbsoluteTop();
				//
				// dialogBox.setPopupPosition(sender_left_pos,
				// sender_top_pos);
				// }
				// });
			}
		});

		b.setTitle(biode);
		return b;
	}

	/**
	 * Makes a Button for removing with a ClickListener.
	 * 
	 * @param biode
	 * 
	 * @return a button
	 */
	private Button makeRemoveButton(String biode) {
		Button b = new Button("-", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				String s = sender.getTitle();
				nv.removeBiode(s);
			}
		});

		b.setTitle(biode);
		return b;
	}

	/**
	 * Set the color of an AbsolutePanel.
	 * 
	 * @param swatch
	 *            AbsolutePanel to modify
	 * @param color
	 *            String for color to use
	 */
	private void setSwatchColor(AbsolutePanel swatch, String color) {
		DOM.setStyleAttribute(swatch.getElement(), "background", color);
	}

	/**
	 * Find the index for row in the currentlyDisplayed FlexTable corresponding
	 * to a biode.
	 * 
	 * @param biode
	 * @return index of the row or -1 if not found
	 */
	private int findRow(String biode) {
		int result = -1;
		for (int i = 0; i < currentlyDisplayedFlexTable.getRowCount(); i++) {
			// column 1 should be a CheckBox object
			if (currentlyDisplayedFlexTable.getWidget(i, 1).getTitle()
					.equalsIgnoreCase(biode)) {
				result = i;
				break;
			}
		}
		return result;
	}

	@Override
	public void setVisibility(boolean isVisible) {
		if (isVisible) {
			this.show();
		} else {
			this.hide();
		}
	}

	@Override
	public void trackAdded(Track T) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trackRemoved(Track T) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sourceSystemSpaceChanged(String systemSpace) {
		// TODO Auto-generated method stub

	}

	@Override
	public void viewSystemSpaceChanged(String viewSystemSpace) {
		// TODO Auto-generated method stub

	}
}
