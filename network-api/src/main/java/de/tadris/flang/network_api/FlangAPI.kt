package de.tadris.flang.network_api

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.tadris.flang.network_api.exception.*
import de.tadris.flang.network_api.model.*
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getNotationV1
import okhttp3.HttpUrl
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.reflect.Type
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit


class FlangAPI @JvmOverloads constructor(
    private val host: String,
    private val port: Int,
    private val root: String,
    private val useSSL: Boolean,
    private val loggingEnabled: Boolean = false) {

    private val client: OkHttpClient
    private val gson = GsonBuilder().serializeNulls().create()

    fun getInfo(): ServerInfo {
        return getObject(
                object : TypeToken<ServerInfo>() {}.type, buildHttpUrl()
                .addPathSegment("info")
                .build()
        ) as ServerInfo
    }

    fun register(username: String, pwdHash: String) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("register")
                .addEncodedQueryParameter("username", username)
                .addEncodedQueryParameter("pwdHash", pwdHash)
                .build()
        )
    }

    fun newSession(username: String, pwdHash: String): Session {
        return getObject(
            object : TypeToken<Session>() {}.type, buildHttpUrl()
                .addPathSegment("newSession")
                .addEncodedQueryParameter("username", username)
                .addEncodedQueryParameter("pwdHash", pwdHash)
                .build()
        ) as Session
    }

    fun login(username: String, sessionKey: String) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("login")
                .addEncodedQueryParameter("username", username)
                .addEncodedQueryParameter("key", sessionKey)
                .build()
        )
    }

    fun requestGame(timeout: Long = 7000, gameConfiguration: GameConfiguration): GameRequestResult {
        return getObject(
            object : TypeToken<GameRequestResult>() {}.type, buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment("request")
                .addPathSegment("add")
                .addEncodedQueryParameter("allowBots", gameConfiguration.isBotRequest.toString())
                .addEncodedQueryParameter("accept", true.toString())
                .addEncodedQueryParameter("timeout", timeout.toString())
                .addEncodedQueryParameter("isRated", gameConfiguration.isRated.toString())
                .addEncodedQueryParameter("infiniteTime", gameConfiguration.infiniteTime.toString())
                .addEncodedQueryParameter("time", gameConfiguration.time.toString())
                .addEncodedQueryParameter("timeIncrement", gameConfiguration.timeIncrement.toString())
                .addEncodedQueryParameter("range", gameConfiguration.ratingDiff.toString())
                .build()
        ) as GameRequestResult
    }

    fun acceptRequest(request: GameRequest): GameRequestResult{
        return getObject(
                object : TypeToken<GameRequestResult>() {}.type, buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment("request")
                .addPathSegment("accept")
                .addPathSegment(request.id.toString())
                .build()
        ) as GameRequestResult
    }

    fun getLobby(): RequestLobby {
        return getObject(
                object : TypeToken<RequestLobby>() {}.type, buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment("request")
                .addPathSegment("lobby")
                .build()
        ) as RequestLobby
    }

    fun getGameInfo(gameId: Long, moves: Int = -1, timeout: Long = 7000): GameInfo {
        return getObject(
            object : TypeToken<GameInfo>() {}.type, buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment(gameId.toString())
                .addEncodedQueryParameter("moves", moves.toString())
                .addEncodedQueryParameter("timeout", timeout.toString())
                .build()
        ) as GameInfo
    }

    fun executeMove(gameId: Long, move: Move) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment("move")
                .addPathSegment(gameId.toString())
                .addEncodedQueryParameter("moveStr", move.getNotationV1())
                .build()
        )
    }

    fun resign(gameId: Long) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment("resign")
                .addPathSegment(gameId.toString())
                .build()
        )
    }

    fun findUser(username: String): User {
        return getObject(
            object : TypeToken<User>() {}.type, buildHttpUrl()
                .addPathSegment("user")
                .addPathSegment(username)
                .build()
        ) as User
    }

    fun findGames(username: String, pageSize: Int, offset: Int): Games {
        return getObject(
                object : TypeToken<Games>() {}.type, buildHttpUrl()
                .addPathSegment("user")
                .addPathSegment(username)
                .addPathSegment("games")
                .addEncodedQueryParameter("max", pageSize.toString())
                .addEncodedQueryParameter("offset", offset.toString())
                .build()
        ) as Games
    }

    fun search(username: String): UserResult {
        return getObject(
                object : TypeToken<UserResult>() {}.type, buildHttpUrl()
                .addPathSegment("search")
                .addPathSegment(username)
                .build()
        ) as UserResult
    }

    fun getTopPlayers(type: String = "blitz"): UserResult {
        return getObject(
                object : TypeToken<UserResult>() {}.type, buildHttpUrl()
                .addPathSegment("users")
                .addPathSegment("top")
                .addEncodedQueryParameter("type", type)
                .build()
        ) as UserResult
    }

    fun getOnlinePlayers(): UserResult {
        return getObject(
            object : TypeToken<UserResult>() {}.type, buildHttpUrl()
                .addPathSegment("users")
                .addPathSegment("online")
                .build()
        ) as UserResult
    }

    fun tv(): FlangTvInfo {
        return getObject(
                object : TypeToken<FlangTvInfo>() {}.type, buildHttpUrl()
                .addPathSegment("tv")
                .build()
        ) as FlangTvInfo
    }

    fun findActive(): Games {
        return getObject(
            object : TypeToken<Games>() {}.type, buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment("findActive")
                .build()
        ) as Games
    }

    fun findComputerResults(fmn: String): ComputerResults {
        return getObject(
                object : TypeToken<ComputerResults>() {}.type, buildHttpUrl()
                .addPathSegment("computer")
                .addPathSegment("results")
                .addEncodedQueryParameter("fmn", fmn)
                .build()
        ) as ComputerResults
    }

    fun queryOpeningDatabase(fmn: String): OpeningDatabaseResult {
        return getObject(
            object : TypeToken<OpeningDatabaseResult>() {}.type, buildHttpUrl()
                .addPathSegment("opening")
                .addPathSegment("query")
                .addEncodedQueryParameter("fmn", fmn)
                .build()
        ) as OpeningDatabaseResult
    }

    fun getGlobalChatMessages(lastMessageDate: Long = 0L, timeout: Long = 7000): ChatMessages {
        return getObject(
            object : TypeToken<ChatMessages>() {}.type, buildHttpUrl()
                .addPathSegment("chat")
                .addPathSegment("global")
                .addPathSegment("messages")
                .addEncodedQueryParameter("lastMessageDate", lastMessageDate.toString())
                .addEncodedQueryParameter("timeout", timeout.toString())
                .build()
        ) as ChatMessages
    }

    fun sendMessage(text: String, attachedGame: String = "") {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("chat")
                .addPathSegment("global")
                .addPathSegment("send")
                .addEncodedQueryParameter("text", text)
                .addEncodedQueryParameter("attachedGame", attachedGame)
                .build()
        )
    }

    fun getStats(): DailyStatistics {
        return getObject(
            object : TypeToken<DailyStatistics>() {}.type, buildHttpUrl()
                .addPathSegment("stats")
                .build()
        ) as DailyStatistics
    }

    fun createDailyGameRequest(isRated: Boolean, time: Long, range: Int): GameRequestResult {
        return getObject(
            object : TypeToken<GameRequestResult>() {}.type, buildHttpUrl()
                .addPathSegment("daily")
                .addPathSegment("request")
                .addPathSegment("add")
                .addEncodedQueryParameter("isRated", isRated.toString())
                .addEncodedQueryParameter("time", time.toString())
                .addEncodedQueryParameter("range", range.toString())
                .build()
        ) as GameRequestResult
    }

    fun acceptDailyGameRequest(id: Long): GameRequestResult {
        return getObject(
            object : TypeToken<GameRequestResult>() {}.type, buildHttpUrl()
                .addPathSegment("daily")
                .addPathSegment("request")
                .addPathSegment("accept")
                .addPathSegment(id.toString())
                .build()
        ) as GameRequestResult
    }

    fun cancelDailyGameRequest(id: Long) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("daily")
                .addPathSegment("request")
                .addPathSegment("cancel")
                .addPathSegment(id.toString())
                .build()
        )
    }

    fun requestAnalysis(fmn: String): AnalysisRequest {
        // Strip resign as '#' is not good for the request
        val strippedFMN = fmn.replace("#+", "").replace("#-", "")
        return getObject(
            object : TypeToken<AnalysisRequest>() {}.type, buildHttpUrl()
                .addPathSegment("analysis")
                .addPathSegment("request")
                .addEncodedQueryParameter("fmn", strippedFMN)
                .build()
        ) as AnalysisRequest
    }

    fun getAnalysisResult(id: Long): AnalysisResult {
        return getObject(
            object : TypeToken<AnalysisResult>() {}.type, buildHttpUrl()
                .addPathSegment("analysis")
                .addPathSegment(id.toString())
                .build()
        ) as AnalysisResult
    }

    fun getAnalysisQuota(): AnalysisQuota {
        return getObject(
            object : TypeToken<AnalysisQuota>() {}.type, buildHttpUrl()
                .addPathSegment("analysis")
                .addPathSegment("quota")
                .build()
        ) as AnalysisQuota
    }

    fun getPuzzles(): PuzzleResult {
        return getObject(
            object : TypeToken<PuzzleResult>() {}.type, buildHttpUrl()
                .addPathSegment("puzzle")
                .addPathSegment("getPuzzles")
                .build()
        ) as PuzzleResult
    }

    fun solvePuzzle(puzzleId: Long, solved: Boolean): PuzzleSolveResult {
        return getObject(
            object : TypeToken<PuzzleSolveResult>() {}.type,
            buildHttpUrl()
                .addPathSegment("puzzle")
                .addPathSegment(puzzleId.toString())
                .addPathSegment("solvePuzzle")
                .addEncodedQueryParameter("solved", solved.toString())
                .build()
        ) as PuzzleSolveResult
    }

    fun ratePuzzle(puzzleId: Long, rating: Int) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("puzzle")
                .addPathSegment(puzzleId.toString())
                .addPathSegment("ratePuzzle")
                .addEncodedQueryParameter("rating", rating.toString())
                .build()
        )
    }

    fun injectPuzzle(startFMN: String, puzzleFMN: String) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("puzzle")
                .addPathSegment("injectPuzzle")
                .addEncodedQueryParameter("startFMN", startFMN)
                .addEncodedQueryParameter("puzzleFMN", puzzleFMN)
                .build()
        )
    }

    fun getPuzzleById(puzzleId: Long): Puzzle {
        return getObject(
            object : TypeToken<Puzzle>() {}.type,
            buildHttpUrl()
                .addPathSegment("puzzle")
                .addPathSegment(puzzleId.toString())
                .addPathSegment("view")
                .build()
        ) as Puzzle
    }

    fun changePassword(currentPwdHash: String, newPwdHash: String) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("changePassword")
                .addEncodedQueryParameter("currentPwdHash", currentPwdHash)
                .addEncodedQueryParameter("newPwdHash", newPwdHash)
                .build()
        )
    }

    fun addPremove(gameId: Long, moveCount: Int, move: String, fmnCondition: String? = null): PremoveAddResult {
        val urlBuilder = buildHttpUrl()
            .addPathSegment("game")
            .addPathSegment(gameId.toString())
            .addPathSegment("premove")
            .addPathSegment("add")
            .addEncodedQueryParameter("moveCount", moveCount.toString())
            .addEncodedQueryParameter("move", move)

        fmnCondition?.let { urlBuilder.addEncodedQueryParameter("fmnCondition", it) }

        return getObject(
            object : TypeToken<PremoveAddResult>() {}.type,
            urlBuilder.build()
        ) as PremoveAddResult
    }

    fun getPremoves(gameId: Long): List<Premove> {
        return getObject(
            object : TypeToken<List<Premove>>() {}.type,
            buildHttpUrl()
                .addPathSegment("game")
                .addPathSegment(gameId.toString())
                .addPathSegment("premoves")
                .build()
        ) as List<Premove>
    }

    fun removePremove(premoveId: Long) {
        fetchJsonString(
            buildHttpUrl()
                .addPathSegment("premove")
                .addPathSegment("remove")
                .addPathSegment(premoveId.toString())
                .build()
        )
    }

    fun getRole(): RoleResult {
        return getObject(
            object : TypeToken<RoleResult>() {}.type,
            buildHttpUrl()
                .addPathSegment("getRole")
                .build()
        ) as RoleResult
    }

    fun chatBanUser(username: String): String {
        return fetchJsonString(
            buildHttpUrl()
                .addPathSegment("admin")
                .addPathSegment("chatBan")
                .addPathSegment(username)
                .build()
        )
    }

    fun unbanUser(username: String): String {
        return fetchJsonString(
            buildHttpUrl()
                .addPathSegment("admin")
                .addPathSegment("unban")
                .addPathSegment(username)
                .build()
        )
    }

    private fun getObject(type: Type, url: HttpUrl): Any {
        val jsonString = fetchJsonString(url)
        return gson.fromJson(jsonString, type)
    }

    private fun buildHttpUrl(): HttpUrl.Builder {
        return HttpUrl.Builder().scheme(if (useSSL) "https" else "http").host(host).port(port)
            .addPathSegments(root)
    }

    private fun fetchJsonString(url: HttpUrl): String {
        val request: Request = Request.Builder().url(url).build()
        return fetchJsonString(url, request)
    }

    private fun fetchJsonString(url: HttpUrl, request: Request): String {
        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body!!.string()
            if (loggingEnabled) {
                println(request.toString())
                println(response.toString())
                println(responseBody)
            }
            if (response.code >= 300) {
                when (response.code) {
                    400 -> throw BadRequestException("url: $url", responseBody)
                    401 -> throw UnauthorizedException(responseBody)
                    403 -> throw ForbiddenException("url: $url", responseBody)
                    404 -> throw NotFoundException("url: $url", responseBody)
                    429 -> throw TooManyRequestsException(responseBody)
                    500 -> throw ServerErrorException()
                    503 -> throw ServiceUnavailableException(responseBody)
                    else -> throw UnknownServerExpeption(response.code, responseBody)
                }
            }
            responseBody
        } catch (e: IOException) {
            throw ServerUnreachableException(e.message)
        }
    }

    fun destroyQuit() {
        try {
            destroy()
        } catch (ignored: IOException) {
        }
    }

    @Throws(IOException::class)
    fun destroy() {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
        client.cache!!.close()
    }

    init {
        // init cookie manager
        val cookieHandler: CookieHandler = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        // init OkHttpClient
        client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieHandler))
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}