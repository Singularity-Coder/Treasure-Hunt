package com.singularitycoder.treasurehunt.helpers

import com.singularitycoder.treasurehunt.Treasure

object IntentKey {
    const val LOCATION_TOGGLE_STATUS = "LOCATION_TOGGLE_STATUS"
}

object DbKey {
    const val DB_TREASURE = "db_treasure"
    const val TABLE_TREASURE = "table_treasure"
}

object BroadcastKey {
    const val LOCATION_TOGGLE_STATUS = "LOCATION_TOGGLE_STATUS"
}

val dummyTreasures = mutableListOf(
    Treasure(
        title = "Legendary Pokemon: HakuTakuPaku",
        filePath = "image.png"
    ),
    Treasure(
        title =   "A super malware capable of taking down any stock market!",
        filePath ="document.java"
    ),
    Treasure(
        title = "I plundered all the world and amassed an enormous amount of wealth. And I stored it at...",
        filePath = "audio.mp3"
    ),
    Treasure(
        title = "Secret UFO tech.",
        filePath = "document.pdf"
    ),
    Treasure(
        title = "Original Ayurveda Shastra.",
        filePath = "document.djvu"
    ),
    Treasure(
        title = "Custom made gambling App that lets you earn a trillion dollars.",
        filePath = "document.app"
    ),
    Treasure(
        title = "Death Note. After Light died I found the book. I got scared so I am waiting for a worthy user.",
        filePath = "document.apk"
    ),
    Treasure(
        title = "A funny picture of my cat talking in klingon. It said an I...",
        filePath = "document.webp"
    ),
    Treasure(
        title = "If 5 is five then what will you get with 5000 - 7000 + 3000 time shuunya.",
        filePath = "video.mp4"
    ),
    Treasure(
        title = "hello world!",
        filePath = "document.kt"
    )
)