package edu.ucsc.ib.server.CircleMap;

import java.awt.Color;
import java.util.ArrayList;

/**
 * 
 * Instances contain the data that Plotter will use to draw a CircleMap. This
 * data includes things like arc lengths and diameters. It should not include
 * things like features scores and subtype groupings data.
 * 
 * @author chrisw
 * 
 */
public class CircleMapData {

	// TODO ArcData class ///////////////////////////

	/**
	 * The drawing data for one arc in one ring in CircleMap image.
	 * 
	 * @author chrisw
	 * 
	 */
	public class ArcData {

		/**
		 * starting angle for the arc. "0" degrees is the 3-o-clock position.
		 */
		private final double startAngle;

		/**
		 * arc length for the arc. The sweep angle increases in the
		 * counter-clockwise direction.
		 */
		private final double sweepAngle;

		/**
		 * color of the arc.
		 */
		private final Color color;

		/**
		 * Constructor.
		 * 
		 * @param startAngle
		 *            starting angle for the arc. "0" degrees is the 3-o-clock
		 *            position.
		 * @param sweepAngle
		 *            arc length for the arc. The sweep angle increases in the
		 *            counter-clockwise direction.
		 * @param color
		 *            color of the arc.
		 */
		public ArcData(double startAngle, double sweepAngle, Color color) {
			this.startAngle = startAngle;
			this.sweepAngle = sweepAngle;
			this.color = color;
		}

		/**
		 * Get the starting angle for the arc. "0" degrees is the 3-o-clock
		 * position.
		 * 
		 * @return
		 */
		public double getStartAngle() {
			return this.startAngle;
		}

		/**
		 * Get the arc length for the arc. The sweep angle increases in the
		 * counter-clockwise direction.
		 * 
		 * @return
		 */
		public double getSweepAngle() {
			return this.sweepAngle;
		}

		/**
		 * Get the color of the arc.
		 * 
		 * @return
		 */
		public Color getColor() {
			return this.color;
		}
	}

	// TODO RingData class ///////////////////////////

	/**
	 * 
	 * 
	 * The drawing data for one ring in CircleMap image.
	 * 
	 * @author chrisw
	 * 
	 */
	public class RingData {

		/**
		 * Relative thickness of this ring. For example, if thickness is 2, then
		 * this ring will be twice as thick as a ring with thickness of 1.
		 */
		private final double thickness;

		/**
		 * ArcData are to be added in the order to be drawn. That is, adjacent
		 * ArcData in this list are adjacent in the ring.
		 */
		private final ArrayList<ArcData> arcDataList;

		/**
		 * Default constructor assigns ring thickness of 1.
		 */
		public RingData() {
			this(new Double(1));
		}

		/**
		 * Constructor with custom ring thickness.
		 * 
		 * @param thickness
		 *            relative thickness of this ring. For example, if thickness
		 *            is 2, then this ring will be twice as thick as a ring with
		 *            thickness of 1.
		 */
		public RingData(double thickness) {
			this.thickness = thickness;
			arcDataList = new ArrayList<ArcData>();
		}

		/**
		 * Get the relative thickness of this ring. For example, if thickness is
		 * 2, then this ring will be twice as thick as a ring with thickness of
		 * 1.
		 * 
		 * @return
		 */
		public double getThickness() {
			return this.thickness;
		}

		/**
		 * Add data for an arc to the ring.
		 * 
		 * @param startAngle
		 * @param sweepAngle
		 * @param color
		 */
		public void addArc(double startAngle, double sweepAngle, Color color) {
			ArcData arcData = new ArcData(startAngle, sweepAngle, color);
			arcDataList.add(arcData);
		}

		/**
		 * Get the ArcData for this ring.
		 * 
		 * @return
		 */
		public ArrayList<ArcData> getArcData() {
			return arcDataList;
		}

		/**
		 * Get the number of arcs in this ring.
		 * 
		 * @return
		 */
		public int getArcCount() {
			return arcDataList.size();
		}
	}

	// TODO CircleMapData class ///////////////////////////

	/**
	 * Label to be assigned to the CircleMap.
	 */
	private final String label;

	/**
	 * RingData are to be added in the order to be drawn. That is, adjacent
	 * RingData in this list are adjacent in the CircleMap.
	 */
	private final ArrayList<RingData> ringDataList;

	/**
	 * Constructor.
	 * 
	 * @param label
	 *            Label to be assigned to the CircleMap.
	 */
	public CircleMapData(String label) {
		this.label = label;
		ringDataList = new ArrayList<RingData>();
	}

	/**
	 * RingData are to be added in the order to be drawn. That is, adjacent
	 * RingData in this list are adjacent in the CircleMap. Outermost ring
	 * should be added first. Innermost ring should be added last.
	 * 
	 * @param ringData
	 */
	public void addRingData(RingData ringData) {
		ringDataList.add(ringData);
	}

	/**
	 * Get the ring data for this CircleMap.
	 * 
	 * @return
	 */
	public ArrayList<RingData> getRingData() {
		return ringDataList;
	}

	/**
	 * Get the number of rings for this CircleMap.
	 * 
	 * @return
	 */
	public int getRingCount() {
		return ringDataList.size();
	}

	/**
	 * Get the label for this CircleMap.
	 * 
	 * @return
	 */
	public String getLabel() {
		return this.label;
	}
}
