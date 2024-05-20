package com.example.meme_creator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var topEditText: EditText
    private lateinit var bottomEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var openGalleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.memePreview)
        topEditText = findViewById(R.id.upperEditText)
        bottomEditText = findViewById(R.id.lowerEditText)
        generateButton = findViewById(R.id.generateButton)

        openGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
            val bitmap = imageView.drawable.toBitmap()
            val scaledBitmap = scaleBitMap(bitmap)
            imageView.setImageBitmap(scaledBitmap)
            val resultBitmap = drawTextOnBitmap(scaledBitmap, text1, text2)
            imageView.setImageBitmap(resultBitmap)
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(galleryIntent)
    }

    private fun scaleBitMap(bitmap: Bitmap): Bitmap {
        val width: Int = bitmap.width
        val height: Int = bitmap.height
        val newWidth = width / 2
        val newHeight = height / 2
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun drawTextOnBitmap(bitmap: Bitmap, text1: String, text2: String): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint()
        paint.textSize = mutableBitmap.height / 7f
        paint.textAlign = Paint.Align.CENTER
        paint.color = android.graphics.Color.WHITE
        paint.isAntiAlias = true
        canvas.drawText(text1, mutableBitmap.width / 2f, mutableBitmap.height / 8f, paint)
        canvas.drawText(text2, mutableBitmap.width / 2f, mutableBitmap.height - mutableBitmap.height / 30f, paint)
        return mutableBitmap
    }
}
