/* * Copyright (c) 2019 * California Department of Water Resources * All Rights Reserved.  DWR PROPRIETARY/CONFIDENTIAL. * Source may not be released without written approval from DWR */package vista.report;import java.awt.BorderLayout;import javax.swing.*;/** *  * A default frame understands MPanel's. It queries *  * the panel for its menu bar and adds it to itself. *  *  *  * @author Nicky Sandhu *  * @version $Id: DefaultFrame.java,v 1.1 2003/10/02 20:49:17 redwood Exp $ */public class DefaultFrame extends JFrame {	/**	 * 	 * creates a frame with the given MPanel and sets	 * 	 * its menu bar accordingly	 * 	 * @see MPanel	 */	public DefaultFrame(MPanel panel) {		getContentPane().setLayout(new BorderLayout());		setMPanel(panel);	}	/**	 * 	 * sets the main panel to this MPanel	 * 	 * @see MPanel	 */	public void setMPanel(MPanel panel) {		getContentPane().removeAll();		getContentPane().add(panel);		setJMenuBar(panel.getJMenuBar());		setTitle(panel.getFrameTitle());	}}