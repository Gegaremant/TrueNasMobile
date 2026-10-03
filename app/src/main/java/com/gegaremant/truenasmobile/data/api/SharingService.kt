package com.gegaremant.truenasmobile.data.api

import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.models.Shares
import com.squareup.moshi.Types

class SharingService(var manager : TrueNASApiManager) {
    suspend fun getSmbSharesWithResult(): ApiResult<List<Shares.SmbShare>>{
        val type = Types.newParameterizedType(List::class.java, Shares.SmbShare::class.java)
        return manager.callWithResult(
            method = ApiMethods.Shares.GET_SMB_SHARES,
            params = listOf(),
            resultType = type
        )
    }
    suspend fun getNfsSharesWithResult(): ApiResult<List<Shares.NfsShare>>{
        val type = Types.newParameterizedType(List::class.java, Shares.NfsShare::class.java)
        return manager.callWithResult(
            method = ApiMethods.Shares.GET_NFS_SHARES,
            params = emptyList(),
            resultType = type,
        )
    }

    /** Web file shares (`sharing.webshare.query`). */
    suspend fun getWebSharesWithResult(): ApiResult<List<Shares.WebShare>> {
        val type = Types.newParameterizedType(List::class.java, Shares.WebShare::class.java)
        return manager.callWithResult(
            method = ApiMethods.Shares.GET_WEBSHARE,
            params = emptyList(),
            resultType = type,
        )
    }

    /**
     * Contents of a directory (`filesystem.listdir`).
     *
     * Backs the file list at the bottom of the Storage tab: the owner wants to
     * see what actually lies inside a share, and hand the file over to a
     * third-party app.
     */
    suspend fun listDirectoryWithResult(path: String): ApiResult<List<Shares.DirectoryEntry>> {
        val type = Types.newParameterizedType(List::class.java, Shares.DirectoryEntry::class.java)
        return manager.callWithResult(
            method = ApiMethods.Shares.FILESYSTEM_LISTDIR,
            params = listOf(path),
            resultType = type,
        )
    }
}