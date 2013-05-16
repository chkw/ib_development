package edu.ucsc.ib.client.viewcontrols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ucsc.ib.client.BiodeInfo;
import edu.ucsc.ib.client.BiodeSet;
import edu.ucsc.ib.client.JsonRpcResponse;
import edu.ucsc.ib.client.datapanels.ConceptsDashboardDialogBox;
import edu.ucsc.ib.client.datapanels.LegendDialogBox;
import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.BasicNode;
import edu.ucsc.ib.client.netviz.NetworkVisualization;
import edu.ucsc.ib.client.netviz.Track;

/**
 * A main MenuBar for the Interaction Browser.
 * 
 * @author cw
 * 
 */
public class MainMenuBar extends MenuBar {
	private static final String WELCOME_PAGE_URL = "ibWelcome.html";
	private static final String GETTING_STARTED_URL = "ibTutorial01.html";
	private static final String CIRCLEMAPS_HELP_URL = "ibTutorialCircleMap01.html";
	private static final String UPLOADED_DATA_HELP_URL = "ibTutorialLoadData01.html";
	private static final String STUART_LAB_WIKI_URL = "https://sysbiowiki.soe.ucsc.edu";

	private Request commonNeighborsCurrentRequest;
	private Request shortestPathCurrentRequest;

	private final NetworkVisualization netViz;

	private static BiodeControlPanel bcp;

	private static TrackControlPanel tcp;

	private final ConceptsDashboardDialogBox conceptsDashboard;

	private static NetworkDashboardDialogBox networkDashboard;

	private final NodeScoresDialogBox nodeScoresDialogBox;

	private final ScoredNodeFilterDialogBox scoredNodeFilterDialogBox;

	private final ShortestPathDialogBox shortestPathDialogBox;

	private final PathwayNameSearchDialogBox pathwayNameSearchDialogBox;

	private final CircleMapDialogBox circleMapDialogBox;

	private final LoggingDialogBox loggingDialogBox;

	/**
	 * Submit HTTP POST to IB's fileBounce servlet. It's invisible and attached
	 * to the default RootPanel so that it may be submitted.
	 */
	private final FileBounceFormPanel fileBounceFormPanel = new FileBounceFormPanel();
	{
		fileBounceFormPanel.setVisible(false);
		RootPanel.get().add(fileBounceFormPanel);
	}

	/**
	 * FormHandler for SubmitSaveStateFormPanel.
	 */
	private final SubmitSaveStateFormHandler sssfh = new SubmitSaveStateFormHandler();

	/**
	 * Submit HTTP POST to IB's save state servlet. It's invisible and attached
	 * to the default RootPanel so that it may be submitted.
	 */
	private final SubmitSaveStateFormPanel submitSaveStateFormPanel = new SubmitSaveStateFormPanel();
	{
		submitSaveStateFormPanel.addSubmitHandler(sssfh);
		submitSaveStateFormPanel.addSubmitCompleteHandler(sssfh);
		submitSaveStateFormPanel.setVisible(false);
		RootPanel.get().add(submitSaveStateFormPanel);
	}

	// TODO ///////////////////////////////////

