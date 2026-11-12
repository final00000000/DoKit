package com.didichuxing.doraemondemo.module.http

import android.util.Log
import com.didichuxing.doraemondemo.App
import com.didichuxing.doraemondemo.old.MainDebugActivityOkhttpV3
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


/**
 * didi Create on 2022/5/27 .
 *
 * Copyright (c) 2022/5/27 by didiglobal.com.
 *
 * @author <a href="realonlyone@126.com">zhangjun</a>
 * @version 1.0
 * @Date 2022/5/27 3:22 下午
 * @Description 用一句话说明文件功能
 */

object OkHttpMock {

    private const val URL = "https://wanandroid.com/user_article/list/0/json"

    private val client: OkHttpClient
        get() = App.httpClient ?: OkHttpClient()

    fun test() {
        val jsonObject = JSONObject()
        jsonObject.put("c", "cc")
        jsonObject.put("d", "dd")
        val request = Request.Builder()
            .url("$URL?b=bb&a=aa")
            .post(RequestBody.create(MediaType.parse("application/json"), jsonObject.toString()))
            .build()
        enqueue(request)
    }

    fun test2() {
        val body = FormBody.Builder()
            .add("c", "cc")
            .add("d", "dd")
            .build()
        val request = Request.Builder()
            .url("$URL?b=bb&a=aa")
            .post(body)
            .build()
        enqueue(request)
    }

    fun test3() {
        val request = Request.Builder()
            .url("$URL?a=aa&b=bb")
            .get()
            .build()
        enqueue(request)
    }

    private fun enqueue(request: Request) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i(MainDebugActivityOkhttpV3.TAG, "okhttp====onError===>" + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.i(
                        MainDebugActivityOkhttpV3.TAG,
                        "okhttp====onSuccess===>" + it.body()?.string()
                    )
                }
            }
        })
    }

}
