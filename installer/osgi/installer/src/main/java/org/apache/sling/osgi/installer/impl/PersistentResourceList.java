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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.SortedSet;

import org.osgi.service.log.LogService;

/** Persistent list of RegisteredResource, used by installer to
 *  keep track of all registered resources
 */
class PersistentResourceList {
    private final HashMap<String, SortedSet<RegisteredResource>> data;
    private final File dataFile;
    
    @SuppressWarnings("unchecked")
    PersistentResourceList(OsgiInstallerContext ctx, File dataFile) {
        this.dataFile = dataFile;
        
        ObjectInputStream ois = null;
        HashMap<String, SortedSet<RegisteredResource>> restoredData = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(dataFile));
            restoredData = (HashMap<String, SortedSet<RegisteredResource>>)ois.readObject();
        } catch(Exception e) {
            if(ctx.getLogService() != null) {
                ctx.getLogService().log(LogService.LOG_INFO, 
                        "Unable to restore data, starting with empty list (" + e.toString());
            }
        } finally {
            if(ois != null) {
                try {
                    ois.close();
                } catch(IOException ignore) {
                    // ignore
                }
            }
        }
        
        data = restoredData != null ? restoredData : new HashMap<String, SortedSet<RegisteredResource>>();
    }
    
    HashMap<String, SortedSet<RegisteredResource>>  getData() {
        return data;
    }

    void save() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile));
        try {
            oos.writeObject(data);
        } finally {
            oos.close();
        }
    }
}
