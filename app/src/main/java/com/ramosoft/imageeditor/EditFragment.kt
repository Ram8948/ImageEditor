package com.ramosoft.imageeditor
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.ramosoft.imageeditor.databinding.FragmentEditImageBinding
import com.theartofdev.edmodo.cropper.CropImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class EditFragment : Fragment(R.layout.fragment_edit_image) {
    private lateinit var binding: FragmentEditImageBinding
    private lateinit var navController: NavController
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditImageBinding.bind(view)
        navController = Navigation.findNavController(view)
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )
        try {
            println("Edit Bitmap "+ (activity as? MainActivity)!!.bitmap)
            binding.editImageView.setImageBitmap((activity as? MainActivity)!!.bitmap)
        } catch (e: Exception) {
            println(e.message)
        }
        binding.cropButton.setOnClickListener()
        {
            crop()
        }
        binding.undoButton.setOnClickListener()
        {
            undo()
        }
        binding.rotateButton.setOnClickListener()
        {
            rotate()
        }
        binding.saveButton.setOnClickListener()
        {
            save()
        }

    }
    private fun makeBitmapNull() {
        (activity as? MainActivity)!!.mCurrRotation = 0
        (activity as? MainActivity)!!.toRotation = 0f
        (activity as? MainActivity)!!.fromRotation = 0f
        (activity as? MainActivity)!!.rotateBitmap = null
        (activity as? MainActivity)!!.croppedBitmap = null
        (activity as? MainActivity)!!.rotateThenCropBitmap = null
        (activity as? MainActivity)!!.cropThenRotateBitmap = null
    }

    private fun undo() {
        val matrix = Matrix()
        (activity as? MainActivity)!!.mCurrRotation += 90
        (activity as? MainActivity)!!.toRotation = (activity as? MainActivity)!!.mCurrRotation.toFloat()
        val rotateAnimation = RotateAnimation((activity as? MainActivity)!!.fromRotation,
            0.0F,(binding.editImageView.width/ 2).toFloat(), (binding.editImageView.height/ 2).toFloat())
        rotateAnimation.duration = 1000
        rotateAnimation.fillAfter = true
        matrix.setRotate((activity as? MainActivity)!!.toRotation)
        println((activity as? MainActivity)!!.toRotation.toString() + "TO ROTATION")
        println((activity as? MainActivity)!!.fromRotation.toString() + "FROM ROTATION")
        if ((activity as? MainActivity)!!.croppedBitmap != null) {
            (activity as? MainActivity)!!.cropThenRotateBitmap = Bitmap.createBitmap(
                (activity as? MainActivity)!!.croppedBitmap!!,
                0,
                0,
                (activity as? MainActivity)!!.croppedBitmap!!.width,
                (activity as? MainActivity)!!.croppedBitmap!!.height,
                matrix,
                true
            )
        } else {
            (activity as? MainActivity)!!.rotateBitmap = Bitmap.createBitmap(
                (activity as? MainActivity)!!.bitmap!!,
                0,
                0,
                (activity as? MainActivity)!!.bitmap?.width!!,
                (activity as? MainActivity)!!.bitmap?.height!!,
                matrix,
                true
            )
        }
        binding.editImageView.setImageBitmap((activity as? MainActivity)!!.bitmap)
        binding.editImageView.startAnimation(rotateAnimation)
        makeBitmapNull()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("onActivityResult EditFragment")
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                (activity as? MainActivity)!!.resultUri = result.uri
                binding.editImageView.setImageURI((activity as? MainActivity)!!.resultUri)
                //                Matrix matrix = new Matrix();
                val bitmapDrawable = binding.editImageView.getDrawable() as BitmapDrawable
                println(binding.editImageView.getRotation())
                (activity as? MainActivity)!!.croppedBitmap = bitmapDrawable.bitmap
                if ((activity as? MainActivity)!!.isRotate) {
                    (activity as? MainActivity)!!.rotateThenCropBitmap =
                        (activity as? MainActivity)!!.croppedBitmap
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun crop() {
        if ((activity as? MainActivity)!!.rotateBitmap != null) {
            val bytes = ByteArrayOutputStream()
            (activity as? MainActivity)!!.rotateBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(
                requireActivity().contentResolver,
                (activity as? MainActivity)!!.rotateBitmap,
                (activity as? MainActivity)!!.imageFileName.toString() + ".jpg",
                null
            )
            //            System.out.println(Uri.parse(path));
            (activity as? MainActivity)!!.uri = Uri.parse(path)
            CropImage.activity((activity as? MainActivity)!!.uri)
                .start(requireActivity(),this)
        } else if ((activity as? MainActivity)!!.uri1 != null) {
            CropImage.activity((activity as? MainActivity)!!.uri1)
                .start(requireActivity(),this)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun save() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, (activity as? MainActivity)!!.imageFileName.toString() + ".jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        val resolver = requireActivity().contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        var imageOutStream: OutputStream? = null
        try {
            if (uri == null) {
                throw IOException("Failed to insert MediaStore row")
            }
            imageOutStream = resolver.openOutputStream(uri)
            if ((activity as? MainActivity)!!.cropThenRotateBitmap != null) {
                if (!(activity as? MainActivity)!!.cropThenRotateBitmap!!.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        imageOutStream
                    )
                ) {
                    throw IOException("Failed to compress bitmap")
                }
            } else if ((activity as? MainActivity)!!.rotateThenCropBitmap != null) {
                if (!(activity as? MainActivity)!!.rotateThenCropBitmap!!.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        imageOutStream
                    )
                ) {
                    throw IOException("Failed to compress bitmap")
                }
            } else if ((activity as? MainActivity)!!.croppedBitmap != null) {
                if (!(activity as? MainActivity)!!.croppedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw IOException("Failed to compress bitmap")
                }
            } else if ((activity as? MainActivity)!!.rotateBitmap != null) {
                if (!(activity as? MainActivity)!!.rotateBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw IOException("Failed to compress bitmap")
                }
            } else {
                if (!(activity as? MainActivity)!!.bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw IOException("Failed to compress bitmap")
                }
            }
            Toast.makeText(requireContext(), "Imave Saved", Toast.LENGTH_SHORT).show()
        } finally {
            if (imageOutStream != null) {
                imageOutStream.close()
                navController.popBackStack()
            }
        }
    }

    private fun rotate() {
        (activity as? MainActivity)!!.isRotate = true
        (activity as? MainActivity)!!.mCurrRotation %= 360
        val matrix = Matrix()
        println(binding.editImageView.rotation)
        (activity as? MainActivity)!!.fromRotation = (activity as? MainActivity)!!.mCurrRotation.toFloat()
        (activity as? MainActivity)!!.mCurrRotation += 90
        (activity as? MainActivity)!!.toRotation = (activity as? MainActivity)!!.mCurrRotation.toFloat()
        val rotateAnimation = RotateAnimation(
            (activity as? MainActivity)!!.fromRotation, (activity as? MainActivity)!!.toRotation, (binding.editImageView.width / 2).toFloat(), (binding.editImageView.height / 2).toFloat()
        )
        rotateAnimation.duration = 1000
        rotateAnimation.fillAfter = true
        matrix.setRotate((activity as? MainActivity)!!.toRotation)
        println((activity as? MainActivity)!!.toRotation.toString() + "TO ROTATION")
        println((activity as? MainActivity)!!.fromRotation.toString() + "FROM ROTATION")
        if ((activity as? MainActivity)!!.croppedBitmap != null) {
            (activity as? MainActivity)!!.cropThenRotateBitmap = Bitmap.createBitmap(
                (activity as? MainActivity)!!.croppedBitmap!!,
                0,
                0,
                (activity as? MainActivity)!!.croppedBitmap!!.width,
                (activity as? MainActivity)!!.croppedBitmap!!.height,
                matrix,
                true
            )
        } else {
            (activity as? MainActivity)!!.rotateBitmap = Bitmap.createBitmap(
                (activity as? MainActivity)!!.bitmap!!,
                0,
                0,
                (activity as? MainActivity)!!.bitmap!!.width,
                (activity as? MainActivity)!!.bitmap!!.height,
                matrix,
                true
            )
        }
        binding.editImageView.startAnimation(rotateAnimation)
    }
}
