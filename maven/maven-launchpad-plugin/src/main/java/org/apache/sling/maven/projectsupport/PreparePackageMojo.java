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
package org.apache.sling.maven.projectsupport;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Initialize a Sling application project by extracting bundles into the correct
 * locations.
 * 
 * @goal prepare-package
 * @requiresDependencyResolution test
 * @phase process-sources
 * @description initialize a Sling application project
 */
public class PreparePackageMojo extends AbstractBundleListMojo {

	/**
	 * Any additional bundles to include in the project's bundles directory.
	 * 
	 * @parameter
	 */
	private ArtifactDefinition[] additionalBundles;

	/**
	 * If true, install the default bundles.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean installDefaultBundles;

	/**
	 * The output directory for the default bundles in a WAR-packaged project,
	 * the base JAR (in the subdirectory named in the baseDestination
	 * parameter), and any additional bundles.
	 * 
	 * @parameter default-value="${project.build.directory}/launchpad-bundles"
	 */
	private File warOutputDirectory;

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The project's packaging type.
	 * 
	 * @parameter expression="${project.packaging}"
	 */
	private String packaging;

	/**
	 * The definition of the base JAR.
	 * 
	 * @parameter
	 */
	private ArtifactDefinition base;

	/**
	 * The definition of the package to be included to provide web support for
	 * JAR-packaged projects (i.e. pax-web).
	 * 
	 * @parameter
	 */
	private ArtifactDefinition jarWebSupport;

	/**
	 * The project's build output directory (i.e. target/classes).
	 * 
	 * @parameter expression="${project.build.outputDirectory}"
	 * @readonly
	 */
	private File buildOutputDirectory;

	public void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
		copyBaseArtifact();
		if (installDefaultBundles) {
			unpackDefaultBundles();
		}
		copyAdditionalBundles();
		copyWebSupportBundle();
		if (JAR.equals(packaging)) {
			unpackBaseArtifact();
		}
	}

	private void copyAdditionalBundles() throws MojoExecutionException {
		if (additionalBundles != null) {
			for (int i = 0; i < additionalBundles.length; i++) {
				copy(additionalBundles[i], getOutputDirectory());
			}
		}
	}

	private void copyBaseArtifact() throws MojoExecutionException {
		Artifact artifact = getBaseArtifact();
		if (artifact == null) {
			throw new MojoExecutionException(
					String
							.format(
									"Project doesn't have a base dependency of groupId %s and artifactId %s",
									base.getGroupId(), base.getArtifactId()));
		}
		File destinationDir = new File(getOutputDirectory(), baseDestination);
		File destinationFile = new File(destinationDir, artifact
				.getArtifactId()
				+ "." + artifact.getArtifactHandler().getExtension());
		if (shouldCopy(artifact.getFile(), destinationFile)) {
			try {
				getLog().info(
						String.format("Copying base artifact from %s to %s.",
								artifact.getFile(), destinationFile));
				FileUtils.copyFile(artifact.getFile(), destinationFile);
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Unable to copy base artifact.", e);
			}
		} else {
			getLog().debug(
					String.format("Skipping copy of base artifact from %s.",
							artifact.getFile()));
		}
	}

	private void copyWebSupportBundle() throws MojoExecutionException {
		if (JAR.equals(packaging)) {
			copy(jarWebSupport, getOutputDirectory());
		}

	}

	private Artifact getBaseArtifact() throws MojoExecutionException {
		Artifact baseDependency = getBaseDependency();
		if (baseDependency == null) {
			return null;
		}

		return getArtifact(base.getGroupId(), base.getArtifactId(),
				baseDependency.getVersion(), base.getType(), base
						.getClassifier());

	}

	private Artifact getBaseDependency() {
		return (Artifact) project.getArtifactMap().get(
				base.getGroupId() + ":" + base.getArtifactId());
	}

	private File getOutputDirectory() {
		if (WAR.equals(packaging)) {
			return warOutputDirectory;
		} else {
			return buildOutputDirectory;
		}
	}
	


	protected void initArtifactDefinitions(Properties dependencies) {
		if (base == null) {
			base = new ArtifactDefinition();
		}
		base.initDefaults(dependencies.getProperty("base"));

		if (jarWebSupport == null) {
			jarWebSupport = new ArtifactDefinition();
		}
		jarWebSupport.initDefaults(dependencies.getProperty("jarWebSupport"));
	}
	
	private void unpackBaseArtifact() throws MojoExecutionException {
		Artifact artifact = getBaseDependency();
		if (artifact == null) {
			throw new MojoExecutionException(
					String
							.format(
									"Project doesn't have a base dependency of groupId %s and artifactId %s",
									base.getGroupId(), base.getArtifactId()));
		}
		unpack(artifact.getFile(), buildOutputDirectory, null, null);
	}

	private void unpackDefaultBundles() throws MojoExecutionException {
		outputBundleList(getOutputDirectory());

	}
}
