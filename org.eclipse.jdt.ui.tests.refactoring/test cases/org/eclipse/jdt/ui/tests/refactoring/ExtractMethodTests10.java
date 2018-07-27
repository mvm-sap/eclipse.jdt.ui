/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

public class ExtractMethodTests10 extends ExtractMethodTests {
	private static ExtractMethodTestSetup10 fgTestSetup;

	public ExtractMethodTests10(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup10(new NoSuperTestsSuite(ExtractMethodTests10.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup10(test);
		return fgTestSetup;
	}

	protected void try10Test() throws Exception {
		performTest(fgTestSetup.getTry10Package(), "A", COMPARE_WITH_OUTPUT, "try10_out");
	}

	//====================================================================================
	// Testing var type 
	//====================================================================================

	public void testVar1() throws Exception {
		try10Test();
	}

}
