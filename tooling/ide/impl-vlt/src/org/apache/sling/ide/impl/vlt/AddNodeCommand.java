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
package org.apache.sling.ide.impl.vlt;

import static org.apache.jackrabbit.vault.util.JcrConstants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.ide.transport.FileInfo;

public class AddNodeCommand extends JcrCommand<Void> {

    private final FileInfo fileInfo;

    public AddNodeCommand(Repository repository, Credentials credentials, FileInfo fileInfo) {

        super(repository, credentials, makePath(fileInfo));

        this.fileInfo = fileInfo;
    }

    @Override
    protected Void execute0(Session session) throws RepositoryException, IOException {

        // TODO - avoid IO
        File file = new File(fileInfo.getLocation());
        boolean isDirectory = file.isDirectory();

        boolean nodeExists = session.nodeExists(getPath());
        Node node;
        if ( nodeExists ) {
            node = session.getNode(getPath());
        } else {

            String parentLocation = Text.getRelativeParent(getPath(), 1);
            if (parentLocation.isEmpty()) {
                parentLocation = "/";
            }
            if ( !session.nodeExists(parentLocation)) {
                throw new RepositoryException("No parent found at " + parentLocation
                        + " ; it's needed to create node at " + getPath());
            }
            
            // TODO - we probably need to .content.xml as well and do all operations in one shot
            // and set the primary type properly
            String primaryType = file.isDirectory() ? NT_FOLDER : NT_FILE;

            node = session.getNode(parentLocation).addNode(fileInfo.getName(), primaryType);
        }

        if (!isDirectory) {
            Node contentNode;
            
            if ( node.hasNode(JCR_CONTENT)) {
                contentNode = node.getNode(JCR_CONTENT);
            } else {
            	if (node.getProperty(JCR_PRIMARYTYPE).getString().equals(NT_RESOURCE)) {
            		contentNode = node;
            	} else {
            		contentNode = node.addNode(JCR_CONTENT, NT_RESOURCE);
            	}
            }
            
            Binary binary = session.getValueFactory().createBinary(new FileInputStream(file));
            contentNode.setProperty(JCR_DATA, binary);
            //TODO: might have to be done differently since the client and server's clocks can differ
            // and the last_modified should maybe be taken from the server's time..
            contentNode.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
        }

        return null;
    }

}
