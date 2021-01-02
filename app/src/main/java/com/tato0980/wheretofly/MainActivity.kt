package com.tato0980.wheretofly

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tato0980.wheretofly.CommonUtils.UserModel

class MainActivity : AppCompatActivity() {
    lateinit var progressDialog: ProgressDialog
    lateinit var btnSingUp : Button
    lateinit var tvRegister : TextView
    lateinit var etEmail : EditText
    lateinit var etPassword : EditText

    lateinit var userEmail : String
    lateinit var userPassword : String
    lateinit var detailmsg : String

    // XML Google SingIn variable ID
    lateinit var rvSigninGoogle : RelativeLayout

    val RC_SIGN_IN = 1000
    var db = FirebaseFirestore.getInstance()

    // SingIn variable client
    var googleSignInClient : GoogleSignInClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvRegister = findViewById(R.id.tvRegister)
        btnSingUp = findViewById(R.id.btnSingUp)
        rvSigninGoogle = findViewById<RelativeLayout>(R.id.rvSigninGoogle)


        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)

        rvSigninGoogle.setOnClickListener {
            var signInIntent = googleSignInClient?.signInIntent
            startActivityForResult(signInIntent,RC_SIGN_IN)
        }

        btnSingUp.setOnClickListener {
            if (etEmail.text.isNotEmpty() && etPassword.text.isNotEmpty()){
                checkField()
            }
        }

        tvRegister.setOnClickListener {
            intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }

    fun checkField() {
        // Take email and password data
        userEmail = etEmail.text.toString()
        userPassword = etPassword.text.toString()

        FirebaseAuth.getInstance().signInWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener {

            if (it.isSuccessful){
                startActivity(Intent(this, ToDo::class.java))
            } else {
                detailmsg = "Email not registered"
                showAlert()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        moveNextPage()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN ){
            // Get the Singin data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            // Decrypt data from variable account
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        }
    }

    fun firebaseAuthWithGoogle(acct : GoogleSignInAccount?){

        var credential = GoogleAuthProvider.getCredential(acct?.idToken,null)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                moveNextPage()
            } else {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
            .addOnFailureListener {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }


//    fix de login with google 1:19 video 2020-11-28 09-46-47

    fun moveNextPage(){

        var name = ""
        var nick = ""
        var phone = ""

        var currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser != null){
            var email = FirebaseAuth.getInstance().currentUser!!.email.toString()

            db.collection("Users").whereEqualTo("email", email).get().addOnSuccessListener {
                var user = it.toObjects(UserModel::class.java)

                for (i in 0..user.size-1) {
                    if (user.get(i) != null) {
                         name = user.get(i).name
                         nick = user.get(i).nick
                         phone = user.get(i).phone
                    }
                }

                Log.d("Usuario", "$email /$name / $nick / $phone")


                if (name != null || phone != null || nick != null ) {

                    if ( name!!.isNotEmpty() || phone!!.isNotEmpty() || nick!!.isNotEmpty()) {
                        startActivity(Intent(this, ToDo::class.java))
                    }else {
                        startActivity(Intent(this, Register::class.java).apply {
                            putExtra("display_info", "no")
                        })
                    }

                } else {
                    startActivity(Intent(this, Register::class.java).apply {
                        putExtra("display_info", "no")
                    })
                }

            }
//          *****************************
        }
    }


    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(detailmsg)
        builder.setPositiveButton("OK", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}