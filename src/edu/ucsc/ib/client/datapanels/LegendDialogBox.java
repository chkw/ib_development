package edu.ucsc.ib.client.datapanels;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.ucsc.ib.client.viewcontrols.CircleMapDialogBox;

/**
 * A dialogbox for displaying a legend for:
 * <UL>
 * <LI>node shapes</LI>
 * <LI>edge types</LI>
 * <LI>CircleMap rings</LI>
 * </UL>
 * 
 * @author cw
 * 
 */
public class LegendDialogBox extends IbDialogBox {
	private static final int POSITION_LEFT = 670;
	private static final int POSITION_TOP = 20;

	private static final String LEGEND_IMAGES_URL = "images/legend";

	private static final String CIRCLE_URL = LEGEND_IMAGES_URL + "/circle.svg";
	private static final String DIAMOND_URL = LEGEND_IMAGES_URL
			+ "/diamond.svg";
	private static final String SQUARE_URL = LEGEND_IMAGES_URL + "/square.svg";
	private static final String TRIANGLE_URL = LEGEND_IMAGES_URL
			+ "/triangle.svg";

	private static final String SOLID_LINE_URL = LEGEND_IMAGES_URL
			+ "/solid_line.svg";
	private static final String DASH_LINE_URL = LEGEND_IMAGES_URL
			+ "/dash_line.svg";
	private static final String SOLID_ARROW_LINE_URL = LEGEND_IMAGES_URL
			+ "/solid_arrow_line.svg";
	private static final String SOLID_BAR_LINE_URL = LEGEND_IMAGES_URL
			+ "/solid_bar_line.svg";

	private static final VerticalPanel outerPanel = new VerticalPanel();

	private static FlexTable legendTable;

	// TODO /////////////////////////////////////////////////////

	public LegendDialogBox() {
		super("Pathway Legend");
		setPopupPosition(POSITION_LEFT, POSITION_TOP);
		setWidget(outerPanel);

		legendTable = prepareLegend();

		outerPanel.add(legendTable);

	}

	private FlexTable prepareLegend() {
		FlexTable result = new FlexTable();
		result.setBorderWidth(1);

		int row = 0;
		int column = 0;

		// nodes

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(row, column++,
				createLegendEntry(new Image(CIRCLE_URL), "gene/protein"));

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(row, column++,
				createLegendEntry(new Image(DIAMOND_URL), "complex"));

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(row, column++,
				createLegendEntry(new Image(SQUARE_URL), "abstract process"));

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(row, column++,
				createLegendEntry(new Image(TRIANGLE_URL), "chemical/drug"));

		// edges

		row++;
		column = 0;

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(
				row,
				column++,
				createLegendEntry(new Image(SOLID_LINE_URL),
						"transcriptional regulation"));

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(
				row,
				column++,
				createLegendEntry(new Image(DASH_LINE_URL),
						"post-translational regulation"));

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(
				row,
				column++,
				createLegendEntry(new Image(SOLID_ARROW_LINE_URL), "activating"));

		result.getFlexCellFormatter().setAlignment(row, column,
				HasHorizontalAlignment.ALIGN_CENTER,
				HasVerticalAlignment.ALIGN_MIDDLE);
		result.setWidget(row, column++,
				createLegendEntry(new Image(SOLID_BAR_LINE_URL), "inhibiting"));

		return result;
	}

	/**
	 * Get a Panel that contains the specified elements. This element can then
	 * be used as an entry in a legend table.
	 * 
	 * @param image
	 * @param text
	 * @return
	 */
	private static Panel createLegendEntry(Image image, String text) {
		VerticalPanel panel = new VerticalPanel();
		panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

		panel.add(image);
		panel.add(new Label(text));

		return panel;
	}

	/**
	 * Get the widgets for drawing circle legend. Makes calls to
	 * CircleMapDialogBox.
	 * 
	 * @return
	 */
	private static ArrayList<HashMap<String, Widget>> getCircleLegendWidgets() {
		ArrayList<HashMap<String, Widget>> ringLegendWidgetsList = new ArrayList<HashMap<String, Widget>>();

		for (String ringName : CircleMapDialogBox.getRingDisplayOrder()) {
			Panel colorKeyPanel = CircleMapDialogBox
					.getGroupRingColorKeyPanel(ringName);

			HashMap<String, Widget> ringWidgets = new HashMap<String, Widget>();
			ringLegendWidgetsList.add(ringWidgets);
			ringWidgets.put("colorKey", colorKeyPanel);
			ringWidgets.put("ringName", new Label(ringName));

		}

		return ringLegendWidgetsList;
	}

	/**
	 * Remove the circle ring legend portion of the legend table by deleting
	 * rows from the bottom until only 2 remain.
	 */
	public static void clearCircleLegend() {
		// remove rows from the bottom
		while (legendTable.getRowCount() > 2) {
			legendTable.removeRow(legendTable.getRowCount() - 1);
		}
	}

	/**
	 * Set the circle legend portion of the legend table to the current rings.
	 */
	public static void resetCircleLegend() {

		ArrayList<HashMap<String, Widget>> circleWidgets = getCircleLegendWidgets();

		clearCircleLegend();

		for (int i = 0; i < circleWidgets.size(); i++) {

			Widget name = circleWidgets.get(i).get("ringName");
			Widget colorKeyPanel = circleWidgets.get(i).get("colorKey");

			String position = (i == 0) ? "innermost ring" : "ring " + (i + 1);
			Label positionLabel = new Label(position);

			int row = legendTable.getRowCount();
			int column = 0;
			legendTable.setWidget(row, column++, positionLabel);
			legendTable.setWidget(row, column, name);
			legendTable.getFlexCellFormatter().setColSpan(row, column, 2);
			legendTable.setWidget(row, ++column, colorKeyPanel);
		}
	}
}
