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

import java.util.Hashtable;

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.osgi.installer.OsgiInstallerStatistics;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator, FrameworkListener, BundleListener {

    /** Interface of the log service */
    private static String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    /** Vendor of all registered services. */
    private static final String VENDOR = "The Apache Software Foundation";

    private OsgiInstallerImpl osgiControllerService;
    private ServiceRegistration osgiControllerServiceReg;

    /** Tracker for the log service. */
    private ServiceTracker logServiceTracker;

    private static long eventsCount;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        // listen to framework and bundle events
        context.addFrameworkListener(this);
        context.addBundleListener(this);

        this.logServiceTracker = new ServiceTracker(context, LOG_SERVICE_NAME, null);
        this.logServiceTracker.open();
        Logger.setTracker(this.logServiceTracker);
        // register OsgiController service
        {
            final Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Install Controller Service");
            props.put(Constants.SERVICE_VENDOR, VENDOR);

            this.osgiControllerService = new OsgiInstallerImpl(context);
            final String [] serviceInterfaces = {
                    OsgiInstaller.class.getName(),
                    OsgiInstallerStatistics.class.getName()
            };
            osgiControllerServiceReg = context.registerService(serviceInterfaces, osgiControllerService, props);
        }
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) {
    	context.removeBundleListener(this);
    	context.removeFrameworkListener(this);

        if ( this.osgiControllerService != null ) {
            this.osgiControllerService.deactivate();
            this.osgiControllerService = null;
        }
        if ( this.osgiControllerServiceReg != null ) {
            this.osgiControllerServiceReg.unregister();
            this.osgiControllerServiceReg = null;
        }
        Logger.setTracker(null);
    	if ( this.logServiceTracker != null ) {
    	    this.logServiceTracker.close();
    	    this.logServiceTracker = null;
    	}
    }

    /** Used for tasks that wait for a framework or bundle event before retrying their operations */
    public static long getTotalEventsCount() {
        return eventsCount;
    }

    /**
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public void frameworkEvent(final FrameworkEvent event) {
        eventsCount++;
    }

    public void bundleChanged(final BundleEvent event) {
        eventsCount++;
    }
}
