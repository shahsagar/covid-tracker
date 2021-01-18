package com.sagar.assignment1

import android.location.Location
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sign(
    @ColumnInfo(name = "heart_rate") val heartRate: Double,
    @ColumnInfo(name = "respiratory_rate") val respiratoryRate: Double,
    @ColumnInfo(name = "nausea") val nausea: Float,
    @ColumnInfo(name = "headache") val headache: Float,
    @ColumnInfo(name = "diarrhea") val diarrhea: Float,
    @ColumnInfo(name = "soar_throat") val soarThroat: Float,
    @ColumnInfo(name = "fever") val fever: Float,
    @ColumnInfo(name = "muscle_ache") val muscleAche: Float,
    @ColumnInfo(name = "loss_of_smell_or_taste") val lossOfSmellOrTaste: Float,
    @ColumnInfo(name = "cough") val cough: Float,
    @ColumnInfo(name = "shortness_of_breath") val shortnessOfBreath: Float,
    @ColumnInfo(name = "feeling_tired") val feelingTired: Float,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    companion object {
        fun create(
            heartRate: Double, respiratoryRate: Double,
            symptomRatings: MutableMap<String, Float>,
            location: Location?
        ) = Sign(
            heartRate,
            respiratoryRate,
            symptomRatings["Nausea"]!!,
            symptomRatings["Headache"]!!,
            symptomRatings["Diarrhea"]!!,
            symptomRatings["Soar Throat"]!!,
            symptomRatings["Fever"]!!,
            symptomRatings["Muscle Ache"]!!,
            symptomRatings["Loss of Smell or Taste"]!!,
            symptomRatings["Cough"]!!,
            symptomRatings["Shortness of Breath"]!!,
            symptomRatings["Feeling Tired"]!!,
            location?.latitude,
            location?.longitude
        )

    }

    override fun toString(): String {
        return "Sign(heartRate=$heartRate, respiratoryRate=$respiratoryRate, latitude=$latitude, longitude=$longitude, id=$id)"
    }
}