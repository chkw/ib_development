package edu.ucsc.ib.server.CircleMap;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import edu.ucsc.ib.server.CircleMap.CircleMapData.ArcData;
import edu.ucsc.ib.server.CircleMap.CircleMapData.RingData;

/**
 * Draw a CircleMap in a BufferedImage object, which can subsequently be written
 * to a file. This class only draws the BufferedImage. The data for drawing the
 * CircleMap should be provided in the form of RingData objects.
 * 
 * @author chrisw
 * 
 */
public class Plotter {

	// private static final double DEFAULT_SIZE_OF_BUFFERED_IMAGE = new
	// Double(200);
	private static final double DEFAULT_SIZE_OF_BUFFERED_IMAGE = new Double(
			4000);
	public static final double DEFAULT_RELATIVE_SIZE_OF_CENTER = new Double(1);
	private final double imageWidth;
	private final double imageHeight;

	// TODO constructor section below ///////////////////////////////

	/**
	 * To produce a square image in the default size.
	 */
	public Plotter() {
		this(DEFAULT_SIZE_OF_BUFFERED_IMAGE);
	}

	/**
	 * To produce a square image.
	 * 
	 * @param width
	 *            a square image is assumed
	 */
	public Plotter(double width) {
		this(width, width);
	}

	/**
	 * To produce a rectangular image.
	 * 
	 * @param width
	 * @param height
	 */
	public Plotter(double width, double height) {
		this.imageWidth = width;
		this.imageHeight = height;
	}

	// TODO method section below ///////////////////////////////

	/**
	 * Draw a filled slice of a centered circle. Uses an Arc2D.Double object.
	 * 
	 * @param g2d
	 * @param diameter
	 * @param startAngle
	 * @param sweepAngle
	 * @param color
	 */
	private void fillCenteredArc2D_svg(SVGGraphics2D g2d, double diameter,
			double startAngle, double sweepAngle, Color color) {
		g2d.setColor(color);
		Arc2D shape = new Arc2D.Double((imageWidth / 2) - (diameter / 2),
				(imageHeight / 2) - (diameter / 2), diameter, diameter,
				startAngle, sweepAngle, Arc2D.PIE);
		g2d.fill(shape);
	}

	/**
	 * Draw a filled slice of a centered circle. Uses an Arc2D.Double object.
	 * 
	 * @param g2d
	 * @param diameter
	 * @param startAngle
	 * @param sweepAngle
	 * @param color
	 */
	private void fillCenteredArc2D(Graphics2D g2d, double diameter,
			double startAngle, double sweepAngle, Color color) {
		g2d.setColor(color);
		Arc2D shape = new Arc2D.Double((imageWidth / 2) - (diameter / 2),
				(imageHeight / 2) - (diameter / 2), diameter, diameter,
				startAngle, sweepAngle, Arc2D.PIE);
		g2d.fill(shape);
	}

	/**
	 * Draw an arc of a centered circle. Uses an Arc2D.Double object.
	 * 
	 * @param g2d
	 * @param diameter
	 * @param startAngle
	 * @param sweepAngle
	 * @param color
	 */
	private void emptyCenteredArc2D(Graphics2D g2d, double diameter,
			double startAngle, double sweepAngle, Color color) {
		g2d.setColor(color);
		Arc2D shape = new Arc2D.Double((imageWidth / 2) - (diameter / 2),
				(imageHeight / 2) - (diameter / 2), diameter, diameter,
				startAngle, sweepAngle, Arc2D.OPEN);
		g2d.fill(shape);
	}

	/**
	 * Draw a centered string in TimesRoman font.
	 * 
	 * @param g2d
	 * @param message
	 * @param color
	 */
	private void drawCenteredString(Graphics2D g2d, String message, Color color) {
		Font font = new Font("TimesRoman", Font.BOLD, 20);
		g2d.setFont(font);
		FontMetrics fontMetrics = g2d.getFontMetrics();
		int stringWidth = fontMetrics.stringWidth(message);
		int stringHeight = fontMetrics.getAscent();
		g2d.setColor(color);
		g2d.drawString(message, ((int) imageWidth - stringWidth) / 2,
				(int) imageHeight / 2 + stringHeight / 4);
	}

	/**
	 * Draw a CircleMap to a BufferedImage, which can subsequently be written to
	 * a file.
	 * 
	 * @param circleMapData
	 * @return
	 */
	public BufferedImage getCircleMapImage(CircleMapData circleMapData) {
		// initialize image

		// TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
		// into integer pixels
		BufferedImage bi = new BufferedImage((int) imageWidth,
				(int) imageHeight, BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics2D = bi.createGraphics();

		renderToG2D(circleMapData, graphics2D);

		return bi;
	}

	public SVGGraphics2D getCircleMapSvgG2D(CircleMapData circleMapData) {
		// Get a DOMImplementation.
		DOMImplementation domImpl = GenericDOMImplementation
				.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);

		// Create an instance of the SVG Generator.
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		renderToSvgG2D(circleMapData, svgGenerator);

		return svgGenerator;
	}

