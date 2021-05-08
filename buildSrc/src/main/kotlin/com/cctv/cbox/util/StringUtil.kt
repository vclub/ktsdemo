package com.cctv.cbox.util

import java.util.regex.Pattern

object StringUtil {

    /**
     * 是否是maven 坐标
     *
     * @return
     */
    fun isMavenArtifact(str: String?): Boolean {
        return if (str == null || str.isEmpty()) {
            false
        } else Pattern.matches(
            "\\S+(\\.\\S+)+:\\S+(:\\S+)?(@\\S+)?",
            str
        )
    }

}