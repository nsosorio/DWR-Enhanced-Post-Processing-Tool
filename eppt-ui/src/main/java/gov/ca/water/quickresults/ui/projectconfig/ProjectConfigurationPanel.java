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

package gov.ca.water.quickresults.ui.projectconfig;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatter;

import gov.ca.water.calgui.EpptInitializationException;
import gov.ca.water.calgui.bo.ResultUtilsBO;
import gov.ca.water.calgui.bo.WaterYearDefinition;
import gov.ca.water.calgui.bo.WaterYearIndex;
import gov.ca.water.calgui.busservice.ScenarioChangeListener;
import gov.ca.water.calgui.busservice.impl.WaterYearDefinitionSvc;
import gov.ca.water.calgui.busservice.impl.WaterYearTableReader;
import gov.ca.water.calgui.constant.Constant;
import gov.ca.water.calgui.constant.EpptPreferences;
import gov.ca.water.calgui.project.EpptProject;
import gov.ca.water.calgui.project.EpptScenarioRun;
import gov.ca.water.calgui.project.EpptScenarioRunValidator;
import gov.ca.water.calgui.project.PlotConfigurationState;
import gov.ca.water.quickresults.ui.EpptPanel;
import gov.ca.water.quickresults.ui.customresults.CustomResultsPanel;
import gov.ca.water.quickresults.ui.dataanalysis.DataAnalysisPanel;
import gov.ca.water.quickresults.ui.projectconfig.scenarioconfig.ScenarioRunEditor;
import gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioProjectUpdater;
import gov.ca.water.quickresults.ui.projectconfig.scenariotable.ScenarioTablePanel;
import gov.ca.water.quickresults.ui.quickresults.PlotConfigurationStateBuilder;
import gov.ca.water.quickresults.ui.quickresults.QuickResultsPanel;
import gov.ca.water.quickresults.ui.report.QAQCReportPanel;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import rma.util.RMAUtil;

/**
 * Company: Resource Management Associates
 *
 * @author <a href="mailto:adam@rmanet.com">Adam Korynta</a>
 * @since 02-21-2019
 */
public final class ProjectConfigurationPanel extends EpptPanel
{
	private static final Logger LOGGER = Logger.getLogger(ProjectConfigurationPanel.class.getName());
	private static final String SCENARIO_CONFIGURATION_XML_FILE = "Project_Configuration.xml";
	private static ProjectConfigurationPanel instance;
	private final Set<ScenarioChangeListener> _scenarioChangeListeners = new HashSet<>();
	private final ProjectConfigurationIO _projectConfigurationIO = new ProjectConfigurationIO();
	private final ScenarioTablePanel _scenarioTablePanel = new ScenarioTablePanel();
	private boolean _ignoreModifiedEvents = false;

	private ProjectConfigurationPanel()
	{
		try
		{
			super.setLayout(new BorderLayout());
			Container swixmlProjectConfigurationPanel = renderSwixml(SCENARIO_CONFIGURATION_XML_FILE);
			super.add(swixmlProjectConfigurationPanel, BorderLayout.CENTER);
			initComponents();
			initModels();
			initListeners();
		}
		catch(Exception e)
		{
			LOGGER.error("Error setting up quick results swing xml: " + SCENARIO_CONFIGURATION_XML_FILE, e);
			throw new IllegalStateException(e);
		}
	}

	/**
	 * This method is for unit testing purposes only
	 *
	 * @return a new ProjectConfigurationPanel with UI initialized
	 */
	static ProjectConfigurationPanel createProjectConfigurationPanel()
	{
		return new ProjectConfigurationPanel();
	}

	public static synchronized ProjectConfigurationPanel getProjectConfigurationPanel()
	{
		if(instance == null)
		{
			synchronized(ProjectConfigurationPanel.class)
			{
				if(instance == null)
				{
					instance = new ProjectConfigurationPanel();
				}
			}
		}
		return instance;
	}

	private void initComponents()
	{
		initScenarioTree();
		revalidate();
	}

