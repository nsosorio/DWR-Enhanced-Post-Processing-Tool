/*
 * Copyright (c) 2019
 * California Department of Water Resources
 * All Rights Reserved.  DWR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from DWR
 */

package gov.ca.water.scenario.presentation;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import gov.ca.water.businessservice.IAllButtonsDele;
import gov.ca.water.businessservice.IApplyDynamicConDele;
import gov.ca.water.businessservice.IScenarioDele;
import gov.ca.water.businessservice.IScenarioSvc;
import gov.ca.water.businessservice.ISeedDataSvc;
import gov.ca.water.businessservice.IVerifyControlsDele;
import gov.ca.water.businessservice.impl.AllButtonsDeleImp;
import gov.ca.water.businessservice.impl.ApplyDynamicConDeleImp;
import gov.ca.water.businessservice.impl.ScenarioDeleImp;
import gov.ca.water.businessservice.impl.ScenarioSvcImpl;
import gov.ca.water.businessservice.impl.SeedDataSvcImpl;
import gov.ca.water.businessservice.impl.VerifyControlsDeleImp;
import gov.ca.water.businessservice.impl.XMLParsingSvcImpl;
import gov.ca.water.calgui.EpptInitializationException;
import gov.ca.water.calgui.bo.DataTableModel;
import gov.ca.water.calgui.bo.RBListItemBO;
import gov.ca.water.calgui.bus_service.IModelRunSvc;
import gov.ca.water.calgui.bus_service.impl.ModelRunSvcImpl;
import gov.ca.water.calgui.constant.Constant;
import gov.ca.water.calgui.presentation.DisplayHelper;
import gov.ca.water.calgui.presentation.ProgressFrame;
import gov.ca.water.calgui.tech_service.IAuditSvc;
import gov.ca.water.calgui.tech_service.IDialogSvc;
import gov.ca.water.calgui.tech_service.IErrorHandlingSvc;
import gov.ca.water.calgui.tech_service.impl.AuditSvcImpl;
import gov.ca.water.calgui.tech_service.impl.DialogSvcImpl;
import gov.ca.water.calgui.tech_service.impl.ErrorHandlingSvcImpl;
import gov.ca.water.quickresults.ui.scenarioconfig.ProjectConfigurationPanel;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.jfree.data.time.Month;
import org.swixml.SwingEngine;

/**
 * This class is for Listening all the action events(Button click) which are
 * generated by the application.
 *
 * @author Mohan
 */
public class GlobalActionListener implements ActionListener
{
	private static final Logger LOG = Logger.getLogger(GlobalActionListener.class.getName());
	private final SwingEngine _swingEngine = XMLParsingSvcImpl.getXMLParsingSvcImplInstance().getSwingEngine();
	private final IScenarioDele _scenarioDele = new ScenarioDeleImp();
	private final IAllButtonsDele _allButtonsDele = new AllButtonsDeleImp();
	private final IScenarioSvc _scenarioSvc = ScenarioSvcImpl.getScenarioSvcImplInstance();
	private final IAuditSvc _auditSvc = AuditSvcImpl.getAuditSvcImplInstance();
	private final IModelRunSvc _modelRunSvc = new ModelRunSvcImpl();
	private final ISeedDataSvc _seedDataSvc = SeedDataSvcImpl.getSeedDataSvcImplInstance();
	private final IDialogSvc _dialogSvc = DialogSvcImpl.getDialogSvcInstance();
	private final IVerifyControlsDele _verifyControlsDele = new VerifyControlsDeleImp();
	private final IErrorHandlingSvc _errorHandlingSvc = new ErrorHandlingSvcImpl();
	private final IApplyDynamicConDele _applyDynamicConDele = new ApplyDynamicConDeleImp();
	private final DisplayHelper _displayHelper;

