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

package gov.ca.water.reportengine.detailedissues;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import com.google.common.flogger.FluentLogger;
import gov.ca.water.calgui.bo.GUILinksAllModelsBO;
import gov.ca.water.calgui.bo.ThresholdLinksBO;
import gov.ca.water.calgui.busservice.impl.DSSGrabber1SvcImpl;
import gov.ca.water.calgui.busservice.impl.GuiLinksSeedDataSvcImpl;
import gov.ca.water.calgui.busservice.impl.ThresholdLinksSeedDataSvc;
import gov.ca.water.calgui.project.EpptScenarioRun;
import gov.ca.water.reportengine.EpptReportException;
import gov.ca.water.reportengine.executivereport.FlagViolation;
import gov.ca.water.reportengine.executivereport.Module;
import gov.ca.water.reportengine.executivereport.SubModule;
import gov.ca.water.reportengine.reportreaders.WaterYearTableReader;
import gov.ca.water.reportengine.reportreaders.WaterYearType;

import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import hec.lang.Const;

import static java.util.stream.Collectors.toList;

public class DetailedIssueProcessor
{

	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

	private final List<DetailedIssue> _baseDetailedIssues = new ArrayList<>();
	private final List<DetailedIssue> _altDetailedIssues = new ArrayList<>();

	private final Map<EpptScenarioRun, Map<SubModule, List<FlagViolation>>> _runsToViolations;
	private final List<Module> _modules;
	private final List<DetailedIssue> _allDetailedIssues;
	private final List<EpptScenarioRun> _runs;
	private final boolean _isCFS;
	private final List<FlagViolation> _baseViolations = new ArrayList<>();
	private final List<FlagViolation> _altViolations = new ArrayList<>();

	private final List<WaterYearType> _waterYearTypes;

	public DetailedIssueProcessor(Path waterYearTypeTable, Path waterYearNameLookup,
								  Map<EpptScenarioRun, Map<SubModule, List<FlagViolation>>> runsToViolations, List<Module> modules,
								  List<DetailedIssue> allDetailedIssues, List<EpptScenarioRun> runs, boolean isCFS) throws EpptReportException
	{
		WaterYearTableReader wyTypeReader = new WaterYearTableReader(waterYearTypeTable, waterYearNameLookup);
		_waterYearTypes = wyTypeReader.read();

		_runsToViolations = runsToViolations;
		_modules = modules;
		_allDetailedIssues = allDetailedIssues;
		_runs = runs;
		_isCFS = isCFS;
	}

	public Map<EpptScenarioRun, Map<Module, List<DetailedIssueViolation>>> process()
	{
		Map<EpptScenarioRun, Map<Module, List<DetailedIssueViolation>>> moduleToDIV = new HashMap<>();

		for(EpptScenarioRun run : _runs)
		{
			if(_runsToViolations.containsKey(run))
			{
				LOGGER.at(Level.INFO).log("Processing Detailed Issues for Scenario Run: %s", run.getName());
				Map<Module, List<DetailedIssueViolation>> modToDIVs = new HashMap<>();
				Map<SubModule, List<FlagViolation>> subModToViolations = _runsToViolations.get(run);
				for(Module mod : _modules)
				{
					LOGGER.at(Level.INFO).log("Processing Detailed Issues for Module: %s", mod.getName());
					modToDIVs.put(mod, processModule(run, subModToViolations, mod));
				}
				moduleToDIV.put(run, modToDIVs);
			}
			else
			{
				LOGGER.at(Level.INFO).log("No Detailed Issues for: %s", run.getName());
			}
		}

		return moduleToDIV;
	}

	private List<DetailedIssueViolation> processModule(EpptScenarioRun run,
													   Map<SubModule, List<FlagViolation>> subModToViolations, Module mod)
	{
		List<DetailedIssueViolation> dIVsForMod = new ArrayList<>();

		for(SubModule subMod : mod.getSubModules())
		{
			if(subModToViolations.containsKey(subMod))
			{
				LOGGER.at(Level.INFO).log("Processing Detailed Issues for Sub-Module: %s", subMod.getName());
				dIVsForMod.addAll(subModToViolations.get(subMod)
													.stream()
													.map(violation -> processFlagViolation(run, violation, subMod))
													.filter(Objects::nonNull)
													.collect(toList()));
			}
			else
			{
				LOGGER.at(Level.INFO).log("No Detailed Issues for: %s", subMod.getName());
			}
		}
		return dIVsForMod;
	}

