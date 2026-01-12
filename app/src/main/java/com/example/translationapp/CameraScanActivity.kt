package com.example.translationapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
// --- NEW IMPORTS FOR OTHER SCRIPTS ---
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
// -------------------------------------
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraScanActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var btnFlash: android.widget.ImageButton
    private lateinit var btnScript: Button // <--- New Button

    private var camera: Camera? = null
    private var isFlashOn = false
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    // Tracking the selected script (Default = 0 for Latin)
    private var selectedScriptIndex = 0
    private val scriptOptions = arrayOf(
        "English (English, Spanish, etc.)",
        "Chinese",
        "Devanagari (Hindi, Sanskrit)",
        "Japanese",
        "Korean"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_scan)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        btnFlash = findViewById(R.id.btnFlash)
        btnScript = findViewById(R.id.btnScript)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnFlash.setOnClickListener { toggleFlash() }

        btnScript.setOnClickListener { showScriptDialog() }
        // ------------------------------------

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnCapture.setOnClickListener {
            takePhotoAndProcess()
        }
    }

    private fun showScriptDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Alphabet/Script")
            .setSingleChoiceItems(scriptOptions, selectedScriptIndex) { dialog, which ->
                selectedScriptIndex = which
                val shortName = scriptOptions[which].split(" ")[0]
                btnScript.text = shortName

                dialog.dismiss()
            }
            .show()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                if (camera?.cameraInfo?.hasFlashUnit() == false) {
                    btnFlash.visibility = android.view.View.GONE
                }
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera start failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlash() {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            isFlashOn = !isFlashOn
            cam.cameraControl.enableTorch(isFlashOn)
            if (isFlashOn) {
                btnFlash.setColorFilter(android.graphics.Color.YELLOW)
                Toast.makeText(this, "Flash On", Toast.LENGTH_SHORT).show()
            } else {
                btnFlash.setColorFilter(android.graphics.Color.WHITE)
                Toast.makeText(this, "Flash Off", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takePhotoAndProcess() {
        val imageCapture = imageCapture ?: return
        btnCapture.isEnabled = false
        btnCapture.text = "Scanning..."
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    processImage(imageProxy)
                }
                override fun onError(exception: ImageCaptureException) {
                    btnCapture.isEnabled = true
                    btnCapture.text = "SCAN TEXT"
                    Toast.makeText(baseContext, "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // --- CHANGED: Select the correct Recognizer based on user choice ---
            val recognizer = when (selectedScriptIndex) {
                1 -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                2 -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                3 -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                4 -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) // Latin
            }
            // ------------------------------------------------------------------

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    showResultDialog(visionText.text)
                }
                .addOnFailureListener {
                    Toast.makeText(baseContext, "No text found", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    btnCapture.isEnabled = true
                    btnCapture.text = "SCAN TEXT"
                    recognizer.close() // Close the specific client to free memory
                }
        } else {
            imageProxy.close()
        }
    }

    // ... (showResultDialog, onSupportNavigateUp, onDestroy remain the same) ...
    private fun showResultDialog(scannedText: String) {
        if (scannedText.isEmpty()) {
            Toast.makeText(this, "No text detected.", Toast.LENGTH_SHORT).show()
            return
        }
        val editText = android.widget.EditText(this).apply {
            setText(scannedText)
            setTextIsSelectable(true)
            keyListener = null
            background = null
            textSize = 16f
            setPadding(50, 40, 50, 0)
        }
        val scrollView = android.widget.ScrollView(this)
        scrollView.addView(editText)
        AlertDialog.Builder(this)
            .setTitle("Scanned Text")
            .setView(scrollView)
            .setPositiveButton("Copy All") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Scanned Text", scannedText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied all to clipboard!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}