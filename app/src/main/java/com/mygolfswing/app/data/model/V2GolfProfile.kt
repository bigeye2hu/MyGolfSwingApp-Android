package com.mygolfswing.app.data.model

data class V2GolfProfile(
    val userId: String,
    val focusAreas: String = "",
    val painPoints: String = "",
    val strengths: String = "",
    val trainingGoals: String = "",
    val coachGuidance: String = "",
)
