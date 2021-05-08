package com.cctv.cbox.manifest

import java.io.File
import com.cctv.cbox.util.Log

object XmlnsSweeper {

    private const val TEMP_NS_PREFIX = "TEMP_NS_PREFIX"
    private const val STANDARD_NS_PREFIX = "xmlns"
    // private const val ANDROID_MANIFEST_NAME = "AndroidManifest.xml"

    var xmlnsShouldSweep = arrayListOf("android=\"http://schemas.android.com/apk/res/android\"")

    fun sweep(processManifestOutputFilePath: String) {

        val manifestFile = File(processManifestOutputFilePath)
        Log.info("start sweep manifest xmlns at $processManifestOutputFilePath")
        if (!manifestFile.exists() /*|| manifestFile.name != ANDROID_MANIFEST_NAME*/) {
            return
        }
        val manifestFileContent = manifestFile.readText(Charsets.UTF_8)
        Log.info("\r\n\r\nstart sweep manifest xmlns:\r\n$manifestFileContent")
        val builder = StringBuilder(manifestFileContent)

        xmlnsShouldSweep.forEach { xmlnsItem ->

            val fullName = "$STANDARD_NS_PREFIX:$xmlnsItem"

            // maintain first xmlns
            val firstIndex = builder.indexOf(fullName)
            if (firstIndex < 0) {
                return
            }
            builder.replace(firstIndex, firstIndex + STANDARD_NS_PREFIX.length, TEMP_NS_PREFIX)

            // replace all except first xmlns
            replaceAll(builder, fullName, "")

            // recovery first item
            replaceAll(builder, TEMP_NS_PREFIX, STANDARD_NS_PREFIX)

        }

        // manifestFile.text = builder.toString()
        manifestFile.writeText(builder.toString())
    }

    private fun replaceAll(sb: StringBuilder, regex: String, replacement: String) {
        var aux = sb.toString()
        aux = aux.replace(regex, replacement)
        sb.setLength(0)
        sb.append(aux)
    }


}
