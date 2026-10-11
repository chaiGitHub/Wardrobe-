package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Wardrobe", appName)
  }

  @Test
  fun `test generateGarmentAuditReport on all seed garments`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val stream = context.assets.open("initial_seed.json")
    val text = stream.bufferedReader().use { it.readText() }
    val json = org.json.JSONObject(text)
    val clothesArr = json.getJSONArray("clothes")
    println("Testing ${clothesArr.length()} garments from initial_seed.json")
    for (i in 0 until clothesArr.length()) {
      val obj = clothesArr.getJSONObject(i)
      val id = obj.optString("id", "g_$i")
      val name = obj.optString("Name", obj.optString("name", "Item $i"))
      val garment = com.example.data.model.GarmentEntity(
        id = id,
        name = name,
        clothesType = obj.optString("Clothes Type", obj.optString("clothesType", "")),
        material = obj.optString("material", ""),
        thickClothes = obj.optString("thick clothes", ""),
        hotAtNoon = obj.optString("Hot at noon", ""),
        diaoDai = obj.optString("diao dai", ""),
        sideBreastOk = obj.optString("side breast ok", ""),
        standBreast = obj.optString("center chest", obj.optString("stand breast", "")),
        sitBreast = obj.optString("sit breast ok", obj.optString("sit breast", "")),
        beautiful = obj.optString("beautiful", ""),
        beautifulReason = obj.optString("beatiful reason", ""),
        wet = obj.optString("wet", ""),
        wetReason = obj.optString("wet reason", ""),
        descriptions = obj.optString("Descriptions", ""),
        descriptions2 = obj.optString("Descriptions 2", ""),
        longDress = obj.optString("long dress", ""),
        price = obj.optDouble("price", 0.0),
        wearingFrequency = obj.optDouble("wearing frequency(how many times legacy washed)", 0.0).toInt(),
        trackedAppWashes = obj.optInt("Tracked App Washes", 0),
        lastWashed = obj.optString("Last Washed", ""),
        mayThrow = obj.optString("may throw", ""),
        lastComment = obj.optString("last comment or action", ""),
        aiClothesAnalysis = obj.optString("AI Clothes Analysis", ""),
        aiReports = obj.optJSONArray("aiReports")?.toString() ?: "[]"
      )
      val totalWashes = garment.wearingFrequency + garment.trackedAppWashes
      val cpw = if (garment.price > 0 && totalWashes > 0) {
        String.format(java.util.Locale.US, "$%.2f", garment.price / totalWashes)
      } else "N/A"
      val fallback = {
        com.example.util.GeminiAiService.buildDefaultGarmentErgonomicReport(context, garment, totalWashes, cpw)
      }
      val resp = com.example.util.GeminiAiService.generateGarmentAuditReport(
        context = context,
        garment = garment,
        fallbackGenerator = fallback
      )
      org.junit.Assert.assertTrue("Report for ${garment.name} should not be blank", resp.text.isNotBlank())
    }
  }

  @Test
  fun `test generateGarmentAuditReport with api key does not crash`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.GeminiAiService.saveCustomApiKey(context, "AIzaSyFakeKeyForTesting123456789")
    val garment = com.example.data.model.GarmentEntity(
      id = "test-key",
      name = "blue diao dai thick",
      price = 57.13,
      wearingFrequency = 22
    )
    val fallback = { "fallback" }
    val resp = com.example.util.GeminiAiService.generateGarmentAuditReport(
      context = context,
      garment = garment,
      fallbackGenerator = fallback
    )
    org.junit.Assert.assertNotNull(resp)
    println("Response with key: " + resp.text)
  }
}
