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
package org.apache.sling.event.impl.jobs;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.JobUtil.JobPriority;
import org.apache.sling.event.jobs.Queue;

/**
 * This object encapsulates all information about a job.
 */
public class JobImpl implements Job {

    /** Internal job property containing the resource path. */
    public static final String PROPERTY_RESOURCE_PATH = "slingevent:path";

    /** Internal job property if this is an bridged event (event admin). */
    public static final String PROPERTY_BRIDGED_EVENT = "slingevent:eventadmin";

    /** Internal job property containing optional delay override. */
    public static final String PROPERTY_DELAY_OVERRIDE = ":slingevent:delayOverride";

    /** Property for log statements. */
    public static final String PROPERTY_LOG = "slingevent:log";

    /** Property for ETA. */
    public static final String PROPERTY_ETA = "slingevent:eta";

    /** Property for Steps. */
    public static final String PROPERTY_STEPS = "slingevent:steps";

    /** Property for Step. */
    public static final String PROPERTY_STEP = "slingevent:step";

    /** Property for final message. */
    public static final String PROPERTY_MESSAGE = "slingevent:message";

    /** Property for finished jobs. */
    public static final String PROPERTY_FINISHED = "slingevent:finished";

    private final ValueMap properties;

    private final String topic;

    private final String path;

    private final String name;

    private final String jobId;

    private final boolean isBridgedEvent;

    private final List<Exception> readErrorList;

    /**
     * Create a new job instance
     *
     * @param topic The job topic
     * @param name  The unique job name (optional)
     * @param jobId The unique (internal) job id
     * @param properties Non-null map of properties, at least containing {@link #PROPERTY_RESOURCE_PATH}
     */
    @SuppressWarnings("unchecked")
    public JobImpl(final String topic,
                   final String name,
                   final String jobId,
                   final Map<String, Object> properties) {
        this.topic = topic;
        this.name = name;
        this.jobId = jobId;
        this.path = (String)properties.remove(PROPERTY_RESOURCE_PATH);
        this.isBridgedEvent = properties.get(PROPERTY_BRIDGED_EVENT) != null;
        this.readErrorList = (List<Exception>) properties.remove(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);

        this.properties = new ValueMapDecorator(properties);
        this.properties.put(JobUtil.NOTIFICATION_PROPERTY_JOB_ID, jobId);
    }

    /**
     * Get the full resource path.
     */
    public String getResourcePath() {
        return this.path;
    }

    /**
     * Is this a bridged event?
     */
    public boolean isBridgedEvent() {
        return this.isBridgedEvent;
    }

    /**
     * Did we have read errors?
     */
    public boolean hasReadErrors() {
        return this.readErrorList != null;
    }

    /**
     * Get all properties
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Update the information for a retry
     */
    public void retry() {
        final int retries = this.getProperty(Job.PROPERTY_JOB_RETRY_COUNT, Integer.class);
        this.properties.put(Job.PROPERTY_JOB_RETRY_COUNT, retries + 1);
        this.properties.remove(Job.PROPERTY_JOB_STARTED_TIME);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getTopic()
     */
    @Override
    public String getTopic() {
        return this.topic;
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getId()
     */
    @Override
    public String getId() {
        return this.jobId;
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getProperty(java.lang.String)
     */
    @Override
    public Object getProperty(final String name) {
        return this.properties.get(name);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getProperty(java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T getProperty(final String name, final Class<T> type) {
        return this.properties.get(name, type);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getProperty(java.lang.String, java.lang.Object)
     */
    @Override
    public <T> T getProperty(final String name, final T defaultValue) {
        return this.properties.get(name, defaultValue);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getPropertyNames()
     */
    @Override
    public Set<String> getPropertyNames() {
        return this.properties.keySet();
    }

    @Override
    public JobPriority getJobPriority() {
        return (JobPriority)this.getProperty(Job.PROPERTY_JOB_PRIORITY);
    }

    @Override
    public int getRetryCount() {
        return (Integer)this.getProperty(Job.PROPERTY_JOB_RETRY_COUNT);
    }

    @Override
    public int getNumberOfRetries() {
        return (Integer)this.getProperty(Job.PROPERTY_JOB_RETRIES);
    }

    @Override
    public String getQueueName() {
        return (String)this.getProperty(Job.PROPERTY_JOB_QUEUE_NAME);
    }

    @Override
    public String getTargetInstance() {
        return (String)this.getProperty(Job.PROPERTY_JOB_TARGET_INSTANCE);
    }

    @Override
    public Calendar getProcessingStarted() {
        return (Calendar)this.getProperty(Job.PROPERTY_JOB_STARTED_TIME);
    }

    @Override
    public Calendar getCreated() {
        return (Calendar)this.getProperty(Job.PROPERTY_JOB_CREATED);
    }

    @Override
    public String getCreatedInstance() {
        return (String)this.getProperty(Job.PROPERTY_JOB_CREATED_INSTANCE);
    }

    /**
     * Update information about the queue.
     */
    public void updateQueueInfo(final Queue queue) {
        this.properties.put(Job.PROPERTY_JOB_QUEUE_NAME, queue.getName());
        this.properties.put(Job.PROPERTY_JOB_RETRIES, queue.getConfiguration().getMaxRetries());
        this.properties.put(Job.PROPERTY_JOB_PRIORITY, queue.getConfiguration().getPriority());
    }

    public void setProperty(final String name, final Object value) {
        if ( value == null ) {
            this.properties.remove(name);
        } else {
            this.properties.put(name, value);
        }
    }

    /**
     * Prepare a new job execution
     */
    public void prepare() {
        this.properties.remove(JobImpl.PROPERTY_DELAY_OVERRIDE);
        this.properties.remove(JobImpl.PROPERTY_LOG);
        this.properties.remove(JobImpl.PROPERTY_ETA);
        this.properties.remove(JobImpl.PROPERTY_STEPS);
        this.properties.remove(JobImpl.PROPERTY_STEP);
        this.properties.remove(JobImpl.PROPERTY_MESSAGE);
    }

    public String update(final long eta) {
        this.setProperty(JobImpl.PROPERTY_ETA, eta);
        return JobImpl.PROPERTY_ETA;
    }

    public String startProgress(final long eta) {
        this.setProperty(JobImpl.PROPERTY_ETA, eta);
        return JobImpl.PROPERTY_ETA;
    }

    public String startProgress(final int steps) {
        this.setProperty(JobImpl.PROPERTY_STEPS, steps);
        return JobImpl.PROPERTY_STEPS;
    }

    public String setProgress(final int step) {
        this.setProperty(JobImpl.PROPERTY_STEP, step);
        return JobImpl.PROPERTY_STEP;
    }

    public String log(final String message, Object... args) {
        final String logEntry = MessageFormat.format(message, args);
        final String[] entries = this.getProperty(JobImpl.PROPERTY_LOG, String[].class);
        if ( entries == null ) {
            this.setProperty(JobImpl.PROPERTY_LOG, new String[] {logEntry});
        } else {
            final String[] newEntries = new String[entries.length + 1];
            System.arraycopy(entries, 0, newEntries, 0, entries.length);
            newEntries[entries.length] = logEntry;
            this.setProperty(JobImpl.PROPERTY_LOG, newEntries);
        }
        return JobImpl.PROPERTY_LOG;
    }

    @Override
    public String toString() {
        return "JobImpl [properties=" + properties + ", topic=" + topic
                + ", path=" + path + ", name=" + name + ", jobId=" + jobId
                + ", isBridgedEvent=" + isBridgedEvent + "]";
    }
}
