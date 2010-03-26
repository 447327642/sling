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
package org.apache.sling.event.impl;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.services.SlingSettingsService;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.jcr.api.SlingRepository;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@RunWith(JMock.class)
public abstract class AbstractRepositoryEventHandlerTest {

    protected AbstractRepositoryEventHandler handler;

    protected static final String REPO_PATH = "/test/events";
    protected static final String SLING_ID = "4711";

    protected static Session session;

    protected abstract Mockery getMockery();

    protected Dictionary<String, Object> getComponentConfig() {
        final Dictionary<String, Object> config = new Hashtable<String, Object>();
        config.put(AbstractRepositoryEventHandler.CONFIG_PROPERTY_REPO_PATH, REPO_PATH);

        return config;
    }

    @org.junit.BeforeClass public static void setupRepository() throws Exception {
        RepositoryTestUtil.startRepository();
        final SlingRepository repository = RepositoryTestUtil.getSlingRepository();
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        assertTrue(RepositoryTestUtil.registerNodeType(session, DistributingEventHandler.class.getResourceAsStream("/SLING-INF/nodetypes/event.cnd")));
        assertTrue(RepositoryTestUtil.registerNodeType(session, DistributingEventHandler.class.getResourceAsStream("/SLING-INF/nodetypes/folder.cnd")));
    }

    @org.junit.AfterClass public static void shutdownRepository() throws Exception {
        RepositoryTestUtil.stopRepository();
    }

    @org.junit.Before public void setup() throws Exception {
        this.handler.repository = RepositoryTestUtil.getSlingRepository();
        this.handler.classLoaderManager = new DynamicClassLoaderManager() {

            public ClassLoader getDynamicClassLoader() {
                return this.getClass().getClassLoader();
            }
        };
        // the event admin
        final EventAdmin eventAdmin = this.getMockery().mock(EventAdmin.class);
        this.handler.eventAdmin = eventAdmin;
        this.getMockery().checking(new Expectations() {{
            allowing(eventAdmin).postEvent(with(any(Event.class)));
            allowing(eventAdmin).sendEvent(with(any(Event.class)));
        }});

        // sling settings service
        this.handler.settingsService = new SlingSettingsService() {
            public String getSlingId() {
                return SLING_ID;
            }

            public URL getSlingHome() {
                return null;
            }

            public String getSlingHomePath() {
                return null;
            }
        };

        // we need a thread pool
        this.handler.threadPool = new ThreadPoolImpl();

        // lets set up the bundle context
        final BundleContext bundleContext = this.getMockery().mock(BundleContext.class);

        // lets set up the component configuration
        final Dictionary<String, Object> componentConfig = this.getComponentConfig();

        // lets set up the compnent context
        final ComponentContext componentContext = this.getMockery().mock(ComponentContext.class);
        this.getMockery().checking(new Expectations() {{
            allowing(componentContext).getBundleContext();
            will(returnValue(bundleContext));
            allowing(componentContext).getProperties();
            will(returnValue(componentConfig));
        }});

        this.handler.activate(componentContext);
        // the session is initialized in the background, so let's sleep some seconds
        Thread.sleep(2 * 1000);
    }

    @org.junit.After public void shutdown() throws Exception {
        // lets set up the bundle context with the sling id
        final BundleContext bundleContext = this.getMockery().mock(BundleContext.class);

        final ComponentContext componentContext = this.getMockery().mock(ComponentContext.class);
        this.getMockery().checking(new Expectations() {{
            allowing(componentContext).getBundleContext();
            will(returnValue(bundleContext));
        }});
        this.handler.deactivate(componentContext);
        try {
            // delete all child nodes to get a clean repository again
            final Node rootNode = (Node) session.getItem(this.handler.repositoryPath);
            final NodeIterator iter = rootNode.getNodes();
            while ( iter.hasNext() ) {
                final Node child = iter.nextNode();
                child.remove();
            }
            session.save();
        } catch ( RepositoryException re) {
            // we ignore this for the test
        }
    }

    @org.junit.Test public void testPathCreation() throws RepositoryException {
        assertTrue(session.itemExists(REPO_PATH));
    }

    final class ThreadPoolImpl implements ThreadPool {

        public void execute(Runnable runnable) {
            final Thread t = new Thread(runnable);
            t.start();
        }

        public String getName() {
            return "default";
        }

        public ThreadPoolConfig getConfiguration() {
            return new ModifiableThreadPoolConfig();
        }

    }
}
