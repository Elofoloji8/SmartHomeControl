package com.elo.smarthomecontrol.TcpManager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.Socket

class TcpManager(
    private val espIp: String = "192.168.200.122",
    private val port: Int = 8080
) {

    // 🎨 RGB → r,g,b
    fun sendRgb(r: Int, g: Int, b: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(espIp, port)
                val out = PrintWriter(socket.getOutputStream(), true)

                val msg = "$r,$g,$b\n"
                out.print(msg)
                out.flush()

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 💡 LED OFF (BUTON İÇİN)
    fun sendOff() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(espIp, port)
                val out = PrintWriter(socket.getOutputStream(), true)

                out.print("OFF\n")
                out.flush()

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🚪 SERVO
    fun sendServo(open: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(espIp, port)
                val out = PrintWriter(socket.getOutputStream(), true)

                val cmd = if (open) "SERVO:OPEN\n" else "SERVO:CLOSE\n"
                out.print(cmd)
                out.flush()

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //GAZ SENSÖRÜ
    fun requestGasStatus(onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(espIp, port)
                val out = PrintWriter(socket.getOutputStream(), true)
                val input = socket.getInputStream().bufferedReader()

                out.println("GAS?")
                val response = input.readLine()

                socket.close()

                withContext(Dispatchers.Main) {
                    onResult(response == "GAS:VAR")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // WATER
    fun requestWaterLevel(onResult: (Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(espIp, port)
                val out = PrintWriter(socket.getOutputStream(), true)
                val input = socket.getInputStream().bufferedReader()

                out.println("WATER?")
                val response = input.readLine()

                socket.close()

                if (response != null && response.startsWith("WATER:")) {
                    val value = response
                        .removePrefix("WATER:")
                        .toIntOrNull() ?: 0

                    withContext(Dispatchers.Main) {
                        onResult(value)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //PIR
    fun requestPirStatus(onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket(espIp, port)
                val out = PrintWriter(socket.getOutputStream(), true)
                val input = socket.getInputStream().bufferedReader()

                out.println("PIR?")
                val response = input.readLine()

                socket.close()

                withContext(Dispatchers.Main) {
                    onResult(response == "PIR:VAR")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}