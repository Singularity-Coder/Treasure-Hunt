package com.singularitycoder.treasurehunt.helpers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.singularitycoder.treasurehunt.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val DB_CONTACT = "db_contact"
const val TABLE_CONTACT = "table_contact"

const val TAG_PERSON_DETAIL_MODAL_BOTTOM_SHEET = "TAG_PERSON_DETAIL_MODAL_BOTTOM_SHEET"

fun View.showSnackBar(
    message: String,
    anchorView: View? = null,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionBtnText: String = "NA",
    action: () -> Unit = {},
) {
    Snackbar.make(this, message, duration).apply {
        this.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
        if (null != anchorView) this.anchorView = anchorView
        if ("NA" != actionBtnText) setAction(actionBtnText) { action.invoke() }
        this.show()
    }
}

fun getDeviceSize(): Point = try {
    Point(deviceWidth(), deviceHeight())
} catch (e: Exception) {
    e.printStackTrace()
    Point(0, 0)
}

fun deviceWidth() = Resources.getSystem().displayMetrics.widthPixels

fun deviceHeight() = Resources.getSystem().displayMetrics.heightPixels

fun File?.customPath(directory: String?, fileName: String?): String {
    var path = this?.absolutePath

    if (directory != null) {
        path += File.separator + directory
    }

    if (fileName != null) {
        path += File.separator + fileName
    }

    return path ?: ""
}

/** /data/user/0/com.singularitycoder.audioweb/files */
fun Context.internalFilesDir(
    directory: String? = null,
    fileName: String? = null,
): File = File(filesDir.customPath(directory, fileName))

/** /storage/emulated/0/Android/data/com.singularitycoder.audioweb/files */
fun Context.externalFilesDir(
    rootDir: String = "",
    subDir: String? = null,
    fileName: String? = null,
): File = File(getExternalFilesDir(rootDir).customPath(subDir, fileName))

inline fun deleteAllFilesFrom(
    directory: File?,
    withName: String,
    crossinline onDone: () -> Unit = {},
) {
    CoroutineScope(Default).launch {
        directory?.listFiles()?.forEach files@{ it: File? ->
            it ?: return@files
            if (it.name.contains(withName)) {
                if (it.exists()) it.delete()
            }
        }

        withContext(Main) { onDone.invoke() }
    }
}

fun Context.isCameraPresent(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
}

// Get Epoch Time
val timeNow: Long
    get() = System.currentTimeMillis()

fun Long.toIntuitiveDateTime(): String {
    val postedTime = this
    val elapsedTimeMillis = timeNow - postedTime
    val elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis)
    val elapsedTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis)
    val elapsedTimeInHours = TimeUnit.MILLISECONDS.toHours(elapsedTimeMillis)
    val elapsedTimeInDays = TimeUnit.MILLISECONDS.toDays(elapsedTimeMillis)
    val elapsedTimeInMonths = elapsedTimeInDays / 30
    return when {
        elapsedTimeInSeconds < 60 -> "Now"
        elapsedTimeInMinutes == 1L -> "$elapsedTimeInMinutes Minute ago"
        elapsedTimeInMinutes < 60 -> "$elapsedTimeInMinutes Minutes ago"
        elapsedTimeInHours == 1L -> "$elapsedTimeInHours Hour ago"
        elapsedTimeInHours < 24 -> "$elapsedTimeInHours Hours ago"
        elapsedTimeInDays == 1L -> "$elapsedTimeInDays Day ago"
        elapsedTimeInDays < 30 -> "$elapsedTimeInDays Days ago"
        elapsedTimeInMonths == 1L -> "$elapsedTimeInMonths Month ago"
        elapsedTimeInMonths < 12 -> "$elapsedTimeInMonths Months ago"
        else -> postedTime toTimeOfType DateType.dd_MMM_yyyy_hh_mm_a
    }
}

infix fun Long.toTimeOfType(type: DateType): String {
    val date = Date(this)
    val dateFormat = SimpleDateFormat(type.value, Locale.getDefault())
    return dateFormat.format(date)
}

val mainActivityPermissions = arrayOf(
    Manifest.permission.READ_CONTACTS,
//    Manifest.permission.WRITE_CONTACTS,
//    Manifest.permission.READ_EXTERNAL_STORAGE,
//    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA,
)

