package com.example.meme_creator

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var topEditText: EditText
    private lateinit var bottomEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var saveButton: Button
    private lateinit var openGalleryLauncher: ActivityResultLauncher<Intent>

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(!checkPermission()){
            requestPermission()
        }

        imageView = findViewById(R.id.memePreview)
        topEditText = findViewById(R.id.upperEditText)
        bottomEditText = findViewById(R.id.lowerEditText)
        generateButton = findViewById(R.id.generateButton)
        saveButton = findViewById(R.id.saveButton)

        openGalleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data
                    val selectedImage = data?.data
                    if (selectedImage != null) {
                        val inputStream = contentResolver.openInputStream(selectedImage)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        imageView.setImageBitmap(bitmap)
                    }
                }
            }

        imageView.setOnClickListener {
            openGallery()
        }

        generateButton.setOnClickListener {
            val text1 = topEditText.text.toString()
            val text2 = bottomEditText.text.toString()
            var bitmap = imageView.drawable.toBitmap()
            bitmap = scaleBitMap(bitmap)
            imageView.setImageBitmap(bitmap)
            val resultBitmap = drawTextOnBitmap(bitmap, text1, text2)
            imageView.setImageBitmap(resultBitmap)
            topEditText.setText("")
            bottomEditText.setText("")
        }

        saveButton.setOnClickListener {
            val drawable = imageView.drawable
            if (drawable != null) {
                if (checkPermission()) {
                    val bitmap = drawable.toBitmap()
                    saveBitmapToGallery(bitmap)
                } else {
                    requestPermission()
                }
            } else {
                Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(galleryIntent)
    }

    private fun scaleBitMap(bitmap: Bitmap): Bitmap {
        val width: Int = bitmap.width
        val height: Int = bitmap.height

        if (width > 1000 || height > 1000) {
            val scale = 1000.0 / maxOf(width, height)
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
        return bitmap
    }

    private fun drawTextOnBitmap(bitmap: Bitmap, text1: String, text2: String): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = TextPaint()
        val outlinePaint = TextPaint()

        paint.textSize = mutableBitmap.height / 14f
        paint.color = android.graphics.Color.WHITE
        paint.isAntiAlias = true

        outlinePaint.textSize = mutableBitmap.height / 14f
        outlinePaint.color = android.graphics.Color.BLACK
        outlinePaint.isAntiAlias = true
        outlinePaint.style = android.graphics.Paint.Style.STROKE
        outlinePaint.strokeWidth = 8f

        val typeface = Typeface.createFromAsset(assets, "fonts/impact.ttf")
        paint.typeface = typeface
        outlinePaint.typeface = typeface

        val maxWidth = mutableBitmap.width - 20

        fun drawWrappedText(
            canvas: Canvas,
            text: String,
            x: Float,
            y: Float,
            paint: TextPaint,
            outlinePaint: TextPaint,
            maxWidth: Int
        ) {
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0.0f, 1.0f)
                .setIncludePad(false)
                .build()

            val outlineStaticLayout =
                StaticLayout.Builder.obtain(text, 0, text.length, outlinePaint, maxWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0.0f, 1.0f)
                    .setIncludePad(false)
                    .build()

            canvas.save()
            canvas.translate(x, y)
            outlineStaticLayout.draw(canvas)
            staticLayout.draw(canvas)
            canvas.restore()
        }

        val topY = 10f
        drawWrappedText(canvas, text1, 10f, topY, paint, outlinePaint, maxWidth)

        val bottomY =
            mutableBitmap.height - mutableBitmap.height / 10f
        drawWrappedText(canvas, text2, 10f, bottomY, paint, outlinePaint, maxWidth)

        return mutableBitmap
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "meme_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MemeCreator")
        }

        val contentResolver = applicationContext.contentResolver
        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            outputStream.use { stream ->
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission(): Boolean {
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                    val drawable = imageView.drawable
                    if (drawable != null) {
                        saveBitmapToGallery(drawable.toBitmap())
                    }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
