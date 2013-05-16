package edu.ucsc.ib.client.netviz;

import com.google.gwt.user.client.Event;

/**
 * Interface for handling context click events. Context click events are events
 * where <code>DOM.eventGetType(event)</code> returns
 * <code>Event.ONCONTEXTMENU</code>, or <code>262144</code>. In order to handle
 * such events, an object must use <code>sinkEvents(Event.ONCONTEXTMENU);</code>
 * .
 * 
 * @author cw
 * 
 */
public interface ContextClickHandler {

	/**
	 * Handle a context click event.
	 * 
	 * @param event
	 */
	void handleContextClick(Event event);
}