	private DetailedIssueViolation processFlagViolation(EpptScenarioRun run, FlagViolation violation, SubModule subMod)
	{
		DetailedIssueViolation div = null;
		DetailedIssue di = getDetailedIssueThatMatchViolation(violation);
		if(di != null)
		{
			if(Const.isValid(di.getGuiLink()) && Const.isValid(di.getThresholdLink()))
			{
				div = createDetailedIssueViolation(violation, di.getGuiLink(), di.getThresholdLink(), run, subMod);
			}
			else
			{
				LOGGER.at(Level.WARNING).log(
						"Could not create a detailed issue violation because the gui link or the threshold link was not a valid number for %s",
						di.getLinkedVar());
			}
		}
		return div;
	}


	private DetailedIssueViolation createDetailedIssueViolation(FlagViolation violation, int guiID, int thresholdID, EpptScenarioRun run, SubModule subMod)
	{
		//create title
		GUILinksAllModelsBO guiLink = GuiLinksSeedDataSvcImpl.getSeedDataSvcImplInstance().getObjById(Integer.toString(guiID));
		ThresholdLinksBO thresholdLink = ThresholdLinksSeedDataSvc.getSeedDataSvcImplInstance().getObjById(thresholdID);
		String title = subMod.getTitle();
		if(guiLink != null)
		{
			title = title.replace("%title%", guiLink.getPlotTitle());
		}
		if(thresholdLink != null)
		{
			title = title.replace("%Threshold Label%", thresholdLink.getLabel());
		}

		if(Objects.equals("", title))
		{
			title = "Undefined";
		}

		DSSGrabber1SvcImpl grabber1Svc = buildDssGrabber(run, guiID, thresholdID);

		//get the specific container for the threshold values
		TimeSeriesContainer[] thresholdTimeSeries = grabber1Svc.getThresholdTimeSeries();
		TimeSeriesContainer thresholdSeriesContainer = null;
		if(thresholdTimeSeries != null && thresholdTimeSeries.length > 0)
		{
			thresholdSeriesContainer = thresholdTimeSeries[0];
		}

		//get the specific container for the values
		TimeSeriesContainer[] primarySeries = grabber1Svc.getPrimarySeries();

		TimeSeriesContainer valueContainer = null;
		if(primarySeries != null && primarySeries.length > 0)
		{
			valueContainer = primarySeries[0];
		}
		LOGGER.at(Level.INFO).log("Processing violations from: %s", violation.getDtsFileName());
		List<HecTime> violationTimes = violation.getTimes();
		if(violationTimes.size() > 10)
		{
			LOGGER.at(Level.INFO).log("Showing first 10 violations. Total violation count: %s", violationTimes.size());
		}
		List<HecTime> times = violationTimes
									   .stream()
									   .limit(10)
									   .collect(toList());
		String valueUnits = "";
		if(valueContainer != null)
		{
			valueUnits = valueContainer.getUnits();
		}
		String standardUnits = "";
		if(thresholdSeriesContainer != null)
		{
			standardUnits = thresholdSeriesContainer.getUnits();
		}
		Map<HecTime, Double> actualValues = getActualValues(times, valueContainer);
		Map<HecTime, Double> thresholdValues = getThresholdValues(times, thresholdSeriesContainer);
		Map<HecTime, String> waterYearTypes = getWaterYearTypes(times);
		return new DetailedIssueViolation(times, title, actualValues, thresholdValues, waterYearTypes, valueUnits, standardUnits);
	}

	private Map<HecTime, Double> getActualValues(List<HecTime> times, TimeSeriesContainer tsc)
	{
		Map<HecTime, Double> values = new HashMap<>();
		if(tsc != null)
		{
			for(HecTime time : times)
			{
				double value = tsc.getValue(time);
				values.put(time, value);
			}
		}
		return values;
	}

