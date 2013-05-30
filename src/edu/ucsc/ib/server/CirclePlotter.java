/**
 * 
 */
package edu.ucsc.ib.server;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import org.apache.batik.svggen.SVGGraphics2D;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ucsc.ib.server.CircleMap.CircleMapData;
import edu.ucsc.ib.server.CircleMap.CircleMapData.RingData;
import edu.ucsc.ib.server.CircleMap.Plotter;

/**
 * This is a class that CirclePlotService uses to paint and write image files to
 * the server filesystem. Clients can then access the image files by URL. This
 * class does NOT have any access to the datasource. It relies on
 * CirclePlotService to grab the relevant data and pass it in.
 * 
 * @author chrisw
 * 
 */
public class CirclePlotter {

	private static final boolean isLoggerOn = true;

	/**
	 * Directory in servlet context to save image files.
	 */
	static final String IMAGE_FILE_OUTPUT_DIRECTORY = "/circleOutput/";

	/**
	 * ServletContext required to determine path to save image files.
	 */
	private final ServletContext servletContext;

	private final double imageWidth;
	private final double imageHeight;

	/**
	 * Ordered list of rings from innermost to outermost in the CircleMap.
	 */
	private ArrayList<String> ringsList;

	/**
	 * Ring to base sample sorting on.
	 */
	private String sortingRing;

	/**
	 * HashMap with the following structure: matrixName -> feature -> sampleName
	 * -> value. Note that not all samples and/or features may be available for
	 * all matrices. Also, there may be some non-numerical values present.
	 */
	private HashMap<String, HashMap<String, HashMap<String, String>>> scoresMatrices = null;

	/**
	 * Metadata for score matrices. Includes [sampleNames, category, dataType,
	 * tableName, cohortMax, cohortMin].
	 */
	private HashMap<String, HashMap<String, String>> scoreMatrixMetadata = null;

	/**
	 * HashMap with the following structure: tablename -> feature -> patientID
	 * -> fature value. Note that not all patients may be available in all
	 * tables.
	 */
	private HashMap<String, HashMap<String, HashMap<String, String>>> clinicalData = null;

	/**
	 * switch to draw sample group summary CircleMaps.
	 */
	private boolean sampleGroupSummarySwitch = false;

	/**
	 * switch to attempt merge rings.
	 */
	private boolean ringMergeSwitch = false;

	/**
	 * Switch to include only samples that have data in each loaded dataset.
	 */
	private boolean onlyCompleteSampleNames = false;

	/**
	 * These are the min/max values for the scores matrices data.
	 */
	private HashMap<String, HashMap<String, Double>> scoreMatricesMinMax = null;

	/**
	 * Ordered sample names.
	 */
	private ArrayList<String> samplesArrayList = null;

	/**
	 * Features for which circle plot images will be drawn.
	 */
	private HashSet<String> featuresHashSet = null;

	/**
	 * Feature for use in sample ordering.
	 */
	private String orderFeature = null;

	/**
	 * Color assigned to minimum score.
	 */
	private Color minColor;

	/**
	 * Color assigned to maximum score.
	 */
	private Color maxColor;

	/**
	 * Color assigned to zero score.
	 */
	private Color zeroColor;

	/**
	 * Color assigned to NA score.
	 */
	private Color naColor;

	/**
	 * Color assigned to samples with "no_label"
	 */
	private Color nlColor;

	/**
	 * Color assigned to indicate some error.
	 */
	private Color errorColor = Color.MAGENTA;

	/**
	 * This object plots BufferedImage. The BufferedImage can then be written to
	 * a file.
	 */
	private final Plotter plotter;

	/**
	 * This is a part of the String to compute the filename for each generated
	 * CircleMap. It is combined with the feature/gene's name to make a String
	 * which is then hashed for the final name.
	 */
	private String baseFilename = "";

	/**
	 * JSONArray of JSONObject with keys [name,members]. Map a TCGA datatype
	 * ring to list of member rings. Unmerged rings, such as clinical ones,
	 * should be mapped to list with one member, itself.
	 */
	private JSONArray ringMergeInfoJA = null;

	/**
	 * Since the sample grouping rings should not change between CircleMaps,
	 * compute them once only. Store the RingData for them here. Keyed on
	 * "tableName__featureName".
	 */
	private HashMap<String, RingData> sampleGroupingRingData = new HashMap<String, RingData>();

	/**
	 * color key for sample grouping rings. ring name -> grouping name -> Color
	 */
	private HashMap<String, HashMap<String, Color>> groupingColorKeys = new HashMap<String, HashMap<String, Color>>();

	/**
	 * If true, draw new CircleMap images.
	 */
	private boolean drawAll = false;

	/**
	 * A list of Color objects where adjacent colors are contrasting.
	 */
	private static final ArrayList<Color> colorList = new ArrayList<Color>();
	{
		// colorList.add(Color.BLACK);
		// colorList.add(Color.DARK_GRAY);
		// colorList.add(Color.GRAY);
		// colorList.add(Color.LIGHT_GRAY);
		// colorList.add(Color.WHITE);

		colorList.add(new Color(0, 128, 128)); // teal
		colorList.add(new Color(237, 145, 33)); // carrot orange
		colorList.add(new Color(0, 183, 235)); // cyan (subtractive primary)
		colorList.add(new Color(46, 139, 87)); // sea green
		colorList.add(new Color(215, 0, 64)); // carmine
		colorList.add(new Color(128, 128, 0)); // olive
		colorList.add(new Color(255, 216, 0)); // school bus yellow
		colorList.add(new Color(120, 81, 169)); // royal purple
		colorList.add(Color.PINK);
		colorList.add(Color.BLUE);
		colorList.add(Color.RED);

		// colorList.add(Color.red);
		// colorList.add(Color.orange);
		// colorList.add(Color.yellow);
		// colorList.add(Color.GREEN);
		// colorList.add(Color.blue);
	}

