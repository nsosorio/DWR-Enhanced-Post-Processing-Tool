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
import javax.swing.*;

/**
 * CalLite GUI version of the main panel the main frame of Calsim GUI. Modified
 * from MainPanel.java by removing menu bar and hiding MessagePanel (Tad
 * Slaweck)
 *
 * @author Yan-Ping Zuo, Nicky Sandhu
 * @version $Id: MainPanel.java,v 1.1.4.86 2001/07/12 01:59:47 amunevar Exp $
 */

public class CalLiteGUIMainPanel extends JPanel
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1355255475366672451L;
	public static boolean DEBUG = true;
	/**
	 *
	 */
	private GeneralRetrievePanel _retrievePanel;
	/*
	 * private variables
	 */
	private MessagePanel _messagePanel;
	private NodeArcMenuBar _nodeArcMenuBar;
	private JTabbedPane _tabbedPane;
	private DtsTreePanel _dtsTreePanel;

	/**
	 * constructor Add layout active messages panel, schematic icon, and status
	 * panel to the main panel. parameters: frame in which the panel will be
	 * displayed
	 */
	public CalLiteGUIMainPanel()
	{

		setBackground(new Color(229, 240, 203));
		setLayout(new BorderLayout(5, 5));

		// Create the main menu and the message panel, but DO NOT display
		_messagePanel = new MessagePanel();

		_tabbedPane = new JTabbedPane();
		setRetrievePanel();
		setDtsTreePanel();
		// set selected to general retrieve
		_tabbedPane.setSelectedIndex(0);
		add(_tabbedPane, BorderLayout.CENTER);

		this.setSize(800, 600);


	}

	public GeneralRetrievePanel getRetrievePanel()
	{
		return _retrievePanel;
	}

	public void setRetrievePanel()
	{
		int i = _tabbedPane.indexOfTab("General");
		if(i == -1)
		{
			_retrievePanel = new GeneralRetrievePanel();
			_tabbedPane.addTab("General", null, _retrievePanel,
					"General Retrieve Panel");
			i = _tabbedPane.indexOfTab("General");

		}
		_tabbedPane.setSelectedIndex(i);
	}

	public void setDtsTreePanel()
	{
		int i = -1; // _tabbedPane.indexOfTab("Dts Tree");
		if(i == -1)
		{
			_dtsTreePanel = new DtsTreePanel();
			_tabbedPane.addTab("Dts Tree", null, _dtsTreePanel,
					"Dts Tree Panel");
			i = _tabbedPane.indexOfTab("Dts Tree");
		}
		_tabbedPane.setSelectedIndex(i);
	}

	JButton getOpenDtsButton()
	{
		return _dtsTreePanel.getTable().getOpenCurrentButton();
	}

	/**
	 * Return the message panel object in the main panel
	 */
	public MessagePanel getMessagePanel()
	{
		return _messagePanel;
	}

	/**
	 * Return the Node/Arc menu bar object in the main panel
	 */
	public JMenuBar getNodeArcMenuBar()
	{ // CB Not called from anywhere
		return _nodeArcMenuBar.getMenuBar(); // CB weird technique
	}

	/**
	 * Return the Main menu bar object in the main panel
	 */
	//	public MainMenuBar getMainMenuBar()
	//	{
	//		return _mainMenuBar;
	//	}
	public JTabbedPane getTabbedPane()
	{
		return _tabbedPane;
	}

	public DtsTreePanel getDtsTreePanel()
	{
		return _dtsTreePanel;
	}
} // end of class MainPanel
/*
 * $Log: MainPanel.java,v $ Revision 1.1.4.86 2001/07/12 01:59:47 amunevar
 * removal of all unneeded files for cleanup
 *
 * Revision 1.1.4.85 2001/04/18 21:07:47 jfenolio dts tree added, start month
 * selection for table display
 *
 * Revision 1.1.4.84 2000/12/20 20:07:21 amunevar commit for ver 1.0.7
 *
 * Revision 1.1.4.83 1999/08/05 23:07:40 nsandhu** empty log message ***
 *
 * Revision 1.1.4.82 1999/07/20 22:25:13 zuo separate status panel from main
 * Panel
 *
 * Revision 1.1.4.81 1999/07/20 18:39:39 zuo removed schematic tab
 *
 * Revision 1.1.4.80 1999/07/20 16:52:57 zuo added status panel to frame
 *
 * Revision 1.1.4.79 1999/07/19 19:18:18 zuo add StudyPanel.java
 *
 * Revision 1.1.4.78 1999/07/18 20:56:49 nsandhu** empty log message ***
 *
 * Revision 1.1.4.77 1999/07/02 22:13:54 nsandhu first move to eliminate extra
 * frames on screen
 *
 * Revision 1.1.4.76 1999/07/02 21:27:18 zuo add tabbed pane
 *
 * Revision 1.1.4.75 1999/07/02 20:13:40 nsandhu** empty log message ***
 */
