package org.jfrog.hudson.util;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Hudson;
import hudson.remoting.Which;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.BuildInfoExtractor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * @author Noam Y. Tenne
 */
public class PluginDependencyHelper {

    public static FilePath getActualDependencyDirectory(File localDependencyFile, FilePath rootPath)
            throws IOException, InterruptedException {

        File localDependencyDir = localDependencyFile.getParentFile();
        String pluginVersion = Hudson.getInstance().getPluginManager().getPlugin("artifactory").getVersion();
        if (pluginVersion.contains(" ")) {
            //Trim the plugin version in case we're working on a snapshot version (contains illegal chars)
            pluginVersion = StringUtils.split(pluginVersion, " ")[0];
        }

        FilePath remoteDependencyDir = new FilePath(rootPath, "cache/artifactory-plugin/" + pluginVersion);

        if (!remoteDependencyDir.exists()) {
            remoteDependencyDir.mkdirs();
        }

        //Check if the dependencies have already been transferred successfully
        FilePath remoteDependencyMark = new FilePath(remoteDependencyDir, "ok");
        if (!remoteDependencyMark.exists()) {

            File[] localDependencies = localDependencyDir.listFiles();
            for (File localDependency : localDependencies) {
                if (localDependency.getName().equals("classes.jar"))
                    // skip classes in this plugin source tree.
                    // TODO: for a proper long term fix, see my comment in JENKINS-18401
                    continue;
                FilePath remoteDependencyFilePath = new FilePath(remoteDependencyDir, localDependency.getName());
                if (!remoteDependencyFilePath.exists()) {
                    FilePath localDependencyFilePath = new FilePath(localDependency);
                    localDependencyFilePath.copyTo(remoteDependencyFilePath);
                }
            }

            //Mark that all the dependencies have been transferred successfully for future references
            remoteDependencyMark.touch(System.currentTimeMillis());
        }

        return remoteDependencyDir;
    }

    public static File getExtractorJar(EnvVars env) throws IOException {
        String libPath = env.get("ARTIFACTORY_JARS_LIB");
        if (StringUtils.isBlank(libPath)) {
            // use the classworlds conf packaged with this plugin and resolve the extractor libs
            return Which.jarFile(BuildInfoExtractor.class);
        } else {
            FileFilter fileFilter = new WildcardFileFilter("build-info-extractor-*.jar");
            File[] jars = new File(libPath).listFiles(fileFilter);
            if (ArrayUtils.isEmpty(jars)) {
                throw new IOException("Artifactory jars lib util doesn't contain the build info extractors");
            }
            return jars[0];
        }
    }

}
