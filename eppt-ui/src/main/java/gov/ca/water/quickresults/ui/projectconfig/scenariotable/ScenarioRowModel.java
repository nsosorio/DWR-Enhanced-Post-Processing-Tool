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

package gov.ca.water.quickresults.ui.projectconfig.scenariotable;

import java.util.Collections;
import java.util.Objects;
import java.util.logging.Logger;

import gov.ca.water.calgui.bo.GUILinksAllModelsBO;
import gov.ca.water.calgui.project.EpptDssContainer;
import gov.ca.water.calgui.project.EpptScenarioRun;
import gov.ca.water.calgui.project.NamedDssPath;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

import com.rma.javafx.treetable.columns.specs.TreeTableColumnSpec;

import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.ALTERNATIVE_COL_SPEC;
import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.BASE_COL_SPEC;
import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.DESCRIPTION_COL_SPEC;
import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.MODEL_COL_SPEC;
import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.NAME_COL_SPEC;
import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.OUTPUT_PATH_COL_SPEC;
import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.WATER_YEAR_PATH_COL_SPEC;
import static gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTableModel.WRESL_MAIN_COL_SPEC;

/**
 * Company: Resource Management Associates
 *
 * @author <a href="mailto:adam@rmanet.com">Adam Korynta</a>
 * @since 05-07-2019
 */
class ScenarioRowModel extends ScenarioTableRowModel
{
	private final EpptScenarioRun _scenarioRun;
	private final SimpleObjectProperty<Boolean> _baseProperty;
	private final SimpleObjectProperty<Boolean> _alternativeProperty;
	private final SimpleObjectProperty<String> _namedProperty;
	private final SimpleObjectProperty<String> _descriptionProperty;
	private final SimpleObjectProperty<GUILinksAllModelsBO.Model> _modelProperty;
	private final SimpleObjectProperty<String> _outputProperty;
	private final SimpleObjectProperty<String> _wreslMainProperty;
	private final SimpleObjectProperty<String> _waterYearTableProperty;
	private final ScenarioTableModel _scenarioRunTableModel;

	ScenarioRowModel(Runnable modified, ScenarioTableModel scenarioRunTableModel, EpptScenarioRun scenarioRun)
	{
		super(null);
		_scenarioRunTableModel = scenarioRunTableModel;
		_scenarioRun = scenarioRun;
		_namedProperty = new SimpleObjectProperty<>(scenarioRun.getName());
		_descriptionProperty = new SimpleObjectProperty<>(scenarioRun.getDescription());
		_modelProperty = new SimpleObjectProperty<>(scenarioRun.getModel());
		_outputProperty = new SimpleObjectProperty<>(Objects.toString(scenarioRun.getOutputPath()));
		_wreslMainProperty = new SimpleObjectProperty<>(Objects.toString(scenarioRun.getWreslDirectory()));
		_waterYearTableProperty = new SimpleObjectProperty<>(Objects.toString(scenarioRun.getLookupDirectory()));
		_baseProperty = new SimpleObjectProperty<>(scenarioRun.isBaseSelected());
		_baseProperty.addListener((e, o, n) ->
		{

			scenarioRun.setBaseSelected(n);
			modified.run();
			if(n)
			{
				ScenarioProjectUpdater.updateWithAllDssFiles(Collections.singletonList(scenarioRun));
			}
		});
		_alternativeProperty = new SimpleObjectProperty<>(scenarioRun.isAltSelected());
		_alternativeProperty.addListener((e, o, n) ->
		{
			scenarioRun.setAltSelected(n);
			modified.run();
		});
		addDssPathChildren();
	}

	@Override
	public Node getCellGraphic(TreeTableColumnSpec spec)
	{
		if(spec == NAME_COL_SPEC)
		{
			return new Rectangle(12, 12, _scenarioRun.getColor());
		}
		else
		{
			return super.getCellGraphic(spec);
		}
	}

	private void addDssPathChildren()
	{
		EpptDssContainer dssContainer = _scenarioRun.getDssContainer();
		NamedDssPath dvDssFile = dssContainer.getDvDssFile();
		if(dvDssFile != null)
		{
			getChildren().add(new DssPathRow(this, dvDssFile, "DV"));
		}
		NamedDssPath svDssFile = dssContainer.getSvDssFile();
		if(svDssFile != null)
		{
			getChildren().add(new DssPathRow(this, svDssFile, "SV"));
		}
		NamedDssPath ivDssFile = dssContainer.getIvDssFile();
		if(ivDssFile != null)
		{
			getChildren().add(new DssPathRow(this, ivDssFile, "INIT"));
		}
		NamedDssPath dtsDssFile = dssContainer.getDtsDssFile();
		if(dtsDssFile != null)
		{
			getChildren().add(new DssPathRow(this, dtsDssFile, "QA_QC"));
		}
		dssContainer.getExtraDssFiles().forEach(child -> getChildren().add(new DssPathRow(this, child, "Extra")));
	}

	EpptScenarioRun getScenarioRun()
	{
		return _scenarioRun;
	}

	@Override
	public ObservableValue<?> getObservableValue(TreeTableColumnSpec spec)
	{
		ObservableValue<?> retval = null;
		if(spec == NAME_COL_SPEC)
		{
			retval = _namedProperty;
		}
		else if(spec == DESCRIPTION_COL_SPEC)
		{
			retval = _descriptionProperty;
		}
		else if(spec == MODEL_COL_SPEC)
		{
			retval = _modelProperty;
		}
		else if(spec == BASE_COL_SPEC)
		{
			retval = _baseProperty;
		}
		else if(spec == ALTERNATIVE_COL_SPEC)
		{
			retval = _alternativeProperty;
		}
		else if(spec == WRESL_MAIN_COL_SPEC)
		{
			retval = _wreslMainProperty;
		}
		else if(spec == OUTPUT_PATH_COL_SPEC)
		{
			retval = _outputProperty;
		}
		else if(spec == WATER_YEAR_PATH_COL_SPEC)
		{
			retval = _waterYearTableProperty;
		}
		return retval;
	}

	boolean isAlternative()
	{
		return _alternativeProperty.get();
	}

	boolean isBase()
	{
		return _baseProperty.get();
	}

	@Override
	public boolean isEditable(TreeTableColumnSpec columnSpec)
	{
		boolean retval = false;
		if(columnSpec == BASE_COL_SPEC)
		{
			if(_baseProperty.get())
			{
				retval = true;
			}
			else if(!_alternativeProperty.get())
			{
				retval = noOtherBaseDefined();
			}
		}
		else if(columnSpec == ALTERNATIVE_COL_SPEC)
		{
			if(_alternativeProperty.get())
			{
				retval = true;
			}
			else
			{
				retval = !_baseProperty.get();
			}
		}
		return retval;
	}

	private boolean noOtherBaseDefined()
	{
		return _scenarioRunTableModel.getBaseScenarioRun() == null;
	}

}
