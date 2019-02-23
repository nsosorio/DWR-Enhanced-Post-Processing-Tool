/*
 * Copyright (c) 2019
 * California Department of Water Resources
 * All Rights Reserved.  DWR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from DWR
 */
package vista.app;

import vista.gui.Command;
import vista.gui.ExecutionException;
import vista.set.Group;
import vista.set.Session;

/**
 * Encapsulates commands implementing session related commands
 * 
 * @author Nicky Sandhu
 * @version $Id: IntersectionGroupCommand.java,v 1.1 2003/10/02 20:48:33 redwood
 *          Exp $
 */
class IntersectionGroupCommand implements Command {
	private Session _session;
	private Group _gIntersection;
	private int[] _indices;

	/**
	 * opens session and sets current session to
	 */
	public IntersectionGroupCommand(Session s, int[] indices) {
		_session = s;
		_indices = indices;
	}

	/**
	 * executes command
	 */
	public void execute() throws ExecutionException {
		if (_session == null)
			throw new ExecutionException(getClass().getName() + ": "
					+ "Command on null session");
		if (_indices == null || _indices.length == 0)
			throw new ExecutionException(getClass().getName() + ": "
					+ "Command on null indices");
		_gIntersection = _session.getGroup(0);
		for (int i = 0; i < _indices.length; i++) {
			_gIntersection = _gIntersection.intersectionWith(_session
					.getGroup(_indices[i]));
		}
		_session.addGroup(_gIntersection);
	}

	/**
	 * unexecutes command or throws exception if not unexecutable
	 */
	public void unexecute() throws ExecutionException {
		_session.removeGroup(_gIntersection);
	}

	/**
	 * checks if command is executable.
	 */
	public boolean isUnexecutable() {
		return true;
	}

	/**
	 * writes to script
	 */
	public void toScript(StringBuffer buf) {
	}
} // end of IntersectionGroupCommand
