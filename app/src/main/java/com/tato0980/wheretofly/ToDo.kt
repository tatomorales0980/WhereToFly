package com.tato0980.wheretofly

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.tato0980.wheretofly.databinding.ActivityToDoBinding


lateinit var btnlogout : TextView

class ToDo : AppCompatActivity() {
    private lateinit var binding: ActivityToDoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_to_do)
        binding = ActivityToDoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        btnlogout = findViewById(R.id.btnlogout)

        binding.bottomMenu.setOnItemSelectedListener { id->
            when(id){
                R.id.home -> message("home")
                R.id.add -> message("add")
                R.id.search -> message("Search")
                R.id.settings -> message("Settings")
            }
        }


        binding.btnlogout.setOnClickListener {
            var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(this,gso)

            googleSignInClient.signOut()
                .addOnCompleteListener(this, OnCompleteListener<Void?> {
                    FirebaseAuth.getInstance().signOut()
                    var currentUser = FirebaseAuth.getInstance().currentUser

                    if(currentUser == null){
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                })


            FirebaseAuth.getInstance().signOut()
            var currentUser = FirebaseAuth.getInstance().currentUser

            if(currentUser == null){
                startActivity(Intent(this, MainActivity::class.java))
            }
        }


    }

    fun message(message : String){
        Snackbar.make(binding.root,message, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.bottomMenu)
            .show()
    }
}