	// TODO constructor section below

	/**
	 * ServletContext required to determine path to save image files.
	 * 
	 * @param servletContext
	 * @param width
	 *            a square image is assumed
	 */
	public CirclePlotter(ServletContext servletContext, double width) {
		this(servletContext, width, width);
	}

	/**
	 * ServletContext required to determine path to save image files.
	 * 
	 * @param servletContext
	 * @param width
	 * @param height
	 */
	public CirclePlotter(ServletContext servletContext, double width,
			double height) {
		this.servletContext = servletContext;
		this.imageWidth = width;
		this.imageHeight = height;

		this.plotter = new Plotter(this.imageWidth, this.imageHeight);
	}

	// TODO method section below ///////////////////////////////

	/**
	 * Print a message to System.out .
	 * 
	 * @param message
	 */
	private void log(String message) {
		if (isLoggerOn) {
			System.out.println("CirclePlotter: " + message);
		}
	}

	/**
	 * 
	 * @param ringsList
	 *            Identity of rings in the order of appearance from inner to
	 *            outer.
	 * @param scoresMatrices
	 *            Features' score data. This is sample data for each gene.
	 * @param clinicalData
	 *            Samples' clinical data. This is the clinical feature for each
	 *            patient.
	 * @param featuresHashSet
	 *            Set of genes for which CircleMaps should be drawn.
	 * @param orderFeature
	 *            Gene to use for determining sample order.
	 * @param sortingRing
	 *            Ring on which to base sample order.
	 * @param scoreMatrixMetadata
	 *            Metadata for score matrices. This should include min/max
	 *            values for each dataset.
	 * @param sampleGroupSummarySwitch
	 *            switch to use sample group summary CircleMaps.
	 * @param ringMergeSwitch
	 *            switch to attempt ring merging.
	 * @param onlyCompleteSampleData
	 *            switch to only include samples that have data in all datasets.
	 *            No missing samples.
	 * @param drawAll
	 *            TODO
	 */
	public void setOptions(
			ArrayList<String> ringsList,
			HashMap<String, HashMap<String, HashMap<String, String>>> scoresMatrices,
			HashMap<String, HashMap<String, HashMap<String, String>>> clinicalData,
			HashSet<String> featuresHashSet, String orderFeature,
			String sortingRing,
			HashMap<String, HashMap<String, String>> scoreMatrixMetadata,
			boolean sampleGroupSummarySwitch, boolean ringMergeSwitch,
			boolean onlyCompleteSampleData, boolean drawAll) {

		// assign values
		this.ringsList = ringsList;

		boolean foundUploadedName = false;
		for (String ringName : this.ringsList) {
			if (ringName.endsWith("_uploaded")) {
				foundUploadedName = true;
				break;
			}
		}

		if (drawAll == true || foundUploadedName == true) {
			this.drawAll = true;
		} else {
			this.drawAll = false;
		}

		this.scoresMatrices = scoresMatrices;
		this.clinicalData = clinicalData;
		this.featuresHashSet = featuresHashSet;
		this.orderFeature = orderFeature;

		this.sortingRing = sortingRing;
		this.scoreMatrixMetadata = scoreMatrixMetadata;

		this.sampleGroupSummarySwitch = sampleGroupSummarySwitch;

		this.ringMergeSwitch = ringMergeSwitch;

		this.onlyCompleteSampleNames = onlyCompleteSampleData;

		this.baseFilename = computeBaseFilename();

		setup();
	}

	/**
	 * Perform some setup steps before processing data. These are steps that can
	 * be performed just once because they are constant between CircleMaps.
	 */
	public void setup() {

		this.samplesArrayList = new ArrayList<String>(
				getAllSampleNames(this.onlyCompleteSampleNames));

		setColors(Color.BLUE, Color.WHITE, Color.RED, Color.LIGHT_GRAY,
				Color.GRAY);

		setSampleGroupingColorKeys();

		// sample sorting will not change between CircleMaps
		sortSamples(this.orderFeature, this.samplesArrayList);

		findMinMaxFromMatrixMetaData();

		// sample group rings will not change between CircleMaps
		this.sampleGroupingRingData = getSampleGroupingRingData(this.samplesArrayList);

		if (this.ringMergeSwitch) {
			// merged ring memberships
			this.ringMergeInfoJA = findRingMergings(this.ringsList);
		}

	}

	/**
	 * Determine the TCGA datatype of the specified ring. Assumes the name fits
	 * this format: "TCGA_cancer_datatype".
	 * 
	 * @param ringName
	 *            "TCGA_cancer_datatype"
	 * @return null if ring is clinical or doesn't start "TCGA_".
	 */
	private static String determineTcgaDatatype(String ringName) {
		String datatype = null;

		if (datasetIsClinical(ringName)) {
			return datatype;
		}

		if (ringName.startsWith("TCGA_")) {
			// "TCGA_cancer_datatype" data
			String[] nameFields = ringName.split("_", 3);
			datatype = nameFields[2];
		}

		return datatype;
	}

