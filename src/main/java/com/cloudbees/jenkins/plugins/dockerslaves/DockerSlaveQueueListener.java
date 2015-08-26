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
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueListener;
import hudson.model.queue.SubTask;
import jenkins.model.Jenkins;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * As an alternative to {@link hudson.slaves.Cloud}, listen the Queue for Jobs to get scheduled, and when label match
 * immediately start a fresh new container executor with a unique label to enforce exclusive usage.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class DockerSlaveQueueListener extends QueueListener {

    // TODO make this configurable
    private Set<LabelAtom> labels = LabelAtom.parse("docker");

    @Override
    public void onEnterBuildable(final Queue.BuildableItem bi) {

        if (bi.task instanceof Job && bi.getAssignedLabel().matches(labels)) {
            Job job = (Job) bi.task;

            final String id = Long.toHexString(System.nanoTime()); //  UUID.randomUUID().toString();
            final Label label = Label.get("docker_" + id);
            bi.addAction(new DockerLabelAssignmentAction(label));

            // Immediately create a Docker slave for this item
            Computer.threadPoolForRemoting.submit(new ProvisioningCallback(id, bi.getId()));

        }
    }

    private static class DockerLabelAssignmentAction implements LabelAssignmentAction {

        private final Label label;

        public DockerLabelAssignmentAction(Label label) {
            this.label = label;
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return null;
        }

        @Override
        public Label getAssignedLabel(SubTask task) {
            return label;
        }
    }

    private class ProvisioningCallback implements Callable<Node> {

        private final String id;
        private final long queueItem;

        public ProvisioningCallback(String id, long queueItem) {
            this.id = id;
            this.queueItem = queueItem;
        }

        @Override
        public Node call() throws Exception {
            DockerSlave slave = new DockerSlave("Docker slave " + id, "docker_"+id);
            Jenkins.getInstance().addNode(slave);
            while (true) {
                LOGGER.fine("Waiting for container to connect as online slave");
                if (slave.getComputer().isOnline()) {
                    break;
                }
                Thread.sleep(1000);
            }
            Jenkins.getInstance().addNode(slave);


            // if anything goes wrong, retrieve the task and cancel it
            /*
            final Queue queue = Jenkins.getInstance().getQueue();
            final Queue.Item item = queue.getItem(queueItem);
            if (item != null) {
                queue.cancel(item);
            }
            */

            return slave;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(DockerSlaveQueueListener.class.getName());
}
