package edu.ucsc.ib.client.viewcontrols;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.IbDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.NetworkVisualization;

public class LoadPathwayFileDialogBox extends IbDialogBox {

	private static final String DECODER_URL = "pathwayBounce/decoder";

	/**
	 * The outermost panel, the object's widget.
	 */
	private final SimplePanel dialogBoxPanel = new SimplePanel();

	/**
	 * additional form submission part
	 */
	private final TextBox fileNameTextBox = new TextBox();
	{
		fileNameTextBox.setName("fileName");
		fileNameTextBox.setTitle("name of file");
		fileNameTextBox.setVisible(false);
	}

	private final FileUpload fileUploadWidget = new FileUpload();
	{
		fileUploadWidget.setName("uploadFormElement");
		// fileUploadWidget.setWidth("250px");
	}

	/**
	 * Control for selecting file format.
	 */
	private final ListBox fileFormatListBox = new ListBox();
	{
		fileFormatListBox.setName("fileFormat");
		fileFormatListBox.setTitle("file format");
		fileFormatListBox.addItem("xgmml", "xgmml");
		fileFormatListBox.addItem("sif", "sif");
//		fileFormatListBox.addItem("UCSC pathway", "ucsc_pathway");
		fileFormatListBox.setSelectedIndex(0);

		fileFormatListBox.setVisible(true);
	}

	/**
	 * for uploading a file.
	 */
	private final FormPanel uploadFileFormPanel = new FormPanel();
	{
		uploadFileFormPanel.setAction(DECODER_URL);
		uploadFileFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadFileFormPanel.setMethod(FormPanel.METHOD_POST);

		VerticalPanel containerPanel = new VerticalPanel();
		uploadFileFormPanel.setWidget(containerPanel);

		/**
		 * SubmitHandler
		 */
		uploadFileFormPanel.addSubmitHandler(new SubmitHandler() {
			@Override
			public void onSubmit(SubmitEvent event) {
				LoggingDialogBox.log("LoadXgmmlDialogBox: begin onSubmit");
				// throbberStart();
				setUploadFileName(fileUploadWidget.getFilename());
				LoggingDialogBox.log("LoadXgmmlDialogBox: end onSubmit");
			}
		});

		/**
		 * SubmitCompleteHandler
		 */
		uploadFileFormPanel
				.addSubmitCompleteHandler(new SubmitCompleteHandler() {
					@Override
					public void onSubmitComplete(SubmitCompleteEvent event) {
						LoggingDialogBox
								.log("LoadXgmmlDialogBox: begin onSubmitComplete");
						handleUploadFileSubmitComplete(event, nv);
						// throbberStop();
						LoggingDialogBox
								.log("LoadXgmmlDialogBox: end onSubmitComplete");
					}
				});

		// controls for form submission parts
		containerPanel.add(fileUploadWidget);
		containerPanel.add(fileNameTextBox);
		containerPanel.add(fileFormatListBox);
	}

	/**
	 * submits file for uploading to server
	 */
	private final Button uploadFileButton = new Button("load pathway",
			new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					uploadFileFormPanel.submit();
				}
			});

	/**
	 * put file upload controls here
	 */
	private final HorizontalPanel uploadFilePanel = new HorizontalPanel();
	{
		uploadFilePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
		uploadFilePanel.add(uploadFileFormPanel);
		uploadFilePanel.add(uploadFileButton);
	}

	private final NetworkVisualization nv;

	// TODO ///////////////////////////////////////////////////////////////

	public LoadPathwayFileDialogBox(NetworkVisualization nv) {
		super("Load pathway file");

		this.setWidget(this.dialogBoxPanel);

		this.nv = nv;
		this.dialogBoxPanel.add(uploadFilePanel);
	}

	/**
	 * Get the JSON-RPC response from submitting file.
	 * 
	 * @param event
	 */
	protected void handleUploadFileSubmitComplete(SubmitCompleteEvent event,
			NetworkVisualization nv) {
		if (event.getResults().isEmpty()) {
			LoggingDialogBox
					.log("handleUploadFileSubmitComplete got a null result");
			return;
		}

		// expect a JSON-RPC compliant result
		JsonRpcResponse jsonRpcResp = new JsonRpcResponse(event.getResults());

		PathwayData pathwayData = new PathwayData(jsonRpcResp.getResult());

		pathwayData.visualize_annotations_included(nv);

	}

	/**
	 * Set the name of the uploaded file.
	 * 
	 * @param name
	 */
	public void setUploadFileName(String name) {
		fileNameTextBox.setText(name);
	}
}
