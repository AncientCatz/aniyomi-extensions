package eu.kanade.tachiyomi.animeextension.en.dramacool.extractors

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.Headers
import okhttp3.OkHttpClient

class StreamSBExtractor(private val client: OkHttpClient) {

    private val hexArray = "0123456789ABCDEF".toCharArray()

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun videosFromUrl(url: String, headers: Headers): List<Video> {
        val sbUrl = url.substringBefore("/e")
        val id = url.substringAfter("e/").substringBefore("?")
        val master = "$sbUrl/sources48/${bytesToHex("||$id||||streamsb".toByteArray())}/"
        val json = Json.decodeFromString<JsonObject>(
            client.newCall(GET(master, headers))
                .execute().body!!.string()
        )
        val masterUrl = json["stream_data"]!!.jsonObject["file"].toString().trim('"')
        val masterPlaylist = client.newCall(GET(masterUrl, headers)).execute().body!!.string()
        val videoList = mutableListOf<Video>()
        masterPlaylist.substringAfter("#EXT-X-STREAM-INF:").split("#EXT-X-STREAM-INF:").forEach {
            val quality = "StreamSB:" + it.substringAfter("RESOLUTION=").substringAfter("x").substringBefore(",") + "p"
            val videoUrl = it.substringAfter("\n").substringBefore("\n")
            videoList.add(Video(videoUrl, quality, videoUrl, headers = headers))
        }
        return videoList
    }
}
