package edu.ucsc.ib.client.viewcontrols;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

public class TestDialogBox extends IbDialogBox {

	private static final String SAVE_PATHWAY_ACTION = "data/test/save_pathway";

	/**
	 * The outermost panel, the object's widget.
	 */
	private final SimplePanel dialogBoxPanel = new SimplePanel();

	/**
	 * name of pathway
	 */
	private final TextBox pathwayNameTextBox = new TextBox();
	{
		pathwayNameTextBox.setName("pathwayName");
		pathwayNameTextBox.setTitle("name of pathway");
		pathwayNameTextBox.setVisible(true);
	}

	/**
	 * pathway data
	 */
	private final TextBox pathwayDataTextBox = new TextBox();
	{
		pathwayDataTextBox.setName("pathwayData");
		pathwayDataTextBox.setTitle("pathwayData");
		pathwayDataTextBox.setVisible(true);
	}

	/**
	 * button for submitting form
	 */
	private final Button submitButton = new Button("save pathway",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					savePathwayFormPanel.submit();
				}
			});

	/**
	 * for uploading via POST.
	 */
	private final FormPanel savePathwayFormPanel = new FormPanel();
	{
		savePathwayFormPanel.setAction(SAVE_PATHWAY_ACTION);
		savePathwayFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
		savePathwayFormPanel.setMethod(FormPanel.METHOD_POST);

		VerticalPanel containerPanel = new VerticalPanel();
		containerPanel
				.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		containerPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		savePathwayFormPanel.setWidget(containerPanel);

		/**
		 * SubmitHandler
		 */
		savePathwayFormPanel.addSubmitHandler(new SubmitHandler() {
			@Override
			public void onSubmit(SubmitEvent event) {
				// throbberStart();
				setFormControls();
			}
		});

		/**
		 * SubmitCompleteHandler
		 */
		savePathwayFormPanel
				.addSubmitCompleteHandler(new SubmitCompleteHandler() {
					@Override
					public void onSubmitComplete(SubmitCompleteEvent event) {
						handleSubmitComplete(event, nv);
						// throbberStop();
					}
				});

		// controls for form submission parts
		containerPanel.add(pathwayNameTextBox);
		containerPanel.add(pathwayDataTextBox);
	}

	/**
	 * put controls here
	 */
	private final HorizontalPanel uploadFilePanel = new HorizontalPanel();
	{
		uploadFilePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
		uploadFilePanel.add(savePathwayFormPanel);
		uploadFilePanel.add(submitButton);
	}

	private final NetworkVisualization nv;

	// TODO ///////////////////////////////////////////////////////////////

	public TestDialogBox(NetworkVisualization nv) {
		super("test");

		this.setWidget(this.dialogBoxPanel);

		this.nv = nv;
		this.dialogBoxPanel.add(uploadFilePanel);
	}

	/**
	 * Get the JSON-RPC response from submitting file.
	 * 
	 * @param event
	 */
	protected void handleSubmitComplete(SubmitCompleteEvent event,
			NetworkVisualization nv) {
		if (event.getResults().isEmpty()) {
			LoggingDialogBox.log("handleSubmitComplete got a null result");
			return;
		}

		LoggingDialogBox.log(event.getResults());

		// expect a JSON-RPC compliant result
		// JsonRpcResponse jsonRpcResp = new
		// JsonRpcResponse(event.getResults());
	}

	/**
	 * Set the name of the pathway.
	 * 
	 * @param name
	 */
	public void setPathwayName(String name) {
		pathwayNameTextBox.setText(name);
	}

	/**
	 * Set the data of the pathway.
	 * 
	 * @param data
	 */
	public void setPathwayData(String data) {
		pathwayDataTextBox.setText(data);
	}

	/**
	 * Set form controls for submission.
	 */
	private void setFormControls() {
		// get name of pathway
		setPathwayName("testPathwayName");

		// get graph as a pathway
		JSONObject pathwayJO = this.nv.getJsonForXgmml();
		setPathwayData(pathwayJO.toString());
	}
}