	public MainMenuBar(final NetworkVisualization nv,
			final BiodeControlPanel bcpl, final TrackControlPanel tcpl,
			final boolean developer_mode) {

		netViz = nv;
		bcp = bcpl;
		tcp = tcpl;

		conceptsDashboard = new ConceptsDashboardDialogBox(netViz,
				developer_mode);
		networkDashboard = new NetworkDashboardDialogBox(netViz);

		nodeScoresDialogBox = new NodeScoresDialogBox(netViz);
		scoredNodeFilterDialogBox = new ScoredNodeFilterDialogBox(netViz);
		shortestPathDialogBox = new ShortestPathDialogBox(netViz);
		loggingDialogBox = new LoggingDialogBox(netViz);

		pathwayNameSearchDialogBox = new PathwayNameSearchDialogBox(netViz);
		circleMapDialogBox = new CircleMapDialogBox(netViz);

		// Make a command that we will execute from leaves.
		Command cmd = new Command() {
			public void execute() {
				Window.alert("You selected a menu item!");
			}
		};

		// layout menus
		final MenuBar layoutMenuBar = new MenuBar(true);
		layoutMenuBar.addItem("circle", new Command() {
			@Override
			public void execute() {
				netViz.doCircleLayout();
			}
		});
		layoutMenuBar.addItem("ring", new Command() {
			@Override
			public void execute() {
				netViz.doRingLayout();
			}
		});
		layoutMenuBar.addItem("spring", new Command() {
			@Override
			public void execute() {
				netViz.doSpringLayout(true);
			}
		});
		layoutMenuBar.addItem("random", new Command() {
			@Override
			public void execute() {
				netViz.doRandomLayout();
			}
		});
		if (developer_mode) {
			layoutMenuBar.addItem("upload coordinates", new Command() {
				@Override
				public void execute() {
					final NodeCoordinatesDialogBox dialogBox = new NodeCoordinatesDialogBox(
							netViz);
					showDialogBox(dialogBox, true);
				}
			});

			layoutMenuBar.addItem("_compartment", cmd);
		}

		// download menus
		final MenuBar downloadMenuBar = new MenuBar(true);
		if (developer_mode) {
			downloadMenuBar.addItem("SVG", new Command() {
				@Override
				public void execute() {
					// get SVG file
					fileBounceFormPanel.setForm("SVG");
					fileBounceFormPanel.setFileString(nv.getDrawPanelSVG());
					fileBounceFormPanel.submit();
				}
			});
		}

		downloadMenuBar.addItem("xgmml", new Command() {
			@Override
			public void execute() {
				// get xgmml file
				fileBounceFormPanel.setForm("xgmml_encode");
				String s = nv.getJsonForXgmml().toString();
				LoggingDialogBox.log(s);
				fileBounceFormPanel.setFileString(s);
				fileBounceFormPanel.submit();
			}
		});
		downloadMenuBar.addItem("links", new Command() {
			@Override
			public void execute() {
				// get links file
				fileBounceFormPanel.setForm("tab");
				fileBounceFormPanel.setFileString(nv.getLinksFileString());
				fileBounceFormPanel.submit();
			}
		});
		downloadMenuBar.addItem("selected nodes", new Command() {
			@Override
			public void execute() {
				// get list of nodes
				fileBounceFormPanel.setForm("list");
				String[] biodes = nv.getSelectedNodeIds().getArray();
				if (biodes.length == 0) {
					biodes = nv.getCurrentNodeIds().getArray();
				}
				StringBuffer sb = new StringBuffer("ID list");
				for (String biode : biodes) {
					sb.append("\t");
					sb.append(biode);
				}
				fileBounceFormPanel.setFileString(sb.toString());
				fileBounceFormPanel.submit();
			}
		});
		downloadMenuBar.addItem("save session ID", new Command() {
			@Override
			public void execute() {
				submitSaveStateFormPanel
						.setSaveStateString(savedStateToJSONObject().toString());
				submitSaveStateFormPanel.submit();
			}
		});

		// node visualization menu bar
		final MenuBar nodeVizMenuBar = new MenuBar(true);
		nodeVizMenuBar.addItem("circle images", new Command() {
			@Override
			public void execute() {
				showDialogBox(circleMapDialogBox, true);
			}
		});
		if (developer_mode) {
			nodeVizMenuBar.addItem("nodeScores", new Command() {
				@Override
				public void execute() {
					nodeScoresDialogBox.center();
					nodeScoresDialogBox.setVisible(true);
				}
			});

			nodeVizMenuBar.addItem("_shape and color", cmd);
		}

		final MenuBar FiltersMenuBar = new MenuBar(true);
		FiltersMenuBar.addItem("scored node-based edge filter", new Command() {
			@Override
			public void execute() {
				showDialogBox(scoredNodeFilterDialogBox, true);
			}
		});

		// networks visualization menu bar
		final MenuBar networksVizMenuBar = new MenuBar(true);

		final MenuBar selectNodesMenuBar = new MenuBar(true);
		selectNodesMenuBar.addItem("neighbors of selection", new Command() {
			@Override
			public void execute() {
				netViz.selectCurrentNeighbors(true);
			}
		});
		selectNodesMenuBar.addItem("hubs", new Command() {
			@Override
			public void execute() {
				BiodeSet selectBiodeSet = new BiodeSet();
				HashSet<BasicNode> hubsHashSet = netViz.getHubNodes();
				for (BasicNode nn : hubsHashSet) {
					if (nn.isVisible()) {
						selectBiodeSet.add(nn.getID());
					}
				}
				netViz.selectBiodeSet(selectBiodeSet);
			}
		});
		selectNodesMenuBar.addItem("invert selection", new Command() {
			@Override
			public void execute() {
				netViz.invertSelection();
			}
		});

		final MenuBar expandNodesMenuBar = new MenuBar(true);
		if (developer_mode) {
			expandNodesMenuBar.addItem("_GeneMANIA", cmd);

			expandNodesMenuBar.addItem("Iterative Bayes", new Command() {
				@Override
				public void execute() {
					final IterativeBayesPathwayExpansionDialogBox dialogBox = new IterativeBayesPathwayExpansionDialogBox(
							netViz);
					showDialogBox(dialogBox, true);
				}
			});
		}
		expandNodesMenuBar.addItem("common neighbors", new Command() {
			@Override
			public void execute() {
				// HashSet<String> allTracks = nv.getTrackNamesHash(true);
				// HashSet<String> allTracks = nv.getTrackNamesHash();
				HashSet<String> allTracks = nv.getTrackNamesHash(false);

				// get lists of regular and custom tracks
				ArrayList<String> tracks = new ArrayList<String>();
				ArrayList<String> customTracks = new ArrayList<String>();

				for (String trackName : allTracks) {
					if (tcp.getTrack(trackName).isCustom()) {
						customTracks.add(trackName);
					} else {
						tracks.add(trackName);
					}
				}

				String[] biodes = (String[]) nv.getSelectedNodeIds().toArray(
						new String[0]);
				if (biodes.length == 0) {
					biodes = (String[]) nv.getCurrentNodeIds().toArray(
							new String[0]);
					LoggingDialogBox.log("getting neighbors for ALL");
				} else {
					LoggingDialogBox.log("getting neighbors for SELECTED");
				}

				// call getNeighbors
				// TODO currently ignores the custom tracks
				if ((tracks.size() > 0) && (biodes.length > 0)) {
					getNeighbors((String[]) tracks.toArray(new String[0]),
							biodes, true, commonNeighborsCurrentRequest);
				}
			}
		});

		expandNodesMenuBar.addItem("get neighbors", new Command() {
			@Override
			public void execute() {
				HashSet<String> allTracks = nv.getTrackNamesHash(false);

				// get lists of regular and custom tracks
				ArrayList<String> tracks = new ArrayList<String>();
				ArrayList<String> customTracks = new ArrayList<String>();

				for (String trackName : allTracks) {
					if (tcp.getTrack(trackName).isCustom()) {
						customTracks.add(trackName);
					} else {
						tracks.add(trackName);
					}
				}

				String[] biodes = (String[]) nv.getSelectedNodeIds().toArray(
						new String[0]);
				if (biodes.length == 0) {
					biodes = (String[]) nv.getCurrentNodeIds().toArray(
							new String[0]);
					LoggingDialogBox.log("getting neighbors for ALL");
				} else {
					LoggingDialogBox.log("getting neighbors for SELECTED");
				}

				// call getNeighbors
				// TODO currently ignores the custom tracks
				if ((tracks.size() > 0) && (biodes.length > 0)) {
					getNeighbors((String[]) tracks.toArray(new String[0]),
							biodes, false, commonNeighborsCurrentRequest);
				}
			}
		});

		expandNodesMenuBar.addItem("add shortest path", new Command() {
			@Override
			public void execute() {
				// HashSet<String> allTracks = nv.getTrackNamesHash(true);
				HashSet<String> allTracks = nv.getTrackNamesHash();

				// get lists of regular and custom tracks
				ArrayList<String> tracks = new ArrayList<String>();
				ArrayList<String> customTracks = new ArrayList<String>();

				for (String trackName : allTracks) {
					if (tcp.getTrack(trackName).isCustom()) {
						customTracks.add(trackName);
					} else {
						tracks.add(trackName);
					}
				}
				if (tracks.size() < 1) {
					Window.alert("shortest path requires at least one network");
					return;
				}

				String[] biodes = (String[]) nv.getSelectedNodeIds().toArray(
						new String[0]);
				if (biodes.length != 2) {
					Window.alert("shortest path requires exactly 2 selected nodes.  The first selected node will be the origin.  The second one will be the destination.");
					return;
				}

				// call getShortestPath
				// TODO currently ignores the custom tracks
				if ((tracks.size() > 0) && (biodes.length == 2)) {
					getShortestPath((String[]) tracks.toArray(new String[0]),
							biodes, shortestPathCurrentRequest);
				}
			}
		});

		expandNodesMenuBar.addItem("ShortestPathDialogBox", new Command() {
			@Override
			public void execute() {
				showDialogBox(shortestPathDialogBox, true);
			}
		});

		// expandNodesMenuBar.addItem("ScoredNodeFilterDialogBox", new Command()
		// {
		// @Override
		// public void execute() {
		// showDialogBox(scoredNodeFilterDialogBox, true);
		// }
		// });

		final MenuBar selectNetsMenuBar = new MenuBar(true);
		if (developer_mode) {
			selectNetsMenuBar.addItem("_multi-supported", cmd);
			selectNetsMenuBar.addItem("_invert selection", cmd);
			selectNetsMenuBar.addItem("_all", cmd);
		}

		// unconnected nodes
		final MenuBar unconnectedNodesMenuBar = new MenuBar(true);
		unconnectedNodesMenuBar.addItem("select unconnected nodes",
				new Command() {
					@Override
					public void execute() {
						nv.selectUnconnectedNodes();
					}
				});
		unconnectedNodesMenuBar.addItem("remove unconnected nodes",
				new Command() {
					@Override
					public void execute() {
						bcp.appendUserInputString(nv.removeUnconnectedNodes());
					}
				});

		// file menu
		final MenuBar fileMenuBar = new MenuBar(true);
		fileMenuBar.addItem("save", downloadMenuBar);
		fileMenuBar.addItem("load ID", new Command() {
			@Override
			public void execute() {
				final RetrieveStateDialogBox dialogBox = new RetrieveStateDialogBox(
						netViz);
				showDialogBox(dialogBox, true);
			}
		});

		// edit menubar
		final MenuBar editMenuBar = new MenuBar(true);
		editMenuBar.addItem("reload page", new Command() {
			@Override
			public void execute() {
				Window.Location.reload();
			}
		});
		editMenuBar.addItem("clear browser", new Command() {
			@Override
			public void execute() {
				StringBuffer newUrlSB = new StringBuffer(Window.Location
						.getHref());

				// remove url query string
				newUrlSB.replace(newUrlSB.indexOf("?"), newUrlSB.length(), "");

				// load the new url
				Window.Location.assign(newUrlSB.toString());
			}
		});

		// visualization menu
		final MenuBar visualizationMenuBar = new MenuBar(true);
		visualizationMenuBar.addItem("layout", layoutMenuBar);
		visualizationMenuBar.addItem("concepts", nodeVizMenuBar);
		if (developer_mode) {
			visualizationMenuBar.addItem("filters", FiltersMenuBar);
			visualizationMenuBar.addItem("networks", networksVizMenuBar);
		}

		// pathways menu
		final MenuBar pathwaysMenuBar = new MenuBar(true);
		pathwaysMenuBar.addItem("search by name", new Command() {
			@Override
			public void execute() {
				showDialogBox(pathwayNameSearchDialogBox, true);
			}
		});

		// load xgmml file
		final LoadPathwayFileDialogBox loadXgmmlDialogBox = new LoadPathwayFileDialogBox(
				netViz);
		pathwaysMenuBar.addItem("load pathway from xgmml file", new Command() {
			@Override
			public void execute() {
				showDialogBox(loadXgmmlDialogBox, true);
			}
		});

		if (developer_mode) {
			final TestDialogBox pathwaySavingDialogBox = new TestDialogBox(
					netViz);

			pathwaysMenuBar.addItem("save pathway on server", new Command() {
				@Override
				public void execute() {
					showDialogBox(pathwaySavingDialogBox, true);
				}
			});
		}

		// concepts menu
		final MenuBar conceptsMenuBar = new MenuBar(true);
		conceptsMenuBar.addItem("browse", new Command() {
			@Override
			public void execute() {
				showDialogBox(bcp, true);
			}
		});
		conceptsMenuBar.addItem("dashboard", new Command() {
			@Override
			public void execute() {
				showDialogBox(conceptsDashboard, true);
			}
		});
		conceptsMenuBar.addItem("expand", expandNodesMenuBar);
		conceptsMenuBar.addItem("select", selectNodesMenuBar);
		conceptsMenuBar.addItem("unconnected", unconnectedNodesMenuBar);

		// networks menu
		final MenuBar networksMenuBar = new MenuBar(true);
		networksMenuBar.addItem("browse", new Command() {
			@Override
			public void execute() {
				showDialogBox(tcp, true);
			}
		});
		networksMenuBar.addItem("dashboard", new Command() {
			@Override
			public void execute() {
				showDialogBox(networkDashboard, true);
			}
		});
		if (developer_mode) {
			networksMenuBar.addItem("_Network Recommender", cmd);
			networksMenuBar.addItem("select", selectNetsMenuBar);
		}

		// help menu
		final MenuBar helpMenuBar = new MenuBar(true);

		final LegendDialogBox legendDialogBox = new LegendDialogBox();
		showDialogBox(legendDialogBox, false);
		helpMenuBar.addItem("legend for pathways", new Command() {
			@Override
			public void execute() {
				showDialogBox(legendDialogBox, true);
			}
		});
				
		helpMenuBar.addItem("Interaction Browser Home Page", new Command() {
			@Override
			public void execute() {
				Window.open(WELCOME_PAGE_URL, "_ib_help", "");
			}
		});
		helpMenuBar.addItem("Help Getting Started", new Command() {
			@Override
			public void execute() {
				Window.open(GETTING_STARTED_URL, "_ib_help", "");
			}
		});
		helpMenuBar.addItem("Help with CircleMaps", new Command() {
			@Override
			public void execute() {
				Window.open(CIRCLEMAPS_HELP_URL, "_ib_help", "");
			}
		});
		helpMenuBar.addItem("Help with Uploading Data", new Command() {
			@Override
			public void execute() {
				Window.open(UPLOADED_DATA_HELP_URL, "_ib_help", "");
			}
		});
		helpMenuBar.addItem("Stuart Lab Home Page", new Command() {
			@Override
			public void execute() {
				Window.open(STUART_LAB_WIKI_URL, "_ib_help", "");
			}
		});

		// main menu items
		this.addItem("File", fileMenuBar);
		this.addItem("Edit", editMenuBar);
		// this.addItem("Browse", browseMenuBar);
		this.addItem("Visualization", visualizationMenuBar);
		this.addItem("Pathways", pathwaysMenuBar);
		this.addItem("Concepts", conceptsMenuBar);
		this.addItem("Networks", networksMenuBar);
		this.addItem("Help", helpMenuBar);
	}

