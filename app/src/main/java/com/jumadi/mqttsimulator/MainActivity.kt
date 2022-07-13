package com.jumadi.mqttsimulator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import com.jumadi.mqttsimulator.databinding.ActivityMainBinding
import com.jumadi.mqttsimulator.mqtt.MqttClientHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttClientHelper: MqttClientHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etPayload.setText("{\"approval_code\":\"ejp6p9\",\"merchant_address2\":\"indonesia\",\"merchant_address1\":\"jakarta\",\"batch_group_id\":1,\"merchant_name\":\"1K\",\"merchant_id\":103,\"qr_pay_app_id\":\"5\",\"batch_num\":\"000008\",\"batch_group_name\":\"QREN-QRIS\",\"qr_pay_app_name\":\"DANA\",\"rrn\":\"000000000013\",\"issuer_name\":\"DANA\",\"qr_generated_at\":\"2022-02-18 13:24:30\",\"print_receipt_merchant_name\":\"Transjakarta 1K\",\"invoice_num\":\"000415\",\"payment_pointer\":\"QRPS-1k-armada-001-000415\",\"base_amount\":1000,\"print_receipt_address_line_2\":\"indonesia\",\"create_at\":\"2022-02-18 13:25:02\",\"print_receipt_address_line_1\":\"jakarta\",\"status\":\"success\"}")

        binding.btnConnect.setOnClickListener { connect() }

        binding.btnPublish.setOnClickListener {
            val message = if (binding.isMessage.isChecked) binding.etPayload.text.toString() else "{\"job\": \"${binding.etJob.text}\", \"payload\":${binding.etPayload.text}}"
            mqttClientHelper.publish(binding.etTopic.text.toString(), message) {
                Toast.makeText(this@MainActivity, if (it) "publish success" else "publish error",Toast.LENGTH_LONG).show()
            }
        }

        binding.isMessage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.hitJob.isVisible = false
                binding.etJob.isVisible = false
                binding.hitPayload.text = "Message"
            } else {
                binding.hitJob.isVisible = true
                binding.etJob.isVisible = true
                binding.hitPayload.text = "Payload"

            }
        }
    }

    private fun connect() {
        val ssl = if (binding.etServer.text.toString().contains("ssl")) binding.etServer.text.toString() else "ssl://36.37.119.117:61067"
        mqttClientHelper = MqttClientHelper(this, R.raw.m2mqtt_dev_ca, ssl)
        mqttClientHelper.connect {
            Toast.makeText(this@MainActivity, if (it) "connect success" else "connect error",Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClientHelper.disconnect()
    }
}