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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.osgi.installer.InstallableResource;
import org.osgi.framework.Constants;

public class RegisteredResourceTest {

    public static final String TEST_URL = "test:url";

    static File getTestBundle(String name) {
        return new File(System.getProperty("osgi.installer.base.dir"),
                "org.apache.sling.osgi.installer-" + System.getProperty("osgi.installer.pom.version") + "-" + name);
    }

    @org.junit.Test public void testResourceType() throws Exception {
        {
            final InputStream s = new FileInputStream(getTestBundle("testbundle-1.0.jar"));
            final RegisteredResource r = new LocalFileRegisteredResource(new InstallableResource("test:1.jar", s, null, "some digest", null, null));
            assertEquals(".jar URL creates a BUNDLE resource",
                    InstallableResource.TYPE_BUNDLE, r.getType());
            final InputStream rs = r.getInputStream();
            assertNotNull("BUNDLE resource provides an InputStream", rs);
            rs.close();
            assertNull("BUNDLE resource does not provide a Dictionary", r.getDictionary());
            assertEquals("RegisteredResource entity ID must match", "bundle:osgi-installer-testbundle", r.getEntityId());
        }

        {
            final Hashtable<String, Object> data = new Hashtable<String, Object>();
            data.put("foo", "bar");
            data.put("other", 2);
            final RegisteredResource r = new LocalFileRegisteredResource(new InstallableResource("test:1", null, data, null, null, null));
            assertEquals("No-extension URL with Dictionary creates a CONFIG resource",
                    InstallableResource.TYPE_CONFIG, r.getType());
            final InputStream rs = r.getInputStream();
            assertNull("CONFIG resource does not provide an InputStream", rs);
            final Dictionary<String, Object> d = r.getDictionary();
            assertNotNull("CONFIG resource provides a Dictionary", d);
            assertEquals("CONFIG resource dictionary has two properties", 2, d.size());
            assertNotNull("CONFIG resource has a pid attribute", r.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE));
        }
    }

	@org.junit.Test public void testLocalFileCopy() throws Exception {
	    final File f = getTestBundle("testbundle-1.0.jar");
        final InputStream s = new FileInputStream(f);
		final LocalFileRegisteredResource r = new LocalFileRegisteredResource(new InstallableResource("test:1.jar", s, null, "somedigest", null, null));
		assertTrue("Local file exists", r.getDataFile().exists());

		assertEquals("Local file length matches our data", f.length(), r.getDataFile().length());
	}

    @org.junit.Test public void testMissingDigest() throws Exception {
        final String data = "This is some data";
        final InputStream in = new ByteArrayInputStream(data.getBytes());

        try {
            new LocalFileRegisteredResource(new InstallableResource("test:1.jar", in, null, null, null, null));
            fail("With jar extension, expected an IllegalArgumentException as digest is null");
        } catch(IllegalArgumentException asExpected) {
        }
    }

    @org.junit.Test public void testBundleManifest() throws Exception {
        final File f = getTestBundle("testbundle-1.0.jar");
        final InstallableResource i = new InstallableResource("test:" + f.getAbsolutePath(), new FileInputStream(f), null, f.getName(), null, null);
        final RegisteredResource r = new LocalFileRegisteredResource(i);
        assertNotNull("RegisteredResource must have bundle symbolic name", r.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("RegisteredResource entity ID must match", "bundle:osgi-installer-testbundle", r.getEntityId());
    }

    @org.junit.Test public void testConfigEntity() throws Exception {
        final InstallableResource i = new InstallableResource("test:/foo/someconfig", null, new Hashtable<String, Object>(), null, null, null);
        final RegisteredResource r = new LocalFileRegisteredResource(i);
        assertNull("RegisteredResource must not have bundle symbolic name", r.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("RegisteredResource entity ID must match", "config:someconfig", r.getEntityId());
    }

    @org.junit.Test public void testConfigDigestIncludesUrl() throws Exception {
        final Dictionary<String, Object> data = new Hashtable<String, Object>();
        final InstallableResource rA = new InstallableResource("test:urlA", null, data, null, null, null);
        final InstallableResource rB = new InstallableResource("test:urlB", null, data, null, null, null);
        assertFalse(
                "Expecting configs with same data but different URLs to have different digests",
                rA.getDigest().equals(rB.getDigest()));
    }

    @org.junit.Test public void testUrlScheme() throws Exception {
        final String [] badOnes = {
                "",
                ":colonTooEarly",
                ":colonTooEarlyAgain:",
        };
        for(String url : badOnes) {
            try {
                new RegisteredResourceImpl(null,
                        new InstallableResource("test", null, new Hashtable<String, Object>(), null, null, null),
                        url);
                fail("Expected bad URL '" + url + "' to throw IllegalArgumentException");
            } catch(IllegalArgumentException asExpected) {
            }
        }

        final String [] goodOnes = {
                "noColon"
        };

        for(String url : goodOnes) {
            final RegisteredResource r = new RegisteredResourceImpl(null,
                    new InstallableResource("test", null, new Hashtable<String, Object>(), "digest1", null, null),
                    url);
            assertEquals("Expected scheme '" + url + "' for URL " + url, url, r.getUrlScheme());
        }
    }
}