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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.osgi.installer.InstallableResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/** Mock RegisteredResource that simulates a bundle */
public class MockBundleResource implements RegisteredResource, Serializable {

    private static final long serialVersionUID = 1L;
    private final Map<String, Object> attributes = new HashMap<String, Object>();
	private boolean installable = true;
	private final String digest;
	private final int priority;
	private final long serialNumber;
	private static long serialNumberCounter = System.currentTimeMillis();
	
    MockBundleResource(String symbolicName, String version) {
        this(symbolicName, version, InstallableResource.DEFAULT_PRIORITY);
    }
    
	MockBundleResource(String symbolicName, String version, int priority) {
		attributes.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		attributes.put(Constants.BUNDLE_VERSION, version);
		digest = symbolicName + "." + version;
		this.priority = priority;
		serialNumber = getNextSerialNumber();
	}
	
    MockBundleResource(String symbolicName, String version, int priority, String digest) {
        attributes.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        attributes.put(Constants.BUNDLE_VERSION, version);
        this.digest = digest;
        this.priority = priority;
        serialNumber = getNextSerialNumber();
    }
    
    private static long getNextSerialNumber() {
        synchronized (MockBundleResource.class) {
            return serialNumberCounter++; 
        }
    }
    
	@Override
	public String toString() {
	    return getClass().getSimpleName() 
	    + ", n=" + attributes.get(Constants.BUNDLE_SYMBOLICNAME)
        + ", v= " + attributes.get(Constants.BUNDLE_VERSION)
        + ", d=" + digest
        + ", p=" + priority
        ;
	}
	
	public void cleanup(OsgiInstallerContext ctx) {
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Dictionary<String, Object> getDictionary() {
		return null;
	}

	public String getDigest() {
		return digest;
	}

	public String getEntityId() {
		return null;
	}

	public InputStream getInputStream(BundleContext ctx) throws IOException {
		return null;
	}

	public ResourceType getResourceType() {
		return RegisteredResource.ResourceType.BUNDLE;
	}

	public String getUrl() {
		return null;
	}

	public String getURL() {
		return null;
	}
	
    public String getUrlScheme() {
        return null;
    }

    public boolean isInstallable() {
        return installable;
    }

    public void setInstallable(boolean installable) {
        this.installable = installable;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public long getSerialNumber() {
        return serialNumber;
    }
}
