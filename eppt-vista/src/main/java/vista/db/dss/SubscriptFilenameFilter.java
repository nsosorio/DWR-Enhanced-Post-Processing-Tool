/*
 * Copyright (c) 2019
 * California Department of Water Resources
 * All Rights Reserved.  DWR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from DWR
 */
package vista.db.dss;

import java.io.File;
import java.io.FilenameFilter;

/**
 * filters accepting anything with the subscript specified in the constructor
 * 
 * @author Nicky Sandhu
 * @version $Id: SubscriptFilenameFilter.java,v 1.1 2003/10/02 20:48:46 redwood
 *          Exp $
 */
class SubscriptFilenameFilter implements FilenameFilter, java.io.Serializable {
	/**
   *
   */
	public SubscriptFilenameFilter(String subscript) {
		_subscript = subscript.toLowerCase();
	}

	/**
	 * Tests if a specified file should be included in a file list.
	 * 
	 * @param dir
	 *            the directory in which the file was found.
	 * @param name
	 *            the name of the file.
	 * @return <code>true</code> if the name should be included in the file
	 *         list; <code>false</code> otherwise.
	 * @since JDK1.0
	 */
	public boolean accept(File dir, String name) {
		return name.toLowerCase().endsWith(_subscript);
	}

	/**
   *
   */
	private String _subscript;
}
