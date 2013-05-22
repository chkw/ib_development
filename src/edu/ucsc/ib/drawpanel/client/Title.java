package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * This Widget represents a "title" tag. It is meant to be used as an SVG title.
 * The SVG should look like: <a
 * href="https://developer.mozilla.org/en-US/docs/SVG/Element/title" >link</a>.
 * 
 * @author chrisw
 * 
 */
public class Title extends Widget implements Drawable {
	public Title(String text) {
		setElement(DrawPanel.impl.createTitleElement());
		setText(text);
	}

	public void setText(String text) {
		DOM.setInnerText(getElement_drawable(), text);
	}

	public String getText() {
		return DOM.getInnerText(getElement_drawable());
	}

	@Override
	public Element getElement_drawable() {
		return getElement();
	}

	@Override
	public double getX_drawable() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getY_drawable() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getHeight_drawable() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getWidth_drawable() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPosition_drawable(double x, double y) {
		// TODO Auto-generated method stub

	}

}
