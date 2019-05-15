package com.ohmycthulhu.businesscarddatabase.activities

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.CursorLoader
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.request.SimpleMultiPartRequest
import com.android.volley.toolbox.Volley
import com.ohmycthulhu.businesscarddatabase.R
import com.ohmycthulhu.businesscarddatabase.utils.ImageUtils
import com.ohmycthulhu.businesscarddatabase.utils.recognizer.RecognizePatterns
import com.ohmycthulhu.businesscarddatabase.utils.recognizer.Recognizer
import kotlinx.android.synthetic.main.activity_new_card_photo.*
import java.io.ByteArrayOutputStream
import java.io.File

class NewCardActivity : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_PICK_IMAGE = 2

    var image: Bitmap? = null
    lateinit var requestQueue: RequestQueue
    var fileToDelete: File? = null
    var imageUri: Uri? = null
    lateinit var sharedPreferences: SharedPreferences

    val recognizer: Recognizer =
        Recognizer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_card_photo)
        setResult(Activity.RESULT_CANCELED)

        requestQueue = Volley.newRequestQueue(this)

        sharedPreferences = getSharedPreferences("com.ohmycthulhu.businesscarddatabase", Context.MODE_PRIVATE)

        makePhotoFab.setOnClickListener { dispatchTakePhoto() }
        choosePhotoFab.setOnClickListener { dispatchChoosePhoto() }
        saveButton.setOnClickListener { createCard() }
    }

    private fun dispatchTakePhoto () {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun dispatchChoosePhoto () {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select picture"), REQUEST_PICK_IMAGE)
    }

    private fun createCard (): Boolean {
        Toast.makeText(this, "Creating card", Toast.LENGTH_SHORT).show()
        val name = newCardName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is empty", Toast.LENGTH_SHORT).show()
            return false
        }

        val company = newCardCompany.text.toString()

        val email = newCardEmail.text.toString()

        val phone = newCardPhone.text.toString()

        val position = newCardPosition.text.toString()

        val website = newCardWebsite.text.toString()

        val address = newCardAddress.text.toString()
        val note = newCardNote.text.toString()

        sendCreateRequest(name, company, email, address, phone, website, position, newCardIsPrivate.isChecked, image, note)
        return true
    }

    private fun sendCreateRequest (name: String, company: String, email: String, address: String, phone: String, website: String, position: String, private: Boolean, image: Bitmap?, note: String) {
        val request = SimpleMultiPartRequest(Request.Method.POST,
            "${sharedPreferences.getString("api_address", "http://192.168.1.8")}/business-cards",
            Response.Listener {
            Toast.makeText(this, "It worked!", Toast.LENGTH_SHORT).show()
            if (fileToDelete != null) {
                (fileToDelete as File).delete()
            }
            setResult(Activity.RESULT_OK)
            finish()
        }, Response.ErrorListener {
            Toast.makeText(this, "Error occurred: ${it.message}", Toast.LENGTH_LONG).show()
            if (fileToDelete != null) {
                (fileToDelete as File).delete()
            }
        })
        request.addStringParam("name", name)
        request.addStringParam("company_name", company)
        request.addStringParam("position", position)
        request.addStringParam("email", email)
        request.addStringParam("mobile", phone)
        request.addStringParam("website", website)
        request.addStringParam("private", if (private) "1" else "0")
        request.addStringParam("address", address)
        request.addStringParam("note", note)
        if (image != null) {
            val bytes = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(contentResolver, image, "Title", null)
            Toast.makeText(this, "Path is $path", Toast.LENGTH_SHORT).show()
            try {
                fileToDelete = File(path)
            } catch (e: Exception) {
                Toast.makeText(this, "Error while opened file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            request.addFile("photo", getPath(Uri.parse(path)))
        }
        request.tag = "new_card"
        requestQueue.add(request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Toast.makeText(this, "You took photo!", Toast.LENGTH_SHORT).show()
            if (imageUri != null) {
                // val exif = ExifInterface(imageUri?.path)
                image = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                if (image != null) {
                    image = ImageUtils.limitImageSize(image as Bitmap, 2000, 2000)
                    if (image != null && newCardRecognition.isChecked) {
                        fillFields(recognizer.recognize(image as Bitmap))
                    }
                }
            }
        }
        if (requestCode == REQUEST_PICK_IMAGE && data != null) {
            Toast.makeText(this, "You picked image", Toast.LENGTH_SHORT).show()
            val pickUri = data.data

            image = if(pickUri != null) MediaStore.Images.Media.getBitmap(contentResolver, pickUri) else null
            if (image != null && newCardRecognition.isChecked) {
                image = ImageUtils.limitImageSize(image as Bitmap, 2000, 2000)
                fillFields(recognizer.recognize(image as Bitmap))
            }
        }
    }

    private fun fillFields (fields: Map<RecognizePatterns, String>) {
        if (fields.containsKey(RecognizePatterns.NAME)) {
            newCardName.setText(fields[RecognizePatterns.NAME])
        }
        if (fields.containsKey(RecognizePatterns.PHONE)) {
            newCardPhone.setText(fields[RecognizePatterns.PHONE])
        }
        if (fields.containsKey(RecognizePatterns.EMAIL)) {
            newCardEmail.setText(fields[RecognizePatterns.EMAIL])
        }
    }

    private fun getPath(contentUri: Uri): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val loader = CursorLoader(applicationContext, contentUri, proj, null, null, null)
        val cursor = loader.loadInBackground()
        val column_index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val result = cursor?.getString(column_index as Int)
        cursor?.close()
        return result as String
    }
}
