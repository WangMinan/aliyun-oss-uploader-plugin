package edu.npu;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class AliyunOssUploader extends Notifier implements SimpleBuildStep {

    private final String endPoint;
    private final String stsUrl;
    private final String accessKeyId;
    private final Secret accessKeySecret;
    private final String bucketName;
    private final String localPath;
    private final String remotePath;
    private final int partSize;
    private final int taskNum;

    @DataBoundConstructor
    public AliyunOssUploader(String endPoint, String stsUrl,
                             String accessKeyId, Secret accessKeySecret,
                             String bucketName, String localPath,
                             String remotePath, int partSize, int taskNum) {
        this.endPoint = endPoint;
        this.stsUrl = stsUrl;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.partSize = partSize;
        this.taskNum = taskNum;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getStsUrl() {
        return stsUrl;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public Secret getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public int getPartSize() {
        return partSize;
    }

    public int getTaskNum() {
        return taskNum;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace,
                        @NonNull EnvVars env, @NonNull Launcher launcher,
                        @NonNull TaskListener listener) {
        listener.getLogger().println(Messages.AliyunOssUploader_PERFORM_LOGGER_StartUploading());
        boolean upload = AliyunOssUploaderHelper.upload(endPoint, stsUrl,
                accessKeyId, accessKeySecret.getPlainText(), bucketName,
                localPath, remotePath, partSize,
                taskNum, listener);
        if (upload) {
            listener.getLogger().println(Messages.AliyunOssUploader_PERFORM_UploadSuccess());
        } else {
            listener.getLogger().println(Messages.AliyunOssUploader_PERFORM_UploadFailed());
        }
    }

    @Symbol("OSS Upload")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckEndPoint(@QueryParameter(required = true) String value) {
            if (value.isEmpty()) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_EndPointRequired());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckStsUrl(
                @QueryParameter(required = true) String value,
                @QueryParameter(required = true) String accessKeyId,
                @QueryParameter(required = true) String accessKeySecret) {
            if ((value.isEmpty() && accessKeyId.isEmpty() && accessKeySecret.isEmpty())) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_StsUrlAKSKNotEmptySameTime());
            } else if (!value.isEmpty() && (!accessKeyId.isEmpty() || !accessKeySecret.isEmpty())) {
                return FormValidation.warning(Messages.AliyunOssUploader_DescriptorImpl_AKSKWillBeIgnored());
            } else if ((value.isEmpty() && (accessKeyId.isEmpty() || accessKeySecret.isEmpty()))) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_AKSKShouldHaveValueAtSameTime());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckAccessKeyId(
                @QueryParameter(required = true) String stsUrl,
                @QueryParameter(required = true) String value,
                @QueryParameter(required = true) String accessKeySecret) {
            if ((stsUrl.isEmpty() && (value.isEmpty() || accessKeySecret.isEmpty()))) {
                return FormValidation.errorWithMarkup(Messages.AliyunOssUploader_DescriptorImpl_AKSKShouldHaveValueAtSameTime());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckBucketName(@QueryParameter(required = true) String value) {
            if (value.isEmpty()) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_BucketNameRequired());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckLocalPath(@QueryParameter(required = true) String value) {
            if (value.isEmpty()) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_LocalPathRequired());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckRemotePath(@QueryParameter(required = true) String value) {
            if (value.isEmpty()) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_RemotePathRequired());
            }else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckPartSize(@QueryParameter(required = true) int value) {
            if (value <= 0) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_LocalPathRequired());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckTaskNum(@QueryParameter(required = true) int value) {
            if (value <= 0) {
                return FormValidation.error(Messages.AliyunOssUploader_DescriptorImpl_RemotePathRequired());
            } else if (value > 2 * Runtime.getRuntime().availableProcessors()) {
                return FormValidation.warning(Messages.AliyunOssUploader_DescriptorImpl_TaskNumBiggerThan2Core());
            }else {
                return FormValidation.ok();
            }
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.AliyunOssUploader_DescriptorImpl_DisplayName();
        }
    }
}
