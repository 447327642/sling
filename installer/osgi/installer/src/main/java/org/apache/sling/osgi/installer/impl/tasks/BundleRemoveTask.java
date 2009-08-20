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

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/** Remove a bundle from a RegisteredResource. 
 *  Creates a SynchronousRefreshPackagesTask when 
 *  executed.
 */
public class BundleRemoveTask extends OsgiInstallerTask {

    private final RegisteredResource resource;
    
    public BundleRemoveTask(RegisteredResource r) {
        this.resource = r;
    }
    
    @Override 
    public String toString() {
    	return getClass().getSimpleName() + ": " + resource;
    }
    
    @Override
    public void execute(OsgiInstallerContext ctx) throws Exception {
        super.execute(ctx);
        final String symbolicName = (String)resource.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        final Bundle b = TaskUtilities.getMatchingBundle(ctx.getBundleContext(), symbolicName);
        if(b == null) {
            throw new IllegalStateException("Bundle to remove (" + symbolicName + ") not found");
        }
        b.stop();
        b.uninstall();
        ctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask());
        ctx.incrementCounter(OsgiInstaller.OSGI_TASKS_COUNTER);
    }

    @Override
    public String getSortKey() {
        return TaskOrder.BUNDLE_REMOVE_ORDER + resource.getUrl();
    }

}
