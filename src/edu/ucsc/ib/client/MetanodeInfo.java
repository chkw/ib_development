package edu.ucsc.ib.client;

/**
 * Store data that applies to a metanode in this object.
 */
public class MetanodeInfo extends EntityInfo {

	/**
	 * Keep track of this Metanode's members by name
	 */
	private final BiodeSet members = new BiodeSet();

	/**
	 * Sets the specified id.
	 * 
	 * @param id
	 */
	public MetanodeInfo(String id) {
		super(id);
	}

	/**
	 * Add a BiodeInfo as a member of this MetanodeInfo.
	 * 
	 * @param bi
	 * @return
	 */
	public boolean addMember(BiodeInfo bi) {
		return this.members.add(bi.getSystematicName());
	}

	/**
	 * Remove the BiodeInfo as a member of this MetanodeInfo. Does not remove
	 * the membership from the biodeInfo.
	 * 
	 * @param bi
	 * @return
	 */
	public boolean removeMember(BiodeInfo bi) {
		return this.members.remove(bi.getSystematicName());
	}

	/**
	 * Check if the id is a member in this MetanodeInfo.
	 * 
	 * @param id
	 * @return
	 */
	public boolean hasMember(String id) {
		return this.members.contains(id);
	}

	/**
	 * Return the BiodeSet of metanode members.
	 * 
	 * @return
	 */
	public BiodeSet getMembers() {
		return this.members;
	}
}
