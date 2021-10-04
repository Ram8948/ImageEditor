package com.ramosoft.imageeditor
import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.ramosoft.imageeditor.databinding.FragmentMainBinding
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment(R.layout.fragment_main) {
    private lateinit var navController: NavController
    private lateinit var binding: FragmentMainBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)
        navController = Navigation.findNavController(view)

        binding.selfieButton.setOnClickListener()
        {
            println("selfieButton")
            clickSelfie()
        }
        binding.galleryButton.setOnClickListener()
        {
            println("galleryButton")
            clickGallery()
//            navController!!.navigate(R.id.action_mainFragment_to_editFragment)
        }
        when {
            (activity as? MainActivity)!!.cropThenRotateBitmap != null -> {
                binding.imageView.setImageBitmap((activity as? MainActivity)!!.cropThenRotateBitmap)
                binding.textView.text = resources.getString(R.string.edited_image)
                makeBitmapNull()
            }
            (activity as? MainActivity)!!.rotateThenCropBitmap != null -> {
                binding.imageView.setImageBitmap((activity as? MainActivity)!!.rotateThenCropBitmap)
                binding.textView.text = resources.getString(R.string.edited_image)
                makeBitmapNull()
            }
            (activity as? MainActivity)!!.rotateBitmap != null -> {
                binding.imageView.setImageBitmap((activity as? MainActivity)!!.rotateBitmap)
                binding.textView.text = resources.getString(R.string.edited_image)
                makeBitmapNull()
            }
            (activity as? MainActivity)!!.croppedBitmap != null -> {
                binding.imageView.setImageBitmap((activity as? MainActivity)!!.croppedBitmap)
                binding.textView.text = resources.getString(R.string.edited_image)
                makeBitmapNull()
            }
            (activity as? MainActivity)!!.bitmap != null -> {
                binding.imageView.setImageBitmap((activity as? MainActivity)!!.bitmap)
                binding.textView.text = resources.getString(R.string.edited_image)
                makeBitmapNull()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhoto()
            }
        }
    }

    private var currentPhotoPath: String? = null

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HH:mm:ss").format(Date())
        (activity as? MainActivity)!!.imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            (activity as? MainActivity)!!.imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        println(currentPhotoPath)
        return image
    }


    private fun getPhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                println(ex.message)
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "com.ramosoft.imageeditor.fileprovider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                startActivityForResult(takePictureIntent, 1)
            }
        }
    }


    private fun makeBitmapNull() {
        (activity as? MainActivity)!!.croppedBitmap = null
        (activity as? MainActivity)!!.rotateBitmap = null
        (activity as? MainActivity)!!.cropThenRotateBitmap = null
        (activity as? MainActivity)!!.rotateThenCropBitmap = null
    }
    @Throws(IOException::class)
    fun clickSelfie() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            getPhoto()
        }
    }

    private val PICK_IMAGE = 2

    private fun clickGallery() {
        println("clickGallery")
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("onActivityResult")
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            try {
                (activity as? MainActivity)!!.bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                println((activity as? MainActivity)!!.bitmap)
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$(activity as? MainActivity)!!.imageFileName.jpg")
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES
                )
                val resolver: ContentResolver = requireActivity().contentResolver
                (activity as? MainActivity)!!.uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                var imageOutStream: OutputStream? = null
                try {
                    if ((activity as? MainActivity)!!.uri == null) {
                        throw IOException("Failed to insert MediaStore row")
                    }
                    imageOutStream = resolver.openOutputStream((activity as? MainActivity)!!.uri!!)
                    if (!(activity as? MainActivity)!!.bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                        throw IOException("Failed to compress (activity as? MainActivity)!!.bitmap")
                    }
                    Toast.makeText(requireContext(), "Image Saved", Toast.LENGTH_SHORT).show()
                } finally {
                    imageOutStream?.close()
//                    val intent = Intent(this, EditImageActivity::class.java)
//                    startActivity(intent)
                    navController.navigate(R.id.action_mainFragment_to_editFragment)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                assert(data != null)
                (activity as? MainActivity)!!.uri1 = data!!.data
                println((activity as? MainActivity)!!.uri1.toString())
                (activity as? MainActivity)!!.bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, (activity as? MainActivity)!!.uri1)
            } catch (e: Exception) {
                println(e.message)
            }
//            val intent = Intent(this, EditImageActivity::class.java)
//            startActivity(intent)
            navController.navigate(R.id.action_mainFragment_to_editFragment)
        }
    }
}
