package edu.ucsc.ib.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.ucsc.ib.client.datapanels.LoggingDialogBox;
import edu.ucsc.ib.client.netviz.WorkingSetListener;

/**
 * Class for keeping track of BiodeInfo objects by mapping to them by
 * biode/systematic name.
 * 
 * @author cw
 * 
 */
public class BiodeInfoCenter implements WorkingSetListener {

	private final HashMap<String, BiodeInfo> biodeHashMap = new HashMap<String, BiodeInfo>();

	public BiodeInfoCenter() {
	}

	/**
	 * Called by NetworkVisualization in response to additions to its working
	 * set. For each biode in the BiodeSet, a BiodeInfo object is created and
	 * added to the biodeHash. This method is required by WorkingSetListener
	 * interface.
	 * 
	 * @param b
	 *            the BiodeSet of additions
	 */
	public void addedBiodes(BiodeSet b) {
		// There is nothing here because biode must be added to BIC *before*
		// other WorkingSetListeners are notified. Other WSL may need to use BIC
		// to get BiodeInfo, so it must be guaranteed to be updated first.
	}

	/**
	 * Called by NetworkVisualization in response to removals from its working
	 * set. Removes the BiodeInfo from the BiodeInfoCenter's biodeHash. This
	 * method is required by WorkingSetListener interface.
	 * 
	 * @param b
	 *            the BiodeSet of BiodeInfo to remove
	 */
	public void removedBiodes(BiodeSet bs) {

		for (String target : bs) {
			this.removeBiodeInfo(target);
		}
	}

	/**
	 * Gets a BiodeInfo object from BiodeInfoCenter's HashMap. Assumes that a
	 * mapping exists. Returns null if mapping does not exist.
	 * 
	 * @param systematicName
	 *            key for HashMap as String biode
	 * @return BiodeInfo
	 */
	public BiodeInfo getBiodeInfo(String systematicName) {
		return this.biodeHashMap.get(systematicName);
	}

	/**
	 * Adds a BiodeInfo object to BiodeInfoCenter's HashMap if the biode is not
	 * already recorded for the systemSpace.
	 * 
	 * @param info
	 *            BiodeInfo to add
	 */
	public void addBiodeInfo(BiodeInfo info) {
		// avoid adding the BiodeInfo for same biode multiple times
		if (!this.isRecorded(info.getSystematicName(), info.getSystemSpace())) {
			this.biodeHashMap.put(info.getSystematicName(), info);
		}
	}

	/**
	 * Adds a HashSet of BiodeInfo objects to BiodeInfoCenter's HashMap.
	 * 
	 * @param infoHashSet
	 */
	public BiodeSet addBiodeInfo(HashSet<BiodeInfo> infoHashSet) {
		BiodeSet bs = new BiodeSet();
		if (infoHashSet.isEmpty()) {
			return bs;
		}
		for (BiodeInfo info : infoHashSet) {
			if (!this.isRecorded(info.getSystematicName(),
					info.getSystemSpace())) {
				this.biodeHashMap.put(info.getSystematicName(), info);
				bs.add(info.getSystematicName());
			}
		}
		return bs;
	}

	/**
	 * Adds a HashMap of BiodeInfo objects to BiodeInfoCenter's HashMap.
	 * 
	 * @param infoHashMap
	 */
	public void addBiodeInfo(HashMap<String, BiodeInfo> infoHashMap) {
		if (!infoHashMap.isEmpty()) {
			this.biodeHashMap.putAll(infoHashMap);
		}
	}

	/**
	 * Removes a BiodeInfo object from BiodeInfoCenter's HashMap.
	 * 
	 * @param systematicName
	 */
	public void removeBiodeInfo(String systematicName) {
		this.biodeHashMap.remove(systematicName);
	}

	/**
	 * Log all of the contents of biodeHash to the logger.
	 * 
	 */
	public void logAllBiodeInfo() {
		LoggingDialogBox.log("*** log BIC contents ***");
		LoggingDialogBox.log("Size of BIC: " + this.biodeHashMap.size());

		for (String key : this.biodeHashMap.keySet()) {
			BiodeInfo bi = getBiodeInfo(key);
			LoggingDialogBox.log("sys: " + bi.getSystematicName());
			LoggingDialogBox.log("common: " + bi.getCommonName());
			LoggingDialogBox.log("desc: " + bi.getDescription());
			LoggingDialogBox.log("systemSpace: " + bi.getSystemSpace());
			LoggingDialogBox.log("biodeSpace: " + bi.getBiodeSpace());
		}
		LoggingDialogBox.log("*** end BIC contents ***");
	}

	/**
	 * Get systematicName for all BiodeInfo objects in this Object.
	 * 
	 * @return
	 */
	public ArrayList<String> getAllSystematicNames() {
		ArrayList<String> nameList = new ArrayList<String>();

		for (String name : this.biodeHashMap.keySet()) {
			nameList.add(name);
		}

		return nameList;
	}

	/**
	 * Check to see if BiodeInfo has been recorded for a biode.
	 * 
	 * @param biode
	 * @param organism
	 * @return
	 */
	public boolean isRecorded(String biode, String organism) {
		if (!this.biodeHashMap.containsKey(biode)) {
			return false;
		} else {
			return this.biodeHashMap.get(biode).getSystemSpace()
					.equalsIgnoreCase(organism);
		}
	}

	/**
	 * Get the keyset for the BiodeInfoCenter's biodeHashMap
	 * 
	 * @return
	 */
	public Set<String> getKeys() {
		return this.biodeHashMap.keySet();
	}
}
