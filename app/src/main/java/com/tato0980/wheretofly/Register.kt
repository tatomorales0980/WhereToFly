package com.tato0980.wheretofly

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileUtils
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.tato0980.wheretofly.FileUtil.from
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

lateinit var ivBackRegister : ImageView
lateinit var ivProfileImage : ImageView
lateinit var tiUserEmail : TextInputEditText
lateinit var tiUserPassword : TextInputEditText
lateinit var tiUserName : TextInputEditText
lateinit var tiUserNickName : TextInputEditText
lateinit var tiUserPhone : TextInputEditText
lateinit var spCountry : Spinner
lateinit var myCountry : String
lateinit var spState : Spinner
lateinit var myState : String
lateinit var btnSave : Button
lateinit var errormsg : String
lateinit var rString : String
var imageUri : Uri? = null
// *************** Images
private val PICK_IMAGE_REQUEST = 101
private var filePath: Uri? = null
private var firebaseStore: FirebaseStorage? = null
private var storageReference: StorageReference? = null

var imagePath : String? = null
private var actualImage: File? = null
private var compressedProfImage: File? = null
// ****************************
class Register : AppCompatActivity() {

    private lateinit var progressDialog: ProgressDialog

    val db = FirebaseFirestore.getInstance()
    val storageReference = FirebaseStorage.getInstance().reference

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

//      adjust layout when soft keyboard appears
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        ivBackRegister = findViewById(R.id.ivBackRegister)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tiUserEmail = findViewById<TextInputEditText>(R.id.tiUserEmail)
        tiUserPassword = findViewById<TextInputEditText>(R.id.tiUserPassword)
        tiUserName = findViewById<TextInputEditText>(R.id.tiUserName)
        tiUserNickName = findViewById<TextInputEditText>(R.id.tiUserNickName)
        tiUserPhone = findViewById<TextInputEditText>(R.id.tiUserPhone)
        spCountry = findViewById<Spinner>(R.id.spCountries)
        spState = findViewById<Spinner>(R.id.spStates)
        btnSave = findViewById(R.id.btnSave)

        var show = intent.getStringExtra("display_info")

        if (show == "no"){
            var logedemail = FirebaseAuth.getInstance().currentUser!!.email.toString()
            tiUserEmail.setText(logedemail)
            tiUserEmail.isEnabled  = false
            tiUserPassword.isEnabled  = false
            tiUserPassword.setText("Password")
            tiUserEmail.setBackgroundColor(Color.parseColor("#394350"))
            tiUserPassword.setBackgroundColor(Color.parseColor("#394350"))
//            tiUserPassword.setVisibility(View.GONE)
        }

        ivBackRegister.setOnClickListener(View.OnClickListener {
            finish()
        })

        ivProfileImage.setOnClickListener(View.OnClickListener {
            checkPermision("gallery")
        })

        btnSave.setOnClickListener(View.OnClickListener {
            checkNullValues(show)
        })
        populateSpinnerCountry()


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if(data == null || data.data == null){
                return
            }

            filePath = data.data

            try {
//                01/01/21
//                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
//                ivProfileImage.setImageBitmap(bitmap)

//               01/01/21
                imageUri = data?.data

                actualImage = FileUtil.from(this, data!!.data!!)?.also {
                    ivProfileImage.setImageURI(data?.data)
                }


            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Log.i("Local Image Path", filePath.toString())
//        uploadImage() 01/01/21
        compressImage()
    }

    private fun compressImage() {

        actualImage?.let { imageFile ->
            lifecycleScope.launch {
                // Default compression
                compressedProfImage =
                        Compressor.compress(this@Register, imageFile) {
                            size(100_000)
                            resolution(600, 600)
                            quality(60)
                            format(Bitmap.CompressFormat.JPEG)
                        }

                setCompressedImage()
            }
        } ?: Log.d("received data", "Please Choose an Image")
//        progressDialog.dismiss()
    }

    lateinit var profPicURL : Uri

    private fun setCompressedImage() {
        Log.i("set compressed image", "OK")
        compressedProfImage?.let {
            ivProfileImage.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
            val uri = Uri.fromFile(it)
            profPicURL = uri

            uploadImageOnServer()

        } ?: Toast.makeText(this@Register, "File not Found ", Toast.LENGTH_LONG).show()

    }



