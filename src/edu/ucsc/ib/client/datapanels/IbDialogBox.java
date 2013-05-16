package edu.ucsc.ib.client.datapanels;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

/**
 * A child class of DialogBox that has a button in the Caption for hiding.
 * 
 * @author cw
 * 
 */
public class IbDialogBox extends DialogBox {

	/**
	 * Implementation of DialogBox.Caption that has a button for hiding the
	 * window.
	 * 
	 * @author cw
	 * 
	 */
	private static class HPanelCaptionImpl extends HorizontalPanel implements
			Caption {

		private IbDialogBox ibDialogBox = null;

		// /**
		// * Close "button" is simply clickable text with attached ClickHandler.
		// */
		// private final Label closeLabel = new Label();
		// {
		// closeLabel.getElement().setInnerText("[close]");
		// closeLabel.addClickHandler(new ClickHandler() {
		// @Override
		// public void onClick(ClickEvent event) {
		// ibDialogBox.hideDialogBox();
		// }
		// });
		// }

		private static final String closeButtonImageURL = "close.png";

		/**
		 * Close button is a clickable image with a ClickHandler that closes the
		 * dialogbox.
		 */
		private final Image closeButtonImage = new Image(closeButtonImageURL);
		{
			closeButtonImage.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					ibDialogBox.hideDialogBox();
				}
			});
		}

		/**
		 * Default constructor of the caption. Must call setIbDialogBox() before
		 * it is useful.
		 */
		public HPanelCaptionImpl() {
			super();
			this.setVerticalAlignment(ALIGN_MIDDLE);
			setStyleName("Caption");

			// setWidth("100%");
			// this.setHorizontalAlignment(ALIGN_CENTER);

			add(closeButtonImage);
		}

		/**
		 * This constructor specified some text for the caption/title bar. Must
		 * call setIbDialogBox() before it is useful.
		 * 
		 * @param captionText
		 */
		public HPanelCaptionImpl(final String captionText) {
			this();

			insert(new Label(captionText), 1);
		}

		/**
		 * Sets this captions IbDialogBox so that it can be closed when the
		 * close button is clicked.
		 * 
		 * @param ibDialogBox
		 */
		public void setIbDialogBox(final IbDialogBox ibDialogBox) {
			this.ibDialogBox = ibDialogBox;
		}

		@Override
		public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
			return addDomHandler(handler, MouseDownEvent.getType());
		}

		@Override
		public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
			return addDomHandler(handler, MouseUpEvent.getType());
		}

		@Override
		public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
			return addDomHandler(handler, MouseOutEvent.getType());
		}

		@Override
		public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
			return addDomHandler(handler, MouseOverEvent.getType());
		}

		@Override
		public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
			return addDomHandler(handler, MouseMoveEvent.getType());
		}

		@Override
		public HandlerRegistration addMouseWheelHandler(
				MouseWheelHandler handler) {
			return addDomHandler(handler, MouseWheelEvent.getType());
		}

		// below methods don't do anything... just there b/c interface

		@Override
		public String getHTML() {
			return null;
		}

		@Override
		public void setHTML(String html) {
		}

		@Override
		public String getText() {
			return null;
		}

		@Override
		public void setText(String text) {
		}

		@Override
		public void setHTML(SafeHtml html) {
		}
	}

	// TODO ////////////////////////////////////////////////////////////

	/**
	 * A child class of DialogBox that has a button in the Caption for hiding.
	 * This one does not have any text in the caption/title bar.
	 */
	public IbDialogBox(final HPanelCaptionImpl caption) {
		super(false, false, caption);
		caption.setIbDialogBox(this);
	}

	/**
	 * A child class of DialogBox that has a button in the Caption for hiding.
	 * 
	 * @param captionText
	 */
	public IbDialogBox(final String captionText) {
		this(new HPanelCaptionImpl(captionText));
	}

	/**
	 * Bring this window to the front (zIndex).
	 */
	public void bringToFront() {
		this.hide();
		this.show();
	}

	/**
	 * Hide this dialogBox. Calls hide(). This is called when the "close" is
	 * clicked.
	 */
	protected void hideDialogBox() {
		this.hide();
	}
}
