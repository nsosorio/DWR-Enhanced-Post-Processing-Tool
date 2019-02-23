/*
 * Copyright (c) 2019
 * California Department of Water Resources
 * All Rights Reserved.  DWR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from DWR
 */

package vista.app;

import javax.swing.*;

/**
 * Just a way to force it to set page width??
 * 
 */
public class ReportPane extends JTextPane {
	/**
    *
    */
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}
}
