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

package gov.ca.water.quickresults.ui.projectconfig.scenarioconfig;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.stream.Stream;
import javax.swing.*;

import gov.ca.water.calgui.project.EpptScenarioRun;
import gov.ca.water.calgui.project.EpptScenarioRunValidator;
import gov.ca.water.quickresults.ui.projectconfig.ProjectConfigurationPanel;

/**
 * Company: Resource Management Associates
 *
 * @author <a href="mailto:adam@rmanet.com">Adam Korynta</a>
 * @since 04-08-2019
 */
public class ScenarioRunEditor extends JDialog implements LoadingDss
{
	private final ScenarioEditorPanel _scenarioEditorPanel;
	private final JProgressBar _progressBar = new JProgressBar();
	private boolean _canceled = true;
	private EpptScenarioRun _originalScenarioRun;

	public ScenarioRunEditor(Frame frame)
	{
		super(frame, "New Scenario Run", true);
		_progressBar.setVisible(false);
		_scenarioEditorPanel = new ScenarioEditorPanel(this);
		setPreferredSize(new Dimension(650, 500));
		setMinimumSize(new Dimension(650, 500));
		initComponents();
		pack();
		setLocationRelativeTo(frame);
	}

	public void fillPanel(EpptScenarioRun scenarioRun)
	{
		_originalScenarioRun = scenarioRun;
		setTitle("Edit Scenario Run: " + scenarioRun.getName());
		_scenarioEditorPanel.fillPanel(scenarioRun);
	}

	private void initComponents()
	{
		setLayout(new BorderLayout(5, 5));
		add(_scenarioEditorPanel.$$$getRootComponent$$$(), BorderLayout.CENTER);
		buildOkCancelButtons();
	}

	private void buildOkCancelButtons()
	{
		JPanel jPanel = new JPanel();
		jPanel.setLayout(new BorderLayout());
		jPanel.add(_progressBar, BorderLayout.NORTH);
		JButton okButton = new JButton("OK");
		okButton.setDefaultCapable(true);
		getRootPane().setDefaultButton(okButton);
		JButton cancelButton = new JButton("Cancel");
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okButton);
		okButton.setPreferredSize(cancelButton.getPreferredSize());
		buttonPanel.add(cancelButton);
		jPanel.add(buttonPanel, BorderLayout.CENTER);
		add(jPanel, BorderLayout.SOUTH);
		okButton.addActionListener(this::okPerformed);
		cancelButton.addActionListener(this::cancelPerformed);
	}

	@Override
	public void loadingStart(String text)
	{
		_progressBar.setVisible(true);
		_progressBar.setIndeterminate(true);
		_progressBar.setToolTipText(text);
	}

	@Override
	public void loadingFinished()
	{
		_progressBar.setVisible(false);
		_progressBar.setIndeterminate(false);
	}

	public void okPerformed(ActionEvent e)
	{
		EpptScenarioRun run = createRun();
		EpptScenarioRunValidator epptScenarioRunValidator = new EpptScenarioRunValidator(run);
		if(epptScenarioRunValidator.isValid())
		{
			boolean duplicateName = ProjectConfigurationPanel.getProjectConfigurationPanel().getAllEpptScenarioRuns()
															 .stream()
															 .filter(s -> s != _originalScenarioRun)
															 .map(EpptScenarioRun::getName)
															 .anyMatch(s -> s.equalsIgnoreCase(run.getName()));
			if(duplicateName)
			{
				JOptionPane.showMessageDialog(this, "Duplicate Scenario Run Name: " + run.getName(),
						"Error", JOptionPane.WARNING_MESSAGE);
			}
			else
			{
				_canceled = false;
				dispose();
			}
		}
		else
		{
			StringBuilder builder = new StringBuilder("Scenario Run is not valid: ");
			epptScenarioRunValidator.getErrors().forEach(s -> builder.append("\n").append(s));
			JOptionPane.showMessageDialog(this, builder.toString(), "Error", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void cancelPerformed(ActionEvent e)
	{
		dispose();
	}

	@Override
	public void dispose()
	{
		_progressBar.setIndeterminate(false);
		_progressBar.setVisible(false);
		_scenarioEditorPanel.shutdown();
		setVisible(false);
		super.dispose();
	}

	/**
	 * @return null if canceled, builds a new Scenario Run otherwise
	 */
	public EpptScenarioRun createRun()
	{
		return _scenarioEditorPanel.createRun();
	}

	public boolean isCanceled()
	{
		return _canceled;
	}
}
