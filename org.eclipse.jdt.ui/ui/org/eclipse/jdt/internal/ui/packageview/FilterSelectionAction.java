/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.packageview;import java.io.StringWriter;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.actions.SelectionProviderAction;import org.eclipse.ui.dialogs.ListSelectionDialog;

/**
 * The FilterAction is the class that adds the filter views to a PackagesView.
 */
class FilterSelectionAction extends SelectionProviderAction {

	
	private PackageExplorerPart fPackagesView; 
	private Shell fShell;
	
	/**
	 * Create a new filter action
	 * @param shell the shell that will be used for the list selection
	 * @param packages the PackagesExplorerPart
	 * @param label the label for the action
	 */
	FilterSelectionAction(Shell shell, PackageExplorerPart packagesView, String label) {
		super(packagesView.getViewer(), label);
		setToolTipText(PackagesMessages.getString("FilterSelectionAction.apply.toolTip")); //$NON-NLS-1$
		setEnabled(true);
		fShell= shell;
		fPackagesView= packagesView;
	}
	
	/**
	 * Implementation of method defined on <code>IAction</code>.
	 */
	public void run() {
		JavaElementPatternFilter filter= fPackagesView.getPatternFilter();
		FiltersContentProvider contentProvider= new FiltersContentProvider(filter);
	
		ListSelectionDialog dialog =
			new ListSelectionDialog(
				fShell,
				fPackagesView.getViewer(),
				contentProvider,
				new LabelProvider(),
				PackagesMessages.getString("FilterSelectionAction.apply.label")); //$NON-NLS-1$
	
		dialog.setInitialSelections(contentProvider.getInitialSelections());		dialog.setTitle(PackagesMessages.getString("FilterSelectionAction.dialog.title")); //$NON-NLS-1$
		dialog.open();
		if (dialog.getReturnCode() == dialog.OK) {
			Object[] results= dialog.getResult();
			String[] selectedPatterns= new String[results.length];
			System.arraycopy(results, 0, selectedPatterns, 0, results.length);
			filter.setPatterns(selectedPatterns);
			saveInPreferences(selectedPatterns);
			TreeViewer viewer= fPackagesView.getViewer();
			viewer.getControl().setRedraw(false);
			viewer.refresh();
			viewer.getControl().setRedraw(true);
		}
	}
	/**
	 * Save the supplied patterns in the preferences for the UIPlugin.
	 * They are saved in the format patern,pattern,.
	 */
	private void saveInPreferences(String[] patterns) {
		JavaPlugin plugin= JavaPlugin.getDefault();
		StringWriter writer= new StringWriter();
	
		for (int i = 0; i < patterns.length; i++) {
			writer.write(patterns[i]);
			writer.write(JavaElementPatternFilter.COMMA_SEPARATOR);
		}
	
		plugin.getPreferenceStore().setValue(
			JavaElementPatternFilter.FILTERS_TAG,
			writer.toString());
	}
}