	/**
	 * Render the CircleMapData to the specified Graphics2D.
	 * 
	 * @param circleMapData
	 * @param graphics2D
	 */
	private void renderToSvgG2D(CircleMapData circleMapData,
			SVGGraphics2D graphics2D) {
		// TODO debug stuff (orange background)
		// graphics2D.setBackground(Color.ORANGE);
		// graphics2D.clearRect(0, 0, (int) this.imageWidth,
		// (int) this.imageHeight);

		// use anti-aliasing, if possible
		// tends to increase the file size of the resulting image file
		// graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);

		// Sum of previous rings' thickenesses.
		double previousRingThicknessUnits = new Double(0);

		// Center of CircleMap will default to 1 unit of thickness.
		double sumOfRingThicknessUnits = DEFAULT_RELATIVE_SIZE_OF_CENTER;
		for (RingData ringData : circleMapData.getRingData()) {
			sumOfRingThicknessUnits += ringData.getThickness();
		}

		// draw rings
		ArrayList<RingData> ringDataList = circleMapData.getRingData();
		for (int i = 0; i < circleMapData.getRingCount(); i++) {
			RingData ringData = ringDataList.get(i);

			// whole diameter minus thickness of previous rings
			double fractionOfDiameterToUse = 1 - (previousRingThicknessUnits / sumOfRingThicknessUnits);

			double diameter = imageWidth * fractionOfDiameterToUse;

			previousRingThicknessUnits += ringData.getThickness();

			// remove ghosting from anti-aliasing
			fillCenteredArc2D_svg(graphics2D, diameter, new Double(0),
					new Double(360), Color.WHITE);

			// arcs
			for (ArcData arcData : ringData.getArcData()) {
				double startAngle = arcData.getStartAngle();
				double sweepAngle = arcData.getSweepAngle();
				Color color = arcData.getColor();

				fillCenteredArc2D_svg(graphics2D, diameter, startAngle,
						sweepAngle, color);
			}
		}

		// draw the center section as white circle
		double diameter = imageWidth
				* (DEFAULT_RELATIVE_SIZE_OF_CENTER / sumOfRingThicknessUnits);
		fillCenteredArc2D_svg(graphics2D, diameter, 0, 360, Color.WHITE);

		// label
		// drawCenteredString(graphics2D, circleMapData.getLabel(),
		// Color.ORANGE);
	}

	/**
	 * Render the CircleMapData to the specified Graphics2D.
	 * 
	 * @param circleMapData
	 * @param graphics2D
	 */
	private void renderToG2D(CircleMapData circleMapData, Graphics2D graphics2D) {
		// TODO debug stuff (orange background)
		// graphics2D.setBackground(Color.ORANGE);
		// graphics2D.clearRect(0, 0, (int) this.imageWidth,
		// (int) this.imageHeight);

		// use anti-aliasing, if possible
		// tends to increase the file size of the resulting image file
		// graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);

		// Sum of previous rings' thickenesses.
		double previousRingThicknessUnits = new Double(0);

		// Center of CircleMap will default to 1 unit of thickness.
		double sumOfRingThicknessUnits = DEFAULT_RELATIVE_SIZE_OF_CENTER;
		for (RingData ringData : circleMapData.getRingData()) {
			sumOfRingThicknessUnits += ringData.getThickness();
		}

		// draw rings
		ArrayList<RingData> ringDataList = circleMapData.getRingData();
		for (int i = 0; i < circleMapData.getRingCount(); i++) {
			RingData ringData = ringDataList.get(i);

			// whole diameter minus thickness of previous rings
			double fractionOfDiameterToUse = 1 - (previousRingThicknessUnits / sumOfRingThicknessUnits);

			double diameter = imageWidth * fractionOfDiameterToUse;

			previousRingThicknessUnits += ringData.getThickness();

			// remove ghosting from anti-aliasing
			// fillCenteredArc2D(graphics2D, diameter, new Double(0), new
			// Double(
			// 360), Color.WHITE);
			
			emptyCenteredArc2D(graphics2D, diameter, 0, 360,
					Color.BLACK);

			// arcs
			for (ArcData arcData : ringData.getArcData()) {
				double startAngle = arcData.getStartAngle();
				double sweepAngle = arcData.getSweepAngle();
				Color color = arcData.getColor();

				fillCenteredArc2D(graphics2D, diameter-2, startAngle, sweepAngle,
						color);
			}
		}

		// draw the center section as white circle
		double diameter = imageWidth
				* (DEFAULT_RELATIVE_SIZE_OF_CENTER / sumOfRingThicknessUnits);
		fillCenteredArc2D(graphics2D, diameter, 0, 360, Color.WHITE);

		// label
		// drawCenteredString(graphics2D, circleMapData.getLabel(),
		// Color.ORANGE);
	}
}
