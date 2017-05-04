package com.jiangwei.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

public class PreDexTransform extends Transform {

    Project project
    // 添加构造，为了方便从plugin中拿到project对象，待会有用
    public PreDexTransform(Project project) {
        this.project = project
        def libPath = project.project(':tagdex').buildDir.absolutePath.concat(File.separator + "intermediates" + File.separator + "classes" + File.separator + "debug")
        Inject.appendClassPath(libPath)
        Inject.appendClassPath("/Users/baidu/Library/Android/sdk/platforms/android-24/android.jar")
    }

    // Transfrom在Task列表中的名字
    // TransfromClassesWithPreDexForXXXX
    @Override
    String getName() {
        return "preDex";
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)

        // Transfrom的inputs有两种类型，一种是目录，一种是jar包，要分开遍历
        inputs.each { TransformInput input ->

            input.directoryInputs.each { DirectoryInput directoryInput ->

                // 获取output目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)


                // 这里可以对input的文件做处理，比如代码注入！
                Inject.injectDir(directoryInput.file.absolutePath)


                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.each { JarInput jarInput ->

                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)


                // 这里可以对input的文件做处理，比如代码注入！
                String jarPath = jarInput.file.absolutePath;
                String projectName = project.rootProject.name;
                if (jarPath.endsWith("classes.jar")
                        && jarPath.contains("exploded-aar" + File.separator + projectName)
                        // hotutils module是用来加载dex，无需注入代码
                        && !jarPath.contains("exploded-aar" + File.separator + projectName + File.separator + "hotutils")) {
                    Inject.injectJar(jarPath)
                }

                // 将input的目录复制到output指定目录
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }
}