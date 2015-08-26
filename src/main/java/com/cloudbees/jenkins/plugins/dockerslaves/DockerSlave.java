/*
 * The MIT License
 *
 *  Copyright (c) 2015, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package com.cloudbees.jenkins.plugins.dockerslaves;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.CloudRetentionStrategy;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collections;

/**
 * An {@link AbstractCloudSlave} to setup a docker container as a jenkins executor.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerSlave extends AbstractCloudSlave {

    public DockerSlave(String name, String labelString) throws IOException, Descriptor.FormException {
        this(name, labelString, LAUNCHER);
    }

    public DockerSlave(String name, String labelString, ComputerLauncher launcher) throws Descriptor.FormException, IOException {
        super(name, "", "/usr/share/jenkins/", 1, Mode.EXCLUSIVE, labelString, launcher, ONCE, Collections.EMPTY_LIST);
    }



    @Override
    public AbstractCloudComputer createComputer() {
        return new DockerComputer(this);
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {

    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "Docker Slave";
        };

        @Override
        public boolean isInstantiable() {
            return false;
        }

    }

    // Simplest way I've found to run a docker slave, just require java available in docker image
    // I wonder we could use docker cp to install a JVM, comparable to ssh-slaves JDK installer hack
    // TODO also support classic ssh/jnlp launchers

    public static final String url = Jenkins.getInstance().getRootUrl() + "/jnlpJars/slave.jar";

    // TODO make launcher part of the 'cloud' configuration
    // and/or make this class abstract to be extended by other docker-* plugins
    private final static ComputerLauncher LAUNCHER = new CommandLauncher("docker run -i --rm java:8 sh -c '" +
            "curl "+url+ " -o slave.jar; java -jar slave.jar'");

    private final static RetentionStrategy ONCE = new CloudRetentionStrategy(1);
}
