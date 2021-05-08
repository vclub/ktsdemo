package com.cctv.cbox

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.cctv.cbox.util.Log
import com.cctv.cbox.util.StringUtil
import com.cctv.cbox.manifest.ManifestMergerImpl
import com.cctv.cbox.manifest.XmlnsSweeper
import com.cctv.cbox.exten.ComExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.DomainObjectSet
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.get
import java.io.File
import java.util.*

class ComBuild : Plugin<Project> {

    //  默认是app，直接运行assembleRelease的时候，等同于运行app:assembleRelease
    private var compileModule: String = "app"
    private lateinit var comBuild: ComExtension

    @Suppress("DefaultLocale")
    override fun apply(target: Project) {
        Log.info("ComBuild apply!!!")
        comBuild = target.extensions.create("comBuild", ComExtension::class.java)

        val taskNames = target.gradle.startParameter.taskNames.toString()
        Log.info("taskNames is $taskNames")
        val module = target.path.replace(":", "")
        Log.info("current module is $module")
        val assembleTask: AssembleTask = getTaskInfo(target.gradle.startParameter.taskNames)

        if (assembleTask.isAssemble) {
            fetchMainModuleName(target, assembleTask)
            Log.info("compile module is : $compileModule")
        }

        if (!target.hasProperty("isRunAlone")) {
            throw RuntimeException("you should set isRunAlone in $module's gradle.properties")
        }


        //对于isRunAlone==true的情况需要根据实际情况修改其值，
        // 但如果是false，则不用修改
        var isRunAlone = target.properties["isRunAlone"].toString().toBoolean()
        val mainModuleName = target.rootProject.property("mainmodulename") as String
        if (isRunAlone && assembleTask.isAssemble) {
            //对于要编译的组件和主项目，isRunAlone修改为true，其他组件都强制修改为false
            //这就意味着组件不能引用主项目，这在层级结构里面也是这么规定的
            isRunAlone = module == compileModule || module == mainModuleName
        }
        target.setProperty("isRunAlone", isRunAlone)

        //  根据配置添加各种组件依赖，并且自动化生成组件加载代码
        if (isRunAlone) {
            target.pluginManager.apply("com.android.application")
            // TODO: merge manifest
            target.afterEvaluate {
                Log.info("target manifest is: " + comBuild.targetManifest)
                (target.extensions.findByType(BaseExtension::class.java) as? AppExtension)?.applicationVariants?.all { variant ->
                    // find seal process task
                    val variantName = variant.name.capitalize()
                    Log.info("find seal process task: process${variantName}Manifest")
                    val processManifestTask = project.tasks["process${variantName}Manifest"]
                    processManifestTask.outputs.upToDateWhen { false }

                    val mergeTask = target.task("runaloneMerge${variantName}Manifest").doLast {
                        Log.info("runaloneMerge...Manifest, mergeEnabled? " + comBuild.enableManifestMerge)
                        Log.info("current path :${target.projectDir.absolutePath}")
                        if(comBuild.enableManifestMerge){
                            val manifestMergerImpl = ManifestMergerImpl(
                                File(comBuild.originalManifest),
                                File(comBuild.runAloneManifest)
                            )
                            manifestMergerImpl.merge(project.logger)
                            manifestMergerImpl.dest(comBuild.targetManifest)
                        }
                    }
                    processManifestTask.dependsOn(mergeTask)
                    processManifestTask.doLast {
                        val processManifestOutputFilePath = comBuild.targetManifest
                        Log.info("start handle xmlns:$processManifestOutputFilePath")

                        val manifestFile = File(processManifestOutputFilePath)
                        if (manifestFile.exists() /*&& manifestFile.name == "AndroidManifest.xml"*/) {
                            val manifestFileContent = manifestFile.readText()
                            val builder = StringBuilder(manifestFileContent)

                            manifestFile.writeText(builder.toString())
                            // manifestFile.text = builder.toString()
                        }
                        XmlnsSweeper.sweep(processManifestOutputFilePath)
                    }
                    true
                }?:Log.info("find seal process task: process no extensions Manifest!!!")
            }

            if (module != mainModuleName) {
                // 在RunAlone模式下 直接指定使用applicationId 减少冲突
                target.extensions.findByType(BaseExtension::class.java)?.apply {
                    defaultConfig.applicationId = comBuild.applicationName
                    sourceSets["main"].apply {
                        manifest.srcFile("src/main/runalone/mergedManifest.xml")
                        java.srcDirs(
                            "src/main/java",
                            "src/main/runalone/java",
                            "src/main/runalone/kotlin"
                        )
                        res.srcDirs("src/main/res", "src/main/runalone/res")
                        assets.srcDirs("src/main/assets", "src/main/runalone/assets")
                        jniLibs.srcDirs("src/main/jniLibs", "src/main/runalone/jniLibs")
                    }
                }
            }
            Log.info("apply plugin is com.android.application")
            if (assembleTask.isAssemble && module == compileModule) {
                // 处理组件添加
                compileComponents(assembleTask, target)
            }
        } else {
            target.pluginManager.apply("com.android.library")
            Log.info("apply plugin is com.android.library")
        }
    }


