package edu.ucsc.ib.drawpanel.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

public class Text extends Widget implements Drawable {
	private double x;
	private double y;

	public Text(String text) {
		this(text, 0, 0);
	}

	public Text(String text, double x, double y) {
		if (text == null) {
			text = "";
		}
		setElement(DrawPanel.impl.createTextElement());
		setText(text);
		setPosition_drawable(x, y);
	}

	public void setText(String text) {
		DOM.setInnerText(getElement_drawable(), text);
	}

	public String getText() {
		return DOM.getInnerText(getElement_drawable());
	}

	public double getTextLength() {
		return DrawPanel.impl.getComputedTextLength(this);
	}

	public double getWidth_drawable() {
		return DrawPanel.impl.getBBoxWidthForDP(this);
	}

	public double getHeight_drawable() {
		return DrawPanel.impl.getBBoxHeightForDP(this);
	}

	public double getX_drawable() {
		return x;
	}

	public double getY_drawable() {
		return y;
	}

	public void setPosition_drawable(double x, double y) {
		this.x = x;
		this.y = y;
		DrawPanel.impl.setTextPosition(this, x, y);
	}
	
	public void setDy(double dy){
		DrawPanel.impl.setTextDy(this, dy);
	}

	@Override
	public Element getElement_drawable() {
		return getElement();
	}

}
