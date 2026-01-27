package com.example.alakey.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "facts",
    primaryKeys = ["entityId", "attribute", "tx"],
    indices = [
        Index(value = ["entityId"]),
        Index(value = ["attribute"]),
        Index(value = ["tx"])
    ]
)
data class FactEntity(
    val entityId: String,
    val attribute: String,
    val value: String,
    val tx: Long = System.currentTimeMillis()
)
