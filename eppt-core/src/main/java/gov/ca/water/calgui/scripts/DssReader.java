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

package gov.ca.water.calgui.scripts;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.flogger.FluentLogger;
import gov.ca.water.calgui.bo.DetailedIssue;
import gov.ca.water.calgui.bo.ThresholdLinksBO;
import gov.ca.water.calgui.busservice.impl.DSSGrabber1SvcImpl;
import gov.ca.water.calgui.busservice.impl.DetailedIssuesReader;
import gov.ca.water.calgui.busservice.impl.ThresholdLinksSeedDataSvc;
import gov.ca.water.calgui.project.EpptScenarioRun;
import gov.ca.water.calgui.project.NamedDssPath;
import vista.set.TimeSeries;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDss;
import hec.heclib.util.HecTime;
import hec.io.DataContainer;
import hec.io.TimeSeriesContainer;

/**
 * Company: Resource Management Associates
 *
 * @author <a href="mailto:adam@rmanet.com">Adam Korynta</a>
 * @since 08-14-2019
 */
public class DssReader
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private static final Pattern DSS_PATH_SPLITTER_PATTERN = Pattern.compile("/");
	private final EpptScenarioRun _scenarioRun;

	public DssReader(EpptScenarioRun scenarioRun)
	{
		_scenarioRun = scenarioRun;
	}

	public NavigableMap<LocalDateTime, Double> getGuiLinkData(int guiID)
	{
		DSSGrabber1SvcImpl dssGrabber1Svc = buildDssGrabber(_scenarioRun, guiID, 0);
		TimeSeriesContainer[] primarySeries = dssGrabber1Svc.getPrimarySeries();
		return timeSeriesContainerToMap(primarySeries);
	}

	private NavigableMap<LocalDateTime, Double> timeSeriesContainerToMap(TimeSeriesContainer[] primarySeries)
	{
		NavigableMap<LocalDateTime, Double> retval = new TreeMap<>();
		if(primarySeries != null && primarySeries[0] != null)
		{
			TimeSeriesContainer tsc = primarySeries[0];
			if(tsc.times != null)
			{
				for(int i = 0; i < tsc.times.length; i++)
				{
					HecTime hecTime = new HecTime();
					hecTime.set(tsc.times[i], tsc.timeGranularitySeconds, tsc.julianBaseDate);
					double value = tsc.getValue(i);
					int offset = (int) TimeUnit.MILLISECONDS.toMinutes(TimeZone.getDefault().getRawOffset());
					Date javaDate = hecTime.getJavaDate(offset);
					LocalDateTime localDateTime = LocalDateTime.ofInstant(javaDate.toInstant(), ZoneId.systemDefault());
					retval.put(localDateTime, value);
				}
			}
		}
		return retval;
	}

	private DSSGrabber1SvcImpl buildDssGrabber(EpptScenarioRun epptScenarioRun, int guiID, int thresholdId)
	{
		DSSGrabber1SvcImpl grabber1Svc = new DSSGrabber1SvcImpl();
		grabber1Svc.setScenarioRuns(epptScenarioRun, Collections.emptyList());
		grabber1Svc.setLocation(Integer.toString(guiID));
		grabber1Svc.setThresholdId(thresholdId);
		return grabber1Svc;
	}

	public NavigableMap<LocalDateTime, Double> getDtsData(int guiID)
	{
		NavigableMap<LocalDateTime, Double> retval = new TreeMap<>();
		String bPart = DetailedIssuesReader.getInstance().getDetailedIssues()
										   .stream()
										   .filter(di -> di.getDetailedIssueId() == guiID)
										   .map(DetailedIssue::getLinkedVar)
										   .findAny()
										   .orElse("");
		NamedDssPath dtsDssFile = _scenarioRun.getDssContainer().getDtsDssFile();
		String aPart = dtsDssFile.getAPart();
		String fPart = dtsDssFile.getFPart();
		String ePart = dtsDssFile.getEPart();
		DSSPathname pathname = new DSSPathname();
		pathname.setAPart(aPart);
		pathname.setFPart(fPart);
		pathname.setEPart(ePart);
		pathname.setBPart(bPart);
		pathname.setCPart("*");
		String fileName = dtsDssFile.getDssPath().toString();
		HecDss hecDss = null;
		try
		{
			hecDss = HecDss.open(fileName);
			String dssPath = hecDss.getCatalogedPathnames()
								 .stream()
								 .filter(Objects::nonNull)
								 .map(s -> new DSSPathname(s.toString()))
								 .filter(d -> ((DSSPathname) d).getAPart().equalsIgnoreCase(aPart))
								   .filter(d -> ((DSSPathname) d).getBPart().equalsIgnoreCase(bPart))
								 .filter(d -> ((DSSPathname) d).getFPart().equalsIgnoreCase(fPart))
								 .findAny()
								 .orElse(new DSSPathname().toString())
								 .toString();
			DataContainer dataContainer = hecDss.get(dssPath);
			if(dataContainer instanceof TimeSeriesContainer)
			{
				retval = timeSeriesContainerToMap(new TimeSeriesContainer[]{(TimeSeriesContainer)dataContainer});
			}
		}
		catch(Exception e)
		{
			LOGGER.atSevere().withCause(e).log("Unable to read DSS file: %s", fileName);
		}
		finally
		{
			if(hecDss != null)
			{
				hecDss.close();
			}
		}
		return retval;
	}

	public NavigableMap<LocalDateTime, Double> getThresholdData(int thresholdId)
	{
		DSSGrabber1SvcImpl dssGrabber1Svc = buildDssGrabber(_scenarioRun, 102, thresholdId);
		TimeSeriesContainer[] threshold = dssGrabber1Svc.getThresholdTimeSeries();
		return timeSeriesContainerToMap(threshold);
	}
}