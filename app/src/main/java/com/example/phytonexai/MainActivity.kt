
package com.example.phytonexai

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import java.io.File

class MainActivity : AppCompatActivity() {

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {
                val intent = Intent(this, PreviewActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "No image selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->

            if (bitmap != null) {
                try {
                    val file = File(
                        cacheDir,
                        "camera_leaf_${System.currentTimeMillis()}.jpg"
                    )

                    file.outputStream().use { output ->
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            95,
                            output
                        )
                    }

                    val intent = Intent(
                        this,
                        PreviewActivity::class.java
                    )

                    intent.putExtra(
                        "image_uri",
                        Uri.fromFile(file).toString()
                    )

                    startActivity(intent)

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Unable to save captured image: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    bitmap.recycle()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            cameraLauncher.launch(null)
        }

        findViewById<View>(R.id.navHistory).setOnClickListener {
            startActivity(
                Intent(this, HistoryActivity::class.java)
            )
        }

        findViewById<View>(R.id.navInfo).setOnClickListener {
            startActivity(
                Intent(this, AboutActivity::class.java)
            )
        }
    }
}
