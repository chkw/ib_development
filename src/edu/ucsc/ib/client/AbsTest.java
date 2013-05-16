/*
 * Interaction Browser
 * 
 * License to be determined.
 */

package edu.ucsc.ib.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import edu.ucsc.ib.drawpanel.client.DrawPanel;
import edu.ucsc.ib.drawpanel.client.Group;
import edu.ucsc.ib.drawpanel.client.Line;
import edu.ucsc.ib.drawpanel.client.Shape;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AbsTest implements EntryPoint {

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {

		int dimension = 500;

		final DrawPanel dp = new DrawPanel(dimension + "px", dimension + "px");

		final Group grid = new Group();
		grid.setID("grid");

		final Shape border = new Shape(Shape.RECT, 250, 250, 498);
		border.setStrokeWidth(10);
		border.setStroke("red");
		grid.add(border);

		for (int i = 1; i < 10; i++) {
			Line gridLine = new Line(50 * i, 0, 50 * i, 500);
			gridLine.setStroke("grey");
			grid.add(gridLine);
			gridLine = new Line(0, 50 * i, 500, 50 * i);
			gridLine.setStroke("grey");
			grid.add(gridLine);
		}

		dp.add(grid);

		final Group gLines = new Group();
		gLines.setID("gLines");

		final Line l = new Line(100, 20, 400, 20);
		l.setStroke("green");
		l.setStrokeWidth(5);
		l.setMarkerStart();
		l.setMarkerEnd();
		gLines.add(l);

		final Line l1 = new Line(100, 30, 400, 30);
		l1.setStroke("green");
		l1.setStrokeWidth(5);
		gLines.add(l1);

		final Line l2 = new Line(150, 50, 200, 70);
		l2.setStroke("pink");
		l2.setStrokeWidth(3);
		l2.setMarkerStart();
		l2.setMarkerEnd();
		gLines.add(l2);

		final Line l3 = new Line(400, 200, 100, 300);
		l3.setStroke("grey");
		l3.setStrokeWidth(3);
		l3.setMarkerStart();
		// l3.setMarkerEnd();
		gLines.add(l3);

		final Group gShapes = new Group();
		gShapes.setID("gShapes");

		final Shape c = new Shape(Shape.CIRCLE, 270, 430, 40);
		c.setFill("blue");
		gShapes.add(c);

		final Shape c2 = new Shape(Shape.CIRCLE, 100, 50, 60);
		c2.setFill("red");
		c2.setStroke("black");
		gShapes.add(c2);

		final Shape c3 = new Shape(Shape.CIRCLE, 200, 50, 60);
		c3.setFill("orange");
		gShapes.add(c3);

		final Shape c4 = new Shape(Shape.CIRCLE, 300, 50, 60);
		c4.setFill("green");
		gShapes.add(c4);

		final Shape c5 = new Shape(Shape.CIRCLE, 50, 100, 20);
		c5.setFill("red");
		gShapes.add(c5);

		final Shape c6 = new Shape(Shape.CIRCLE, 50, 200, 15);
		c6.setFill("orange");
		gShapes.add(c6);

		final Shape c7 = new Shape(Shape.CIRCLE, 50, 300, 10);
		c7.setFill("green");
		gShapes.add(c7);

		final Shape c8 = new Shape(Shape.CIRCLE, 50, 100, 5);
		c8.setFill("brown");
		gShapes.add(c8);

		final Group gRects = new Group();
		gRects.setID("gRects");

		final Shape r1 = new Shape(Shape.RECT, 440, 250, 50); // makes a square
		r1.setSizeY(20); // sets a new height
		r1.setPosition_drawable(440, 250); // re-calculates the attributes for a
											// rectangle
		r1.setFill("cyan");
		r1.setStroke("orange");
		gRects.add(r1);

//		dp.add(gLines);
//
//		// gLines.setTransform("scale(1.5) translate(-70,100)");
//
//		gLines.setTransform("translate(-70,100)");
//		dp.add(gLines);
//
//		gLines.setTransform("scale(1.5)");
//
//		dp.add(gRects);
//		dp.add(gShapes);
//		dp.add(gLines);

		// Assume that the host HTML has elements defined whose
		// IDs are "slot1", "slot2". In a real app, you probably would not want
		// to hard-code IDs. Instead, you could, for example, search for all
		// elements with a particular CSS class and replace them with widgets.
		//
		RootPanel.get("netVizSlot").add(dp);
		// RootPanel.get("slot1").add(button1);
		// RootPanel.get("slot2").add(label);
		// // RootPanel.get("slot3").add(label2);
		// // RootPanel.get("slot4");
		// RootPanel.get("absSlot").add(absPanel);

		// SVGPanel svg = new SVGPanel(500, 500);
		// RootPanel.get("svgSlot").add(svg);
		// SVGCircle svgC = new SVGCircle(30, 30, 15);
		// svgC.setFill(Color.CYAN);
		// svg.add(svgC);
	}
}
