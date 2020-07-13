/*
 * Enhanced Post Processing Tool (EPPT) Copyright (c) 2019.
 *
 * EPPT is copyrighted by the State of California, Department of Water Resources. It is licensed
 * under the GNU General Public License, version 2. This means it can be
 * copied, distributed, and modified freely, but you may not restrict others
 * in their ability to copy, distribute, and modify it. See the license below
 * for more details.
 *
 * GNU General Public License
 */

package calsim.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;

import calsim.app.AppUtils;
import vista.gui.VistaUtils;
import vista.set.DataReference;
import vista.set.Group;
import vista.set.GroupTableModel;
import vista.set.PartNamePredicate;
import vista.set.Pathname;
import vista.set.SortMechanism;

//import java.io.*;

/**
 * The split pane that let user select A/B/C/D/E/F parts individually, and
 * display the searched pathnames in a list, and let user plot/tabulate the
 * data.
 *
 * @author Nicky Sandhu, Armin Munevar
 * @version $Id: GeneralRetrievePanel.java,v 1.1.2.9 2001/10/23 16:28:38
 * jfenolio Exp $
 */
public class GeneralRetrievePanel extends JPanel
{
	public static final int NUM_PATH_PARTS = 6;
	public static boolean DEBUG = false;
	private static boolean[] ascVals = new boolean[Pathname.MAX_PARTS];
	private JTextField[] _pathText;
	private JComboBox _varTypeBox;
	private JButton _retrieveBtn;
	private Group _group;
	private JTable _table;
	private JPanel _upperPanel, _lowerPanel;
	private JButton _filterBtn;

	/**
	 * constructor
	 */
	public GeneralRetrievePanel()
	{
		_upperPanel = createUpperPanel();
		_lowerPanel = createLowerPanel();
		setLayout(new BorderLayout());
		setBackground(new Color(207, 220, 200));
		add(_upperPanel, BorderLayout.NORTH);
		add(_lowerPanel, BorderLayout.CENTER);
	}