	/**
	 * Formhandler for SubmitSaveStateFormPanel. Implements
	 * FormPanel.SubmitHandler and FormPanel.SubmitCompleteHandler.
	 * 
	 * @author cw
	 * 
	 */
	final class SubmitSaveStateFormHandler implements FormPanel.SubmitHandler,
			FormPanel.SubmitCompleteHandler {

		@Override
		public void onSubmit(SubmitEvent event) {
			// This event is fired just before the form is submitted. We can
			// take this opportunity to perform validation.
			if (submitSaveStateFormPanel.getSaveStateString().length() <= 0) {
				Window.alert("Nothing to save.");
				event.cancel();
			} else {
				LoggingDialogBox.log("Submitting save state request: "
						+ submitSaveStateFormPanel.getAction());
			}
		}

		@Override
		public void onSubmitComplete(SubmitCompleteEvent event) {
			// When the form submission is successfully completed, this
			// event is fired. Assuming the service returned a response of
			// type text/html, we can get the result text here (see the
			// FormPanel documentation for further explanation).

			String results = event.getResults();
			if (results != null) {
				String href = Window.Location.getHref();
				int end = href.indexOf("?");
				if (end != -1) {
					href = href.substring(0, end);
				}
				Window.alert("Use the link: " + href + "?savedState=" + results);
			} else {
				Window.alert("Got bad vibes.");
			}
		}
	}

