/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client.viewcontrols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.netviz.SourceSystemSpaceListener;

/**
 * A panel for selecting search space.
 */
public class SearchSpaceControl extends Composite {

	public static final String CSS_CLASS = "ib-searchSpace";

	public static final HashMap<String, String> SOURCE_OPTIONS = new HashMap<String, String>();
	static {
		SOURCE_OPTIONS.put("Human", "9606");
		SOURCE_OPTIONS.put("Mouse", "10090");
		SOURCE_OPTIONS.put("Worm", "6239");
		SOURCE_OPTIONS.put("Yeast", "4932");
		SOURCE_OPTIONS.put("Drug", "drug");
	}

	private static final HashMap<String, String> VIEW_OPTIONS = new HashMap<String, String>();
	static {
		VIEW_OPTIONS.put("Normal View", "normal");
		VIEW_OPTIONS.put("Human", "9606");
		VIEW_OPTIONS.put("Mouse", "10090");
		VIEW_OPTIONS.put("Worm", "6239");
		VIEW_OPTIONS.put("Yeast", "4932");
	}

	public static final String[] biodespaces = { "gene", "protein", "chemical",
			"unspecified" };

	private static ListBox sourceSystemspaceListBox;
	private static ListBox sourceBiodespaceListBox;

	private ListBox viewSystemspaceListBox;

	private static List<SourceSystemSpaceListener> systemSpaceListeners;

	private String currentSourceSystemSpace;
	private String currentViewSystemSpace;

	/**
	 * A panel for selecting search space.
	 */
	public SearchSpaceControl() {

		// This is the panel for this SearchSpaceControl.
		HorizontalPanel outermostPanel = new HorizontalPanel();

		initWidget(outermostPanel);
		setStyleName(CSS_CLASS);

		initializeListBoxes();

		outermostPanel.add(sourceSystemspaceListBox);
		outermostPanel.add(sourceBiodespaceListBox);

		this.viewSystemspaceListBox.setVisible(false);
		outermostPanel.add(this.viewSystemspaceListBox);

		systemSpaceListeners = new ArrayList<SourceSystemSpaceListener>();
	}

