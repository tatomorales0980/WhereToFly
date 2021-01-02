package com.tato0980.wheretofly.CommonUtils

class UserModel(val country : String,val email : String, val id : String, val name : String, val nick : String, val phone : String, val state : String, val image : String)
{
    constructor() : this(  "","","","", "", "", "", "")
}