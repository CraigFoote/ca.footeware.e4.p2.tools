package ca.footeware.e4.p2.tools.ui.parts;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ca.footeware.e4.p2.tools.ui.runnables.MirrorRunnable;

/**
 * @author <a href="http://Footeware.ca">Footeware.ca</a>
 */
public class P2MirrorPart implements IRunnableContext
{
	private Composite parentComposite;
	Text destinationFolder;
	ProgressMonitorPart progressMonitor;
	Button runButton;
	Text sourceFolder;
	@Inject
	UISynchronize ui;

	/**
	 *
	 */
	@Focus
	public void onFocus()
	{
		sourceFolder.setFocus();
	}

	/**
	 * @param parent {@link Composite}
	 * @param provisioningAgent {@link IProvisioningAgent}
	 */
	@PostConstruct
	public void postConstruct(final Composite parent,
			final IProvisioningAgent provisioningAgent)
	{
		this.parentComposite = parent;
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5)
		.applyTo(parent);

		// reusable decoration image
		Image decorationImage = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
				.getImage();

		// source
		Label sourceLabel = new Label(parent, SWT.NONE);
		sourceLabel.setText("Source:");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER)
		.applyTo(sourceLabel);

		sourceFolder = new Text(parent, SWT.BORDER);
		sourceFolder
		.setText("http://download.eclipse.org/technology/swtbot/releases/2.1.1/");
		ControlDecoration sourceDecoration = new ControlDecoration(
				sourceFolder, SWT.TOP | SWT.LEFT);
		sourceDecoration
		.setDescriptionText("Enter the address of the p2 repository to be mirrored, e.g. 'http://download.eclipse.org/releases/kepler'.");
		sourceDecoration.setImage(decorationImage);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false)
		.align(SWT.FILL, SWT.CENTER).applyTo(sourceFolder);
		sourceFolder.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				runButton.setEnabled(!sourceFolder.getText().trim().isEmpty()
						&& !destinationFolder.getText().trim().isEmpty());
			}
		});

		// destination
		Label destinationLabel = new Label(parent, SWT.NONE);
		destinationLabel.setText("Destination:");
		GridDataFactory.fillDefaults().applyTo(destinationLabel);

		destinationFolder = new Text(parent, SWT.BORDER);
		ControlDecoration destinationDecoration = new ControlDecoration(
				destinationFolder, SWT.TOP | SWT.LEFT);
		destinationDecoration
		.setDescriptionText("Enter or browse for the local folder into which the p2 repository will be mirrored and zipped.");
		destinationDecoration.setImage(decorationImage);
		GridDataFactory.fillDefaults().grab(true, false)
		.align(SWT.FILL, SWT.CENTER).applyTo(destinationFolder);
		destinationFolder.addModifyListener(new ModifyListener()
		{
			/*
			 * (non-Javadoc)
			 * @see
			 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.
			 * swt.events.ModifyEvent)
			 */
			@Override
			public void modifyText(ModifyEvent e)
			{
				runButton.setEnabled(!sourceFolder.getText().trim().isEmpty()
						&& !destinationFolder.getText().trim().isEmpty());
			}
		});

		Button browse = new Button(parent, SWT.PUSH);
		browse.setText("Browse...");
		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER)
		.applyTo(browse);
		browse.addSelectionListener(new SelectionAdapter()
		{
			/*
			 * (non-Javadoc)
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events. SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),
						SWT.NONE);
				String folder = dialog.open();
				if (folder != null)
				{
					destinationFolder.setText(folder);
				}
			}
		});

		// 'Run/Cancel' buttons ina composite
		Composite buttonsParent = new Composite(parent, SWT.NONE);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 10;
		fillLayout.marginWidth = 20;
		fillLayout.spacing = 20;
		buttonsParent.setLayout(fillLayout);
		GridDataFactory.fillDefaults().span(3, 1).grab(true, false)
		.applyTo(buttonsParent);

		runButton = new Button(buttonsParent, SWT.PUSH);
		runButton.setText("Run");
		runButton.setEnabled(false);
		runButton.addSelectionListener(new SelectionAdapter()
		{
			/*
			 * (non-Javadoc)
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events. SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				execute(provisioningAgent);
			}
		});

		// ProgressMonitor
		progressMonitor = new ProgressMonitorPart(parent, null, true);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(progressMonitor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableContext#run(boolean, boolean,
	 * org.eclipse.jface.operation.IRunnableWithProgress)
	 */
	@Override
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException
	{
		ModalContext.run(runnable, fork, progressMonitor,
				parentComposite.getDisplay());
	}

	/**
	 * @param provisioningAgent
	 */
	protected void execute(IProvisioningAgent provisioningAgent)
	{
		runButton.setEnabled(false);
		// source
		String sourceStr = sourceFolder.getText().trim();
		URI source;
		try
		{
			source = new URI(sourceStr);
		}
		catch (URISyntaxException e)
		{
			MessageDialog.openError(parentComposite.getShell(), "Error",
					e.getMessage());
			return;
		}

		// destination
		String destinationStr = destinationFolder.getText().trim();
		File destination = new File(destinationStr);
		destination.mkdir();

		IRunnableWithProgress runnable = new MirrorRunnable(source,
				destination, ui, provisioningAgent, runButton);

		try
		{
			run(true, true, runnable);
		}
		catch (InvocationTargetException | InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
