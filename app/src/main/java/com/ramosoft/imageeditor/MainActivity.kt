package com.ramosoft.imageeditor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ramosoft.imageeditor.databinding.ActivityMainBinding
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    var bitmap: Bitmap? = null
    var imageFileName: String? = null
    var uri: Uri? = null
    var uri1: Uri? = null
    var resultUri: Uri? = null

    var mCurrRotation = 0
    var rotateBitmap: Bitmap? = null
    var cropThenRotateBitmap: Bitmap? = null
    var rotateThenCropBitmap: Bitmap? = null
    var croppedBitmap: Bitmap? = null
    var isRotate = false
    var fromRotation = 0f
    var toRotation = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
