package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject

enum class OperationType {
  CREATE,
  UPDATE,
  DELETE,
  GET,
  LIST
}

fun handleFirestoreError(
  exception: Exception,
  operationType: OperationType,
  path: String
) {
  val firestoreException = exception as? FirebaseFirestoreException
  val errorCode = firestoreException?.code?.name ?: "UNKNOWN"
  val errorMessage = exception.message ?: "No error message provided"

  val errorPayload = JSONObject().apply {
    put("error", errorCode)
    put("operation", operationType.name)
    put("path", path)
    put("message", errorMessage)
  }

  Log.e("FirestoreError", errorPayload.toString())
}
