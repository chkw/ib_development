package edu.ucsc.ib.client;

import java.util.HashMap;
import java.util.Set;

/**
 * Class for keeping track of MetanodeInfo objects by mapping to them by
 * biode/systematic name. It is not a WorkingSetListener, so it does not respond
 * to additions or removals from the working set of biodes. Instead, each biode
 * should take care of its own metanode memberships when it is removed. This
 * would eliminate the need to look through all membership BiodeSets for each
 * Metanode. Also, it is assumed there are no new BiodeSets passed in via
 * parameters.
 * 
 * @author cw
 * 
 */
public class MetanodeInfoCenter {

	/**
	 * Map a metanode ID to MetanodeInfo object.
	 */
	private final HashMap<String, MetanodeInfo> metaNodeHashMap = new HashMap<String, MetanodeInfo>();

	private final BiodeInfoCenter bic;

	public MetanodeInfoCenter(BiodeInfoCenter bic) {
		this.bic = bic;
	}

	/**
	 * Add a MetanodeInfo to the HashMap and add its members. Also, the metanode
	 * name is added to each BiodeInfo. Assumes there are no new Biodes in the
	 * specified BiodeSet. Any new Biodes should have been added to
	 * BiodeInfoCenter *before* calling this method.
	 * 
	 * @param id
	 * @param bs
	 * @return
	 */
	public MetanodeInfo addMetanode(String id, BiodeSet bs) {
		MetanodeInfo mi = this.getMetanodeInfo(id);

		// check if metanode exists
		if (mi == null) {
			mi = new MetanodeInfo(id);
		} else {
			return null;
		}

		// don't do anything if empty biodeset
		if (bs.size() == 0) {
			return null;
		}

		for (String biode : bs) {
			BiodeInfo bi = bic.getBiodeInfo(biode);
			// didn't check for valid BiodeInfo

			bi.addMembership(id);
			mi.addMember(bi);
		}

		this.metaNodeHashMap.put(id, mi);

		return mi;
	}

	/**
	 * Remove the MetanodeInfo object from the MetanodeInfoCenter's HashMap.
	 * Return the previously associated value. Also, remove the metanode
	 * membership from each of the member biodes.
	 * 
	 * @param id
	 * @return
	 */
	public MetanodeInfo removeMetanode(String id) {
		BiodeSet bs = this.metaNodeHashMap.get(id).getMembers();

		for (String biode : bs) {
			bic.getBiodeInfo(biode).removeMembership(id);
		}

		return this.metaNodeHashMap.remove(id);
	}

	/**
	 * Get the MetanodeInfo object that is mapped to the specified id.
	 * 
	 * @param id
	 * @return
	 */
	public MetanodeInfo getMetanodeInfo(String id) {
		return this.metaNodeHashMap.get(id);
	}

	/**
	 * Check if metanode name is in use.
	 * 
	 * @param id
	 * @return
	 */
	public boolean metanodeNameInUse(String id) {
		return ((Set<String>) this.metaNodeHashMap.keySet()).contains(id);
	}

	/**
	 * Get all of the metanodes' names.
	 * 
	 * @return
	 */
	public Set<String> getMetaNodeNames() {
		return (Set<String>) this.metaNodeHashMap.keySet();
	}
}
