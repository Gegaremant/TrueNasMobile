package com.gegaremant.truenasmobile.data.api

import com.gegaremant.truenasmobile.data.ApiResult
import com.gegaremant.truenasmobile.data.TrueNASClient
import java.lang.reflect.Type

abstract class BaseApiService(protected open val manager: TrueNASApiManager) {
    protected suspend fun <T> apiCallWithResult(
        method: String,
        params: List<Any?> = listOf(),
        resultType: Type
    ): ApiResult<T> = manager.callWithResult(method, params, resultType)
}
