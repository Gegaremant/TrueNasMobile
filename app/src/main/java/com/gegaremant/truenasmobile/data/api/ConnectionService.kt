package com.gegaremant.truenasmobile.data.api

import com.gegaremant.truenasmobile.data.ApiResult

class ConnectionService(val manager: TrueNASApiManager){
    suspend fun pingConnectionWithResult(): ApiResult<String> {
        return manager.callWithResult(
            method = ApiMethods.Connection.CONNECTION_KEEP_ALIVE,
            params = listOf(),
            resultType = String::class.java
        )
    }
}