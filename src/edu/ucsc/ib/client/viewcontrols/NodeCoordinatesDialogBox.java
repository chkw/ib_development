package edu.ucsc.ib.client.viewcontrols;

import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.BasicNode;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

/**
 * A dialog box for working with user-submitted node coordinates.
 * 
 * @author cw
 * 
 */
public class NodeCoordinatesDialogBox extends IbDialogBox implements
		FormPanel.SubmitCompleteHandler {
	private static final String PANEL_WIDTH = "20em";
	private static final String FORM_ACTION = "fileBounce/returnFile";

	/**
	 * Space to keep around the edge of the netviz panel area.
	 */
	public static final double CUSHION_SPACE = 100;

	private final NetworkVisualization nv;

	/**
	 * Provide a quick instruction for the dialogBox.
	 */
	private final Label instruction = new Label();
	{
		instruction
				.setText("Provide coordinates for nodes that are displayed in the graph.  There should be 3 tab-delimited fields for each row.  First is the ID of the node.  For human, use the EntrezGene ID.  The second and third fields are the 2D coordinates for the node.  The point (0,0) is the upper left corner.  Increasing values go to the right and down.");
	}

	/**
	 * Detect typing <tab> characters in a TextArea. Prevents widget from losing
	 * focus and inserts <tab> character at cursor position.
	 * 
	 * @see <a
	 *      href="http://albertattard.blogspot.com/2009/11/capturing-tab-key-in-gwt-textarea.html">http://albertattard.blogspot.com/2009/11/capturing-tab-key-in-gwt-textarea.html</a>
	 */
	public static final KeyDownHandler DEFAULT_TEXTAREA_TAB_HANDLER = new KeyDownHandler() {
		@Override
		public final void onKeyDown(KeyDownEvent event) {
			LoggingDialogBox.log("KeyDownEvent: " + event.getNativeKeyCode());
			if (event.getNativeKeyCode() == KeyCodes.KEY_TAB) {
				LoggingDialogBox.log("tab key detected");
				event.preventDefault();
				LoggingDialogBox.log("preventDefault done");
				event.stopPropagation();
				LoggingDialogBox.log("stopPropagation done");
				if (event.getSource() instanceof TextArea) {
					TextArea ta = (TextArea) event.getSource();
					int index = ta.getCursorPos();
					String text = ta.getText();
					ta.setText(text.substring(0, index) + "\t"
							+ text.substring(index));
					ta.setCursorPos(index + 1);
					LoggingDialogBox.log("insert tab done");
				}
			}
		}
	};

	/**
	 * TextArea for inputing coordinates information.
	 */
	private final TextArea coordinatesTextArea = new TextArea();
	{
		coordinatesTextArea.setTitle("Coordinate data has 3 parts: ID, x, y");
		coordinatesTextArea.setWidth(PANEL_WIDTH);
		coordinatesTextArea.setVisibleLines(15);

		// TODO somehow, the key events are still not detected.
		coordinatesTextArea.addHandler(DEFAULT_TEXTAREA_TAB_HANDLER,
				KeyDownEvent.getType());
		LoggingDialogBox.log("added KeyDownHandler");
	}

	/**
	 * For reading in a file and outputing the contents to a text area.
	 */
	private final FormPanel submitFileForm = new FormPanel();
	{
		this.submitFileForm.addSubmitCompleteHandler(this);
		this.submitFileForm.setAction(FORM_ACTION);
		this.submitFileForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		this.submitFileForm.setMethod(FormPanel.METHOD_POST);
		FileUpload upload = new FileUpload();
		upload.setName("uploadFormElement");
		this.submitFileForm.add(upload);
	}

	/**
	 * Button for getting data from file.
	 */
	private final Button submitFileButton = new Button(
			"get coordinates from file", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					submitFileForm.submit();
				}
			});

	/**
	 * Apply the coordinates to the nodes.
	 */
	private final Button applyCoordinatesButton = new Button("apply",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					setUserCoordsForNodes();
				}
			});
	{
		this.applyCoordinatesButton.setTitle("apply the coordinates");
	}

	/**
	 * Clears the TextArea.
	 */
	private final Button clearTextAreaButton = new Button("clear",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					coordinatesTextArea.setValue("", false);
				}
			});
	{
		this.clearTextAreaButton.setTitle("clear the text area");
	}

	// /////////////////////////////////////////////////////////////////////

	/**
	 * Construct a dialog box for working with metanodes.
	 * 
	 * @param nv
	 */
	public NodeCoordinatesDialogBox(NetworkVisualization nv) {
		super("select a file to read in coordinate data");

		this.nv = nv;

		this.setWidth(PANEL_WIDTH);

		VerticalPanel outermostPanel = new VerticalPanel();
		this.setWidget(outermostPanel);
		this.setText("upload node coordinates");

		VerticalPanel fileBounceControlPanel = new VerticalPanel();
		fileBounceControlPanel.add(this.submitFileForm);
		fileBounceControlPanel.add(this.submitFileButton);

		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.add(this.clearTextAreaButton);
		buttonPanel.add(this.applyCoordinatesButton);

		outermostPanel.add(this.instruction);
		outermostPanel.add(fileBounceControlPanel);
		outermostPanel.add(this.coordinatesTextArea);
		outermostPanel.add(buttonPanel);
	}

	/**
	 * Set user-supplied node coordinates. Scales the coordinates so that the
	 * repositioned nodes use the available space.
	 */
	private void setUserCoordsForNodes() {
		String[] rows = this.coordinatesTextArea.getText().split("\n");
		if (rows.length == 0) {
			return;
		}

		// get supplied coords
		HashMap<String, HashMap<String, Double>> savedCoords = new HashMap<String, HashMap<String, Double>>();
		double maxX = -1;
		double maxY = -1;
		double minX = -1;
		double minY = -1;
		for (String row : rows) {
			boolean firstRow = (savedCoords.keySet().size() == 0);
			String[] fields = row.trim().split("\\s+");
			if (fields.length < 3 || fields[0].isEmpty() || fields[1].isEmpty()
					|| fields[2].isEmpty()) {
				LoggingDialogBox
						.log("skipped a row due to insufficient input: " + row);
				continue;
			}
			String id = fields[0];
			double xcoord = Double.valueOf(fields[1]);
			double ycoord = Double.valueOf(fields[2]);

			if (!this.nv.containsNode(id)) {
				LoggingDialogBox.log("no node found for " + id);
				continue;
			}

			if (firstRow || xcoord > maxX) {
				maxX = xcoord;
			}
			if (firstRow || xcoord < minX) {
				minX = xcoord;
			}
			if (firstRow || ycoord > maxY) {
				maxY = ycoord;
			}
			if (firstRow || ycoord < minY) {
				minY = ycoord;
			}

			HashMap<String, Double> coordSet = new HashMap<String, Double>();
			coordSet.put("x", xcoord);
			coordSet.put("y", ycoord);

			savedCoords.put(id, coordSet);
		}

		// calc values for scaling use
		double allowedSpanX = this.nv.getVportWidth() - (2 * CUSHION_SPACE);
		double allowedSpanY = this.nv.getVportHeight() - (2 * CUSHION_SPACE);

		double observedSpanX = maxX - minX;
		double observedSpanY = maxY - minY;

		// scale and apply coords
		String selectedSystemSpace = SearchSpaceControl.getSystemspace();
		String selectedBiodeSpace = SearchSpaceControl.getBiodespace();

		LoggingDialogBox.log("selectedSystemSpace: " + selectedSystemSpace);
		LoggingDialogBox.log("selectedBiodeSpace: " + selectedBiodeSpace);

		for (String id : savedCoords.keySet()) {
			HashMap<String, Double> coordSet = savedCoords.get(id);
			BasicNode nn = this.nv.getNode(id);

			if (!this.nv.BIC.getBiodeInfo(id).getSystemSpace()
					.equalsIgnoreCase(selectedSystemSpace)) {
				continue;
			}

			double normalizedX = 0.5;
			double normalizedY = 0.5;
			if (observedSpanX != 0) {
				normalizedX = (coordSet.get("x") - minX) / observedSpanX;

			}
			if (observedSpanY != 0) {
				normalizedY = (coordSet.get("y") - minY) / observedSpanY;

			}

			nn.setPositionInVpBounds(CUSHION_SPACE
					+ (normalizedX * allowedSpanX), CUSHION_SPACE
					+ (normalizedY * allowedSpanY));
		}
	}

	@Override
	public void onSubmitComplete(SubmitCompleteEvent event) {
		// expect a JSON-RPC compliant result
		JSONObject jsonRpcResult = JSONParser.parseStrict(event.getResults())
				.isObject();

		// check for error
		JSONValue jv = jsonRpcResult.get("error");
		if (jv.isNull() == null) {
			LoggingDialogBox.log("errorMessage: "
					+ jv.isObject().get("message").isString().stringValue());
			return;
		}

		// copy fileContents to textArea
		jv = jsonRpcResult.get("result");

		String fileContents = jv.isObject().get("fileContents").isString()
				.stringValue();

		this.coordinatesTextArea.setText(fileContents);
	}
}
