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

package gov.ca.water.calgui.project;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Company: Resource Management Associates
 *
 * @author <a href="mailto:adam@rmanet.com">Adam Korynta</a>
 * @since 04-01-2019
 */
public class EpptDssContainer
{
	private final NamedDssPath _dvDssFile;
	private final NamedDssPath _svDssFile;
	private final NamedDssPath _ivDssFile;
	private final List<NamedDssPath> _extraDssFiles;

	public EpptDssContainer(NamedDssPath dvDssFile, NamedDssPath svDssFile, NamedDssPath ivDssFile, List<NamedDssPath> extraDssFiles)
	{

		_dvDssFile = dvDssFile;
		_svDssFile = svDssFile;
		_ivDssFile = ivDssFile;
		_extraDssFiles = Collections.unmodifiableList(extraDssFiles);
	}

	/**
	 * @return path to the Derived Variable DSS file path
	 */
	public NamedDssPath getDvDssFile()
	{
		return _dvDssFile;
	}

	/**
	 * @return path to the State Variable DSS file path
	 */
	public NamedDssPath getSvDssFile()
	{
		return _svDssFile;
	}

	public NamedDssPath getIvDssFile()
	{
		return _ivDssFile;
	}

	/**
	 * @return unmodifiable list of extra DSS file paths
	 */
	public List<NamedDssPath> getExtraDssFiles()
	{
		return _extraDssFiles;
	}

}