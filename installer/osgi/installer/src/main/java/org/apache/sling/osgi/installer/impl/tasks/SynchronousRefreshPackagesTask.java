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
package org.apache.sling.osgi.installer.impl.tasks;

import org.apache.sling.osgi.installer.impl.Logger;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** Execute an OSGi "refresh packages" operation, synchronously */
public class SynchronousRefreshPackagesTask extends OsgiInstallerTask implements FrameworkListener {

    /** Tracker for the package admin. */
    private final ServiceTracker packageAdminTracker;

    private static final String REFRESH_PACKAGES_ORDER = "60-";

    /** Max time allowed to refresh packages (TODO configurable??) */
    public static final int MAX_REFRESH_PACKAGES_WAIT_SECONDS = 30;

	private volatile int packageRefreshEventsCount;

	public SynchronousRefreshPackagesTask(final ServiceTracker packageAdminTracker) {
	    this.packageAdminTracker = packageAdminTracker;
	}

    /**
     * Handles the PACKAGES_REFRESHED framework event which is sent after
     * the PackageAdmin.refreshPackages has finished its work of refreshing
     * the packages. When packages have been refreshed all bundles which are
     * expected to be active (those active before refreshing the packages and
     * newly installed or updated bundles) are started.
     */
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
    	    Logger.logDebug("FrameworkEvent.PACKAGES_REFRESHED");
        	packageRefreshEventsCount++;
        }
    }

	@Override
	public String getSortKey() {
		return REFRESH_PACKAGES_ORDER;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

    private PackageAdmin getPackageAdmin() {
        return (PackageAdmin)this.packageAdminTracker.getService();
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerTask#execute(org.apache.sling.osgi.installer.impl.OsgiInstallerContext)
     */
    public void execute(OsgiInstallerContext ctx) throws Exception {
        logExecution();
        final int targetEventCount = packageRefreshEventsCount + 1;
        final long start = System.currentTimeMillis();
        final long timeout = System.currentTimeMillis() + MAX_REFRESH_PACKAGES_WAIT_SECONDS * 1000L;

        // Refreshing packages might cause some bundles to be stopped,
        // make sure all currently active ones are restarted after
        // this task executes
    	for(Bundle b : ctx.getBundleContext().getBundles()) {
    		if(b.getState() == Bundle.ACTIVE) {
    			final OsgiInstallerTask t = new BundleStartTask(b.getBundleId());
    			ctx.addTaskToCurrentCycle(t);
    			Logger.logDebug("Added " + t + " to restart bundle if needed after refreshing packages");
    		}
    	}

        // It seems like (at least with Felix 1.0.4) we won't get a FrameworkEvent.PACKAGES_REFRESHED
        // if one happened very recently and there's nothing to refresh
        ctx.getBundleContext().addFrameworkListener(this);
        try {
            this.getPackageAdmin().refreshPackages(null);
            while(true) {
                if(System.currentTimeMillis() > timeout) {
                    Logger.logWarn("No FrameworkEvent.PACKAGES_REFRESHED event received within "
        	    				+ MAX_REFRESH_PACKAGES_WAIT_SECONDS
        	    				+ " seconds after refresh");
                    break;
                }
                if(packageRefreshEventsCount >= targetEventCount) {
                    final long delta = System.currentTimeMillis() - start;
                    Logger.logDebug("FrameworkEvent.PACKAGES_REFRESHED received "
        	    				+ delta
        	    				+ " msec after refreshPackages call");
                    break;
                }
                try {
                    Thread.sleep(250L);
                } catch(InterruptedException ignore) {
                }
            }
        } finally {
        	ctx.getBundleContext().removeFrameworkListener(this);
        }
	}
}