	public GlobalActionListener(DisplayHelper displayHelper)
	{
		_displayHelper = displayHelper;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			final JList<String> lstReports = (JList<String>) _swingEngine.find("lstReports");
			final JList<RBListItemBO> lstScenarios = (JList<RBListItemBO>) _swingEngine.find("SelectedList");
			JTable table = null;
			switch(ae.getActionCommand())
			{

				case "AC_Power":

					List<RBListItemBO> scendarios = new ArrayList<>();
					ListModel<RBListItemBO> model = lstScenarios.getModel();
					for(int i = 0; i < model.getSize(); i++)
					{
						scendarios.add(model.getElementAt(i));
					}
					PowerFrame pf = new PowerFrame(scendarios);
					break;

				case "AC_SaveScen":
					saveScenario();
					break;
				case "AC_SaveScenAs":
					this._allButtonsDele.saveAsButton();
					break;
				case "AC_ViewScen":
					loadViewScen();
					break;
				case "AC_LoadScen":
					loadScenario();
					break;
				case "AC_Help":
					this._allButtonsDele.helpButton();
					break;
				case "AC_RUN":
					runSingleBatch();
					break;
				case "AC_BATCH":
					this._allButtonsDele.runMultipleBatch();
					break;
				case "AC_About":
					this._allButtonsDele.aboutButton();
					break;
				case "AC_Select_DSS_SV":
					this._allButtonsDele.selectingSVAndInitFile("hyd_DSS_SV", "hyd_DSS_SV_F", "txf_Manual_SV",
							"txf_Manual_SV_F");
					break;
				case "AC_Select_DSS_Init":
					this._allButtonsDele.selectingSVAndInitFile("hyd_DSS_Init", "hyd_DSS_Init_F", "txf_Manual_Init",
							"txf_Manual_Init_F");
					break;
				case "Fac_TableEdit":
					TitledBorder title = null;
					if(ae.getSource() instanceof JButton)
					{
						JButton btn = (JButton) ae.getSource();
						String titlestr = btn.getText();
						title = BorderFactory.createTitledBorder(titlestr);
					}
					JPanel pan = (JPanel) _swingEngine.find("fac_pan6");
					pan.setBorder(title);
					break;
				case "Reg_Copy":
					table = (JTable) _swingEngine.find("tblRegValues");
					this._allButtonsDele.copyTableValues(table);
					break;
				case "Reg_Paste":
					table = (JTable) _swingEngine.find("tblRegValues");
					JRadioButton userDefined = (JRadioButton) _swingEngine.find("btnRegUD");
					if(userDefined.isSelected())
					{
						this._allButtonsDele.pasteTableValues(table);
					}
					else
					{
						_errorHandlingSvc.validationeErrorHandler("You can't paste untel you select user defined.",
								"You can't paste untel you select user defined.");
					}
					break;
				case "Op_TableEdit":
					this._allButtonsDele.editButtonOnOperations((JComponent) ae.getSource());
					break;
				case "Op_Generate":

					runSingleBatchForWsiDi();
					break;
				case "Op_Read":
					this._allButtonsDele.readButtonInOperations();
					break;
				case "Op_Default":
					this._allButtonsDele.defaultButtonOnOperations();
					break;
				case "Op_Copy":
					table = (JTable) _swingEngine.find("tblOpValues");
					this._allButtonsDele.copyTableValues(table);
					break;
				case "Op_Paste":
					table = (JTable) _swingEngine.find("tblOpValues");
					this._allButtonsDele.pasteTableValues(table);
					break;
				// From Quick Results and External PDF
				case "AC_PresetClear":
					clearQRCheckBoxes("presets");
					break;
				case "AC_ShortageClear":
					clearQRCheckBoxes("shortage");
					break;
				case "AC_SJRClear":
					clearQRCheckBoxes("SJR Results");
					break;
				case "AC_WMAClear":
					clearQRCheckBoxes("WMA");
					break;
				case "AC_DShortClear":
					clearQRCheckBoxes("DShort");
					break;
				case "AC_CompScen":
					compareScenarios();
					break;
				case "AC_DfcClear":
					clearQRCheckBoxes("delta_flow_criteria");
					break;
				case "Rep_DispCur":
					if(lstScenarios.getModel().getSize() == 0)
					{
						_dialogSvc.getOK("Error - No scenarios loaded", JOptionPane.ERROR_MESSAGE);
					}
					else if(lstReports.getSelectedValue() == null)
					{
						_dialogSvc.getOK("Error - No display group selected", JOptionPane.ERROR_MESSAGE);
					}
					else
					{

						List<RBListItemBO> scenarios = new ArrayList<>();
						model = lstScenarios.getModel();
						for(int i = 0; i < model.getSize(); i++)
						{
							scenarios.add(model.getElementAt(i));
						}
						ProjectConfigurationPanel projectConfigurationPanel = ProjectConfigurationPanel.getProjectConfigurationPanel();
						String quickState = projectConfigurationPanel.quickState();
						Month startMonth = projectConfigurationPanel.getStartMonth();
						Month endMonth = projectConfigurationPanel.getEndMonth();
						_displayHelper.showDisplayFrames(
								(String) ((JList) _swingEngine.find("lstReports")).getSelectedValue(), scenarios, startMonth, endMonth);
					}
					break;
				case "Time_SELECT":

					break;
			}
		}
		catch(HeadlessException e)
		{
			LOG.error(e.getMessage());
			String messageText = "Unable to initialize action listeners.";
			_errorHandlingSvc.businessErrorHandler(messageText, e);
		}
		catch(EpptInitializationException ex)
		{
			//do something? There was an issue finding the file or reading the file
		}
	}

	private void saveScenario()
	{
		if(FilenameUtils.removeExtension(((JTextField) _swingEngine.find("run_txfScen")).getText())
						.toUpperCase().equals("DEFAULT") && _allButtonsDele.defaultCLSIsProtected())
		{

			_dialogSvc.getOK(
					"The CalLite GUI is not allowed to overwrite DEFAULT.CLS. Please use Save As to save to a different scenario file.",
					JOptionPane.ERROR_MESSAGE);

		}
		else
		{
			this._allButtonsDele.saveCurrentStateToFile();
		}
		return;
	}

	private void loadScenario()
	{
		// Check for changed scenario prior to
		boolean doLoad = true;
		// load -
		// tad 20170202
		if(_auditSvc.hasValues())
		{
			String clsFileName = FilenameUtils
					.removeExtension(((JTextField) _swingEngine.find("run_txfScen")).getText());

			String option = _dialogSvc.getYesNoCancel("Scenario selections have changed for " + clsFileName
					+ ". Would you like to save the changes?", JOptionPane.QUESTION_MESSAGE);
			switch(option)
			{
				case "Cancel":
					doLoad = false;
					break;
				case "Yes":
					doLoad = this._allButtonsDele.saveCurrentStateToFile(clsFileName);
					break;
				case "No":
					doLoad = true;
					break;
			}
		}
		if(doLoad)
		{
			JFileChooser fChooser = new JFileChooser(Constant.SCENARIOS_DIR);
			fChooser.setMultiSelectionEnabled(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("CLS FILES (.cls)", "cls");
			fChooser.setFileFilter(filter);
			int val = fChooser.showOpenDialog(_swingEngine.find(Constant.MAIN_FRAME_NAME));
			if(val == JFileChooser.APPROVE_OPTION
					&& this._allButtonsDele.verifyTheSelectedFiles(fChooser, Constant.CLS_EXT))
			{
				String fileName = fChooser.getSelectedFile().getName();
				loadScenarioButton(fileName);
			}
		}
	}

	/**
	 * This method is used for the "load Scenario" button on the "Run Settings"
	 * tab.
	 *
	 * @param fileName The Name of the file.
	 */
	public void loadScenarioButton(String fileName)
	{
		try
		{
			LOG.debug("loading this cls file " + fileName);
			fileName = FilenameUtils.removeExtension(fileName);
			this._verifyControlsDele.verifyTheDataBeforeUI(Constant.SCENARIOS_DIR + fileName + Constant.CLS_EXT);
			this._scenarioSvc.applyClsFile(Constant.SCENARIOS_DIR + fileName + Constant.CLS_EXT, _swingEngine,
					_seedDataSvc.getTableIdMap());
			((JTextField) _swingEngine.find("run_txfScen")).setText(fileName + Constant.CLS_EXT);
			((JTextField) _swingEngine.find("run_txfoDSS")).setText(fileName + Constant.DV_NAME + Constant.DSS_EXT);
			_applyDynamicConDele.changeSVInitFilesAndTableInOperations(true);
			String[] c1 = new String[0];
			Object[][] data = new Object[0][0];
			((JTable) this._swingEngine.find("tblOpValues")).setModel(new DataTableModel("", c1, data, false));
			_applyDynamicConDele.applyDynamicControlForListFromFile();
			_allButtonsDele.decisionSVInitFilesAndTableInOperations();
			_auditSvc.clearAudit();
		}
		catch(Exception e)
		{
			LOG.error(e.getMessage());
			String messageText = "Unable to load scenarios.";
			_errorHandlingSvc.businessErrorHandler(messageText, e);
		}
	}


	private void compareScenarios()
	{
		ProjectConfigurationPanel projectConfigurationPanel = ProjectConfigurationPanel.getProjectConfigurationPanel();
		IScenarioDele scenarioDele = new ScenarioDeleImp();
		List<String> fileNames = new ArrayList<>();
		for(int i = 0; i < projectConfigurationPanel.getScenarios().size(); i++)
		{
			String name = Paths.get(projectConfigurationPanel.getScenarios().get(i).toString())
							   .getFileName().toString();
			fileNames.add(name.substring(0, name.length() - 7) + Constant.CLS_EXT);
		}
		try
		{
			List<DataTableModel> dtmList = scenarioDele.getScenarioTableData(fileNames, _swingEngine);
			ScenarioFrame scenarioFrame = new ScenarioFrame(dtmList);
			scenarioFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			scenarioFrame.setVisible(true);
		}
		catch(EpptInitializationException ex)
		{
			LOG.fatal("Unable to compare Scenarios", ex);
		}
		try
		{
			Files.delete(
					Paths.get(Constant.SCENARIOS_DIR + Constant.CURRENT_SCENARIO + Constant.CLS_EXT));
		}
		catch(IOException ex)
		{
			LOG.debug(ex);
		}
	}

	/**
	 * This method is used to display the "View Scenario Settings" button on the
	 * "Run Settings" tab.
	 */
	public void loadViewScen() throws EpptInitializationException
	{
		boolean pro = this._allButtonsDele.saveForViewScen();
		if(pro)
		{
			List<DataTableModel> dtmList = _scenarioDele.getScenarioTableData(null, _swingEngine);
			ScenarioFrame scenarioFrame = new ScenarioFrame(dtmList);
			scenarioFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			scenarioFrame.setVisible(true);
			try
			{
				Files.delete(Paths.get(Constant.SCENARIOS_DIR + Constant.CURRENT_SCENARIO + Constant.CLS_EXT));
			}
			catch(IOException ex)
			{
				LOG.error(ex.getMessage());
				String messageText = "Unable to load scenarios.";
				_errorHandlingSvc.businessErrorHandler(messageText, ex);
			}
		}
	}

	/**
	 * This method is used to run single batch program.
	 */
	public void runSingleBatch()
	{
		try
		{
			String clsFileName = ((JTextField) _swingEngine.find("run_txfScen")).getText();
			clsFileName = FilenameUtils.removeExtension(clsFileName);
			if(decisionBeforeTheBatchRun())
			{
				ProgressFrame progressFrame = ProgressFrame.getProgressFrameInstance();
				List<String> fileName = Arrays.asList(clsFileName);
				progressFrame.addScenarioNamesAndAction(clsFileName, Constant.BATCH_RUN);
				progressFrame.setBtnText(Constant.STATUS_BTN_TEXT_STOP);
				progressFrame.makeDialogVisible();
				_modelRunSvc.doBatch(fileName, _swingEngine, false);
			}
		}
		catch(Exception e)
		{
			LOG.error(e.getMessage());
			String messageText = "Unable to run batch file.";
			_errorHandlingSvc.businessErrorHandler(messageText, e);
		}
	}

	/**
	 * This method is used to run batch program for wsidi.
	 */
	public void runSingleBatchForWsiDi()
	{
		try
		{
			String clsFileName = ((JTextField) _swingEngine.find("run_txfScen")).getText();
			clsFileName = FilenameUtils.removeExtension(clsFileName);
			if(decisionBeforeTheBatchRun())
			{
				ProgressFrame progressFrame = ProgressFrame.getProgressFrameInstance();
				List<String> fileName = Arrays.asList(clsFileName);
				progressFrame.addScenarioNamesAndAction(clsFileName, Constant.BATCH_RUN_WSIDI);
				progressFrame.setBtnText(Constant.STATUS_BTN_TEXT_STOP);
				progressFrame.makeDialogVisible();
				_modelRunSvc.doBatch(fileName, _swingEngine, true);
			}
		}
		catch(Exception e)
		{
			LOG.error(e.getMessage());
			String messageText = "Unable to run wsidi.";
			_errorHandlingSvc.businessErrorHandler(messageText, e);
		}
	}

	/**
	 * This will ask the user whether to save and run the batch or run without
	 * saving or Cancel.
	 *
	 * @return Will return true if the user wants to run the batch program.
	 */
	public boolean decisionBeforeTheBatchRun()
	{
		boolean isSaved;
		try
		{
			String clsFileName = FilenameUtils
					.removeExtension(((JTextField) _swingEngine.find("run_txfScen")).getText());
			if(clsFileName.equalsIgnoreCase(Constant.DEFAULT))
			{
				_dialogSvc.getOK(
						"Error - The CalLite GUI is not allowed to modify the default scenario 'DEFAULT.CLS'. Please use Save As to save to a different scenario file",
						JOptionPane.ERROR_MESSAGE);

				return false;
			}
			if(!Files.isExecutable(Paths.get(Constant.RUN_DETAILS_DIR + clsFileName)))
			{
				String option = _dialogSvc.getYesNo(
						"The cls file does not have a corresponding directory structure.\nThe batch will not run without this.\nDo you want to save to create that directory?",
						JOptionPane.QUESTION_MESSAGE);
				switch(option)
				{
					case "Yes":
						return this._allButtonsDele.saveCurrentStateToFile(clsFileName);
					case "No":
						return false;
				}
			}
			isSaved = false;
			if(_auditSvc.hasValues())
			{
				String option = _dialogSvc.getYesNo(
						"Scenario selections have changed. Would you like to save the changes?",
						JOptionPane.QUESTION_MESSAGE);
				switch(option)
				{
					case "Yes":
						isSaved = this._allButtonsDele.saveCurrentStateToFile(clsFileName);
						break;
					case "No":
						loadScenarioButton(((JTextField) _swingEngine.find("run_txfScen")).getText());
						isSaved = true;
						break;
				}

			}
			else
			{
				isSaved = true;
			}
			return isSaved;
		}
		catch(HeadlessException e)
		{
			LOG.error(e.getMessage());
			String messageText = "Unable to run batch file.";
			_errorHandlingSvc.businessErrorHandler(messageText, e);
		}
		return false;
	}

	/**
	 * Clears Quick Result checkboxes that are on panel named "panelName"
	 *
	 * @param panelName
	 */
	private void clearQRCheckBoxes(String panelName)
	{
		JPanel panel = (JPanel) _swingEngine.find(panelName);
		Component[] components = panel.getComponents();
		for(final Component component : components)
		{
			if(component instanceof JCheckBox)
			{
				JCheckBox c = (JCheckBox) component;
				c.setSelected(false);
			}
		}
	}

}