	private void initScenarioTree()
	{
		Component component = getSwingEngine().find("ScenarioTree");
		if(component instanceof JPanel)
		{
			JPanel treePanel = (JPanel) component;
			treePanel.setLayout(new BorderLayout());
			treePanel.add(_scenarioTablePanel, BorderLayout.CENTER);
		}
	}

	private void setSummaryTableEnabled(boolean selected, Container container)
	{
		container.setEnabled(selected);
		for(Component component : container.getComponents())
		{
			component.setEnabled(selected);
			if(component instanceof Container)
			{
				setSummaryTableEnabled(selected, (Container) component);
			}
		}
	}

	private void initModels()
	{
		initializeSpinners();
		initializeCombos();
	}

	private void initializeCombos()
	{
		JComboBox waterYearDefinitionCombo = (JComboBox) getSwingEngine().find("waterYearDefinitionCombo");
		WaterYearDefinitionSvc.getWaterYearDefinitionSvc().getDefinitions().forEach(waterYearDefinitionCombo::addItem);
	}

	private void initListeners()
	{
		JCheckBox summaryTableCheckbox = (JCheckBox) getSwingEngine().find("RepckbSummaryTable");
		summaryTableCheckbox.addActionListener(e ->
		{
			boolean selected = summaryTableCheckbox.isSelected();
			Container controls3 = (Container) getSwingEngine().find("controls3");
			setSummaryTableEnabled(selected, controls3);
		});

		DocumentListener documentListener = new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				ProjectConfigurationPanel.this.setModified(true);
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				ProjectConfigurationPanel.this.setModified(true);
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				ProjectConfigurationPanel.this.setModified(true);
			}
		};
		JTextField projectNameField = (JTextField) getSwingEngine().find("prj_name");
		JTextField descriptionField = (JTextField) getSwingEngine().find("prj_desc");
		projectNameField.getDocument().addDocumentListener(documentListener);
		descriptionField.getDocument().addDocumentListener(documentListener);
		JSpinner spnSY = (JSpinner) getSwingEngine().find("spnStartYear");
		spnSY.addChangeListener(e -> setModified(true));
		JSpinner spnEY = (JSpinner) getSwingEngine().find("spnEndYear");
		spnEY.addChangeListener(e -> setModified(true));
		JCheckBox tafCheckBox = ((JCheckBox) getSwingEngine().find("chkTAF"));
		tafCheckBox.addActionListener(e -> setModified(true));
	}

	private void initializeSpinners()
	{
		// Set up year spinners
		JSpinner spnSY = (JSpinner) getSwingEngine().find("spnStartYear");
		ResultUtilsBO.SetNumberModelAndIndex(spnSY, 1921, 1921, 2003, 1, "####", null, true);
		JSpinner spnEY = (JSpinner) getSwingEngine().find("spnEndYear");
		ResultUtilsBO.SetNumberModelAndIndex(spnEY, 2003, 1921, 2003, 1, "####", null, true);
		makeSpinnerCommitOnEdit(spnSY);
		makeSpinnerCommitOnEdit(spnEY);
		JFormattedTextField textField2 = ((JSpinner.DefaultEditor) spnSY.getEditor()).getTextField();
		textField2.addKeyListener(new MyKeyAdapter(textField2));
		JFormattedTextField textField3 = ((JSpinner.DefaultEditor) spnEY.getEditor()).getTextField();
		textField3.addKeyListener(new MyKeyAdapter(textField3));
	}

	private void makeSpinnerCommitOnEdit(JSpinner spinner)
	{
		JComponent comp = spinner.getEditor();
		if(comp.getComponents().length > 0)
		{
			Component component = comp.getComponent(0);
			if(component instanceof JFormattedTextField)
			{
				JFormattedTextField field = (JFormattedTextField) component;
				JFormattedTextField.AbstractFormatter formatter = field.getFormatter();
				if(formatter instanceof DefaultFormatter)
				{
					DefaultFormatter defaultFormatter = (DefaultFormatter) formatter;
					defaultFormatter.setCommitsOnValidEdit(true);
				}
			}
		}
	}

	@Override
	public String getJavaHelpId()
	{
		return "3.1_ProjectConfiguration.htm";
	}

	JPanel getControls2()
	{
		return (JPanel) getSwingEngine().find("controls2");
	}


	void updateRadioState()
	{
		SwingUtilities.invokeLater(() ->
		{
			EpptScenarioRun baseScenario = getBaseScenario();
			updateWaterYearIndexCombo(baseScenario);
			_scenarioChangeListeners.forEach(this::postScenarioChanged);
		});
	}

	private void updateWaterYearIndexCombo(EpptScenarioRun baseScenario)
	{
		JComboBox waterYearIndexCombo = (JComboBox) getSwingEngine().find("waterYearIndexCombo");
		waterYearIndexCombo.removeAllItems();
		if(baseScenario != null)
		{
			WaterYearTableReader waterYearTableReader = new WaterYearTableReader(baseScenario.getLookupDirectory());
			try
			{
				waterYearTableReader.read().forEach(waterYearIndexCombo::addItem);
			}
			catch(EpptInitializationException e)
			{
				LOGGER.fatal("Error reading water year index for scenario: " + baseScenario, e);
			}
		}
	}

	void clearAllScenarios()
	{
		if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(SwingUtilities.windowForComponent(this),
				"Are you sure you want to delete all Scenario Runs?\nThis operation cannot be undone.",
				"Clear All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE))
		{
			_scenarioTablePanel.clearScenarios();
			setModified(true);
		}
	}

	public Path getSelectedDssPath()
	{
		return _scenarioTablePanel.getSelectedDssFile();
	}

	private void postScenarioChanged(ScenarioChangeListener scenarioChangeListener)
	{
		EpptScenarioRun base = _scenarioTablePanel.getBaseScenarioRun();
		List<EpptScenarioRun> alternatives = _scenarioTablePanel.getAlternativeScenarioRuns();
		if(scenarioChangeListener != null)
		{
			try
			{
				scenarioChangeListener.fillScenarioRuns(base, alternatives);
			}
			catch(RuntimeException e)
			{
				LOGGER.error(e);
			}
		}
	}

	private void checkValidScenarioRuns()
	{
		for(EpptScenarioRun epptScenarioRun : _scenarioTablePanel.getAllScenarioRuns())
		{
			EpptScenarioRunValidator validator = new EpptScenarioRunValidator(epptScenarioRun);
			if(!validator.isValid())
			{
				StringBuilder builder = new StringBuilder("Scenario Run: ")
						.append(epptScenarioRun.getName())
						.append(" is invalid. Would you like to edit?");
				validator.getErrors().forEach(s -> builder.append("\n").append(s));
				Frame frame = Frame.getFrames()[0];
				int response = JOptionPane.showConfirmDialog(frame, builder.toString(), "Misconfigured Scenario Run",
						JOptionPane.YES_NO_OPTION);
				if(response == JOptionPane.YES_OPTION)
				{
					replaceScenarioDialog(epptScenarioRun, frame);
				}
			}
		}
	}

	private void replaceScenarioDialog(EpptScenarioRun epptScenarioRun, Frame frame)
	{
		try
		{
			SwingUtilities.invokeAndWait(() ->
			{
				ScenarioRunEditor scenarioRunEditor = new ScenarioRunEditor(frame);
				scenarioRunEditor.fillPanel(epptScenarioRun);
				scenarioRunEditor.setVisible(true);
				if(!scenarioRunEditor.isCanceled())
				{
					replaceScenario(epptScenarioRun, scenarioRunEditor.createRun());
				}
			});
		}
		catch(InterruptedException e)
		{
			LOGGER.info("Thread interrupted", e);
			Thread.currentThread().interrupt();
		}
		catch(InvocationTargetException e)
		{
			LOGGER.error("Error updating Scenario run: " + epptScenarioRun, e);
		}
	}

	public void addScenarioChangedListener(ScenarioChangeListener scenarioChangeListener)
	{
		_scenarioChangeListeners.add(scenarioChangeListener);
	}

	void deleteScenario()
	{
		EpptScenarioRun selectedScenario = _scenarioTablePanel.getSelectedScenario();
		if(selectedScenario != null)
		{
			int clear = JOptionPane.showConfirmDialog(SwingUtilities.windowForComponent(this),
					"Are you sure you want to delete Scenario Runs: " + selectedScenario
							+ "?\nThis operation cannot be undone.",
					"Clear", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(JOptionPane.YES_OPTION == clear)
			{
				_scenarioTablePanel.deleteSelectedScenarioRun();
				setModified(true);
			}
		}
	}

	public String quickStateString()
	{
		PlotConfigurationStateBuilder builder = new PlotConfigurationStateBuilder(getSwingEngine());
		return builder.createQuickStateString();
	}

	public PlotConfigurationState plotConfigurationState()
	{
		PlotConfigurationStateBuilder builder = new PlotConfigurationStateBuilder(getSwingEngine());
		return builder.createPlotConfigurationState();
	}

	@Override
	public void setModified(boolean b)
	{
		if(!_ignoreModifiedEvents)
		{
			updateRadioState();
			super.setModified(b);
			RMAUtil.setParentModified(this);
		}
	}

	public YearMonth getStartMonth()
	{
		WaterYearDefinition waterYearDefinition = getWaterYearDefinition();
		JSpinner yearSpinner = (JSpinner) getSwingEngine().find("spnStartYear");
		int year = Integer.parseInt(yearSpinner.getValue().toString());
		return YearMonth.of(year, waterYearDefinition.getStartMonth());
	}

	private void setStartYear(int start)
	{
		JSpinner yearSpinner = (JSpinner) getSwingEngine().find("spnStartYear");
		yearSpinner.setValue(start);
	}

	public void saveConfigurationToPath(Path selectedPath, String projectName, String projectDescription)
			throws IOException
	{
		boolean mkdirs = selectedPath.getParent().toFile().mkdirs();
		if(!mkdirs)
		{
			LOGGER.debug("Path not created: " + selectedPath);
		}
		_projectConfigurationIO.saveConfiguration(selectedPath, projectName, projectDescription);
		EpptPreferences.setLastProjectConfiguration(selectedPath);
	}

	public void saveAsConfigurationToPath(Path newProjectPath, String projectName, String projectDescription)
			throws IOException
	{
		newProjectPath.toFile().mkdirs();
		Path projectFile = newProjectPath.resolve(projectName + "." + Constant.EPPT_EXT);
		Path oldProjectPath = EpptPreferences.getLastProjectConfiguration().getParent();
		_scenarioTablePanel.relativizeScenariosToNewProject(newProjectPath, oldProjectPath);
		_projectConfigurationIO.saveConfiguration(projectFile, projectName, projectDescription);
		EpptPreferences.setLastProjectConfiguration(projectFile);
	}

	public void loadProjectConfiguration(Path selectedPath) throws IOException
	{
		try
		{
			_ignoreModifiedEvents = true;
			if(selectedPath.toFile().exists())
			{
				loadProjectFile(selectedPath);
			}
			else
			{
				loadDefaultProject();
			}
		}
		catch(RuntimeException ex)
		{
			LOGGER.fatal("Error loading project configuration", ex);
		}
		finally
		{
			_ignoreModifiedEvents = false;
		}
	}

	private void loadProjectFile(Path selectedPath) throws IOException
	{
		EpptProject project = _projectConfigurationIO.loadConfiguration(
				selectedPath);
		EpptPreferences.setLastProjectConfiguration(selectedPath);
		JTextField projectNameField = (JTextField) getSwingEngine().find("prj_name");
		JTextField descriptionField = (JTextField) getSwingEngine().find("prj_desc");
		projectNameField.setText(project.getName());
		descriptionField.setText(project.getDescription());
		setStartYear(project.getStartYear());
		setEndYear(project.getEndYear());
		updateSelectedComponents(project.getSelectedComponents());
		_scenarioTablePanel.clearScenarios();
		List<EpptScenarioRun> scenarioRuns = project.getScenarioRuns();
		boolean hasBase = scenarioRuns.stream().anyMatch(EpptScenarioRun::isBaseSelected);
		boolean hasAlt = scenarioRuns.stream().anyMatch(EpptScenarioRun::isAltSelected);

		if(!hasBase && !scenarioRuns.isEmpty())
		{
			scenarioRuns.get(0).setBaseSelected(true);
			updateWaterYearIndexCombo(scenarioRuns.get(0));
		}
		if(!hasAlt && scenarioRuns.size() > 1)
		{
			scenarioRuns.get(1).setAltSelected(true);
		}
		addScenarios(scenarioRuns);
		//Need to ensure this is called after scenarios are added to TreeTable model
		Platform.runLater(() ->
		{
			ScenarioProjectUpdater.updateWithAllDssFiles();
			SwingUtilities.invokeLater(this::updateRadioState);
		});
	}

	private void loadDefaultProject()
	{
		try
		{
			String defaultProjectName = "EPPT Project";
			JTextField projectNameField = (JTextField) getSwingEngine().find("prj_name");
			projectNameField.setText(defaultProjectName);
			saveAsConfigurationToPath(EpptPreferences.getProjectsPath().resolve(defaultProjectName),
					defaultProjectName, "");
		}
		catch(IOException e)
		{
			LOGGER.error("Unable to create default project", e);
		}
	}

	public void addScenarios(List<EpptScenarioRun> scenarioRuns)
	{
		scenarioRuns.forEach(_scenarioTablePanel::addScenarioRun);
		Platform.runLater(() ->
		{
			checkValidScenarioRuns();
			SwingUtilities.invokeLater(ProjectConfigurationPanel.this::updateRadioState);
		});
	}

	private void updateSelectedComponents(Map<String, Boolean> selectedComponents)
	{
		for(Map.Entry<String, Boolean> entry : selectedComponents.entrySet())
		{
			String key = entry.getKey();
			Boolean selected = entry.getValue();
			Component component = getSwingEngine().find(key);
			if(component instanceof JCheckBox)
			{
				((JCheckBox) component).setSelected(selected);
			}
			else if(component instanceof JRadioButton)
			{
				((JRadioButton) component).setSelected(selected);
			}
		}
	}

	public boolean isTaf()
	{
		return ((JCheckBox) getSwingEngine().find("chkTAF")).isSelected();
	}

	public YearMonth getEndMonth()
	{
		WaterYearDefinition waterYearDefinition = getWaterYearDefinition();
		JSpinner yearSpinner = (JSpinner) getSwingEngine().find("spnEndYear");
		int year = Integer.parseInt(yearSpinner.getValue().toString());
		return YearMonth.of(year, waterYearDefinition.getEndMonth());
	}

	private void setEndYear(int end)
	{
		JSpinner yearSpinner = (JSpinner) getSwingEngine().find("spnEndYear");
		yearSpinner.setValue(end);
	}

	public String getProjectName()
	{
		JTextField projectNameField = (JTextField) getSwingEngine().find("prj_name");
		return projectNameField.getText();
	}

	public String getProjectDescription()
	{
		JTextField descriptionField = (JTextField) getSwingEngine().find("prj_desc");
		return descriptionField.getText();
	}

	public void resetQuickState() throws Exception
	{
		String projectName = getProjectName();
		String projectDescription = getProjectDescription();
		removeAll();
		Container swixmlProjectConfigurationPanel = renderSwixml(SCENARIO_CONFIGURATION_XML_FILE);
		super.add(swixmlProjectConfigurationPanel, BorderLayout.CENTER);
		initComponents();
		initModels();
		setActionListener(getActionListener());
		JSplitPane splitPane = (JSplitPane) getSwingEngine().find("split_pane");
		splitPane.setDividerLocation(275);
		JTextField projectNameField = (JTextField) getSwingEngine().find("prj_name");
		JTextField descriptionField = (JTextField) getSwingEngine().find("prj_desc");
		projectNameField.setText(projectName);
		descriptionField.setText(projectDescription);
	}

	public void resetProjectConfiguration() throws Exception
	{
		removeAll();
		Container swixmlProjectConfigurationPanel = renderSwixml(SCENARIO_CONFIGURATION_XML_FILE);
		super.add(swixmlProjectConfigurationPanel, BorderLayout.CENTER);
		initComponents();
		initModels();
		_scenarioTablePanel.clearScenarios();
		setActionListener(getActionListener());
		JSplitPane splitPane = (JSplitPane) getSwingEngine().find("split_pane");
		splitPane.setDividerLocation(350);
	}

	public EpptScenarioRun getBaseScenario()
	{
		return _scenarioTablePanel.getBaseScenarioRun();
	}

	EpptScenarioRun getSelectedScenario()
	{
		return _scenarioTablePanel.getSelectedScenario();
	}

	void addScenario(EpptScenarioRun scenarioRun)
	{
		_scenarioTablePanel.addScenarioRun(scenarioRun);
		updateRadioState();
		setModified(true);
	}

	public List<EpptScenarioRun> getEpptScenarioAlternatives()
	{
		return _scenarioTablePanel.getAlternativeScenarioRuns();
	}

	public List<EpptScenarioRun> getAllEpptScenarioRuns()
	{
		return new ArrayList<>(_scenarioTablePanel.getAllScenarioRuns());
	}

	public ScenarioTablePanel getScenarioTablePanel()
	{
		return _scenarioTablePanel;
	}

	void replaceScenario(EpptScenarioRun oldScenarioRun, EpptScenarioRun newScenarioRun)
	{
		_scenarioTablePanel.updateScenario(oldScenarioRun, newScenarioRun);
		if(!Objects.equals(oldScenarioRun, newScenarioRun))
		{
			setModified(true);
		}
	}

	void moveSelectedScenarioUp()
	{
		_scenarioTablePanel.moveSelectedScenarioUp();
		setModified(true);
	}

	void moveSelectedScenarioDown()
	{
		_scenarioTablePanel.moveSelectedScenarioDown();
		setModified(true);
	}

	private void enableChildComponents(boolean enable, Component component)
	{
		component.setEnabled(enable);
		if(component instanceof Container)
		{
			Component[] components = ((Container) component).getComponents();
			for(Component child : components)
			{
				enableChildComponents(enable, child);
			}
		}
	}

	public void topComponentActivated(Class<? extends EpptPanel> epptPanelClass)
	{
		Component quickCustomResultsPanel = getSwingEngine().find("QuickCustomResultsPanel");
		boolean enableQuickCustomResults = epptPanelClass == ProjectConfigurationPanel.class
				|| epptPanelClass == QuickResultsPanel.class
				|| epptPanelClass == CustomResultsPanel.class;
		enableChildComponents(enableQuickCustomResults, quickCustomResultsPanel);
		getSwingEngine().find("chkTAF").setEnabled(epptPanelClass != QAQCReportPanel.class && epptPanelClass != DataAnalysisPanel.class);
	}

	public WaterYearDefinition getWaterYearDefinition()
	{
		return (WaterYearDefinition) ((JComboBox) getSwingEngine().find("waterYearDefinitionCombo")).getSelectedItem();
	}

	public WaterYearIndex getWaterYearIndex()
	{
		return (WaterYearIndex) ((JComboBox) getSwingEngine().find("waterYearIndexCombo")).getSelectedItem();
	}

	private static final class MyKeyAdapter extends KeyAdapter
	{
		private final Component _component;

		private MyKeyAdapter(Component component)
		{
			_component = component;
		}

		@Override
		public void keyPressed(KeyEvent evt)
		{
			int key = evt.getKeyCode();
			if(key == KeyEvent.VK_ENTER
					|| key == KeyEvent.VK_TAB)
			{
				_component.transferFocus();
				SwingUtilities.invokeLater(() ->
				{
					Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
					if(focusOwner instanceof JTextField)
					{
						JTextField textField = (JTextField) focusOwner;
						textField.selectAll();
					}
				});
			}
		}
	}
}
