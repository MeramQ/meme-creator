package com.example.meme_creator

import android.content.Intent
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
        paint.textSize = mutableBitmap.height / 14f
        paint.color = android.graphics.Color.WHITE
        paint.isAntiAlias = true

        val typeface = Typeface.createFromAsset(assets, "fonts/impact.ttf")
        paint.typeface = typeface
        paint.setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)



        val maxWidth = mutableBitmap.width - 20
        fun drawWrappedText(
            canvas: Canvas,
            text: String,
            x: Float,
            y: Float,
            paint: TextPaint,
            maxWidth: Int
        ) {
            val staticLayout = StaticLayout(
                text,
                paint,
                maxWidth,
                Layout.Alignment.ALIGN_CENTER,
                1.0f,
                0.0f,
                false
            )
            canvas.save()
            canvas.translate(x, y)
            staticLayout.draw(canvas)
            canvas.restore()
        }

        val topY = Float.fromBits(mutableBitmap.height)
        drawWrappedText(canvas, text1, 10f, topY, paint, maxWidth)

        val bottomY =
            mutableBitmap.height - mutableBitmap.height / 10f
        drawWrappedText(
            canvas,
            text2,
            10f,
            bottomY,
            paint,
            maxWidth
        )

        return mutableBitmap
    }
}
