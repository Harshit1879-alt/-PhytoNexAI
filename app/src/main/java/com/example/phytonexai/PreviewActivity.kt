
package com.example.phytonexai

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PreviewActivity : AppCompatActivity() {

    private lateinit var ivLeafPreview: ImageView
    private lateinit var btnAnalyze: Button
    private lateinit var btnChangeImage: Button
    private lateinit var btnCamera: Button
    private lateinit var classifier: DiseaseClassifier

    private lateinit var resultCard: View
    private lateinit var tvResult: TextView
    private lateinit var tvConfidence: TextView

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private var selectedImageUri: Uri? = null
    private var cameraBitmap: Bitmap? = null

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                cameraBitmap = null
                ivLeafPreview.setImageURI(uri)
                resultCard.visibility = View.GONE
                btnAnalyze.isEnabled = true
            }
        }

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->
            if (bitmap != null) {
                cameraBitmap = bitmap
                selectedImageUri = null
                ivLeafPreview.setImageBitmap(bitmap)
                resultCard.visibility = View.GONE
                btnAnalyze.isEnabled = true

                // Classify the captured leaf immediately.
                analyzeImage()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        ivLeafPreview = findViewById(R.id.ivLeafPreview)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        btnChangeImage = findViewById(R.id.btnChangeImage)
        btnCamera = findViewById(R.id.btnCamera)

        resultCard = findViewById(R.id.resultCard)
        tvResult = findViewById(R.id.tvResult)
        tvConfidence = findViewById(R.id.tvConfidence)

        val uriString = intent.getStringExtra("image_uri")

        if (uriString.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Please select an image.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        selectedImageUri = Uri.parse(uriString)
        ivLeafPreview.setImageURI(selectedImageUri)

        resultCard.visibility = View.GONE

        try {
            classifier = DiseaseClassifier(this)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Model loading failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            btnAnalyze.isEnabled = false
            btnCamera.isEnabled = false
            return
        }

        btnChangeImage.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            cameraLauncher.launch(null)
        }

        btnAnalyze.setOnClickListener {
            analyzeImage()
        }
    }

    private fun analyzeImage() {
        val uri = selectedImageUri
        val bitmap = cameraBitmap

        if (uri == null && bitmap == null) {
            Toast.makeText(
                this,
                "Please select or capture a leaf image.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnAnalyze.isEnabled = false
        btnCamera.isEnabled = false
        btnChangeImage.isEnabled = false
        btnAnalyze.text = "Analyzing..."

        executor.execute {
            try {
                val result = if (bitmap != null) {
                    classifier.classify(bitmap, this)
                } else {
                    classifier.classify(uri!!, this)
                }

                runOnUiThread {
                    tvResult.text = "Prediction: ${result.disease}"
                    tvConfidence.text = String.format(
                        Locale.US,
                        "Confidence: %.2f%%",
                        result.confidence
                    )
                    ScanHistory.saveResult(
                        this,
                        result.disease,
                        result.confidence
                    )

                    resultCard.visibility = View.VISIBLE
                    btnAnalyze.text = "Analyze Leaf"
                    btnAnalyze.isEnabled = true
                    btnCamera.isEnabled = true
                    btnChangeImage.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    btnAnalyze.text = "Analyze Leaf"
                    btnAnalyze.isEnabled = true
                    btnCamera.isEnabled = true
                    btnChangeImage.isEnabled = true

                    Toast.makeText(
                        this,
                        "Detection failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()

        if (::classifier.isInitialized) {
            classifier.close()
        }

        cameraBitmap?.recycle()
        cameraBitmap = null

        super.onDestroy()
    }
}