	/**
	 * Attempt to merge rings based on common TCGA datatype. Unmerged rings will
	 * be mapped to an ArrayList with one member, itself.
	 * 
	 * @param individualRingList
	 * @return Ring mergings in JSONArray of JSONOBjects with keys
	 *         [name,members].
	 */
	private static JSONArray findRingMergings(
			ArrayList<String> individualRingList) {
		HashMap<String, ArrayList<String>> ringMembers = new HashMap<String, ArrayList<String>>();

		// for determining order of drawing
		ArrayList<String> keyOrder = new ArrayList<String>();

		for (String individualRing : individualRingList) {
			String tcgaDatatype = determineTcgaDatatype(individualRing);

			if (tcgaDatatype == null) {
				// unmerged ring
				tcgaDatatype = individualRing;
			}

			if (!ringMembers.containsKey(tcgaDatatype)) {
				ringMembers.put(tcgaDatatype, new ArrayList<String>());

				keyOrder.add(tcgaDatatype);
			}

			ringMembers.get(tcgaDatatype).add(individualRing);
		}

		// rings to be drawn from the outermost to innermost
		Collections.reverse(keyOrder);

		// use JSON bc can use a JSONArray as a value
		JSONArray mergedRingsInfoJA = new JSONArray();
		try {

			for (String key : keyOrder) {
				JSONObject ringInfoJO = new JSONObject();

				mergedRingsInfoJA.put(ringInfoJO);

				ringInfoJO.put("name", key);

				JSONArray memberRingsJA = new JSONArray(ringMembers.get(key));

				ringInfoJO.put("members", memberRingsJA);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return mergedRingsInfoJA;
	}

	/**
	 * Draw circleMaps for each of the loaded features.
	 * 
	 * @return Maps feature name to URL.
	 */
	public HashMap<String, String> drawCircleMaps() {

		HashMap<String, String> circleMapURLs = new HashMap<String, String>();

		for (String feature : this.featuresHashSet) {

			if (feature.isEmpty()) {
				continue;
			}

			String url = drawCircleMapFile(feature, false);

			circleMapURLs.put(feature, url);
		}

		return circleMapURLs;
	}

	/**
	 * Plot one CircleMap image and write it to a PNG file.
	 * 
	 * @param feature
	 * @param plotSvg
	 *            TODO
	 * @return name of the image file.
	 */
	public String drawCircleMapFile(String feature, boolean plotSvg) {

		// check if circle plot image already exists
		String imageFileName = computeHashedFileName(this.baseFilename, feature);

		String fileSuffix = "PNG";
		if (plotSvg) {
			fileSuffix = "SVG";
		}

		// return the imageFileName if it already exists
		if ((this.drawAll == false)
				&& (checkIfFileExists(imageFileName) == true)) {
			return imageFileName + "." + fileSuffix;
		}

		// create CircleMapData object
		CircleMapData circleMapData;

		if (this.ringMergeSwitch) {
			// merged rings mode
			circleMapData = getMergedRingCircleMapData(feature);

		} else {

			// ring drawing order reversed (outside to inside)
			ArrayList<String> ringDrawingOrder = new ArrayList<String>(
					this.ringsList);
			Collections.reverse(ringDrawingOrder);

			circleMapData = new CircleMapData(feature);

			for (String ringName : ringDrawingOrder) {

				if (datasetIsClinical(ringName)) {
					// sample grouping ring
					circleMapData.addRingData(this.sampleGroupingRingData
							.get(ringName));
				} else {
					// sample score ring
					if (this.sampleGroupSummarySwitch) {
						circleMapData.addRingData(computeSummaryRingData(
								feature, ringName, this.sortingRing));
					} else {
						circleMapData.addRingData(computeRingData(feature,
								ringName));
					}
				}
			}
		}

		if (plotSvg) {
			// plot the image
			SVGGraphics2D svgG2D = this.plotter
					.getCircleMapSvgG2D(circleMapData);

			// write image to file
			writeSVG(svgG2D, imageFileName);

			return imageFileName + "." + fileSuffix;
		} else {
			// plot the image
			BufferedImage bi = this.plotter.getCircleMapImage(circleMapData);

			// write image to file
			writePNG(bi, imageFileName);

			return imageFileName + "." + fileSuffix;
		}

	}

	/**
	 * Get a CircleMapData for this feature in merged ring CircleMap mode. That
	 * is, rings for different TCGA cancer type but similar TCGA datatypes will
	 * be merged into one ring.
	 * 
	 * @param feature
	 * @return
	 */
	private CircleMapData getMergedRingCircleMapData(String feature) {

		CircleMapData circleData = new CircleMapData(feature);

		if (this.ringMergeInfoJA == null) {
			return circleData;
		}

		// add on one ring at a time
		for (int i = 0; i < this.ringMergeInfoJA.length(); i++) {
			String name;
			ArrayList<String> ringMembersList = new ArrayList<String>();
			try {
				JSONObject mergeRingInfoJO = this.ringMergeInfoJA
						.getJSONObject(i);
				name = mergeRingInfoJO.getString("name");
				JSONArray memberRingsJA = mergeRingInfoJO
						.getJSONArray("members");

				for (int j = 0; j < memberRingsJA.length(); j++) {
					ringMembersList.add(memberRingsJA.getString(j));
				}

			} catch (JSONException e) {
				e.printStackTrace();
				name = null;
			}

			if (datasetIsClinical(name)) {
				circleData.addRingData(this.sampleGroupingRingData.get(name));
			} else {
				circleData.addRingData(computeMergedRingData(feature,
						ringMembersList));
			}
		}

		return circleData;
	}

	/**
	 * Compute RingData object for merged rings mode. This attempts to merge
	 * multiple rings into one. Hopefully, samples occur in only one ring at a
	 * time.
	 * 
	 * @param feature
	 * @param memberRingsList
	 *            list of ring names
	 * @return
	 */
	private RingData computeMergedRingData(String feature,
			ArrayList<String> memberRingsList) {
		RingData ringData = new CircleMapData(feature).new RingData();

		// ADD ARCDATA
		double startAngle = new Double(0);
		double sweepAngle = new Double(360 / new Double(
				this.samplesArrayList.size()));

		// get a score to represent the sample
		for (String sampleName : this.samplesArrayList) {

			ArrayList<Double> scoresList = new ArrayList<Double>();

			double minScore = Double.NaN;
			double maxScore = Double.NaN;

			for (String ringName : memberRingsList) {

				double sampleScoreDbl = Double.NaN;

				// check member rings for scores
				HashMap<String, String> sampleScoresMap = this.scoresMatrices
						.get(ringName).get(feature);

				if (sampleScoresMap == null) {
					// dataset does not contain data for the feature
					continue;
				}

				// get metadata for dataset
				minScore = this.scoreMatricesMinMax.get(ringName).get("min");
				maxScore = this.scoreMatricesMinMax.get(ringName).get("max");

				// find the color
				String sampleScoreStr = sampleScoresMap.get(sampleName);

				if (sampleScoreStr == null) {

					sampleScoreDbl = Double.NaN;

				} else {
					try {
						sampleScoreDbl = Double.parseDouble(sampleScoreStr);

						scoresList.add(sampleScoreDbl);

					} catch (NumberFormatException nfe) {
						sampleScoreDbl = Double.NaN;
					}
				}
			}

			// set color
			Color color;

			// make sure only one score found
			if (scoresList.size() == 0) {
				color = this.naColor;
			} else if (scoresList.size() > 1) {
				color = this.errorColor;
			} else {
				color = this.getColor(scoresList.get(0), minScore, maxScore);
			}

			// add arc
			ringData.addArc(startAngle, sweepAngle, color);

			// increment startAngle
			startAngle += sweepAngle;

		}

		//

		return ringData;
	}

	/**
	 * Compute RingData object for a sample score SUMMARY ring.
	 * 
	 * @param feature
	 * @param ringName
	 * @param summaryRingName
	 *            Ring name to get summary groupings from.
	 * @return
	 */
	private RingData computeSummaryRingData(String feature, String ringName,
			String summaryRingName) {
		RingData ringData = new CircleMapData(feature).new RingData();

		if (!this.scoresMatrices.get(ringName).containsKey(feature)) {
			// add an "NA" ring if feature not found
			ringData.addArc(360, 360, naColor);
			return ringData;
		}

		// get score matrix
		HashMap<String, String> sampleScoresMap = this.scoresMatrices.get(
				ringName).get(feature);

		// get metadata
		double minScore = this.scoreMatricesMinMax.get(ringName).get("min");
		double maxScore = this.scoreMatricesMinMax.get(ringName).get("max");

		// sum_scores and num_samples for each grouping bin. group name ->
		// [sum_scores,num_samples]
		HashMap<String, HashMap<String, Double>> summaryDataHash = new HashMap<String, HashMap<String, Double>>();

		if (datasetIsClinical(summaryRingName)) {
			HashSet<String> groupingNames = new HashSet<String>(
					this.groupingColorKeys.get(summaryRingName).keySet());

			// for samples that have no label in the grouping data
			groupingNames.add("no_label");

			for (String groupingName : groupingNames) {
				HashMap<String, Double> dataHash = new HashMap<String, Double>();
				dataHash.put("sum_scores", new Double(0));
				dataHash.put("num_samples", new Double(0));

				summaryDataHash.put(groupingName, dataHash);
			}

		} else {
			HashMap<String, Double> dataHash = new HashMap<String, Double>();
			dataHash.put("sum_scores", new Double(0));
			dataHash.put("num_samples", new Double(0));

			// all samples in "one_group"
			summaryDataHash.put("one_group", dataHash);
		}

		// compute sum_scores and num_samples according to grouping
		for (String sampleName : this.samplesArrayList) {

			// get sample score for this dataset
			double sampleScoreDbl;

			String sampleScoreStr = sampleScoresMap.get(sampleName);

			if (sampleScoreStr == null) {
				// no contribution to any group
				sampleScoreDbl = Double.NaN;
				continue;
			} else {
				try {
					sampleScoreDbl = Double.parseDouble(sampleScoreStr);
				} catch (NumberFormatException nfe) {
					// no contribution to any group
					sampleScoreDbl = Double.NaN;
					continue;
				}
			}

			// get group
			String sampleGroup;
			if (datasetIsClinical(summaryRingName)) {
				sampleGroup = this.getClinicalFeatureValue(summaryRingName,
						sampleName);
				if (sampleGroup == null) {
					sampleGroup = "no_label";
				}
			} else {
				sampleGroup = "one_group";
			}

			sampleGroup = sampleGroup.toUpperCase();

			// update summary data
			double newScore = sampleScoreDbl
					+ summaryDataHash.get(sampleGroup).get("sum_scores");
			double newCount = 1 + summaryDataHash.get(sampleGroup).get(
					"num_samples");

			summaryDataHash.get(sampleGroup).put("sum_scores", newScore);
			summaryDataHash.get(sampleGroup).put("num_samples", newCount);
		}

		// compute group averages
		for (String groupName : summaryDataHash.keySet()) {
			double sum_scores = summaryDataHash.get(groupName)
					.get("sum_scores");
			double num_samples = summaryDataHash.get(groupName).get(
					"num_samples");

			if (num_samples == 0) {
				summaryDataHash.get(groupName).put("ave", Double.NaN);
			} else {
				double average = sum_scores / num_samples;
				summaryDataHash.get(groupName).put("ave", average);
			}
		}

		// ADD ARCDATA
		double startAngle = new Double(0);
		double sweepAngle = new Double(360 / new Double(
				this.samplesArrayList.size()));

		double sampleScoreDbl;
		Color color;

		for (String sampleName : this.samplesArrayList) {

			String sampleGroup;

			if (datasetIsClinical(summaryRingName)) {
				sampleGroup = this.getClinicalFeatureValue(summaryRingName,
						sampleName);
				if (sampleGroup == null) {
					sampleGroup = "no_label";
				}
			} else {
				sampleGroup = "one_group";
			}

			sampleGroup = sampleGroup.toUpperCase();

			// find the color
			sampleScoreDbl = summaryDataHash.get(sampleGroup).get("ave");
			color = this.getColor(sampleScoreDbl, minScore, maxScore);

			// add arc
			ringData.addArc(startAngle, sweepAngle, color);

			// increment startAngle
			startAngle += sweepAngle;
		}

		return ringData;
	}

	/**
	 * Compute RingData object for a sample score ring.
	 * 
	 * @param feature
	 * @param ringName
	 * @return
	 */
	private RingData computeRingData(String feature, String ringName) {
		RingData ringData = new CircleMapData(feature).new RingData();

		if (ringName.endsWith("_uploaded")) {
			ringName = ringName.replaceFirst("_uploaded", "");
		}

		if (!this.scoresMatrices.get(ringName).containsKey(feature)) {
			// add an "NA" ring if feature not found
			ringData.addArc(360, 360, naColor);
			return ringData;
		}

		// get score matrix
		HashMap<String, String> sampleScoresMap = this.scoresMatrices.get(
				ringName).get(feature);

		// get metadata
		double minScore = this.scoreMatricesMinMax.get(ringName).get("min");
		double maxScore = this.scoreMatricesMinMax.get(ringName).get("max");

		// ADD ARCDATA
		double startAngle = new Double(0);
		double sweepAngle = new Double(360 / new Double(
				this.samplesArrayList.size()));

		double sampleScoreDbl;
		Color color;

		for (String sampleName : this.samplesArrayList) {

			// find the color
			String sampleScoreStr = sampleScoresMap.get(sampleName);

			if (sampleScoreStr == null) {

				color = this.naColor;

			} else {
				try {
					sampleScoreDbl = Double.parseDouble(sampleScoreStr);

					color = this.getColor(sampleScoreDbl, minScore, maxScore);
				} catch (NumberFormatException nfe) {
					color = this.errorColor;
				}
			}

			// add arc
			ringData.addArc(startAngle, sweepAngle, color);

			// increment startAngle
			startAngle += sweepAngle;
		}

		return ringData;
	}

	/**
	 * Get all the sample names in the loaded datasets.
	 * 
	 * @param onlySamplesWithCompleteData
	 *            If true, return just the sample names that are found in all
	 *            loaded datasets.
	 * @return
	 */
	private Set<String> getAllSampleNames(boolean onlySamplesWithCompleteData) {

		Set<String> samplesNamesToReturn = new HashSet<String>();

		// scored matrices
		for (String dataset : scoreMatrixMetadata.keySet()) {
			samplesNamesToReturn
					.addAll(getSampleNamesFromScoresMatrix(dataset));
		}

		// clinical matrices
		for (String dataset : clinicalData.keySet()) {
			samplesNamesToReturn
					.addAll(getSampleNamesFromClinicalMatrix(dataset));
		}

		// get intersection of all dataset sample names
		if (onlySamplesWithCompleteData) {
			// score matrices
			for (String dataset : scoreMatrixMetadata.keySet()) {
				Set<String> datasetSampleNames = getSampleNamesFromScoresMatrix(dataset);

				samplesNamesToReturn.retainAll(datasetSampleNames);
			}

			// clinical matrices
			for (String ringName : ringsList) {
				if (!datasetIsClinical(ringName)) {
					continue;
				}
				for (String dataset : clinicalData.keySet()) {
					Set<String> datasetSampleNames = getSampleNamesFromClinicalMatrix(dataset);

					// remove samples where the clinical data is Null or ""
					Set<String> namesToRemove = new HashSet<String>();
					for (String sampleName : datasetSampleNames) {
						String value = getClinicalFeatureValue(ringName,
								sampleName);
						if (value == null || value.equals("")) {
							namesToRemove.add(sampleName);
						}
					}
					datasetSampleNames.removeAll(namesToRemove);

					// keep only the sample names that occur in BOTH lists
					samplesNamesToReturn.retainAll(datasetSampleNames);
				}
			}

		}

		return samplesNamesToReturn;
	}

	/**
	 * Get sample names from the specified clinical matrix.
	 * 
	 * @param matrixName
	 * @return If matrix name is not found, then returns an empty set.
	 */
	private Set<String> getSampleNamesFromClinicalMatrix(String matrixName) {

		Set<String> sampleNamesSet = new HashSet<String>();

		if (clinicalData.containsKey(matrixName)) {

			HashMap<String, HashMap<String, String>> tableHashMap = clinicalData
					.get(matrixName);
			for (String featureName : tableHashMap.keySet()) {
				HashMap<String, String> featureValuesHashMap = tableHashMap
						.get(featureName);

				sampleNamesSet.addAll(featureValuesHashMap.keySet());

			}

		} else {
			// matrix not found
		}

		return sampleNamesSet;
	}

	/**
	 * Get sample names from the specified scores matrix.
	 * 
	 * @param matrixName
	 * @return If matrix name is not found, then returns an empty set.
	 */
	private Set<String> getSampleNamesFromScoresMatrix(String matrixName) {
		Set<String> sampleNamesSet = new HashSet<String>();

		if (scoreMatrixMetadata.containsKey(matrixName)) {

			HashMap<String, String> datasetMetadata = scoreMatrixMetadata
					.get(matrixName);

			// turn a String into an array into List into HashSet
			sampleNamesSet.addAll(new HashSet<String>(Arrays
					.asList(datasetMetadata.get("sampleNames").split(","))));
		} else {
			// matrix not found
		}

		return sampleNamesSet;
	}

	/**
	 * Set the sample grouping color key in groupingColorKeys global HashMap.
	 */
	private void setSampleGroupingColorKeys() {
		groupingColorKeys.clear();

		for (String requestedRingName : ringsList) {
			if (!datasetIsClinical(requestedRingName)) {
				continue;
			}

			HashMap<String, Color> assignedColors = new HashMap<String, Color>();
			groupingColorKeys.put(requestedRingName, assignedColors);

			String[] nameFields = parseClinicalDatasetName(requestedRingName);

			String tableName = nameFields[0];
			String featureName = nameFields[1];

			ArrayList<String> groupingNamesList = new ArrayList<String>(
					new HashSet<String>(this.clinicalData.get(tableName)
							.get(featureName).values()));

			Collections.sort(groupingNamesList);

			for (String groupingName : groupingNamesList) {
				Color color;

				if (groupingName.equalsIgnoreCase("")
						|| groupingName.equalsIgnoreCase("na")) {
					color = this.naColor;
				} else {
					int mappings = assignedColors.size();

					for (String key : assignedColors.keySet()) {
						if (key.equalsIgnoreCase("")) {
							mappings--;
						}
						if (key.equalsIgnoreCase("na")) {
							mappings--;
						}
					}

					color = selectColor(mappings);
				}

				assignedColors.put(groupingName.toUpperCase(), color);
			}
		}
	}

	/**
	 * Get all RingData for sample group rings.
	 * 
	 * @param sampleList
	 * @return Map sampleGroupingRingName to its RingData
	 */
	private HashMap<String, RingData> getSampleGroupingRingData(
			ArrayList<String> sampleList) {

		/**
		 * map ring names to RingData.
		 */
		HashMap<String, RingData> sampleGroupRings = new HashMap<String, RingData>();

		CircleMapData circleMapData = new CircleMapData("groupRings");

		Double sweepAngle = new Double(360 / new Double(sampleList.size()));

		for (String requestedRingName : ringsList) {
			if (!datasetIsClinical(requestedRingName)) {
				continue;
			}

			RingData ringData = circleMapData.new RingData();
			sampleGroupRings.put(requestedRingName, ringData);

			// start at 0
			double startAngle = new Double(0);

			// add arcData
			for (String sample : sampleList) {

				Color color;

				try {

					String featureValue = getClinicalFeatureValue(
							requestedRingName, sample).toUpperCase();

					color = groupingColorKeys.get(requestedRingName).get(
							featureValue);

				} catch (NullPointerException e) {
					// sample not found in dataset
					color = this.nlColor;
				}

				// add arc
				ringData.addArc(startAngle, sweepAngle, color);

				// increment startAngle for next arc
				startAngle += sweepAngle;
			}
		}

		return sampleGroupRings;
	}

	/**
	 * Get the current sample group ring color keys in the form of a JSONObject.
	 * ring name -> array of feature val -> [r,g,b,a] -> int value
	 * 
	 * @return
	 */
	public JSONObject getSampleGroupingColorKeys() {
		JSONObject resultJO = new JSONObject();

		for (String ringName : groupingColorKeys.keySet()) {
			HashMap<String, Color> ringColorKey = groupingColorKeys
					.get(ringName);

			// ordered list of groupName color info
			JSONArray ringJA = new JSONArray();

			try {
				resultJO.put(ringName, ringJA);

				ArrayList<String> featureValList = new ArrayList<String>(
						ringColorKey.keySet());
				Collections.sort(featureValList);

				for (String featureVal : featureValList) {

					Color color = ringColorKey.get(featureVal);

					// color information
					JSONObject colorJO = new JSONObject();
					ringJA.put(colorJO);

					colorJO.put("groupName", featureVal);
					colorJO.put("r", color.getRed());
					colorJO.put("g", color.getGreen());
					colorJO.put("b", color.getBlue());
					colorJO.put("a", color.getAlpha());
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return resultJO;
	}

	/**
	 * Get the clinical feature value for the feature in the specified ring. No
	 * feature (gene) is needed because all genes for a sample will share the
	 * same clinical feature value.
	 * 
	 * @param ringName
	 *            The clinical feature table.
	 * @param sampleName
	 *            The row in the clinical feature table. (Corresponds to a
	 *            patient or sample.)
	 * @return Result is trimmed, with the original casing. May return null if
	 *         there is no mapping.
	 */
	private String getClinicalFeatureValue(String ringName, String sampleName) {
		String[] nameFields = parseClinicalDatasetName(ringName);

		String tableName = nameFields[0];
		String featureName = nameFields[1];

		try {
			return this.clinicalData.get(tableName).get(featureName)
					.get(sampleName).trim();
		} catch (NullPointerException e) {
			return null;
		}

	}

	/**
	 * From a clinical data request (which has table and field), get the table
	 * and clinical field separately.
	 * 
	 * @param name
	 * @return First field is the table name. Second field is the field name.
	 */
	public static String[] parseClinicalDatasetName(String name) {

		StringBuffer sb = new StringBuffer(name);

		// remove "_clinical"
		sb.replace(sb.lastIndexOf("_clinical"), sb.length(), "");

		// split on "__"
		String[] fields = sb.toString().split("__", 2);

		return fields;
	}

	/**
	 * If "_clinical" is at the end of the datasetName and also contains
	 * delimiter "__", then return true (it is a clinical dataset).
	 * 
	 * @param datasetName
	 * @return
	 */
	public static boolean datasetIsClinical(String datasetName) {
		Boolean result = false;

		if (datasetName.toUpperCase().endsWith("_CLINICAL")
				&& datasetName.contains("__")) {
			return true;
		}

		return result;
	}

	/**
	 * Compute a String that represents this CirclePlotter based on the options
	 * passed in.
	 * 
	 * @return
	 */
	private String computeBaseFilename() {
		StringBuffer sb = new StringBuffer();

		sb.append(ringsList.toString());
		sb.append(sortingRing);
		sb.append(scoresMatrices.toString());
		sb.append(clinicalData.toString());
		sb.append(featuresHashSet);
		sb.append(orderFeature);
		sb.append(scoreMatrixMetadata);
		sb.append(sampleGroupSummarySwitch + "");
		sb.append(ringMergeSwitch + "");
		sb.append(onlyCompleteSampleNames + "");

		return sb.toString();
	}

	/**
	 * Get a unique String that is generated from the options passed in to this
	 * CirclePlotter.
	 * 
	 * @param baseFilename
	 * @param feature
	 * @return
	 */
	private static String computeHashedFileName(String baseFilename,
			String feature) {
		return DatabaseService.getMD5Hash(baseFilename + feature);
	}

	/**
	 * Get a Color for a score. There are actually 2 color gradients used. First
	 * is from minScore to 0. Second is from 0 to maxScore.
	 * 
	 * @param score
	 * @param minScore
	 *            should be some negative value
	 * @param maxScore
	 *            should be some positive value
	 * @return naColor for NaN, neg_inf, and pos_inf
	 */
	private Color getColor(double score, double minScore, double maxScore) {
		int rVal = 0;
		int gVal = 0;
		int bVal = 0;

		if (score == Double.NaN || score == Double.NEGATIVE_INFINITY
				|| score == Double.POSITIVE_INFINITY) {
			return this.naColor;
		} else if (score > 0) {
			if (score > maxScore) {
				score = maxScore;
			}

			double val = score / maxScore;

			rVal = (int) linearInterpolation(val, this.zeroColor.getRed(),
					this.maxColor.getRed());
			gVal = (int) linearInterpolation(val, this.zeroColor.getGreen(),
					this.maxColor.getGreen());
			bVal = (int) linearInterpolation(val, this.zeroColor.getBlue(),
					this.maxColor.getBlue());
		} else if (score < 0) {
			if (score < minScore) {
				score = minScore;
			}

			double val = Math.abs(score / minScore);

			rVal = (int) linearInterpolation(val, this.zeroColor.getRed(),
					this.minColor.getRed());
			gVal = (int) linearInterpolation(val, this.zeroColor.getGreen(),
					this.minColor.getGreen());
			bVal = (int) linearInterpolation(val, this.zeroColor.getBlue(),
					this.minColor.getBlue());
		} else if (score == 0) {
			rVal = this.zeroColor.getRed();
			gVal = this.zeroColor.getBlue();
			bVal = this.zeroColor.getGreen();
		}

		return new Color(rVal, gVal, bVal);
	}

	/**
	 * Set the colors for CirclePlotter.
	 * 
	 * @param minColor
	 * @param zeroColor
	 * @param maxColor
	 * @param naColor
	 * @param nlColor
	 */
	private void setColors(Color minColor, Color zeroColor, Color maxColor,
			Color naColor, Color nlColor) {
		this.minColor = minColor;
		this.maxColor = maxColor;
		this.zeroColor = zeroColor;
		this.naColor = naColor;
		this.nlColor = nlColor;
	}

	/**
	 * Get a color from the colorList.
	 * 
	 * @param index
	 * @return
	 */
	private static Color selectColor(int index) {

		int mod = index % colorList.size();

		Color color = colorList.get(mod);

		return color;
	}

	/**
	 * Interpolate the value along a line between two points in one dimension.
	 * 
	 * @param percent
	 * @param minVal
	 * @param maxVal
	 * @return
	 */
	public static double linearInterpolation(final double percent,
			final double minVal, final double maxVal) {
		return ((maxVal - minVal) * percent) + minVal;
	}

	/**
	 * Check if an image file exists.
	 * 
	 * @param fileName
	 * @return
	 */
	private boolean checkIfFileExists(String fileName) {
		boolean result = false;

		String pathName = servletContext
				.getRealPath(IMAGE_FILE_OUTPUT_DIRECTORY + fileName + ".PNG");

		try {
			File file = new File(pathName);
			result = file.exists();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return result;
		// return false;
	}

	private void writeSVG(SVGGraphics2D svgG2D, String fileName) {
		String sanitizedName = DatabaseService.sanitizeString(fileName);
		if (!sanitizedName.equals(fileName)) {
			return;
		}

		String pathName = servletContext
				.getRealPath(IMAGE_FILE_OUTPUT_DIRECTORY + sanitizedName
						+ ".SVG");

		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
		FileWriter out;
		try {
			out = new FileWriter(new File(pathName));
			svgG2D.stream(out, useCSS);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Write BufferedImage to PNG file in pre-determined path.
	 * 
	 * @param bi
	 * @param fileName
	 * @return
	 */
	private boolean writePNG(BufferedImage bi, String fileName) {
		boolean result = false;

		String sanitizedName = DatabaseService.sanitizeString(fileName);
		if (!sanitizedName.equals(fileName)) {
			return result;
		}

		String pathName = servletContext
				.getRealPath(IMAGE_FILE_OUTPUT_DIRECTORY + sanitizedName
						+ ".PNG");

		try {
			result = ImageIO.write(bi, "PNG", new File(pathName));
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}

		return result;
	}

	/**
	 * get min/max values for each dataset using matrixMetaData.
	 * 
	 * @param matrixMetaDataHashMap
	 * @return
	 */
	private void findMinMaxFromMatrixMetaData() {
		this.scoreMatricesMinMax = new HashMap<String, HashMap<String, Double>>();

		for (String datasetName : this.scoreMatrixMetadata.keySet()) {

			HashMap<String, Double> minMaxHashMap = new HashMap<String, Double>();
			this.scoreMatricesMinMax.put(datasetName, minMaxHashMap);

			double min;
			double max;

			if (!this.scoreMatricesMinMax.containsKey(datasetName)) {
				// no metadata for this dataset, use default "0"
				min = -1 * Double.MIN_VALUE;
				max = Double.MIN_VALUE;
			} else {
				HashMap<String, String> metaData = this.scoreMatrixMetadata
						.get(datasetName);

				min = Double.parseDouble(metaData.get("cohortMin"));
				max = Double.parseDouble(metaData.get("cohortMax"));

			}

			if (min == 0) {
				min = -1 * Double.MIN_VALUE;
			}

			if (max == 0) {
				max = Double.MIN_VALUE;
			}

			minMaxHashMap.put("min", min);
			minMaxHashMap.put("max", max);
		}
	}

	/**
	 * Sort the sample names based on sample score for the specified feature.
	 * 
	 * @param orderFeature
	 * @param sampleNamesList
	 *            list of all sample names in all of the requested rings.
	 */
	private void sortSamples(String orderFeature,
			ArrayList<String> sampleNamesList) {

		if (orderFeature == null) {
			return;
		}

		// Collections.sort(sampleNamesList, Collections
		// .reverseOrder(new SampleScoreComparator(orderFeature)));

		Collections.sort(sampleNamesList, new SampleScoreComparator(
				orderFeature));
	}

	/**
	 * Get the sample list in its current ordering.
	 * 
	 * @return
	 */
	public ArrayList<String> getSamplesList() {
		return this.samplesArrayList;
	}

	/**
	 * Get the sorting ring dataset.
	 * 
	 * @return
	 */
	public String getSortingRing() {
		return this.sortingRing;
	}

	/**
	 * Get the ring display order.
	 * 
	 * @param innerToOuter
	 *            If true, rings are returned from innermost to outermost. If
	 *            false, rings are returned from outermost to innermost.
	 * @return
	 */
	public ArrayList<String> getRingDisplayOrder(boolean innerToOuter) {
		ArrayList<String> ringOrder = new ArrayList<String>(this.ringsList);

		if (!innerToOuter) {
			Collections.reverse(ringOrder);
		}

		return ringOrder;
	}

	/**
	 * Comparator for sorting sample names based on the sample scores for a
	 * feature.
	 * 
	 * @author chrisw
	 * 
	 */
	private class SampleScoreComparator implements Comparator<String> {

		/**
		 * feature to use for ordering sample IDs
		 */
		private final String feature;

		/**
		 * sorting priority of rings
		 */
		private ArrayList<String> ringSortPriorityList;

		/**
		 * Constructor for comparator.
		 * 
		 * @param orderFeature
		 *            feature whose sample scores will be used for comparison
		 */
		SampleScoreComparator(String orderFeature) {
			this.feature = orderFeature;

			// bump the sorting ring to the top of the list
			this.ringSortPriorityList = new ArrayList<String>();
			ringSortPriorityList.add(sortingRing);
			ringSortPriorityList.addAll(ringsList);
			ringSortPriorityList.remove(ringSortPriorityList
					.lastIndexOf(sortingRing));
		}

		@Override
		public int compare(String sampleName1, String sampleName2) {

			for (String ringName : this.ringSortPriorityList) {
				if (datasetIsClinical(ringName)) {

					// get sample values
					String sampleVal1 = getClinicalFeatureValue(ringName,
							sampleName1);
					String sampleVal2 = getClinicalFeatureValue(ringName,
							sampleName2);

					// compare values
					if (sampleVal1 == null && sampleVal2 == null) {
						// tie due to no data - maybe break tie in next matrix
						continue;
					} else if (sampleVal1 != null && sampleVal2 == null) {
						return -1;
					} else if (sampleVal1 == null && sampleVal2 != null) {
						return 1;
					} else {
						int compare = sampleVal1
								.compareToIgnoreCase(sampleVal2);
						if (compare == 0) {
							continue;
						} else {
							// if (compare < 0) {
							// System.out.println(sampleVal1 + " lt "
							// + sampleVal2);
							// } else {
							// System.out.println(sampleVal1 + " gt "
							// + sampleVal2);
							// }
							return compare;
						}
					}

				} else if (!sampleGroupSummarySwitch) {
					// for sample group summary mode, don't sort score matrices

					if (ringName.endsWith("_uploaded")) {
						ringName = ringName.replaceFirst("_uploaded", "");
					}

					// get values
					String scoreStr1 = getMatrixScoreString(ringName,
							sampleName1, feature);
					String scoreStr2 = getMatrixScoreString(ringName,
							sampleName2, feature);

					// convert String to number for comparison
					double score1 = -1 * Double.MAX_VALUE;
					double score2 = -1 * Double.MAX_VALUE;

					try {
						score1 = Double.parseDouble(scoreStr1);
					} catch (NumberFormatException e) {
						// could not convert to a number
					} catch (NullPointerException e) {
						// null
					}

					try {
						score2 = Double.parseDouble(scoreStr2);
					} catch (NumberFormatException e) {
						// could not convert to a number
					} catch (NullPointerException e) {
						// null
					}

					// compare scores
					if (score1 == score2) {
						// tie due to numerical tie or both NaN
						continue;
					} else if (score1 > score2) {
						// System.out.println(score1 + " > " + score2);
						return 1;
					} else if (score1 < score2) {
						// System.out.println(score1 + " < " + score2);
						return -1;
					}

				}
			}

			// default comparison result is a tie
			return 0;
		}
	}

	/**
	 * Get a score for the specified data in the form of a String.
	 * 
	 * @param matrixName
	 *            The dataset.
	 * @param sampleName
	 *            The sample column in the dataset.
	 * @param feature
	 *            The gene row in the dataset.
	 * @return May return null if no mapping exists. Also, the returned String
	 *         may or may not be parsed as a numerical value.
	 */
	private String getMatrixScoreString(String matrixName, String sampleName,
			String feature) {

		try {
			return this.scoresMatrices.get(matrixName).get(feature)
					.get(sampleName).trim();
		} catch (NullPointerException e) {
			return null;
		}

	}
}