    private fun uploadImageOnServer(){
        if(filePath != null){
            val ref = storageReference?.child("Profiles/${getRandomString(10)}")
//            val uploadTask = ref?.putFile(filePath!!)
            val uploadTask = ref?.putFile(profPicURL!!)

            val urlTask = uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation ref.downloadUrl
            })?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
//                    addUploadRecordToDb(downloadUri.toString())
                    imagePath = downloadUri.toString()


                } else {
//                    imagePath = ""
                    // Handle failures
                }
            }?.addOnFailureListener{

            }
        }else{
            Toast.makeText(this, "Please Upload an Image", Toast.LENGTH_SHORT).show()
        }

    }


    private fun checkNullValues(show: String?) {

        if (tiUserEmail.text.toString().isEmpty() ) {
            tiUserEmail.error = getString(R.string.errorEmail)
        } else if (tiUserPassword.text.toString().isEmpty() ) {
            tiUserPassword.error = getString(R.string.errorPassword)
        } else if (tiUserName.text.toString().isEmpty()) {
            tiUserName.error = getString(R.string.errorName)
        } else if(tiUserNickName.text.toString().isEmpty()) {
            tiUserNickName.error = getString(R.string.errorNick)
        } else if(tiUserPhone.text.toString().isEmpty()) {
            tiUserPhone.error = getString(R.string.errorPhone)
        } else {
            if (show == "no"){
                insertUserDatainFirebase()
            } else {
                createNewUser()
            }

        }
    }
    fun populateSpinnerCountry(){
        val countries = resources.getStringArray(R.array.countries)
        var spadpt = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        spCountry.adapter = spadpt

        spCountry.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                (view as TextView).setTextColor(Color.WHITE) //Change selected text color
                (view as TextView).setTextSize(20f)
                myCountry  = countries[position]

                populateSpinnerStates()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
    }

    fun populateSpinnerStates(){
        val states = resources.getStringArray(R.array.usStates_arrays)
        var spadpt = ArrayAdapter(this, android.R.layout.simple_spinner_item, states)
        spState.adapter = spadpt
//        Toast.makeText(this, "$myCountrie", Toast.LENGTH_SHORT).show()
        if (myCountry != "United States of America"){
            spState.setSelection(50)
        }


        spState.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                (view as TextView).setTextColor(Color.WHITE) //Change selected text color
                (view as TextView).setTextSize(20f)
                myState = states[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
    }

    fun createNewUser() {
//        Firebase create a NEW USER with Email and Password
//        revisar esta linea para que cree el usuario adelante
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                tiUserEmail.text.toString(),
                tiUserPassword.text.toString()
        ).addOnCompleteListener {

            if (it.isSuccessful){
                progressDialog = ProgressDialog(this@Register)
                progressDialog.setMessage("Saving Data on Server")
                progressDialog.setCancelable(false)
                progressDialog.show()
                insertUserDatainFirebase()
            } else {
                errormsg = "Error saving data into the server"
                showAlert()
            }
        }
    }


    private fun insertUserDatainFirebase() {
        Log.i("Remote path Image", imagePath.toString())
        rString = getRandomString(12)
        progressDialog = ProgressDialog(this@Register)
        progressDialog.setMessage("Saving Data on Server")
        progressDialog.setCancelable(false)
        progressDialog.show()

        var data = HashMap<String, String> ()
        data.put("id", rString)
        data.put("email", FirebaseAuth.getInstance().currentUser!!.email.toString())
        data.put("name", tiUserName.text.toString())
        data.put("nick", tiUserNickName.text.toString())
        data.put("phone", tiUserPhone.text.toString())
        data.put("country", myCountry)
        data.put("state", myState)
        data.put("image", imagePath.toString())


        db.collection("Users")
                .document(rString)
                .set(data)
                .addOnSuccessListener {
                   progressDialog.dismiss()
                    Toast.makeText(this, "Successfully user created ${tiUserName.text.toString()}", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                    moveNextPage()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Something went wrong saving data", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
    }

    fun moveNextPage(){

        var currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser != null){
            startActivity(Intent(this, ToDo::class.java))
        }
    }


    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
    }


    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(errormsg)
        builder.setPositiveButton("OK", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun checkPermision(s: String) {
        askPermission(
                android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
        ){

            if (s == "camera") {
                var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, 100)
            } else {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
            }


        }.onDeclined { e ->
            if (e.hasDenied()) {

                AlertDialog.Builder(this@Register)
                        .setMessage("Please accept our permissions")
                        .setPositiveButton("yes") { dialog, which ->
                            e.askAgain();
                        } //ask again
                        .setNegativeButton("no") { dialog, which ->
                            dialog.dismiss();
                        }
                        .show();
            }

            if(e.hasForeverDenied()) {

                e.goToSettings();
            }
        }
    }

}