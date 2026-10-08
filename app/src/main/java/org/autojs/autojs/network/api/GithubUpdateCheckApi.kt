package org.autojs.autojs.network.api

import com.aiselp.autox.AppLinks
import org.autojs.autojs.network.entity.GithubReleaseInfo
import org.autojs.autojs.network.entity.GithubReleaseInfoList
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * 更新检查接口。
 *
 * 仓库地址集中在 [AppLinks] —— 原先硬编码为原作者仓库 `aiselp/AutoX`，
 * 会把用户引导去下载上游安装包（签名不同，装不上）。现在只改 AppLinks.GITHUB_REPO 一处。
 */
interface GithubUpdateCheckApi {
    @GET(AppLinks.API_LATEST_RELEASE)
    @Headers("Cache-Control: no-cache")
    suspend fun getGithubLastReleaseInfo(): GithubReleaseInfo

    @GET("${AppLinks.API_RELEASE_BY_TAG}/{tag}")
    @Headers("Cache-Control: no-cache")
    suspend fun getGithubLastReleaseInfo(@Path("tag") tag: String): Response<GithubReleaseInfo>

    @GET(AppLinks.API_RELEASES)
    @Headers("Cache-Control: no-cache")
    suspend fun getGithubReleaseInfoList(): GithubReleaseInfoList
}
