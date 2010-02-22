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
package org.apache.sling.event.impl.job;

import org.osgi.service.event.Event;

public interface JobStatusNotifier {

    String CONTEXT_PROPERTY_NAME = JobStatusNotifier.class.getName();

    public static final class NotifierContext {
        public final JobStatusNotifier notifier;
        public final String eventNodePath;

        public NotifierContext(JobStatusNotifier n, String path) {
            this.notifier = n;
            this.eventNodePath = path;
        }
    }

    /**
     * Send an acknowledge message that someone is processing the job.
     * @param job The job.
     * @param eventNodePath The storage node in the repository.
     * @return <code>true</code> if the ack is ok, <code>false</code> otherwise (e.g. if
     *   someone else already send an ack for this job.
     */
    boolean sendAcknowledge(Event job, String eventNodePath);

    /**
     * Notify that the job is finished.
     * If the job is not rescheduled, a return value of <code>false</code> indicates an error
     * during the processing. If the job should be rescheduled, <code>true</code> indicates
     * that the job could be rescheduled. If an error occurs or the number of retries is
     * exceeded, <code>false</code> will be returned.
     * @param job The job.
     * @param eventNodePath The storage node in the repository.
     * @param reschedule Should the event be rescheduled?
     * @return <code>true</code> if everything went fine, <code>false</code> otherwise.
     */
    boolean finishedJob(Event job, String eventNodePath, boolean reschedule);
}
