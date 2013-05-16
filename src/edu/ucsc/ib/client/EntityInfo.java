package edu.ucsc.ib.client;

import java.util.Comparator;
import java.util.HashMap;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import edu.ucsc.ib.client.viewcontrols.SearchSpaceControl;
import edu.ucsc.ib.drawpanel.client.Shape;

/**
 * Super class to store data that applies to some entity.
 */
public class EntityInfo {
	protected String systematicName;
	private String commonName;
	private String description;

	/**
	 * Is it for human, or worm, or mouse, ...
	 */
	private String systemSpace;

	/**
	 * Is it a gene, or protein, or ...
	 */
	private String biodeSpace;

	// - localization (cellular location: nucleolus, nucleus, nuclear envelope,
	// cytoplasm, plasma membrane, ER, golgi, vacuole, etc)
	private String localization;

	// - activated (activation state)
	private String activationState;

	// - isoform
	private String isoform;

	// - mutation
	private String mutation;

	private double xPosition;
	private double yPosition;
	private String color;
	private int shapeCode;

	protected HashMap<String, Double> scores = new HashMap<String, Double>();

	/**
	 * Comparator for comparing common name of EntityInfo objects. Does a
	 * case-insensitive, lower-case comparison.
	 */
	public static final Comparator<EntityInfo> commonNameComparator = new Comparator<EntityInfo>() {
		public int compare(EntityInfo ei1, EntityInfo ei2) {
			return ei1.getCommonName().toLowerCase()
					.compareTo(ei2.getCommonName().toLowerCase());
		}
	};

	// TODO //////////////////////////////////////////

	/**
	 * Constructor does nothing.
	 */
	public EntityInfo() {
	}

	/**
	 * Sets the specified systematicName.
	 * 
	 * @param systematicName
	 */
	public EntityInfo(String systematicName) {
		this.systematicName = systematicName;

		// set attributes to default values.

		this.setPosition(0, 0);
		this.setColor("");
		this.setShapeCode(-1);

		this.setCommonName("none");
		this.setDescription("none");
		this.setSystemSpace("none");
		this.setBiodeSpace("none");

		this.setActivationState("");
		this.setIsoform("");
		this.setLocalization("");
		this.setMutation("");

	}

	/**
	 * Child classes should override this.
	 * 
	 * @param jo
	 */
	public EntityInfo(JSONObject jo) {
		this();

		JSONValue jv;

		jv = jo.get("systematicName");
		if (jv != null) {
			this.systematicName = (jv.isString().stringValue());

			// set some initial values that may be replaced.
			this.setCommonName("none");
			this.setDescription("none");
			this.setSystemSpace("none");
			this.setBiodeSpace("none");

			this.setPosition(0, 0);
			this.setColor("");
			this.setShapeCode(-1);
		}

		jv = jo.get("commonName");
		if (jv != null) {
			this.setCommonName(jv.isString().stringValue());
		}

		jv = jo.get("systemSpace");
		if (jv != null) {
			this.setSystemSpace(jv.isString().stringValue());
		}

		jv = jo.get("biodeSpace");
		if (jv != null) {
			this.setBiodeSpace(jv.isString().stringValue());
		}

		jv = jo.get("description");
		if (jv != null) {
			this.setDescription(jv.isString().stringValue());
		}

		jv = jo.get("xPosition");
		JSONValue jv2 = jo.get("yPosition");

		if (jv != null && jv2 != null) {
			this.setPosition(jv.isNumber().doubleValue(), jv2.isNumber()
					.doubleValue());
		}

		jv = jo.get("color");
		if (jv != null) {
			this.setColor(jv.isString().stringValue());
		}

		jv = jo.get("shape");
		if (jv != null) {
			this.setShapeCode(new Double(jv.isNumber().doubleValue())
					.intValue());
		}

		jv = jo.get("activationState");
		if (jv != null) {
			this.setActivationState(jv.isString().stringValue());
		}

		jv = jo.get("isoform");
		if (jv != null) {
			this.setIsoform(jv.isString().stringValue());
		}

		jv = jo.get("localization");
		if (jv != null) {
			this.setLocalization(jv.isString().stringValue());
		}

		jv = jo.get("mutation");
		if (jv != null) {
			this.setMutation(jv.isString().stringValue());
		}

		jv = jo.get("score");
		if (jv != null) {
			this.setScoresFromJSON(jv.isObject());
		}

	}

