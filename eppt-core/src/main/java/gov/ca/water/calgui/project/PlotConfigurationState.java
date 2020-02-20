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

import java.time.Month;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.ca.water.calgui.bo.WaterYearDefinition;
import gov.ca.water.calgui.bo.WaterYearIndex;
import gov.ca.water.calgui.busservice.impl.EpptStatistic;
import gov.ca.water.calgui.busservice.impl.ScriptedEpptStatistics;
import gov.ca.water.calgui.busservice.impl.WaterYearIndexAliasReader;

import static java.util.stream.Collectors.toList;

public class PlotConfigurationState
{

	private static final Logger LOGGER = Logger.getLogger(PlotConfigurationState.class.getName());
	private final ComparisonType _comparisonType;
	private final boolean _isDisplayTaf;
	private final boolean _isDisplayTimeSeriesPlot;
	private final boolean _isDisplayBoxAndWhiskerPlot;
	private final boolean _isDisplayMonthlyTable;
	private final boolean _isDisplaySummaryTable;
	private final boolean _isDisplayExceedanceAll;
	private final boolean _isDisplayExceedanceAnnualFlow;
	private final List<Month> _exceedancePlotMonths;
	private final List<EpptStatistic> _summaryTableComponents;
	private final WaterYearDefinition _waterYearDefinition;
	private final List<WaterYearIndexAliasReader.WaterYearIndexAlias> _summaryWaterYearIndex;

	public PlotConfigurationState(ComparisonType comparisonType, boolean isDisplayTaf, boolean isDisplayTimeSeriesPlot,
								  boolean isDisplayBoxAndWhiskerPlot,
								  boolean isDisplayExceedanceAll, List<Month> exceedancePlotMonths,
								  boolean isDisplayMonthlyTable, boolean isDisplaySummaryTable,
								  boolean isDisplayExceedanceAnnualFlow, List<EpptStatistic> summaryTableComponents,
								  List<WaterYearIndexAliasReader.WaterYearIndexAlias> summaryWaterYearIndex,
								  WaterYearDefinition waterYearDefinition)
	{
		_comparisonType = comparisonType;
		_isDisplayTaf = isDisplayTaf;
		_isDisplayTimeSeriesPlot = isDisplayTimeSeriesPlot;
		_isDisplayBoxAndWhiskerPlot = isDisplayBoxAndWhiskerPlot;
		_isDisplayExceedanceAll = isDisplayExceedanceAll;
		_exceedancePlotMonths = exceedancePlotMonths;

		_isDisplayMonthlyTable = isDisplayMonthlyTable;
		_isDisplaySummaryTable = isDisplaySummaryTable;
		_isDisplayExceedanceAnnualFlow = isDisplayExceedanceAnnualFlow;

		_summaryTableComponents = summaryTableComponents;
		_waterYearDefinition = waterYearDefinition;
		_summaryWaterYearIndex = summaryWaterYearIndex;
	}

	public WaterYearDefinition getWaterYearDefinition()
	{
		return _waterYearDefinition;
	}

	public static PlotConfigurationState fromString(String displayGroup)
	{
		boolean doTimeSeries = false;
		boolean doBoxPlot = false;
		boolean isCFS = false;
		boolean doMonthlyTable = false;
		boolean doSummaryTable = false;
		boolean doExceedanceAll = false;
		boolean doExceedanceAnnual = false;
		List<Month> exceedMonths = new ArrayList<>();
		List<EpptStatistic> summaryTags = new ArrayList<>();

		String[] groupParts = displayGroup.split(";");

		ComparisonType comparisonType = ComparisonType.BASE;
		List<EpptStatistic> trendStatistics = ScriptedEpptStatistics.getTrendStatistics();
		for(final String groupPart : groupParts)
		{
			if("Base".equals(groupPart))
			{
				comparisonType = ComparisonType.BASE;
			}
			if("Comp".equals(groupPart))
			{
				comparisonType = ComparisonType.COMPARISON;
			}
			else if("Diff".equals(groupPart))
			{
				comparisonType = ComparisonType.DIFF;
			}
			else if("TS".equals(groupPart))
			{
				doTimeSeries = true;
			}
			else if("BP".equals(groupPart))
			{
				doBoxPlot = true;
			}
			else if(groupPart.startsWith("EX-"))
			{
				String exceedanceParts = groupPart.substring(3);
				addToExceedanceMonths(exceedMonths, exceedanceParts);
				doExceedanceAll = exceedanceParts.toLowerCase().contains("all");
				doExceedanceAnnual = exceedanceParts.toLowerCase().contains("annual");
			}
			else if("CFS".equals(groupPart))
			{
				isCFS = true;
			}
			else if("TAF".equals(groupPart))
			{
				isCFS = false;
			}
			else if("Monthly".equals(groupPart))
			{
				doMonthlyTable = true;
			}
			else if(groupPart.startsWith("ST-"))
			{
				doSummaryTable = true;
				summaryTags = trendStatistics.stream().filter(s->groupPart.toLowerCase().contains(s.getName().toLowerCase())).collect(toList());
			}
		}
		return new PlotConfigurationState(comparisonType, !isCFS, doTimeSeries, doBoxPlot, doExceedanceAll, exceedMonths, doMonthlyTable, doSummaryTable,
				doExceedanceAnnual, summaryTags, new ArrayList<>(), new WaterYearDefinition("", Month.OCTOBER, Month.SEPTEMBER));
	}

	private static void addToExceedanceMonths(List<Month> exceedMonths, String exceedanceParts)
	{
		for(String exceedanceDescriptor : exceedanceParts.split(","))
		{
			try
			{
				TemporalAccessor mmm = new DateTimeFormatterBuilder()
						.parseCaseInsensitive()
						.appendPattern("MMM")
						.toFormatter()
						.parse(exceedanceDescriptor);
				exceedMonths.add(Month.from(mmm));
			}
			catch(DateTimeParseException e)
			{
				LOGGER.log(Level.FINE, "Unable to parse month: " + exceedanceDescriptor, e);
			}
		}
	}

	public ComparisonType getComparisonType()
	{
		return _comparisonType;
	}

	public boolean isDisplayTaf()
	{
		return _isDisplayTaf;
	}

	public boolean isDisplayTimeSeriesPlot()
	{
		return _isDisplayTimeSeriesPlot;
	}

	public boolean isDisplayBoxAndWhiskerPlot()
	{
		return _isDisplayBoxAndWhiskerPlot;
	}

	public boolean isDisplayMonthlyTable()
	{
		return _isDisplayMonthlyTable;
	}

	public boolean isDisplaySummaryTable()
	{
		return _isDisplaySummaryTable;
	}

	public boolean isPlotAllExceedancePlots()
	{
		return _isDisplayExceedanceAll;
	}

	public boolean isAnnualFlowExceedancePlots()
	{
		return _isDisplayExceedanceAnnualFlow;
	}

	public List<Month> getSelectedExceedancePlotMonths()
	{
		return _exceedancePlotMonths;
	}

	public List<EpptStatistic> getSelectedSummaryTableItems()
	{
		return _summaryTableComponents;
	}

	public boolean isDoExceedance()
	{
		return !getSelectedExceedancePlotMonths().isEmpty() || isAnnualFlowExceedancePlots() || isPlotAllExceedancePlots();
	}

	public List<WaterYearIndexAliasReader.WaterYearIndexAlias> getSummaryWaterYearIndexes()
	{
		return _summaryWaterYearIndex;
	}

	public enum ComparisonType
	{
		BASE, COMPARISON, DIFF
	}


}