	/**
	 * Submit HTTP POST request to IB's save state service.
	 * 
	 * @author cw
	 * 
	 */
	private final class SubmitSaveStateFormPanel extends FormPanel {
		{
			// Create a FormPanel and point it at a service - mapped in web.xml
			setAction("data/savedStateDB/submit");
			setEncoding(FormPanel.ENCODING_URLENCODED);
			setMethod(FormPanel.METHOD_POST);
		}

		// hidden text box for submitting the save state JSON Text as
		// "fileString"
		private final TextBox savedStateStringTextBox = new TextBox();
		{
			savedStateStringTextBox.setName("saveStateString");
			savedStateStringTextBox.setVisible(false);
			add(savedStateStringTextBox);
		}

		/**
		 * Set the String for savedStateStringTextBox.
		 * 
		 * @param data
		 */
		public void setSaveStateString(String data) {
			savedStateStringTextBox.setText(data);
		}

		/**
		 * Get the String for savedStateStringTextBox.
		 * 
		 * @return
		 */
		public String getSaveStateString() {
			return savedStateStringTextBox.getText();
		}
	}

	/**
	 * Submit HTTP POST request to IB's file bounce service.
	 * 
	 * @author cw
	 * 
	 */
	private final class FileBounceFormPanel extends FormPanel {

