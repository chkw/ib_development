package edu.ucsc.ib.client.viewcontrols;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;

import edu.ucsc.ib.client.datapanels.ConceptsDashboardDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;

/**
 * This is panel that contains widgets for submitting track data to the
 * SubmitTrackService.
 * 
 * @author Chris
 * 
 */
public class TrackSubmitPanel extends Composite implements
		FormPanel.SubmitHandler, FormPanel.SubmitCompleteHandler {

	private static final String CSS_CLASS = "ib-uploadFilePanel";

	private final FormPanel form;

	private final Button uploadButton;

	private final TextBox trackNameTextBox;

	private final ListBox trackColorListBox;

	private final TextBox trackDescriptionTextBox;

	private final ListBox element1TypeListBox;

	private final ListBox element2TypeListBox;

	private final TextArea trackDataTextArea;

	private final ListBox dataFormatListBox;

	private final TrackControlPanel tcp;

	/**
	 * Constructor
	 * 
	 * @param tcp
	 */
	public TrackSubmitPanel(TrackControlPanel tcp) {
		this.tcp = tcp;

		this.form = new FormPanel();

		this.form.addSubmitHandler(this);
		this.form.addSubmitCompleteHandler(this);

		// this.form.setAction("data/submit/track/");
		this.form.setAction("data/customTrackDB/submit");
		// this.form.setEncoding(FormPanel.ENCODING_URLENCODED);
		this.form.setEncoding(FormPanel.ENCODING_MULTIPART);
		this.form.setMethod(FormPanel.METHOD_POST);

		// SimplePanel can only have one child widget.
		FlexTable table = new FlexTable();
		table.setWidth("50em");
		table.setBorderWidth(1);

		int row = 0;

		this.form.add(table);

		// text box to enter track name
		this.trackNameTextBox = new TextBox();
		this.trackNameTextBox.setName("trackName");
		this.trackNameTextBox.setTitle("Name of custom network.");

		table.setText(row, 0, "1- name");
		table.setWidget(row, 1, this.trackNameTextBox);
		row++;

		// list box to pick edge color
		this.trackColorListBox = ConceptsDashboardDialogBox.makeColorListBox();
		this.trackColorListBox.setName("color");

		table.setText(row, 0, "2- edge color");
		table.setWidget(row, 1, this.trackColorListBox);
		row++;

		// text box to enter description
		this.trackDescriptionTextBox = new TextBox();
		this.trackDescriptionTextBox.setName("trackDescription");
		this.trackDescriptionTextBox
				.setTitle("Short description of custom network.");

		table.setText(row, 0, "3- description");
		table.setWidget(row, 1, this.trackDescriptionTextBox);
		row++;

		// list box to pick element1 and element2 type
		this.element1TypeListBox = new ListBox();
		this.element2TypeListBox = new ListBox();
		this.element1TypeListBox.setTitle("element1TypeListBox");
		this.element2TypeListBox.setTitle("element2TypeListBox");
		for (int i = 0; i < SearchSpaceControl.biodespaces.length; i++) {
			String s = SearchSpaceControl.biodespaces[i];
			this.element1TypeListBox.addItem(s, s);
			this.element2TypeListBox.addItem(s, s);
		}
		this.element1TypeListBox.setItemSelected(0, true);
		this.element1TypeListBox.setName("element1_type");
		this.element2TypeListBox.setItemSelected(0, true);
		this.element2TypeListBox.setName("element2_type");

		table.setText(row, 0, "4- element1 node type");
		table.setWidget(row, 1, this.element1TypeListBox);
		row++;

		table.setText(row, 0, "5- element2 node type");
		table.setWidget(row, 1, this.element2TypeListBox);
		row++;

		// text area to enter track data
		this.trackDataTextArea = new TextArea();
		this.trackDataTextArea.setName("trackData");
		this.trackDataTextArea
				.setTitle("A list of node pairs where each line represents an edge.");
		this.trackDataTextArea.setWidth("100%");
		this.trackDataTextArea.setVisibleLines(10);

		table.setText(row, 0, "6a- type in some interactions ... OR ...");
		table.setWidget(row, 1, this.trackDataTextArea);
		row++;

		// Create a FileUpload widget -- alternate way to enter track data
		FileUpload upload = new FileUpload();
		upload.setName("uploadFormElement");

		table.setText(row, 0, "6b- upload a file of interactions");
		table.setWidget(row, 1, upload);
		row++;

		this.dataFormatListBox = new ListBox();
		this.dataFormatListBox.setTitle("data format");
		this.dataFormatListBox.addItem("simple tab", "tab");
		this.dataFormatListBox.addItem("UCSC pathway format", "ucsc");
		this.dataFormatListBox.addItem("SIF format", "sif");
		this.dataFormatListBox.setItemSelected(0, true);
		this.dataFormatListBox.setName("dataFormat");

		// TODO
		// table.setText(row, 0, "6c- data format");
		// table.setWidget(row, 1, this.dataFormatListBox);
		// row++;

		// button to submit form
		this.uploadButton = new Button("submit", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				form.submit();
			}
		});

		table.setText(row, 0, "7- submit your network");
		table.setWidget(row, 1, uploadButton);
		row++;

		initWidget(this.form);
		setStyleName(CSS_CLASS);
	}

	@Override
	public void onSubmit(SubmitEvent event) {
		// This event is fired just before the form is submitted. We can
		// take this opportunity to perform validation.

		if (trackNameTextBox.getText().length() == 0) {
			Window.alert("The network must have a name.");
			event.cancel();
		} else if (trackNameTextBox.getText().indexOf(" ") >= 0) {
			Window.alert("The network's name should have no spaces.");
			event.cancel();
		} else {
			LoggingDialogBox.log("Submitting network upload request: "
					+ form.getAction());
		}
	}

	@Override
	public void onSubmitComplete(SubmitCompleteEvent event) {
		// When the form submission is successfully completed, this
		// event is fired. Assuming the service returned a response of
		// type text/html, we can get the result text here (see the
		// FormPanel documentation for further explanation).
		String stringifiedJSONObject = event.getResults();
		JSONObject mainJO = (JSONObject) JSONParser
				.parseStrict(stringifiedJSONObject);
		boolean success = mainJO.get("submitSuccess").isBoolean()
				.booleanValue();
		if (success) {
			tcp.requestCustomTrackUpdate();
			Window.alert("Success submitting the network.  Here's the response we got: "
					+ stringifiedJSONObject);
		} else {
			Window.alert("Got bad vibes submitting the network.  Here's the response we got: "
					+ stringifiedJSONObject);
		}
	}
}