    /**
     * 根据当前的task，获取要运行的组件，规则如下：
     * assembleRelease ---app
     * app:assembleRelease :app:assembleRelease ---app
     * sharecomponent:assembleRelease :sharecomponent:assembleRelease ---sharecomponent
     * @param assembleTask
     */
    private fun fetchMainModuleName(project: Project, assembleTask: AssembleTask) {
        if (!project.rootProject.hasProperty("mainmodulename")) {
            throw RuntimeException("you should set compilemodule in rootproject's gradle.properties")
        }
        compileModule = if (assembleTask.modules.size > 0 &&
            assembleTask.modules[0].trim().isNotEmpty() &&
            assembleTask.modules[0] != "all"
        ) {
            assembleTask.modules[0]
        } else {
            project.rootProject.property("mainmodulename") as String
        }
        if (compileModule.trim().isEmpty()) {
            compileModule = "app"
        }
    }

    private fun getTaskInfo(taskNames: List<String>): AssembleTask {
        val assembleTask = AssembleTask()
        taskNames.forEach { task ->
            if (task.toUpperCase(Locale.getDefault()).contains("ASSEMBLE")
                || task.contains("aR")
                || task.contains("asR")
                || task.contains("asD")
                || task.toUpperCase(Locale.getDefault()).contains("TINKER")
                || task.toUpperCase(Locale.getDefault()).contains("INSTALL")
                || task.toUpperCase(Locale.getDefault()).contains("RESGUARD")
            ) {
                if (task.toUpperCase(Locale.getDefault()).contains("DEBUG")) {
                    assembleTask.isDebug = true
                }
                assembleTask.isAssemble = true
                Log.info("debug assembleTask info:$task")
                val strs = task.split(":")
                assembleTask.modules.add(
                    if (strs.size > 1) {
                        strs[strs.size - 2]
                    } else {
                        "all"
                    }
                )
            }
        }
        return assembleTask
    }

    /**
     * 自动添加依赖，只在运行assemble任务的才会添加依赖，因此在开发期间组件之间是完全感知不到的，这是做到完全隔离的关键
     * 支持两种语法：module或者groupId:artifactId:version(@aar),前者之间引用module工程，后者使用maven中已经发布的aar
     * @param assembleTask
     * @param project
     */
    private fun compileComponents(assembleTask: AssembleTask, project: Project) {
        val components =
            if (assembleTask.isDebug) {
                project.properties["debugComponent"]?.toString()
            } else {
                project.properties["compileComponent"]?.toString()
            }

        if (components == null || components.isEmpty()) {
            Log.info("there is no add dependencies ")
            return
        }
        val compileComponents = components.split(",")
        if (compileComponents.isEmpty()) {
            Log.info("there is no add dependencies ")
            return
        }

        compileComponents.forEach { tstr ->
            Log.info("comp is $tstr")
            var str = tstr.trim()
            if (str.startsWith(":")) {
                str = str.substring(1)
            }
            // 是否是maven 坐标
            if (StringUtil.isMavenArtifact(str)) {
                /**
                 * 示例语法:groupId:artifactId:version(@aar)
                 * compileComponent=com.luojilab.reader:readercomponent:1.0.0
                 * 注意，前提是已经将组件aar文件发布到maven上，并配置了相应的repositories
                 */
                project.dependencies.add("compile", str)
                Log.info("add dependencies lib  : $str")
            } else {
                /**
                 * 示例语法:module
                 * compileComponent=readercomponent,sharecomponent
                 */
                project.dependencies.add("compile", project.project(":$str"))
                Log.info("add dependencies project : $str")
            }
        }
    }

    private data class AssembleTask(
        var isAssemble: Boolean = false,
        var isDebug: Boolean = false,
        var modules: MutableList<String> = arrayListOf()

    )
}