fun Context.isLocationPermissionGranted(): Boolean = ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED

fun Context.showPermissionSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", this@showPermissionSettings.packageName, null)
    }
    startActivity(intent)
}

fun Context.showToast(
    message: String,
    duration: Int = Toast.LENGTH_LONG,
) = Toast.makeText(this, message, duration).show()

fun doAfter(duration: Long, task: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed(task, duration)
}

fun Number.dpToPx(): Float = this.toFloat() * Resources.getSystem().displayMetrics.density

fun Number.pxToDp(): Float = this.toFloat() / Resources.getSystem().displayMetrics.density

fun Number.spToPx(): Float = this.toFloat() * Resources.getSystem().displayMetrics.scaledDensity

fun Number.pxToSp(): Float = this.toFloat() / Resources.getSystem().displayMetrics.scaledDensity

// https://stackoverflow.com/questions/44109057/get-video-thumbnail-from-uri
@RequiresApi(Build.VERSION_CODES.O_MR1)
fun Context.getVideoThumbnailBitmap(docUri: Uri): Bitmap? {
    return try {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(this, docUri)
        mmr.getScaledFrameAtTime(
            1000, /* Time in Video */
            MediaMetadataRetriever.OPTION_NEXT_SYNC,
            128,
            128
        )
    } catch (e: Exception) {
        null
    }
}

// https://stackoverflow.com/questions/33222918/sharing-bitmap-via-android-intent
fun Context.shareImageAndTextViaApps(
    uri: Uri,
    title: String,
    subtitle: String,
    intentTitle: String? = null
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, subtitle)
    }
    startActivity(Intent.createChooser(intent, intentTitle ?: "Share to..."))
}

fun Context.makeCall(phoneNum: String) {
    val callIntent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNum, null))
    callIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
    startActivity(callIntent)
}

fun Context.sendSms(phoneNum: String) = try {
    val smsIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("sms:$phoneNum")
        putExtra("sms_body", "")
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
    }
    startActivity(smsIntent)
} catch (e: Exception) {
}

fun Context.sendWhatsAppMessage(whatsAppPhoneNum: String) {
    try {
        // checks if such an app exists or not
        packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
        val uri = Uri.parse("smsto:$whatsAppPhoneNum")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply { setPackage("com.whatsapp") }
        startActivity(Intent.createChooser(intent, "Dummy Title"))
    } catch (e: PackageManager.NameNotFoundException) {
        Toast.makeText(this, "WhatsApp not found. Install from PlayStore.", Toast.LENGTH_SHORT)
            .show()
        try {
            val uri = Uri.parse("market://details?id=com.whatsapp")
            val intent = Intent(
                Intent.ACTION_VIEW,
                uri
            ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) }
            startActivity(intent)
        } catch (e: Exception) {
        }
    }
}

