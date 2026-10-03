package com.gegaremant.truenasmobile.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

object Shares {
    @Suppress("PropertyName")
    @JsonClass(generateAdapter = true)
    data class SmbShare(
        val id: Int,
        val purpose: String?,
        val path: String,
        val path_suffix: String?,
        val home: Boolean?,
        val name: String,
        val comment: String?,
        val ro: Boolean?,
        val browsable: Boolean,
        val recyclebin: Boolean?,
        val guestok: Boolean?,
        val hostsallow: List<String>?,
        val hostsdeny: List<String>?,
        val auxsmbconf: String?,
        val aapl_name_mangling: Boolean?,
        val abe: Boolean?,
        val acl: Boolean?,
        val durablehandle: Boolean?,
        val streams: Boolean?,
        val timemachine: Boolean?,
        val timemachine_quota: Int?,
        val vuid: String?,
        val shadowcopy: Boolean?,
        val fsrvp: Boolean?,
        val enabled: Boolean,
        val afp: Boolean?,
        val audit: Audit,
        val path_local: String?,
        val locked: Boolean
    ) {
        @Suppress("PropertyName")
        @JsonClass(generateAdapter = true)
        data class Audit(
            val enable: Boolean,
            val watch_list: List<String>,
            val ignore_list: List<String>
        )
    }
    @Suppress("PropertyName")
    @JsonClass(generateAdapter = true)
    data class NfsShare(
        val id: Int,
        val path: String,
        val aliases: List<String> = emptyList(),
        val comment: String = "",
        val networks: List<String> = emptyList(),
        val hosts: List<String> = emptyList(),
        val ro: Boolean = false,
        val maproot_user: String ?= "",
        val maproot_group: String ?= "",
        val mapall_user: String ?= "",
        val mapall_group: String ?= "",
        val security: List<String> = emptyList(),
        val enabled: Boolean = false,
        val locked: Boolean = false,
        val expose_snapshots: Boolean = false
    )

    /**
     * `sharing.webshare.query` - TrueNAS's built-in web file shares.
     *
     * Note: there is no `webdav` namespace in the 26.0 API at all
     * (`core.get_methods` on a 26.0.0-BETA.3 stand lists 815 methods and none
     * of them match "dav"), so web exposure is served by this namespace. Every
     * field carries a default because the stand has no web shares configured
     * and the record shape could not be observed live.
     */
    @Suppress("PropertyName")
    @JsonClass(generateAdapter = true)
    data class WebShare(
        val id: Int = 0,
        val path: String = "",
        val comment: String = "",
        val enabled: Boolean = false,
        val index_doc: Boolean? = null,
        val files: List<String>? = null
    )

    /**
     * One entry of `filesystem.listdir`.
     *
     * Verified against a 26.0.0-BETA.3 stand: the method answers with
     * `{name, path, realpath, type, size, allocation_size, mode, uid, gid,
     * is_mountpoint, acl, attributes, xattrs, zfs_attrs}`. Only the fields the
     * file browser shows are modelled; the rest are ignored on purpose.
     */
    @Suppress("PropertyName")
    @JsonClass(generateAdapter = true)
    data class DirectoryEntry(
        val name: String = "",
        val path: String = "",
        val realpath: String = "",
        /** `DIRECTORY`, `FILE`, `SYMLINK` - compared case-insensitively. */
        val type: String = "FILE",
        val size: Long = 0,
        val allocation_size: Long = 0,
        val is_mountpoint: Boolean = false
    ) {
        val isDirectory: Boolean get() = type.equals("DIRECTORY", ignoreCase = true)
    }
//    @JsonClass(generateAdapter = true)
//    enum class NfsSecurity{
//        SYS,
//        KRB5,
//        KRB5I,
//        KRB5P
//    }


}