package com.example.instagramproject.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.instagramproject.R
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_profile.*


class ProfileActivity : AppCompatActivity() {
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    private val storageReference = FirebaseStorage.getInstance().reference
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var imageUri: Uri
    private var uid: String = firebaseAuth.currentUser?.uid.toString()
    private var isPhotoSelected: Boolean = false
    private lateinit var progressBar: ProgressBar
    private val REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.hide()

        progressBar = progressBarProfile


        firebaseFirestore.collection("Users").document(uid).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (task.result.exists()) {
                    val name = task.result.getString("name")
                    val imageUrl = task.result.getString("image")
                    profile_nameEditText.setText(name)
                    imageUri = Uri.parse(imageUrl)
                    Glide.with(this).load(imageUrl).into(circleImageView)
                }
            }
        }

        circleImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE)
        }

        save_btn_profile.setOnClickListener { v ->
            progressBar.visibility = View.VISIBLE
            val name: String = profile_nameEditText.text.toString()
            val imageRef: StorageReference =
                storageReference.child("Profile_pics").child(uid + ".jpg")
            if (isPhotoSelected) {
                if (name.isNotEmpty()) {
                    imageRef.putFile(imageUri).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            imageRef.downloadUrl.addOnSuccessListener { uri ->
                                saveToFireStore(task, name, uri, v)
                            }
                        } else {
                            progressBar.visibility = View.INVISIBLE
                            Toast.makeText(this,
                                task.exception?.message.toString(),
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    progressBar.visibility = View.INVISIBLE
                    Snackbar.make(v, "Empty fields are not allowed", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                saveToFireStore(null, name, imageUri, v)
            }
        }
    }

    private fun saveToFireStore(
        task: Task<UploadTask.TaskSnapshot>?,
        name: String,
        downloadUri: Uri?,
        v: View,
    ) {
        val userMap: HashMap<String, Any> = HashMap()
        userMap["name"] = name
        userMap["image"] = downloadUri.toString()

        firebaseFirestore.collection("Users").document(uid).set(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressBar.visibility = View.INVISIBLE
                    Snackbar.make(v, "Profile created successfully", Snackbar.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    progressBar.visibility = View.VISIBLE
                    Snackbar.make(v, task.exception?.message.toString(), Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            data?.data?.let {
                imageUri = it
                circleImageView.setImageURI(it)
                isPhotoSelected = true
            }
        }
    }
}