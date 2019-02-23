/*
 * Copyright (c) 2019
 * California Department of Water Resources
 * All Rights Reserved.  DWR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from DWR
 */
package vista.app;

import java.awt.GridLayout;
import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import vista.db.dss.DSSData;
import vista.db.dss.DSSDataReader;
import vista.db.dss.DSSUtil;
import vista.set.DataReference;
import vista.time.TimeWindow;

/**
 * 
 * 
 * @author Nicky Sandhu
 * @version $Id: StatsDisplayPanel.java,v 1.1 2003/10/02 20:48:42 redwood Exp $
 */
@SuppressWarnings("serial")
public class DetailedInfoPanel extends JPanel {
	public DetailedInfoPanel(DataReference ref) {
		if (ref == null)
			throw new IllegalArgumentException(
					"Null data set cannot have stats");
		setLayout(new GridLayout(2, 1));
		String filename = ref.getFilename();
		String pathname = ref.getPathname().getFullPath();
		TimeWindow tw = DSSUtil.createTimeWindow(ref.getPathname());
		int st = (int) tw.getStartTime().getTimeInMinutes();
		int et = (int) tw.getEndTime().getTimeInMinutes();
		DSSDataReader reader = new DSSDataReader();
		int recType = reader.recordType(filename, pathname);
		DSSData data =  reader.getData(filename, pathname, st, et,
				false);
		setLayout(new MigLayout("wrap 2"));
		add(new JLabel("Record Type")); add(new JLabel(""+recType));
		add(new JLabel("Data Offset")); add(new JLabel(""+data._offset));
		add(new JLabel("Number Read")); add(new JLabel(""+data._numberRead));
		add(new JLabel("X Type")); add(new JLabel(""+data._xType));
		add(new JLabel("Y Type")); add(new JLabel(""+data._yType));
		add(new JLabel("X Units")); add(new JLabel(""+data._xUnits));
		add(new JLabel("Y Units")); add(new JLabel(""+data._yUnits));
	}
}