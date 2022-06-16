package com.example.instagramproject.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.instagramproject.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_post.*

class PostActivity : AppCompatActivity() {
    private var postImageUri: Uri? = null
    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var currentUserId = firebaseAuth.currentUser?.uid
    private val REQUEST_CODE_IMAGE_PICK = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        supportActionBar?.hide()



        post_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
        }

        add_post_btn.setOnClickListener { v ->
            post_progressBar.visibility = View.VISIBLE
            val caption: String = caption_EditText.text.toString()
            if (caption.isNotEmpty() && postImageUri != null) {
                val postRef = storageReference.child("post_images")
                    .child(FieldValue.serverTimestamp().toString() + ".jpg")
                postRef.putFile(postImageUri!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        postRef.downloadUrl.addOnSuccessListener {
                            val postMap: HashMap<String, Any> = HashMap()
                            postMap["image"] = it.toString()
                            postMap["user"] = currentUserId.toString()
                            postMap["caption"] = caption
                            postMap["time"] = FieldValue.serverTimestamp()

                            firebaseFirestore.collection("Posts").add(postMap)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        post_progressBar.visibility = View.INVISIBLE
                                        Snackbar.make(v,"Post added successfully",Snackbar.LENGTH_SHORT).show()
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        post_progressBar.visibility = View.INVISIBLE
                                        Toast.makeText(this,
                                            task.exception?.message.toString(),
                                            Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        post_progressBar.visibility = View.INVISIBLE
                        Toast.makeText(this, task.exception?.message.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                post_progressBar.visibility = View.INVISIBLE
                Toast.makeText(this, "Please select image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_IMAGE_PICK) {
            data?.data?.let {
                postImageUri = it
                post_image.setImageURI(it)
            }
        }
    }
}