	/**
	 * Fills in the biodespaceList and systemspaceList ListBoxes with available
	 * searchspaces. The searchspaces come from arrays of Strings, biodespaces[]
	 * and systemspaces[]. The first item in the ListBoxes are selected by
	 * default.
	 * 
	 */
	private void initializeListBoxes() {

		// initialize source systemspace listbox
		sourceSystemspaceListBox = new ListBox();
		sourceSystemspaceListBox.setTitle("select the source systemspace");
		sourceSystemspaceListBox.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				String newSystemSpace = sourceSystemspaceListBox
						.getValue(sourceSystemspaceListBox.getSelectedIndex());
				if (!currentSourceSystemSpace.equalsIgnoreCase(newSystemSpace)) {
					currentSourceSystemSpace = newSystemSpace;
					notifyListenersSourceChange(newSystemSpace);
				}

				if (currentSourceSystemSpace.equalsIgnoreCase("drug")) {
					for (int i = 0; i < sourceBiodespaceListBox.getItemCount(); i++) {
						if (sourceBiodespaceListBox.getValue(i)
								.equalsIgnoreCase("chemical")) {
							sourceBiodespaceListBox.setItemSelected(i, true);
						}
					}
				}
			}
		});

		String[] organisms = (String[]) SOURCE_OPTIONS.keySet().toArray(
				new String[0]);
		for (int i = 0; i < organisms.length; i++) {
			sourceSystemspaceListBox.addItem((String) organisms[i],
					(String) SOURCE_OPTIONS.get(organisms[i]));
			if (organisms[i].equalsIgnoreCase("human")) {
				sourceSystemspaceListBox.setItemSelected(i, true);
				currentSourceSystemSpace = (String) SOURCE_OPTIONS
						.get(organisms[i]);
			}
		}

		// initialize biodespace listbox
		sourceBiodespaceListBox = new ListBox();
		sourceBiodespaceListBox.setTitle("select the source biodespace");

		for (int i = 0; i < biodespaces.length; i++) {
			sourceBiodespaceListBox.addItem(biodespaces[i], biodespaces[i]);
		}
		sourceBiodespaceListBox.setItemSelected(0, true);

		// initialize viewspace listbox
		this.viewSystemspaceListBox = new ListBox();
		this.viewSystemspaceListBox.setTitle("select the view systemspace");
		this.viewSystemspaceListBox.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				String newSystemSpace = viewSystemspaceListBox
						.getValue(viewSystemspaceListBox.getSelectedIndex());
				if (!currentViewSystemSpace.equalsIgnoreCase(newSystemSpace)) {
					currentViewSystemSpace = newSystemSpace;
					notifyListenersViewChange(currentViewSystemSpace);
				}
			}
		});

		String[] views = (String[]) VIEW_OPTIONS.keySet()
				.toArray(new String[0]);
		for (int i = 0; i < views.length; i++) {
			this.viewSystemspaceListBox.addItem((String) views[i],
					(String) VIEW_OPTIONS.get(views[i]));
			if (views[i].equalsIgnoreCase("Normal View")) {
				this.viewSystemspaceListBox.setItemSelected(i, true);
				this.currentViewSystemSpace = (String) VIEW_OPTIONS
						.get(views[i]);
			}
		}
	}

	/**
	 * Notify sourceSystemSpaceListener objects that view system changed.
	 * 
	 * @param newSystemSpace
	 */
	protected void notifyListenersViewChange(String newView) {
		for (int i = 0; i < systemSpaceListeners.size(); ++i) {
			((SourceSystemSpaceListener) systemSpaceListeners.get(i))
					.viewSystemSpaceChanged(newView);
		}
	}

	/**
	 * Notify sourceSystemSpaceListener objects that source system changed.
	 * 
	 * @param newSystemSpace
	 */
	private void notifyListenersSourceChange(String newSystemSpace) {
		for (int i = 0; i < systemSpaceListeners.size(); ++i) {
			((SourceSystemSpaceListener) systemSpaceListeners.get(i))
					.sourceSystemSpaceChanged(newSystemSpace);
		}
	}

	/**
	 * Get the selected organism system from the systemspaceList ListBox. This
	 * is the NCBI tax ID.
	 * 
	 * @return selected String value
	 */
	public static String getSystemspace() {
		return sourceSystemspaceListBox.getValue(sourceSystemspaceListBox
				.getSelectedIndex());
	}

	/**
	 * Get the selected biode type from the systemspaceList ListBox.
	 * 
	 * @return selected String value
	 */
	public static String getBiodespace() {
		return sourceBiodespaceListBox.getValue(sourceBiodespaceListBox
				.getSelectedIndex());
	}

	/**
	 * Add a SystemSpaceListener and notify it of the current systemSpace.
	 * 
	 * @param ssl
	 *            SystemSpaceListener to add
	 */
	public static void addSystemSpaceListener(SourceSystemSpaceListener ssl) {
		systemSpaceListeners.add(ssl);
		ssl.sourceSystemSpaceChanged(sourceSystemspaceListBox
				.getValue(sourceSystemspaceListBox.getSelectedIndex()));
	}

	/**
	 * Set the biodespace and systemspace for a BiodeInfo object to match the
	 * selected options.
	 * 
	 * @param bi
	 */
	public void setBiodeInfoSpaces(BiodeInfo bi) {
		bi.setSystemSpace(SearchSpaceControl.getSystemspace());
		bi.setBiodeSpace(SearchSpaceControl.getBiodespace());
	}

	/**
	 * Set the source organism.
	 * 
	 * @param organism
	 */
	void setSourceSearchSpace(String organism) {
		// set organism space
		for (int i = 0; i < sourceSystemspaceListBox.getItemCount(); i++) {
			if ((sourceSystemspaceListBox.getItemText(i)
					.equalsIgnoreCase(organism))
					|| (sourceSystemspaceListBox.getValue(i)
							.equalsIgnoreCase(organism))) {
				sourceSystemspaceListBox.setItemSelected(i, true);
				currentSourceSystemSpace = sourceSystemspaceListBox
						.getValue(sourceSystemspaceListBox.getSelectedIndex());
			}
		}
		notifyListenersSourceChange(sourceSystemspaceListBox
				.getValue(sourceSystemspaceListBox.getSelectedIndex()));
	}
}
