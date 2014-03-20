/**
 *
 */
package ca.footeware.e4.p2.tools.ui.runnables;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;

/**
 * @author <a href="http://Footeware.ca">Footeware.ca</a>
 */
public final class MirrorRunnable implements IRunnableWithProgress
{
	private File destination;
	private IProvisioningAgent provisioningAgent;
	private URI source;
	private UISynchronize ui;
	Button runButton;
	
	/**
	 * Constructor.s
	 * @param source {@link URI}
	 * @param destination {@link File}
	 * @param ui {@link UISynchronize}
	 * @param provisioningAgent {@link IProvisioningAgent}
	 * @param runButton {@link Button}
	 */
	public MirrorRunnable(URI source, File destination, UISynchronize ui,
			IProvisioningAgent provisioningAgent, Button runButton)
	{
		this.source = source;
		this.destination = destination;
		this.ui = ui;
		this.provisioningAgent = provisioningAgent;
		this.runButton = runButton;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException
	{
		monitor.beginTask("Mirroring p2 repository", 4);
		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) provisioningAgent
				.getService(IMetadataRepositoryManager.SERVICE_NAME);
		monitor.worked(1);
		if (monitor.isCanceled())
		{
			monitor.done();
			enableRunButton();
			return;
		}
		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) provisioningAgent
				.getService(IArtifactRepositoryManager.SERVICE_NAME);
		monitor.worked(1);
		IMetadataRepository metadataRepository;
		try
		{
			metadataRepository = metadataManager.loadRepository(source, null);
			System.err.println(metadataRepository);
			monitor.worked(1);
			if (monitor.isCanceled())
			{
				monitor.done();
				enableRunButton();
				return;
			}
			IArtifactRepository artifactRepository = artifactManager
					.loadRepository(source, null);
			System.err.println(artifactRepository);
			monitor.worked(1);
		}
		catch (ProvisionException | OperationCanceledException e)
		{
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Error", e.getMessage());
		}
		if (monitor.isCanceled())
		{
			monitor.done();
			enableRunButton();
			return;
		}
		monitor.done();
		enableRunButton();
	}
	
	/**
	 * Enable the 'Run" button.
	 */
	private void enableRunButton()
	{
		ui.asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				runButton.setEnabled(true);
			}
		});
	}
}
