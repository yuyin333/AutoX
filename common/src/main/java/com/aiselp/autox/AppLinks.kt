package com.aiselp.autox

/**
 * 本 App 对外链接的**唯一来源**。
 *
 * 这些地址原先全部指向原作者仓库 `aiselp/AutoX`。本仓库是个人 fork（`yuyin333/AutoX`），
 * 继续沿用上游地址会有两个实际后果：
 * 1. 每次启动都会向上游仓库发请求（`api.github.com/repos/aiselp/AutoX/releases`）；
 * 2. App 内「检查更新 / 下载」会把用户引导去装**上游**的安装包 —— 那份包用的是上游签名，
 *    与本仓库的 release 密钥不同，用户装不上（或被要求卸载重装）。
 *
 * 因此统一改为自己的仓库。以后换仓库**只改 [GITHUB_REPO] 一处**，派生地址自动跟随，
 * 避免"改了两处漏一处"（同类问题在本项目已出现过多次）。
 *
 * 下面全部是 `const val`，用字符串模板由 [GITHUB_REPO] 拼出，因此仍是编译期常量，
 * 可以直接作为 Retrofit 注解参数。
 */
object AppLinks {

    /** GitHub 仓库，owner/repo 形式。 */
    const val GITHUB_REPO = "yuyin333/AutoX"

    /** 项目主页（抽屉里的「项目地址」）。 */
    const val PROJECT = "https://github.com/$GITHUB_REPO"

    /** Release 列表（抽屉里的「下载」）。 */
    const val RELEASES = "$PROJECT/releases"

    /** Issue 反馈（抽屉里的「问题反馈」）。 */
    const val ISSUES = "$PROJECT/issues"

    // ---- GitHub Releases API（Retrofit 用，baseUrl 为 https://api.github.com/）----

    /** `/repos/<owner>/<repo>/releases` */
    const val API_RELEASES = "/repos/$GITHUB_REPO/releases"

    /** `/repos/<owner>/<repo>/releases/latest` */
    const val API_LATEST_RELEASE = "$API_RELEASES/latest"

    /** `/repos/<owner>/<repo>/releases/tags`（后面拼 `/{tag}`） */
    const val API_RELEASE_BY_TAG = "$API_RELEASES/tags"
}