	private Map<HecTime, Double> getThresholdValues(List<HecTime> times, TimeSeriesContainer tsc)
	{
		Map<HecTime, Double> thresholds = new HashMap<>();
		if(tsc != null)
		{
			for(HecTime time : times)
			{
				double threshold = tsc.getValue(time);
				thresholds.put(time, threshold);
			}
		}
		return thresholds;
	}

	private Map<HecTime, String> getWaterYearTypes(List<HecTime> times)
	{
		Map<HecTime, String> types = new HashMap<>();
		for(HecTime time : times)
		{
			String waterYearTypeString = "Undefined";
			for(WaterYearType wyt : _waterYearTypes)
			{
				if(wyt.getYear() == time.year())
				{
					waterYearTypeString = wyt.getWaterYearType();
					break;
				}
			}
			types.put(time, waterYearTypeString);
		}

		return types;
	}

	private DSSGrabber1SvcImpl buildDssGrabber(EpptScenarioRun epptScenarioRun, int guiID, int thresholdId)
	{
		DSSGrabber1SvcImpl grabber1Svc = new DSSGrabber1SvcImpl();
		grabber1Svc.setIsCFS(_isCFS);
		grabber1Svc.setScenarioRuns(epptScenarioRun, Collections.emptyList());
		grabber1Svc.setLocation(Integer.toString(guiID));
		grabber1Svc.setThresholdId(thresholdId);
		return grabber1Svc;
	}

	//private List<EpptScenarioRun> getAltRuns(List<EpptScenarioRun> allRuns)
	//{
	//    List<EpptScenarioRun> altRuns = new ArrayList<>();
	//    if(allRuns.size()>1)
	//    {
	//        for(int i = 1;i<allRuns.size();i++)
	//        {
	//            altRuns.add(allRuns.get(i));
	//        }
	//    }
	//    return altRuns;
	//}
	//
	//
	//
	//
	//
	//    private Map<EpptScenarioRun, TimeSeriesContainer> getActualValues(int guiID, EpptScenarioRun baseRun, List<EpptScenarioRun> altRuns, boolean isCFS)
	//    {
	//        Map<EpptScenarioRun, TimeSeriesContainer> runToTimeSeriesContainer = new HashMap<>();
	//        DetailedIssueViolation div = null;
	//        String title = "Untitled Measurement";
	//        if (Const.isValid(guiID))
	//        {
	//            GUILinksAllModelsBO objById = GuiLinksSeedDataSvcImpl.getSeedDataSvcImplInstance().getObjById(Integer.toString(guiID));
	//            Map<GUILinksAllModelsBO.Model, String> primary = objById.getPrimary();
	//            title = objById.getPlotTitle();
	//            String bAndCPart = primary.get(baseRun.getModel());
	//
	//
	//            DSSGrabber1SvcImpl grabber1Svc = new DSSGrabber1SvcImpl();
	//            grabber1Svc.setIsCFS(isCFS);
	//            grabber1Svc.setScenarioRuns(baseRun, altRuns);
	//            grabber1Svc.setLocation(Integer.toString(guiID), baseRun.getModel());
	//            //grabber1Svc.setDateRange(); figure this out at some point
	//            TimeSeriesContainer[] primarySeries = grabber1Svc.getPrimarySeries();
	//
	//            runToTimeSeriesContainer.put(baseRun, primarySeries[0]);
	//
	//            for(int i = 1;i<primarySeries.length;i++)
	//            {
	//                runToTimeSeriesContainer.put(altRuns.get(i-1),primarySeries[i] );
	//
	//            }
	//
	//            //title
	//            //list of violations?
	//                //date, wateryeartype, cfs, standard
	//
	//           // div = new DetailedIssueViolation(violation.getTimes(), title, primarySeries);
	//        }
	//        return runToTimeSeriesContainer;
	//    }
	//
	//    private Map<EpptScenarioRun, TimeSeriesContainer> getThreshold(FlagViolation violation, int thresholdId, EpptScenarioRun baseRun, List<EpptScenarioRun> altRuns, boolean isCFS)
	//    {
	//        Map<EpptScenarioRun, TimeSeriesContainer> runToTimeSeriesContainer = new HashMap<>();
	//        DetailedIssueViolation div = null;
	//        String title = "Untitled Measurement";
	//        if (Const.isValid(thresholdId))
	//        {
	//            GUILinksAllModelsBO objById = GuiLinksSeedDataSvcImpl.getSeedDataSvcImplInstance().getObjById(Integer.toString(thresholdId));
	//            Map<GUILinksAllModelsBO.Model, String> primary = objById.getPrimary();
	//            title = objById.getPlotTitle();
	//            String bAndCPart = primary.get(baseRun.getModel());
	//
	//
	//            DSSGrabber1SvcImpl grabber1Svc = new DSSGrabber1SvcImpl();
	//            grabber1Svc.setIsCFS(isCFS);
	//            grabber1Svc.setScenarioRuns(baseRun, altRuns);
	//            grabber1Svc.setLocation(Integer.toString(thresholdId), baseRun.getModel());
	//            //grabber1Svc.setDateRange(); figure this out at some point
	//            TimeSeriesContainer[] primarySeries = grabber1Svc.getPrimarySeries();
	//
	//            runToTimeSeriesContainer.put(baseRun, primarySeries[0]);
	//
	//            for(int i = 1;i<primarySeries.length;i++)
	//            {
	//                runToTimeSeriesContainer.put(altRuns.get(i-1),primarySeries[i] );
	//
	//            }
	//
	//            //title
	//            //list of violations?
	//            //date, wateryeartype, cfs, standard
	//
	//            div = new DetailedIssueViolation(violation.getTimes(), title, primarySeries);
	//        }
	//        return runToTimeSeriesContainer;
	//    }

