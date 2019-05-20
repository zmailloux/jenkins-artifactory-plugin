package org.jfrog.hudson.pipeline.declarative.steps.generic;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.SpecConfiguration;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.EditPropsExecutor;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.SpecUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper.EditPropertiesActionType;

@SuppressWarnings("unused")
public class EditPropsStep extends AbstractStepImpl {
    protected String serverId;
    protected String spec;
    private String props;
    private String specPath;
    private boolean failNoOp;
    private EditPropertiesActionType editType;

    EditPropsStep(String serverId, EditPropertiesActionType editType) {
        this.serverId = serverId;
        this.editType = editType;
    }

    @DataBoundSetter
    public void setSpec(String spec) {
        this.spec = spec;
    }

    @DataBoundSetter
    public void setSpecPath(String specPath) {
        this.specPath = specPath;
    }

    @DataBoundSetter
    public void setProps(String props) {
        this.props = props;
    }

    @DataBoundSetter
    public void setFailNoOp(boolean failNoOp) {
        this.failNoOp = failNoOp;
    }

    public static abstract class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        protected transient EditPropsStep step;

        protected ArtifactoryServer artifactoryServer;
        protected String spec;

        void editPropsRun(Run build, TaskListener listener, EditPropsStep step, FilePath ws, EnvVars env) throws IOException, InterruptedException {
            // Set Artifactory server
            org.jfrog.hudson.pipeline.common.types.ArtifactoryServer pipelineServer = DeclarativePipelineUtils
                    .getArtifactoryServer(build, ws, getContext(), step.serverId);
            artifactoryServer = Utils.prepareArtifactoryServer(null, pipelineServer);

            // Set spec
            SpecConfiguration specConfiguration = new SpecConfiguration(step.spec, step.specPath);
            spec = SpecUtils.getSpecStringFromSpecConf(specConfiguration, env, ws, listener.getLogger());

            EditPropsExecutor editPropsExecutor = new EditPropsExecutor(artifactoryServer, listener, build, ws, spec,
                    step.editType, step.props, step.failNoOp);
            editPropsExecutor.execute();
        }
    }
}