	/**
	 * create the upper panel to let user select path parts
	 */
	JPanel createUpperPanel()
	{
		// create filter panel
		JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new GridLayout(1, 7));
		filterPanel.setBackground(new Color(207, 220, 200));
		_varTypeBox = new JComboBox(new String[]{AppUtils.DVAR, AppUtils.SVAR});
		_varTypeBox.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.lightGray), "TYPE"));
		_pathText = new JTextField[6];
		_pathText[0] = new JTextField(30);
		_pathText[0].setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.lightGray), "A"));
		_pathText[1] = new JTextField(30);
		_pathText[1].setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.lightGray), "B"));
		_pathText[2] = new JTextField(30);
		_pathText[2].setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.lightGray), "C"));
		_pathText[3] = new JTextField(30);
		_pathText[3].setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.lightGray), "D"));
		_pathText[4] = new JTextField(30);
		_pathText[4].setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.lightGray), "E"));
		_pathText[5] = new JTextField(30);
		_pathText[5].setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.lightGray), "F"));
		for(JTextField textField : _pathText)
		{
			textField.addActionListener(e ->
			{
				for(ActionListener listener : _filterBtn.getActionListeners())
				{
					listener.actionPerformed(new ActionEvent(GeneralRetrievePanel.this, 0, "Enter Filter"));
				}
			});
		}
		//
		for(int i = 0; i < NUM_PATH_PARTS; i++)
		{
			filterPanel.add(_pathText[i]);
		}
		filterPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.blue), "Filter"));
		//
		_filterBtn = new JButton("Filter");
		_retrieveBtn = new JButton("Display");
		//
		_filterBtn.addActionListener(new GuiTaskListener("Filtering...")
		{
			public void doWork()
			{
				filter();
			}
		});
		_retrieveBtn.addActionListener(new GuiTaskListener("Retrieving data...")
		{
			public void doWork()
			{
				retrieve();
			}

		});
		Box btnPanel = new Box(BoxLayout.X_AXIS);
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.add(_filterBtn);
		btnPanel.add(_retrieveBtn);
		btnPanel.add(Box.createHorizontalGlue());
		//
		Box filterBox = new Box(BoxLayout.Y_AXIS);
		filterBox.add(Box.createVerticalGlue());
		filterBox.add(filterPanel);
		filterBox.add(Box.createVerticalGlue());
		//
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createVerticalGlue());
		box.add(filterBox);
		box.add(btnPanel);
		box.add(Box.createVerticalGlue());
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalGlue());
		box1.add(box);
		box1.add(Box.createHorizontalGlue());
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new BorderLayout());
		upperPanel.setBackground(new Color(207, 220, 200));
		upperPanel.add(box1, BorderLayout.CENTER);
		return upperPanel;
	}

	/**
	 * create the lower panel to display the matched path names and allow user
	 * view the data
	 */
	JPanel createLowerPanel()
	{
		// Create the list of pathnames and put it in a scroll pane
		_table = new JTable();
		_table.getTableHeader()
			  .addMouseListener(new TableHeaderMouseListener());
		_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		_table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		if(_table.getSelectedRowCount() > 0)
		{
			_table.setRowSelectionInterval(0, 0);
		}
		_table.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent me)
			{
				if(me.getClickCount() < 2)
				{
					return;
				}
				_retrieveBtn.doClick();
			}
		});
		//
		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BorderLayout());
		lowerPanel.setBackground(new Color(207, 220, 200));
		lowerPanel.add(new JScrollPane(_table), BorderLayout.CENTER);
		return lowerPanel;
	}

	/**
	 * search the path names associate with the path parts, and display them in
	 * the lower panel
	 */
	void filter()
	{
		if(DEBUG)
		{
			System.out.println("filter");
		}
		if(!AppUtils.baseOn)
		{
			JOptionPane.showMessageDialog(null,
					"The Base DSS files need to be selected",
					"DSS Not Selected", JOptionPane.WARNING_MESSAGE);
			return;
		}
		// store filter on parts in array or null if empty string
		String[] parts = getStringParts();
		// filter and get array of desired references depending upon varible
		// type
		try
		{
			// if out of date recatalog...
			AppUtils.getCurrentProject().getDVGroup(); // recatalogs
			String selected;
			// set table to these references
			updateTable(AppUtils.getDataReferences(parts,
					selected = (String) _varTypeBox.getSelectedItem()));
			AppUtils.isDvarsFilter = selected.equals(AppUtils.DVAR);
		}
		catch(RuntimeException re)
		{
			VistaUtils.displayException(this, re);
		}
	}

	public String[] getStringParts()
	{
		String[] parts = new String[NUM_PATH_PARTS];
		for(int i = 0; i < NUM_PATH_PARTS; i++)
		{
			String txt = _pathText[i].getText().trim().toUpperCase();
			if(txt.length() > 0)
			{
				parts[i] = txt;
			}
			else
			{
				parts[i] = null;
			}
		}
		return parts;
	}

	/**
	 *
	 */
	public void updateTable(DataReference[] refs)
	{
		if(refs == null || refs.length == 0)
		{
			return;
		}
		_group = Group.createGroup("filtered", refs);
		_table.setModel(new GroupTableModel(_group));
		_table.sizeColumnsToFit(-1);
		_table.revalidate();
		_table.repaint();
	}

	/**
	 * retrieve and display(plot/tabulate) the selected data
	 */
	void retrieve()
	{
		if(!AppUtils.baseOn)
		{
			JOptionPane.showMessageDialog(null,
					"The Base DSS files need to be selected",
					"DSS Not Selected", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try
		{
			String noRowsString = "";
			if(_table.getRowCount() == 0)
			{
				noRowsString = " after using \"Filter\" to load variables";
			}
			if(_group == null || _table.getSelectedRowCount() == 0)
			{
				JOptionPane.showMessageDialog(null,
						"Select one or more variables" + noRowsString,
						"Variable(s) Not Selected",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			int[] rows = _table.getSelectedRows(); // checked if count > 0 above
			DataReference[] array = new DataReference[rows.length];
			for(int i = 0; i < rows.length; i++)
			{
				array[i] = _group.getDataReference(rows[i]);
			}
			GuiUtils.displayData(array);
		}
		catch(Exception e)
		{
			VistaUtils.displayException(GuiUtils.getMainPanel(), e);
		}
	}

	public JButton getRetrieveBtn()
	{
		return _retrieveBtn;
	}

	public JButton getFilterBtn()
	{
		return _filterBtn;
	}


	public JTable getTable()
	{
		return _table;
	}

	public Group getGroup()
	{
		return _group;
	}

	/**
	 * Sorts table by hits on header
	 *
	 * @author Nicky Sandhu
	 * @version $Id: GeneralRetrievePanel.java,v 1.1.2.9 2001/10/23 16:28:38
	 * jfenolio Exp $
	 */
	private class TableHeaderMouseListener extends MouseAdapter
	{

		public TableHeaderMouseListener()
		{
			for(int i = 0; i < ascVals.length; i++)
			{
				ascVals[i] = true;
			}
		}

		/**
		 *
		 */
		public void mouseClicked(MouseEvent e)
		{
			TableColumnModel columnModel = GeneralRetrievePanel.this._table
					.getColumnModel();
			int viewColumn = columnModel.getColumnIndexAtX(e.getX());
			int column = GeneralRetrievePanel.this._table
					.convertColumnIndexToModel(viewColumn);
			if(column <= 0)
			{
				return;
			}
			column = column - 1;
			if(e.getClickCount() == 1 && column != -1)
			{
				SortMechanism sortMechanism = null;
				sortMechanism = new PartNamePredicate(column,
						SortMechanism.INCREASING);
				sortMechanism.setAscendingOrder(ascVals[column]);
				GeneralRetrievePanel.this._group.sortBy(sortMechanism);
				GeneralRetrievePanel.this._table
						.tableChanged(new TableModelEvent(
								GeneralRetrievePanel.this._table.getModel(), 0,
								GeneralRetrievePanel.this._table.getModel()
																.getRowCount()));
				GeneralRetrievePanel.this._table.repaint();
				ascVals[column] = !ascVals[column];
			}
		}
	} // end of tableheadermouselistener
}
/*
 * $Log: GeneralRetrievePanel.java,v $ Revision 1.1.2.9 2001/10/23 16:28:38
 * jfenolio Changes made are: 1) Bug Fixes to the dts, mts, and the dts tree 2)
 * MSRGui added and MSR modifications made 3) Changes to option pane including
 * new controller for multi-year position analysis 4) Output tab changes include
 * 4 different dss files can be viewed and other modifications to the message
 * panel and menus
 *
 * Revision 1.1.2.8 2000/12/20 20:07:08 amunevar commit for ver 1.0.7
 *
 * Revision 1.1.2.7 1999/10/13 16:46:43 nsandhu ** empty log message ***
 *
 * Revision 1.1.2.6 1999/08/18 19:50:24 nsandhu ** empty log message ***
 *
 * Revision 1.1.2.5 1999/07/25 19:25:00 nsandhu fixed repaint bug
 *
 * Revision 1.1.2.4 1999/07/08 00:42:27 nsandhu ** empty log message ***
 *
 * Revision 1.1.2.3 1999/07/02 20:26:26 nsandhu ** empty log message ***
 */
