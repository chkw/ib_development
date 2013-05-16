package edu.ucsc.ib.client;

import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

/**
 * Store data that applies to a biode in this object.
 */
public class BiodeInfo extends EntityInfo {

	/**
	 * Keep track of this BiodeInfo's metanode memberships by name.
	 */
	private final HashSet<String> metaNodeMembership = new HashSet<String>();

	/**
	 * 
	 */
	private final HashMap<String, String> pBLAST = new HashMap<String, String>();

	/**
	 * Sets the specified systematicName.
	 * 
	 * @param systematicName
	 */
	public BiodeInfo(String systematicName) {
		super(systematicName);
	}

	/**
	 * Construct a BiodeInfo using information from an EntityInfo.
	 * 
	 * @param ei
	 */
	public BiodeInfo(EntityInfo ei) {
		super(ei.getSystematicName());
	}

	/**
	 * Create a BiodeInfo from the JSONObject. JSONObject can have the fields:
	 * "systematicName", "commonName", "systemSpace", "biodeSpace",
	 * "description", "xPosition", "yPosition", "color", "shape",
	 * "metaNodeMemberships". Of these, the following should have data:
	 * "systematicName", "commonName", "systemSpace", "biodeSpace",
	 * "description"
	 * 
	 * @param jo
	 */
	public BiodeInfo(JSONObject jo) {
		super();

		jsonToAttributes(jo);
	}

	/**
	 * Use data in JSONObject to set attributes in this object.
	 * 
	 * @param jo
	 */
	private void jsonToAttributes(JSONObject jo) {
		JSONValue jv;

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

		jv = jo.get("systematicName");
		if (jv != null) {
			this.systematicName = (jv.isString().stringValue());
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

		jv = jo.get("metaNodeMemberships");
		if (jv != null) {
			JSONArray metaJA = jv.isArray();
			for (int i = 0; i < metaJA.size(); i++) {
				this.addMembership(metaJA.get(i).isString().stringValue());
			}
		}

		jv = jo.get("bestPBlast");
		if (jv != null) {
			JSONObject pBLASTJO = jv.isObject();

			for (String organism : pBLASTJO.keySet()) {
				this.addPBlast(organism, pBLASTJO.get(organism).isString()
						.stringValue());
			}

		}

		jv = jo.get("score");
		if (jv != null) {
			this.setScoresFromJSON(jv.isObject());
		}

		overrideAttributes();
	}

	/**
	 * Get a JSON text that has the attributes of this BiodeInfo.
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

		if (this.metaNodeMembership.size() > 0) {
			JSONArray metaNodeJA = new JSONArray();
			for (String metaMembership : this.metaNodeMembership) {
				metaNodeJA.set(metaNodeJA.size(),
						new JSONString(metaMembership));
			}
			infoJO.put("metaNodeMemberships", metaNodeJA);
		}

		if (this.pBLAST.size() > 0) {
			JSONObject pBlastJO = new JSONObject();
			for (String organism : this.pBLAST.keySet()) {
				pBlastJO.put(organism,
						new JSONString(this.pBLAST.get(organism)));
			}
			infoJO.put("bestPBlast", pBlastJO);
		}

		return infoJO;
	}

	/**
	 * Check to see if the BiodeInfo is a member of a specified metanode.
	 * 
	 * @param metanodeName
	 * @return
	 */
	public boolean isMemberOf(String metanodeName) {
		return this.metaNodeMembership.contains(metanodeName);
	}

	/**
	 * Get a clone of the HashSet of all metanode memberships for this
	 * BiodeInfo. This is a shallow copy of the HashSet in which only the top
	 * level structure is duplicated. Lower structure is same. (Modifying the
	 * HashSet may produce unexpected results.)
	 * 
	 * @return
	 */
	public HashSet<String> getAllMemberships() {
		return (HashSet<String>) this.metaNodeMembership.clone();
	}

	/**
	 * Add a metanode membership if not already a member. Returns
	 * <code>true</code> if successful. BiodeInfo does not add itself to the
	 * MetanodeInfo. Maybe it should
	 * 
	 * @param metanodeName
	 * @return
	 */
	public boolean addMembership(String metanodeName) {
		// TODO
		return this.metaNodeMembership.add(metanodeName);
	}

	/**
	 * Remove a metanode membership. Returns <code>true</code> if successful.
	 * BiodeInfo does not remove itself from the MetanodeInfo. Maybe it should.
	 * 
	 * @param metanodeName
	 * @return
	 */
	public boolean removeMembership(String metanodeName) {
		// TODO
		return this.metaNodeMembership.remove(metanodeName);
	}

	/**
	 * Set the best pBLAST for this biode to another organism.
	 * 
	 * @param organism
	 * @param id
	 */
	public void addPBlast(String organism, String id) {
		this.pBLAST.put(organism, id);
	}

	/**
	 * Get the best pBLAST. Return null if there is none recorded.
	 * 
	 * @param organism
	 * @return
	 */
	public String getPBlast(String organism) {
		return this.pBLAST.get(organism);
	}

	/**
	 * Get all the pBLAST mappings. These are id mappings keyed on oragnism.
	 * 
	 * @return
	 */
	public HashMap<String, String> getAllPBlast() {
		return this.pBLAST;
	}
}