	/**
	 * Override certain set attributes. This is very much a hack for now.
	 */
	protected void overrideAttributes() {
		// TODO Auto-generated method stub
		if (getCommonName().toLowerCase().endsWith("(drug)")) {
			if (getShapeCode() == -1) {
				setShapeCode(Shape.TRIANGLE);
			}
			setSystemSpace("chemical");
			setBiodeSpace("drug");
		} else if (getCommonName().toLowerCase().endsWith("(complex)")) {
			if (getShapeCode() == -1) {
				// only change the shape if it hasn't yet been set
				setShapeCode(Shape.DIAMOND);
			}
			setSystemSpace(SearchSpaceControl.getSystemspace());
			setBiodeSpace("complex");
		} else if (getCommonName().toLowerCase().endsWith("(abstract)")) {
			if (getShapeCode() == -1) {
				setShapeCode(Shape.RECT);
			}
			setSystemSpace(SearchSpaceControl.getSystemspace());
			setBiodeSpace("abstract");
		} else if (getCommonName().toLowerCase().endsWith("(family)")) {
			if (getShapeCode() == -1) {
				setShapeCode(Shape.RECT);
			}
			setSystemSpace(SearchSpaceControl.getSystemspace());
			setBiodeSpace("family");
		} else if (getCommonName().toLowerCase().endsWith("(mirna)")) {
			setSystemSpace(SearchSpaceControl.getSystemspace());
			setBiodeSpace("miRNA");
		} else if (getCommonName().toLowerCase().endsWith("(rna)")) {
			setSystemSpace(SearchSpaceControl.getSystemspace());
			setBiodeSpace("RNA");
		} else if (getCommonName().toLowerCase().endsWith("(smallmolecule)")) {
			if (getShapeCode() == -1) {
				setShapeCode(Shape.TRIANGLE);
			}
			setSystemSpace("chemical");
			setBiodeSpace("small molecule");
		}
	}