// https://stackoverflow.com/questions/37104960/bottomsheetdialog-with-transparent-background
fun BottomSheetDialogFragment.setTransparentBackground() {
    dialog?.apply {
        // window?.setDimAmount(0.2f) // Set dim amount here
        setOnShowListener {
            val bottomSheet =
                findViewById<View?>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
    }
}

fun Context.color(@ColorRes colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun Context.drawable(@DrawableRes drawableRes: Int): Drawable? =
    ContextCompat.getDrawable(this, drawableRes)

fun AppCompatActivity.showScreen(
    fragment: Fragment,
    tag: String
) {
    supportFragmentManager.beginTransaction()
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        .add(R.id.cl_home_container, fragment, tag)
        .addToBackStack(null)
        .commit()
}

fun View.setMargins(
    all: Int? = null,
    start: Int = 0,
    top: Int = 0,
    end: Int = 0,
    bottom: Int = 0
) {
    if (this.layoutParams !is ViewGroup.MarginLayoutParams) return
    val params = this.layoutParams as ViewGroup.MarginLayoutParams
    if (all != null) {
        params.setMargins(all, all, all, all)
    } else {
        params.setMargins(start, top, end, bottom)
    }
    this.requestLayout()
}

// https://stackoverflow.com/questions/2004344/how-do-i-handle-imeoptions-done-button-click
inline fun EditText.onImeDoneClick(crossinline callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE ||
            (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
        ) {
            callback.invoke()
            return@setOnEditorActionListener true
        }
        return@setOnEditorActionListener false
    }
}

// https://stackoverflow.com/questions/7200535/how-to-convert-views-to-bitmaps
// https://www.youtube.com/watch?v=laySURtxUTk
/** If layout inflated already */
fun View.toBitmapWith(defaultColor: Int): Bitmap? = try {
    val bitmap = Bitmap.createBitmap(
        /* width = */ this.width,
        /* height = */ this.height,
        /* config = */ Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap).apply {
        drawColor(defaultColor)
    }
    this.draw(canvas)
    bitmap
} catch (e: Exception) {
    println("Error: $e")
    null
}

// https://stackoverflow.com/questions/7200535/how-to-convert-views-to-bitmaps
// https://www.youtube.com/watch?v=laySURtxUTk
/** When layout not inflated yet. Measure the view first before extracting the bitmap.
 * Else the width and height will be 0. Which means u cant do view.width & view.height.
 * If unsure of width and height specify MeasureSpec.UNSPECIFIED */
fun View.toBitmapOf(width: Int, height: Int): Bitmap? = try {
    this.measure(
        /* widthMeasureSpec = */ View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        /* heightMeasureSpec = */ View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
    val bitmap = Bitmap.createBitmap(
        /* width = */ this.measuredWidth,
        /* height = */ this.measuredHeight,
        /* config = */ Bitmap.Config.ARGB_8888 // Each pixel is set to 4 bytes of memory in this config
    )
    this.layout(
        /* l = */ 0,
        /* t = */ 0,
        /* r = */ this.measuredWidth,
        /* b = */ this.measuredHeight
    )
    this.draw(Canvas(bitmap /* The canvas is drawn on the bitmap */)) // We are basically drawing the view on the Canvas
    bitmap
} catch (e: Exception) {
    println("Error: $e")
    null
}

// https://gist.github.com/antocara/5fb2904df2c7de34ebe9
fun saveBitmapFromViewToFile(context: Context) {
    // inflate layout
    val layout: View = LayoutInflater.from(context).inflate(R.layout.layout_treasure_image, null, false)
    val cl = layout.findViewById<View>(R.id.cl_extension) as ConstraintLayout

    // reference View with image
    cl.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    val bitmap = Bitmap.createBitmap(cl.measuredWidth, cl.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    cl.layout(0, 0, cl.measuredWidth, cl.measuredHeight)
    cl.draw(canvas)

    // save to File
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val fileName = "imageFromView.jpg"
    val file = File(Environment.getExternalStorageDirectory().toString() + File.separator + fileName)
    var fo: FileOutputStream? = null
    try {
        fo = FileOutputStream(file)
        fo.write(bytes.toByteArray())
        fo.close()
    } catch (e: Exception) {
        println("Error File: $e")
    }
}

// Credit: Philip Lackner
fun <T> AppCompatActivity.collectLatestLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(collect)
        }
    }
}

enum class DateType(val value: String) {
    dd_MMM_yyyy(value = "dd MMM yyyy"),
    dd_MMM_yyyy_h_mm_a(value = "dd-MMM-yyyy h:mm a"),
    dd_MMM_yyyy_hh_mm_a(value = "dd MMM yyyy, hh:mm a"),
    dd_MMM_yyyy_hh_mm_ss_a(value = "dd MMM yyyy, hh:mm:ss a"),
    dd_MMM_yyyy_h_mm_ss_aaa(value = "dd MMM yyyy, h:mm:ss aaa"),
    yyyy_MM_dd_T_HH_mm_ss_SS_Z(value = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
}

enum class Tab(val value: String) {
    EXPLORE(value = "Explore"),
    MY_TREASURES(value = "My Treasures"),
}

sealed class TreasureType {
    class TEXT(val value: String) : TreasureType()
    class IMAGE(val value: String) : TreasureType()
    class AUDIO(val value: String) : TreasureType()
    class VIDEO(val value: String) : TreasureType()
    class FILE(val value: String) : TreasureType()
}