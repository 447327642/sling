/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.osgi.installer.impl;

import java.io.File;
import java.io.IOException;

import org.apache.sling.osgi.installer.InstallableResource;
import org.osgi.framework.BundleContext;

/** RegisteredResource that stores data to a local temporary file */
class LocalFileRegisteredResource extends RegisteredResourceImpl {
	private File storage;

	LocalFileRegisteredResource(InstallableResource r) throws IOException {
		super(new MockOsgiInstallerContext(), r, "test");
	}

	@Override
	protected File getDataFile(BundleContext ctx) {
		if(storage == null) {
	        try {
	            storage = File.createTempFile(getClass().getName(), "test");
	        } catch(IOException ioe) {
	            throw new Error("IOException on file creation", ioe);
	        }
			storage.deleteOnExit();
		}
		return storage;
	}
}