		/**
		 * Submitted to the server as "fileString".
		 */
		private final TextBox dataTextBox = new TextBox();
		{
			dataTextBox.setName("fileString");
			dataTextBox.setVisible(false);
			add(dataTextBox);
		}

		/**
		 * Set the String for dataTextBox.
		 * 
		 * @param data
		 */
		public void setFileString(String data) {
			dataTextBox.setText(data);
		}

		/**
		 * Prepare the form for sending with specified urlSuffix.
		 * 
		 * @param urlSuffix
		 */
		public void setForm(String urlSuffix) {
			setAction("fileBounce/" + urlSuffix);
			setEncoding(FormPanel.ENCODING_URLENCODED);
			setMethod(FormPanel.METHOD_POST);
		}
	}

	/**
	 * Send servlet request for shortest path.
	 * 
	 * @param tracks
	 * @param biodes
	 * @return
	 */
	private void getShortestPath(String[] tracks, String[] biodes,
			Request currentRequest) {

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback requestCallback = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("getShortestPath failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
			}

			public void onResponseReceived(Request request, Response response) {
				// get the results
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					return;
				}

				LoggingDialogBox
						.log("no error in JSON-RPC result object, continue");

				JSONObject resultJO = jsonRpcResp.getResult();

				// get JSONarray of biodes
				JSONArray ja = resultJO.get("shortestPaths").isArray().get(0)
						.isObject().get("path").isArray();

				// get array of biodes
				String[] biodes = new String[ja.size()];
				for (int i = 0; i < ja.size(); i++) {
					biodes[i] = ja.get(i).isString().stringValue();
				}

				// find out which biodes are new biodes
				final BiodeSet resultBiodes = netViz.getNewBiodes(new BiodeSet(
						Arrays.asList(biodes)));

				// using DialogBox instead of PopupPanel
				final DialogBox resultsDialogBox = new DialogBox();
				resultsDialogBox.setText("shortest path");

				HorizontalPanel buttonPanel = new HorizontalPanel();

				Button createNodesButton = new Button("create nodes in graph",
						new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								// add biodes to netviz
								// this step should involve lookup service to
								// get all biodeinfo for the items to add

								BiodeUserInputPanel
										.processSubmissionWithLookupService_single_sp(
												SearchSpaceControl
														.getSystemspace(),
												resultBiodes.getArray(), true);

								resultsDialogBox.hide();
							}

						});

				Button sendIDsButton = new Button("send the IDs to text area",
						new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								// send biodes to BiodeUserInputPanel
								bcp.appendUserInputString(resultBiodes);
								resultsDialogBox.hide();
							}

						});

				Button cancelButton = new Button("cancel", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						resultsDialogBox.hide();
					}

				});

				buttonPanel.add(createNodesButton);
				buttonPanel.add(sendIDsButton);
				buttonPanel.add(cancelButton);

				VerticalPanel vp = new VerticalPanel();

				int numResults = resultBiodes.size();

				if (numResults < 1) {
					createNodesButton.setEnabled(false);
					vp.add(new Label("No path was found."));
				} else {
					vp.add(new Label("The shortest path has " + numResults
							+ " nodes not currently displayed."));
				}

				vp.add(buttonPanel);

				resultsDialogBox.add(vp);

				// resultsDialogBox.center();
				// resultsDialogBox.show();
				showDialogBox(resultsDialogBox, true);

				createNodesButton.setFocus(true);
			}
		};

		// get a result object
		String trackListString = BiodeControlPanel
				.arrayToCommaSeparatedString(tracks);

		String urlString = "data/trackdb/shortestPath?trackList="
				+ trackListString + "&origins=" + biodes[0] + "&destinations="
				+ biodes[1];

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, urlString);
		try {
			currentRequest = rb.sendRequest(null, requestCallback);
		} catch (Exception e) {
			LoggingDialogBox.log("exception sending request " + urlString
					+ ": " + e.toString());
		}
		return;
	}

	/**
	 * Send servlet request to get neighbors of biodes.
	 * 
	 * @param tracks
	 * @param biodes
	 * @param restrictToCommonOnes
	 *            If true, restrict to neighbors common to all biodes. If false,
	 *            get all neighbors.
	 * @param currentRequest
	 */
	private void getNeighbors(String[] tracks, String[] biodes,
			final boolean restrictToCommonOnes, Request currentRequest) {

		// cancel any running request
		if (currentRequest != null) {
			currentRequest.cancel();
			currentRequest = null;
		}

		RequestCallback requestCallback = new RequestCallback() {
			public void onError(Request request, Throwable exception) {
				LoggingDialogBox.log("getNeighbors failed");
				LoggingDialogBox.log("=====================================");
				LoggingDialogBox.log(exception.toString());
				LoggingDialogBox.log("=====================================");
			}

			public void onResponseReceived(Request request, Response response) {
				// get the results
				JsonRpcResponse jsonRpcResp = new JsonRpcResponse(
						response.getText());

				// check for error message in JSON-RPC response
				if (jsonRpcResp.hasError()) {
					LoggingDialogBox.log("got an error: "
							+ jsonRpcResp.getError().toString());
					return;
				}

				LoggingDialogBox
						.log("no error in JSON-RPC result object, continue");

				JSONObject resultJO = jsonRpcResp.getResult();

				// get JSONarray of biodes
				JSONArray ja = resultJO.get("neighbors").isArray();

				// get array of biodes
				String[] biodes = new String[ja.size()];
				for (int i = 0; i < ja.size(); i++) {
					biodes[i] = ja.get(i).isString().stringValue();
				}

				// find out which biodes are new biodes
				final BiodeSet newNeighbors = netViz.getNewBiodes(new BiodeSet(
						Arrays.asList(biodes)));

				// filter out "special nodes"
				final BiodeSet filteredBS = new BiodeSet();
				for (String biode : newNeighbors) {
					if (biode.endsWith(" (complex)")
							|| biode.endsWith(" (abstract)")
							|| biode.endsWith(" (family)")) {
						// skip it
					} else {
						filteredBS.add(biode);
					}
				}

				// using DialogBox instead of PopupPanel
				final DialogBox resultsDialogBox = new DialogBox();

				if (restrictToCommonOnes) {
					resultsDialogBox.setText("new common neighbors");
				} else {
					resultsDialogBox.setText("new immediate neighbors");
				}

				VerticalPanel buttonPanel = new VerticalPanel();

				Button createRestrictedNodesButton = new Button("create "
						+ filteredBS.size()
						+ " nodes - no complex, abstract, etc.",
						new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								// add biodes to netviz
								// this step should involve lookup service to
								// get all biodeinfo for the items to add

								BiodeUserInputPanel
										.processSubmissionWithLookupService_single_sp(
												SearchSpaceControl
														.getSystemspace(),
												filteredBS.getArray(), true);

								resultsDialogBox.hide();
							}

						});

				Button createNodesButton = new Button("create "
						+ newNeighbors.size() + " nodes", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						// add biodes to netviz
						// this step should involve lookup service to
						// get all biodeinfo for the items to add

						// BiodeUserInputPanel
						// .processSubmissionWithLookupService_multi_spp(newNeighbors
						// .getArray());

						BiodeUserInputPanel
								.processSubmissionWithLookupService_single_sp(
										SearchSpaceControl.getSystemspace(),
										newNeighbors.getArray(), true);

						resultsDialogBox.hide();
					}

				});

				Button sendIDsButton = new Button("send the IDs to text area",
						new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								// send biodes to BiodeUserInputPanel
								bcp.appendUserInputString(newNeighbors);
								resultsDialogBox.hide();
							}

						});

				Button cancelButton = new Button("cancel", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						resultsDialogBox.hide();
					}

				});

				// add buttons to buttonPanel
				buttonPanel.add(createNodesButton);
				buttonPanel.add(createRestrictedNodesButton);
				buttonPanel.add(sendIDsButton);
				buttonPanel.add(cancelButton);

				// don't allow too many nodes at once
				int currentNumNodes = netViz.getCurrentNodeIds().size();
				if (newNeighbors.size() + currentNumNodes > 200) {
					createNodesButton.setEnabled(false);
					createNodesButton.setTitle("too many nodes to add");
				}

				if (filteredBS.size() + currentNumNodes > 200) {
					createRestrictedNodesButton.setEnabled(false);
					createRestrictedNodesButton
							.setTitle("too many nodes to add");
				}

				VerticalPanel vp = new VerticalPanel();

				vp.add(new Label("The selected nodes have "
						+ newNeighbors.size() + " neighbors in common."));

				vp.add(buttonPanel);

				resultsDialogBox.add(vp);

				// resultsDialogBox.center();
				// resultsDialogBox.show();
				showDialogBox(resultsDialogBox, true);

				createNodesButton.setFocus(true);
			}
		};

		// get a result object
		String trackListString = BiodeControlPanel
				.arrayToCommaSeparatedString(tracks);
		String biodeListString = BiodeControlPanel
				.arrayToCommaSeparatedString(biodes);

		StringBuffer sb = new StringBuffer();

		if (restrictToCommonOnes) {
			sb.append("data/trackdb/commonNeighbors?");
		} else {
			sb.append("data/trackdb/neighbors?");
		}

		sb.append("trackList=" + trackListString + "&biodeList="
				+ biodeListString);

		RequestBuilder rb = new RequestBuilder(RequestBuilder.GET,
				sb.toString());
		try {
			currentRequest = rb.sendRequest(null, requestCallback);
		} catch (Exception e) {
			LoggingDialogBox.log("exeception sending request " + sb.toString()
					+ ": " + e.toString());
		}
		return;
	}

	/**
	 * Create a JSONObject that has data for biodes and tracks in a saved state.
	 * 
	 * @return The returned JSONObject has two parts, "biodeInfo" and "tracks".
	 *         "biodeInfo" is a JSONArray of BiodeInfo data in JSONObjects. Each
	 *         of the JSONObjects has key-value pairings for the BiodeInfo.
	 *         "tracks" is a JSONArray of JSONObjects. The track JSONObjects
	 *         have key-value pairings.
	 */
	private JSONObject savedStateToJSONObject() {
		JSONObject savedStateJSONObject = new JSONObject();

		// make a JSONObject for each BiodeInfo object
		JSONArray biodeJA = new JSONArray();
		String[] strArray = netViz.getCurrentNodeIds().getArray();
		for (String biode : strArray) {
			// add JSONObject for each biode info
			BiodeInfo info = netViz.BIC.getBiodeInfo(biode);

			BasicNode node = netViz.getNode(info.getSystematicName());

			info.setColor(node.getColor());
			info.setShapeCode(node.getShapeType());
			info.setPosition(node.getX_drawable(), node.getY_drawable());

			biodeJA.set(biodeJA.size(), info.toJSONObject());
		}

		// Add biodeInfo to savedStateJSONObject
		savedStateJSONObject.put("biodeInfo", biodeJA);

		// make a JSONArray of track names
		JSONArray trackJA = new JSONArray();
		for (Track t : netViz.getTracks()) {
			JSONObject trackJO = t.getTrackListing().getJOSNObject();
			trackJA.set(trackJA.size(), trackJO);
		}

		// Add list of tracks to savedStateJSONObject
		savedStateJSONObject.put("tracks", trackJA);

		// TODO get the active scoreset from netviz

		return savedStateJSONObject;
	}

	/**
	 * Add MenuItems to this object for testing.
	 */
	public void addDeveloperMenuItems() {
		this.addSeparator();

		this.addItem("BEAST", new Command() {
			@Override
			public void execute() {
				Window.open("http://sysbio.soe.ucsc.edu/beast/", "beast", null);
			}
		});

		this.addItem("log", new Command() {
			@Override
			public void execute() {
				showDialogBox(loggingDialogBox, true);
			}
		});

		this.addItem("BioIntPathwayDialogBox", new Command() {
			@Override
			public void execute() {
				final BioIntPathwayDialogBox dialogBox = new BioIntPathwayDialogBox(
						netViz);
				showDialogBox(dialogBox, true);
			}
		});
	}

	/**
	 * show the dialog box, bring it to front (zIndex)
	 * 
	 * @param dialogBox
	 * @param bringToFront
	 */
	public static void showDialogBox(final DialogBox dialogBox,
			final boolean bringToFront) {
		if (!dialogBox.isShowing() && !(dialogBox instanceof LegendDialogBox)) {
			dialogBox.center();
		} else {
			if (bringToFront) {
				dialogBox.hide();
			}
		}
		dialogBox.show();
	}

	/**
	 * show the networks dashboard
	 */
	public static void showNetsDash() {
		showDialogBox(networkDashboard, false);
	}

	/**
	 * show the networks browser
	 */
	public static void showNetsBrowse() {
		showDialogBox(tcp, false);
	}
}
