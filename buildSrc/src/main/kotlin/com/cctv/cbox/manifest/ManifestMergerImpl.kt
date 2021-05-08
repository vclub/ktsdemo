package com.cctv.cbox.manifest

import groovy.util.Node
import groovy.util.XmlNodePrinter
import groovy.util.XmlParser
import org.slf4j.Logger
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import com.cctv.cbox.util.Log

class ManifestMergerImpl {

    private lateinit var manifestRoot: Node
    private lateinit var fromManifestRoot: Node

    constructor(manifestPath: String, fromManifestPath: String) {
        ManifestMergerImpl(File(manifestPath), File(fromManifestPath))
    }

    constructor(manifestFile: File, fromManifestFile: File) {
        try {
            manifestRoot = XmlParser().parse(manifestFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("ManifestMergerImpl: ${manifestFile.absolutePath} file parsed error!")
        }

        try {
            fromManifestRoot = XmlParser().parse(fromManifestFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("ManifestMergerImpl: ${fromManifestFile.absolutePath} file parsed error!")
        }
    }

    fun dest(destPath: String) {
        dest(File(destPath))
    }

    fun dest(destFile: File) {
        XmlNodePrinter(PrintWriter(FileWriter(destFile))).print(manifestRoot)
    }

    fun merge(logger: Logger) {
        doMerge(fromManifestRoot, manifestRoot, logger)
    }

    private fun doMerge(manifestRoot: Node, targetManifestNode: Node, logger: Logger) {
        targetManifestNode.attributes()["xmlns:android"] =
            "http://schemas.android.com/apk/res/android"
        val targetChildren = targetManifestNode.children()
        var fromApplicationNode: Node? = null
        var targetApplicationNode: Node? = null

        targetChildren.forEach { child ->
            (child as? Node)?.apply {
                if (name().toString() == "application") {
                    targetApplicationNode = child
                }
            }
        }
        manifestRoot.children().forEach { child ->
            (child as? Node)?.apply {
                if (name().toString() == "application") {
                    fromApplicationNode = child
                }
            }
        }

        if (targetApplicationNode != null && fromApplicationNode != null) {
            logger.debug("start merge runalone manifest")
            Log.info("start merge runalone manifest")
            fromApplicationNode?.children()?.forEach { c ->
                logger.debug("find:" + c.toString())
                Log.info("find:" + c.toString())
                val node = c as Node
                node.attributes().remove("xmlns:android")
                targetApplicationNode?.append(node)
            }
            fromApplicationNode?.attributes()?.keys?.forEach { k ->
                if (k.toString().contains("name"))
                    targetApplicationNode?.attributes()
                        ?.put("android:name", fromApplicationNode?.attribute(k))
            }

            //冗余命名空间移除
            targetChildren.forEach { child ->
                (child as? Node)?.attributes()?.remove("xmlns:android")
            }

        } else {
            val log =
                "t is null:" + (targetManifestNode == null) + " ; from is null:" + (manifestRoot == null)
            logger.error(log)
            Log.info(log)
        }

//        NodeList targetApplicationNodes = targetManifestNode.application
//        NodeList fromApplicationNodes = targetManifestNode.application
//        targetApplicationNodes.
    }
}
