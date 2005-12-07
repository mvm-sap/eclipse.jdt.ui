/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.internal.ui.refactoring.history;

import org.eclipse.ltk.core.refactoring.Change;

/**
 * Interface for refactoring preview change requestors.
 * 
 * @since 3.2
 */
public class RefactoringPreviewChangeRequestor {

	/**
	 * Is the specified change accepted?
	 * 
	 * @param change
	 *            the change to test
	 * @return <code>true</code> if it is accepted for preview,
	 *         <code>false</code> otherwise
	 */
	public boolean accept(final Change change) {
		return true;
	}
}