	private DetailedIssue getDetailedIssueThatMatchViolation(FlagViolation violation)
	{
		for(DetailedIssue di : _allDetailedIssues)
		{
			if(Objects.equals(di.getLinkedVar(), violation.getDtsFileName()))
			{
				return di;
			}
		}
		return null;
	}

	//    private List<DetailedIssue> getDetailedIssuesForSubModID(int id)
	//    {
	//        List<DetailedIssue> retval = new ArrayList<>();
	//        for(DetailedIssue di : _allDetailedIssues)
	//        {
	//            if(di.getSubModuleID() == id)
	//            {
	//                retval.add(di);
	//            }
	//        }
	//        return retval;
	//    }


	//
	//    private List<FlagViolation> getAltFlagViolationsFromModule(Module mod, String altName)
	//    {
	//        List<SubModule> subModules = mod.getSubModules();
	//        for(SubModule subMod : subModules)
	//        {
	//            subMod.getBaseViolations()
	//        }
	//    }


	//    private List<DetailedIssue> getBaseDetailedIssuesFromModules()
	//    {
	//        Map<Module, List<DetailedIssue>> moduleToIssues = new HashMap<>();
	//
	//        List<DetailedIssue> detailedIssues = new ArrayList<>();
	//        for (Module mod : _modules)
	//        {
	//            List<SubModule> subModules = mod.getSubModules();
	//            for (SubModule subMod : subModules)
	//            {
	//                List<FlagViolation> baseViolations = subMod.getBaseViolations();
	//                for (FlagViolation violation : baseViolations)
	//                {
	//
	//                    String dtsFileName = violation.getDtsFileName();
	//                    for (DetailedIssue di : _baseDetailedIssues)
	//                    {
	//                        if (Objects.equals(di.getLinkedVar(), dtsFileName))
	//                        {
	//
	//                        }
	//                    }
	//
	//                    violation.get
	//                    DetailedIssue di = new DetailedIssue(subMod.getId(), violation.getTimes(), dtsFileName);
	//                    detailedIssues.add(di);
	//                    //dtsFileNames.add(violation.getDtsFileName());
	//                }
	//            }
	//        }
	//        return detailedIssues;
	//    }


}