/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.osgi.installer.it;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/** Repeatedly install/remove/reinstall semi-random sets
 * 	of bundles, to stress-test the installer and framework.
 *  
 *  Randomly selects bundles to remove and reinstall in a folder
 *  containing from 4 to N bundles - by supplying a folder with many 
 *  bundles, and increasing the number of cycles executed (via 
 *  system properties, see pom.xml) the test can be turned into a 
 *  long-running stress test. 
 */
@RunWith(JUnit4TestRunner.class)
public class BundleInstallStressTest extends OsgiInstallerTestBase {
	
	public static final String PROP_BUNDLES_FOLDER = "osgi.installer.BundleInstallStressTest.bundles.folder";  
	public static final String PROP_CYCLE_COUNT = "osgi.installer.BundleInstallStressTest.cycle.count";
	public static final String PROP_EXPECT_TIMEOUT_SECONDS = "osgi.installer.BundleInstallStressTest.expect.timeout.seconds";
	public static final int MIN_TEST_BUNDLES = 4;
	
	/** Folder where test bundles are found */
	private File bundlesFolder;
	
	/** How many cycles to run */
	private int cycleCount;
	
	/** List of available test bundles */
	private List<File> testBundles;
	
	/** Always use the same random sequence */
	private Random random;
	
	/** Timeout for expectBundles() */
	private long expectBundlesTimeoutMsec;
	
	/** Synchronize (somewhat) with OSGi operations, to be fair */
	private EventsDetector eventsDetector;
	public static final long MSEC_WITHOUT_EVENTS = 1000L;
	
    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
    	return defaultConfiguration();
    }
    
    @Before
    public void setUp() {
        setupInstaller();
        
        final String bf = System.getProperty(PROP_BUNDLES_FOLDER);
        if(bf == null) {
        	fail("Missing system property: " + PROP_BUNDLES_FOLDER);
        }
        bundlesFolder = new File(bf);
        if(!bundlesFolder.isDirectory()) {
        	fail("Bundles folder '" + bundlesFolder.getAbsolutePath() + "' not found");
        }
        
        final String cc = System.getProperty(PROP_CYCLE_COUNT);
        if(cc == null) {
        	fail("Missing system property:" + PROP_CYCLE_COUNT);
        }
        cycleCount = Integer.parseInt(cc);
        
        final String et = System.getProperty(PROP_EXPECT_TIMEOUT_SECONDS);
        if(et == null) {
        	fail("Missing system property:" + PROP_EXPECT_TIMEOUT_SECONDS);
        }
        expectBundlesTimeoutMsec = Integer.parseInt(et) * 1000L;
        
        log(LogService.LOG_INFO, getClass().getSimpleName() 
        		+ ": cycle count=" + cycleCount
        		+ ", expect timeout (msec)=" + expectBundlesTimeoutMsec
        		+ ", test bundles folder=" + bundlesFolder.getAbsolutePath());
        
        testBundles = new LinkedList<File>();
        final String [] files = bundlesFolder.list();
        for(String filename : files) {
        	if(filename.endsWith(".jar")) {
        		testBundles.add(new File(bundlesFolder, filename));
        	}
        }
        
        if(testBundles.size() < MIN_TEST_BUNDLES) {
        	fail("Found only " + testBundles.size() 
        			+ " bundles in test folder, expected at least " + MIN_TEST_BUNDLES
        			+ " (test folder=" + bundlesFolder.getAbsolutePath() + ")"
        			);
        }
        
        random = new Random(42 + cycleCount);
        eventsDetector = new EventsDetector(bundleContext);
    }
    
    @After
    public void tearDown() {
        super.tearDown();
        eventsDetector.close();
    }
    
    @Test
    public void testSemiRandomInstall() throws Exception {
    	if(cycleCount < 1) {
    		fail("Cycle count (" + cycleCount + ") should be >= 1");
    	}
    	
    	final int initialBundleCount = bundleContext.getBundles().length;
    	log(LogService.LOG_INFO,"Initial bundle count=" + initialBundleCount);
    	logInstalledBundles();
    	
    	// Start by installing all bundles
    	log(LogService.LOG_INFO,"Registering all test bundles, " + testBundles.size() + " resources");
    	install(testBundles);
        waitForInstallerAction("After registering all bundles", OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 
        		1, expectBundlesTimeoutMsec);
    	expectBundleCount("After installing all test bundles", initialBundleCount + testBundles.size());
    	
    	// And run a number of cycles where randomly selected bundles are removed and reinstalled
    	for(int i=0; i < cycleCount; i++) {
    		final long start = System.currentTimeMillis();
    		log(LogService.LOG_DEBUG, "Test cycle " + i + ", semi-randomly selecting a subset of our test bundles");
    		final List<File> toInstall = selectRandomBundles();
        	log(LogService.LOG_INFO,"Re-registering " + toInstall.size() + " randomly selected resources (other test bundles should be uninstalled)");
    		install(toInstall);
            waitForInstallerAction("At cycle " + i, OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 
            		1, expectBundlesTimeoutMsec);
            eventsDetector.waitForNoEvents(MSEC_WITHOUT_EVENTS, expectBundlesTimeoutMsec);
        	expectBundleCount("At cycle " + i, initialBundleCount + toInstall.size());
        	log(LogService.LOG_INFO,"Test cycle " + i + " successful, " 
        			+ toInstall.size() + " bundles, " 
        			+ (System.currentTimeMillis() - start) + " msec");
    	}
    }
    
    private void install(List<File> bundles) throws IOException {
    	final List<InstallableResource> toInstall = new LinkedList<InstallableResource>();
    	for(File f : bundles) {
    		toInstall.add(getInstallableResource(f, f.getAbsolutePath() + f.lastModified()));
    	}
    	installer.registerResources(toInstall, URL_SCHEME);
    }
    
    private void expectBundleCount(String info, final int nBundles) throws Exception {
    	log(LogService.LOG_INFO,"Expecting " + nBundles + " bundles to be installed");
    	final Condition c = new Condition() {
    		int actualCount = 0;
			public boolean isTrue() throws Exception {
				actualCount = bundleContext.getBundles().length;
				return actualCount == nBundles;
			}
			
			@Override
			String additionalInfo() {
				return "Expected " + nBundles + " installed bundles, got " + actualCount;
			}
			
			@Override
			void onFailure() {
				log(LogService.LOG_INFO, "Failure: " + additionalInfo());
				logInstalledBundles();
			}
			
			@Override
	    	long getMsecBetweenEvaluations() { 
				return 1000L; 
			}
    	};
    	waitForCondition(info, expectBundlesTimeoutMsec, c);
    }
    
    private List<File> selectRandomBundles() {
    	final List<File> result = new LinkedList<File>();
    	for(File f : testBundles) {
    		if(random.nextBoolean()) {
    			log(LogService.LOG_DEBUG, "Test bundle selected: " + f.getName());
    			result.add(f);
    		}
    	}
    	
    	if(result.size() == 0) {
    		result.add(testBundles.get(0));
    	}
    	
    	return result;
    }
    
    private void logInstalledBundles() {
		for(Bundle b : bundleContext.getBundles()) {
			log(LogService.LOG_INFO, "Installed bundle: " + b.getSymbolicName());
		}
    }
}
