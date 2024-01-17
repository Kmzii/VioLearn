package com.example.mystudytracker

import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import id.zelory.compressor.Compressor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class UserProfileDialogFragment : DialogFragment() {

    private lateinit var deleteButton: ImageButton
    private lateinit var editButton: ImageButton
    private lateinit var userProfileImageView: ImageView
    private lateinit var storageRef: StorageReference
    private lateinit var compressedImageFile: File
    private var userProfileUpdateListener: UserProfileUpdateListener? = null
    private var isUploadingImage: Boolean = false
    private lateinit var loadingProgressBar: ProgressBar


    fun setUserProfileUpdateListener(listener: UserProfileUpdateListener) {
        userProfileUpdateListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_profile_dialog, container, false)
        userProfileImageView = view.findViewById(R.id.userProfileImageView)
        deleteButton = view.findViewById(R.id.deleteButton)
        editButton = view.findViewById(R.id.editButton)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

        // Set the image resource (replace with your actual drawable)
        userProfileImageView.setImageResource(R.drawable.user_person_profile_block_account_circle)

        // Set onClickListener for the close button
        deleteButton.setOnClickListener {
            // Call uploadImageToFirebase with the default profile image

            Log.d("Delete Button","Delete Button Pressed")
            val defaultImageUri = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE +
                    "://" + resources.getResourcePackageName(R.drawable.user_person_profile_block_account_circle)
                    + '/' + resources.getResourceTypeName(R.drawable.user_person_profile_block_account_circle)
                    + '/' + resources.getResourceEntryName(R.drawable.user_person_profile_block_account_circle))

            uploadImageToFirebase(defaultImageUri, compressImage = false)
        }

        // Set onClickListener for the other button (add your custom logic)
        editButton.setOnClickListener {
            pickImageFromGallery()

        }

        loadUserProfileImage()

        return view
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            if (selectedImageUri != null) {
                // Upload the selected image to Firebase Storage
                uploadImageToFirebase(selectedImageUri)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun uploadImageToFirebase(imageUri: Uri, compressImage: Boolean = true) {
        isUploadingImage = true

        // Disable cancel on touch outside and back press
        dialog?.setCanceledOnTouchOutside(false)
        isCancelable = false

        //Progress Bar
        loadingProgressBar.visibility = View.VISIBLE

        GlobalScope.launch(Dispatchers.IO) {
            Glide.get(requireContext()).clearDiskCache()

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {

                // Save the compressed image to app's private storage with timestamp
                val timestamp = System.currentTimeMillis()
                val privateImagePath = File(requireContext().filesDir, "profile_image_${userId}_$timestamp.jpg")

                saveImagePathToLocal(privateImagePath)

                // Conditionally compress the image
                compressedImageFile = if (compressImage) {
                    Compressor.compress(requireContext(), FileUtil.from(requireContext(), imageUri))
                } else {
                    FileUtil.from(requireContext(), imageUri)
                }

                compressedImageFile.copyTo(privateImagePath, overwrite = true)


                // Create a storage reference
                storageRef = FirebaseStorage.getInstance().getReference("profile_images/$userId")

                // Upload the compressed file and get the download URL
                val uploadTask = storageRef.putFile(Uri.fromFile(compressedImageFile))

                FileUtil.deleteTempFiles(requireContext())

                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    storageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        updateImageUrlInDatabase(downloadUri.toString())
                    } else {
                        // Handle failures
                        Log.e("Firebase", "Error uploading image: ${task.exception?.message}")
                    }

                    isUploadingImage = false

                    loadingProgressBar.visibility = View.GONE

                    // Delay enabling cancel on touch outside and back press by 1 second
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Enable cancel on touch outside and back press if not uploading anymore
                        if (!isUploadingImage) {
                            dialog?.setCanceledOnTouchOutside(true)
                            isCancelable = true
                        }
                    }, 1000)
                }
            }
        }
    }

    private fun updateImageUrlInDatabase(imageUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users/$userId/imageUrl")
            databaseRef.setValue(imageUrl)
                .addOnSuccessListener {
                    Log.d("Firebase", "Image updated successfully!")

                    loadUserProfileImage()

                }
                .addOnFailureListener { e ->
                    // Handle the error
                    Log.e("Firebase", "Error updating image URL: ${e.message}")
                }
        }
    }

    private fun loadUserProfileImage() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Find the latest timestamped file in app's private storage
            val privateImageFiles = requireContext().filesDir.listFiles { _, name ->
                name.startsWith("profile_image_$userId") && name.endsWith(".jpg")
            }

            val latestFile = privateImageFiles?.maxByOrNull { it.name }

            if (latestFile != null) {

                // Load the image using Glide
                Glide.with(this)
                    .load(latestFile)
                    .error(R.drawable.user_person_profile_block_account_circle)
                    .into(userProfileImageView)

                userProfileUpdateListener?.onUserProfileUpdated()

            } else {
                // Handle case when no files are found
                // You might want to set a default image or handle it based on your app's logic.
                Glide.with(this)
                    .load(R.drawable.user_person_profile_block_account_circle)
                    .into(userProfileImageView)
            }
        }
    }

    private fun saveImagePathToLocal(imagePath: File) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Use SharedPreferences to store the imagePath
            val sharedPreferences =
                requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("userProfileImagePath_$userId", imagePath.absolutePath)
            editor.apply()
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onResume() {
        super.onResume()
        // Reset the flag when the dialog is resumed
        isUploadingImage = false
    }


}



