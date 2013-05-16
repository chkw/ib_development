/*
 * InteractionBrowser
 * 
 * Master's Thesis by Greg Dougherty
 * Created: Aug 13, 2007
 * 
 * Copyright 2007 by Greg Dougherty
 * License to be determined.
 */

package edu.ucsc.ib.client.datapanels;

import edu.ucsc.ib.drawpanel.client.Shape;

/**
 * @author Greg Dougherty
 * Interface used by a MouseSelectListener so that we can have panels that
 * get clicks in "empty" areas.
 */
public interface MousePanelListener
{
	public void	addSelectBox (Shape b);
	public void selectInBox (double x1, double y1, double x2, double y2);
}
