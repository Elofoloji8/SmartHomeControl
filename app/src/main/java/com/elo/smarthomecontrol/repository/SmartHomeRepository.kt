package com.elo.smarthomecontrol.repository

import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await

class SmartHomeRepository(
    private val ref: DatabaseReference   // users/{userId}
) {

    suspend fun setFanPwm(value: Int) {
        ref.child("commands/fan_pwm").setValue(value).await()
    }

    suspend fun setBuzzer(value: Int) {
        ref.child("commands/buzzer").setValue(value).await()
    }

    suspend fun setServo(angle: Int) {
        ref.child("commands/servo").setValue(angle).await()
    }

    suspend fun setLed(r: Int, g: Int, b: Int) {
        ref.child("commands/rgb").setValue(mapOf(
            "r" to r,
            "g" to g,
            "b" to b
        )).await()
    }
}