	/**
	 * Check if there is a record of this score.
	 * 
	 * @param key
	 * @return
	 */
	public boolean hasScore(final String key) {
		if (this.scores.containsKey(key)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get a single score. Returns 0 if there is no score.
	 * 
	 * @param key
	 * @return
	 */
	public double getScore(final String key) {
		if (!hasScore(key)) {
			return 0;
		}
		return this.scores.get(key);
	}

	/**
	 * Set a single score.
	 * 
	 * @param key
	 * @param score
	 */
	public void setScore(final String key, double score) {
		this.scores.put(key, score);
	}

	/**
	 * remove the named score from this object
	 * 
	 * @param scoreName
	 */
	public void removeScore(String scoreName) {
		this.scores.remove(scoreName);
	}

	/**
	 * Get all scores for this object in JSON format.
	 * 
	 * @return
	 */
	public JSONObject getScoresInJSON() {
		JSONObject resultJO = new JSONObject();

		for (String key : scores.keySet()) {
			resultJO.put(key, new JSONNumber(scores.get(key).doubleValue()));
		}

		return resultJO;
	}

	/**
	 * Set multiple scores for this object.
	 * 
	 * @param scoresJO
	 */
	public void setScoresFromJSON(final JSONObject scoresJO) {
		for (String key : scoresJO.keySet()) {
			setScore(key, scoresJO.get(key).isNumber().doubleValue());
		}
	}

	/**
	 * Set the common name. Currently also calls {@link #overrideAttributes()}.
	 * 
	 * @param commonName
	 */
	public void setCommonName(String commonName) {
		this.commonName = commonName;

		overrideAttributes();
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setSystemSpace(String sysSpace) {
		this.systemSpace = sysSpace;
	}

	public void setBiodeSpace(String bSpace) {
		this.biodeSpace = bSpace;
	}

	/**
	 * Sets the position attributes of the BiodeInfo. This does not actually
	 * reposition any node.
	 * 
	 * @param x
	 * @param y
	 */
	public void setPosition(double x, double y) {
		this.xPosition = x;
		this.yPosition = y;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public void setShapeCode(int shapeCode) {
		this.shapeCode = shapeCode;
	}

	/**
	 * Set the localization. This is the cellular location: nucleolus, nucleus,
	 * nuclear envelope, // cytoplasm, plasma membrane, ER, golgi, vacuole, etc
	 * 
	 * @param localization
	 */
	public void setLocalization(String localization) {
		this.localization = localization;
	}

	/**
	 * Get the localization. This is the cellular location: nucleolus, nucleus,
	 * nuclear envelope, // cytoplasm, plasma membrane, ER, golgi, vacuole, etc
	 * 
	 * @return
	 */
	public String getLocalization() {
		return this.localization;
	}

	/**
	 * Set the activation state. If it is set, uses "active" or "inactive".
	 * 
	 * @param state
	 */
	public void setActivationState(String state) {
		this.activationState = state;
	}

	/**
	 * Get the activation state. If it is set, uses "active" or "inactive".
	 * 
	 * @param state
	 */
	public String getActivationState() {
		return this.activationState;
	}

	/**
	 * Set the mutation. If it is set, this is something like "non-sense" or
	 * "mis-sense".
	 * 
	 * @param mutation
	 */
	public void setMutation(String mutation) {
		this.mutation = mutation;
	}

	/**
	 * Get the mutation. If it is set, this is something like "non-sense" or
	 * "mis-sense".
	 * 
	 * @param mutation
	 */
	public String getMutation() {
		return this.mutation;
	}

	/**
	 * Set the isoform. If it is set, this is something like "a" or "b".
	 * 
	 * @param isoform
	 */
	public void setIsoform(String isoform) {
		this.isoform = isoform;
	}

	/**
	 * SGt the isoform. If it is set, this is something like "a" or "b".
	 * 
	 * @param isoform
	 */
	public String getIsoform() {
		return this.isoform;
	}

	public String getColor() {
		return this.color;
	}

	public int getShapeCode() {
		return this.shapeCode;
	}

	public double getXPosition() {
		return this.xPosition;
	}

	public double getYPosition() {
		return this.yPosition;
	}

	public String getSystematicName() {
		return this.systematicName;
	}

	/**
	 * If the common name is "no alias" or "none" or "", then use the systematic
	 * name.
	 * 
	 * @return
	 */
	public String getCommonName() {
		if (this.commonName.equalsIgnoreCase("no alias")
				|| this.commonName.equalsIgnoreCase("none")
				|| this.commonName.equals("")) {
			return this.systematicName;
		} else {
			return this.commonName;
		}
	}

	public String getDescription() {
		return this.description;
	}

	public String getSystemSpace() {
		return this.systemSpace;
	}

	public String getBiodeSpace() {
		return this.biodeSpace;
	}

	/**
	 * Child classes should override this.
	 * 
	 * @return
	 */
	public JSONObject toJSONObject() {
		JSONObject infoJO = new JSONObject();

		infoJO.put("systematicName", new JSONString(this.getSystematicName()));
		infoJO.put("commonName", new JSONString(this.getCommonName()));
		infoJO.put("systemSpace", new JSONString(this.getSystemSpace()));
		infoJO.put("biodeSpace", new JSONString(this.getBiodeSpace()));
		infoJO.put("description", new JSONString(this.getDescription()));

		Double xPos = this.getXPosition();
		Double yPos = this.getYPosition();
		if (xPos != null && yPos != null) {
			infoJO.put("xPosition", new JSONNumber(Math.floor(xPos)));
			infoJO.put("yPosition", new JSONNumber(Math.floor(yPos)));
		}

		if (!scores.isEmpty()) {
			infoJO.put("score", this.getScoresInJSON());
		}

		String str = this.getColor();
		if (str != null) {
			infoJO.put("color", new JSONString(str));
		}

		Integer shapInt = this.getShapeCode();
		if (shapInt != null) {
			infoJO.put("shape", new JSONNumber(shapInt));
		}

		str = this.getActivationState();
		if ((str != null) && (str.length() > 0)) {
			infoJO.put("activationState", new JSONString(str));
		}

		str = this.getIsoform();
		if ((str != null) && (str.length() > 0)) {
			infoJO.put("isoform", new JSONString(str));
		}

		str = this.getLocalization();
		if ((str != null) && (str.length() > 0)) {
			infoJO.put("localization", new JSONString(str));
		}

		str = this.getMutation();
		if ((str != null) && (str.length() > 0)) {
			infoJO.put("mutation", new JSONString(str));
		}

		return infoJO;
	}
}