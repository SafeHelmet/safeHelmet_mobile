package com.safehelmet.safehelmet_mobile.parse

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.safehelmet.safehelmet_mobile.api.HttpClient
import org.json.JSONObject
import kotlin.math.log

object ParseCollector {

    private var parseData: ParseData? = null
    private var parseCrash1: ParseCrash1? = null
    private var parseCrash2: ParseCrash2? = null
    private var parseSleep: ParseSleep? = null

    var state = mutableStateOf("")
        private set

    fun processParse(parse: BaseParse?) {

        when (parse) {
            is ParseData -> parseData = parse
            is ParseCrash1 -> parseCrash1 = parse
            is ParseCrash2 -> parseCrash2 = parse
            is ParseSleep -> parseSleep = parse
        }

        if (allValuesCollected() && (parseSleep?.sleep == false || parseSleep?.sleep == null )) {
            //sendDataToBackend()
            // Aggiorna lo stato ogni volta che vengono modificati i dati
            state.value = createReadingJson()
            resetValues()
        }


    }

    private fun allValuesCollected(): Boolean {
        return parseData != null && parseCrash1 != null && parseCrash2 != null
    }

    private fun resetValues() {
        parseData = null
        parseCrash1 = null
        parseCrash2 = null
        parseSleep = null
    }

    private fun createReadingJson(): String {
        val json = JSONObject()
        json.put("temperature", parseData!!.temp)
        json.put("humidity", parseData!!.hum)
        json.put("brightness", parseData!!.lux)
        json.put("methane", parseData!!.gas[0])
        json.put("carbon_monoxide", parseData!!.gas[1])
        json.put("smoke_detection", parseData!!.gas[2])
        json.put("uses_welding_protection", parseData!!.wearables[0])
        json.put("uses_gas_protection", parseData!!.wearables[1])
        json.put("avg_x", parseCrash1!!.avg_x)
        json.put("avg_y", parseCrash1!!.avg_y)
        json.put("avg_z", parseCrash1!!.avg_z)
        json.put("avg_g", parseCrash1!!.avg_g)
        json.put("std_g", parseCrash1!!.std_g)
        json.put("std_x", parseCrash2!!.std_x)
        json.put("std_y", parseCrash2!!.std_y)
        json.put("std_z", parseCrash2!!.std_z)
        json.put("incorrect_posture", parseCrash2!!.incorrect_posture_percentuage)
        return json.toString()
    }

    private fun sendDataToBackend() {
        HttpClient.postRequest(
            "/api/v1/readings",
            createReadingJson()
        ) { response ->
            response?.body?.let { Log.i("ParseCollector", it.string()) }
        }
    }
}