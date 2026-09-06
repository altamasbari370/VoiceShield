package com.altamas.voiceshield.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altamas.voiceshield.data.ProfileRepository
import com.altamas.voiceshield.data.TokenManager
import com.altamas.voiceshield.models.ProfileCreateRequest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

private val ShieldBlue = Color(0xFF2563EB)
private val ShieldCyan = Color(0xFF06B6D4)
private val ShieldPurple = Color(0xFF7C3AED)
private val ShieldGreen = Color(0xFF10B981)
private val ShieldTextSecondary = Color(0xFF64748B)
private val ProfileBackground = Color(0xFFF8FAFC)


// ================================================================
// IMAGE HELPERS
// ================================================================

/**
 * Decode an image from a URI while sampling it down.
 *
 * This prevents loading a huge camera/gallery image such as
 * 4000x3000 directly into memory.
 */
private fun decodeSampledBitmap(
    context: Context,
    uri: android.net.Uri,
    maxWidth: Int = 512,
    maxHeight: Int = 512
): Bitmap? {

    return try {

        val resolver = context.contentResolver

        // --------------------------------------------------------
        // First pass: get image dimensions
        // --------------------------------------------------------

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        resolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(
                inputStream,
                null,
                boundsOptions
            )
        }

        val imageWidth = boundsOptions.outWidth
        val imageHeight = boundsOptions.outHeight

        if (imageWidth <= 0 || imageHeight <= 0) {
            return null
        }

        // --------------------------------------------------------
        // Calculate sampling factor
        // --------------------------------------------------------

        var sampleSize = 1

        if (
            imageWidth > maxWidth ||
            imageHeight > maxHeight
        ) {

            var halfWidth = imageWidth / 2
            var halfHeight = imageHeight / 2

            while (
                halfWidth / sampleSize >= maxWidth &&
                halfHeight / sampleSize >= maxHeight
            ) {

                sampleSize *= 2
            }
        }

        // --------------------------------------------------------
        // Second pass: actually decode image
        // --------------------------------------------------------

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        resolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(
                inputStream,
                null,
                decodeOptions
            )
        }

    } catch (e: Exception) {

        println(
            "VoiceShield Image Decode Error: ${e.message}"
        )

        null
    }
}


/**
 * Convert bitmap to a small Base64 JPEG string.
 *
 * The resulting string is stored in the existing
 * profile_picture database field.
 */
private fun bitmapToBase64(
    bitmap: Bitmap
): String? {

    return try {

        val maxDimension = 512

        val largestDimension =
            maxOf(bitmap.width, bitmap.height)

        val scale =
            if (largestDimension > maxDimension) {
                maxDimension.toFloat() / largestDimension.toFloat()
            } else {
                1f
            }

        val finalBitmap =
            if (scale < 1f) {

                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).roundToInt(),
                    (bitmap.height * scale).roundToInt(),
                    true
                )

            } else {
                bitmap
            }

        val outputStream =
            ByteArrayOutputStream()

        finalBitmap.compress(
            Bitmap.CompressFormat.JPEG,
            80,
            outputStream
        )

        val imageBytes =
            outputStream.toByteArray()

        outputStream.close()

        if (finalBitmap !== bitmap) {
            finalBitmap.recycle()
        }

        val base64 =
            Base64.encodeToString(
                imageBytes,
                Base64.NO_WRAP
            )

        // Store it as a data URI so we know that this is JPEG.
        "data:image/jpeg;base64,$base64"

    } catch (e: Exception) {

        println(
            "VoiceShield Image Encode Error: ${e.message}"
        )

        null
    }
}


/**
 * Convert the Base64 profile picture back into a Bitmap.
 */
private fun base64ToBitmap(
    profilePicture: String?
): Bitmap? {

    if (profilePicture.isNullOrBlank()) {
        return null
    }

    return try {

        val base64Data =
            if (profilePicture.contains(",")) {
                profilePicture.substringAfter(",")
            } else {
                profilePicture
            }

        val imageBytes =
            Base64.decode(
                base64Data,
                Base64.DEFAULT
            )

        BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size
        )

    } catch (e: Exception) {

        println(
            "VoiceShield Image Decode Base64 Error: ${e.message}"
        )

        null
    }
}


@Composable
fun ProfileScreen(
    userName: String = "User",
    userEmail: String = "user@email.com",
    userAge: Int? = null,
    onBackClick: () -> Unit = {},
    onSaveClick: (name: String, age: Int) -> Unit = { _, _ -> },
    onChangePasswordClick: () -> Unit = {},
    onChangePhotoClick: () -> Unit = {},

) {

    val context = LocalContext.current

    val tokenManager = remember {
        TokenManager(context.applicationContext)
    }

    val profileRepository = remember {
        ProfileRepository()
    }

    val coroutineScope = rememberCoroutineScope()

    var name by remember {
        mutableStateOf(userName)
    }

    var email by remember {
        mutableStateOf(userEmail)
    }

    var age by remember {
        mutableStateOf(userAge?.toString() ?: "")
    }

    var gender by remember {
        mutableStateOf("")
    }

    var profilePicture by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var profileError by remember {
        mutableStateOf<String?>(null)
    }

    var saveMessage by remember {
        mutableStateOf<String?>(null)
    }

    // ============================================================
    // GALLERY PICKER
    // ============================================================

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            try {

                val bitmap =
                    decodeSampledBitmap(
                        context = context,
                        uri = uri
                    )

                if (bitmap == null) {

                    profileError =
                        "Unable to read the selected image."

                    return@rememberLauncherForActivityResult
                }

                val encodedImage =
                    bitmapToBase64(bitmap)

                bitmap.recycle()

                if (encodedImage == null) {

                    profileError =
                        "Unable to process the selected image."

                    return@rememberLauncherForActivityResult
                }

                // ------------------------------------------------
                // Keep selected image locally.
                //
                // It will be sent to backend when the user taps
                // SAVE CHANGES.
                // ------------------------------------------------

                profilePicture = encodedImage

                profileError = null

                saveMessage =
                    "Photo selected. Save changes to update your profile."

            } catch (e: Exception) {

                println(
                    "VoiceShield Photo Picker Error: ${e.message}"
                )

                profileError =
                    "Unable to select this image."
            }
        }

    // ============================================================
    // PROFILE IMAGE FOR UI
    // ============================================================

    val profileBitmap = remember(profilePicture) {
        base64ToBitmap(profilePicture)
    }

    // ============================================================
    // LOAD REAL PROFILE
    // ============================================================

    LaunchedEffect(Unit) {

        try {

            val token = tokenManager.getToken()

            if (token.isNullOrBlank()) {

                profileError =
                    "Authentication session expired"

                isLoading = false

            } else {

                val profile =
                    profileRepository.getProfile(token)

                if (profile != null) {

                    name =
                        profile.name ?: ""

                    email =
                        profile.email

                    age =
                        profile.age?.toString() ?: ""

                    gender =
                        profile.gender ?: ""

                    profilePicture =
                        profile.profile_picture

                    profileError = null

                } else {

                    profileError =
                        "Unable to load profile. Please try again."
                }

                isLoading = false
            }

        } catch (e: Exception) {

            println(
                "VoiceShield Profile Error: ${e.message}"
            )

            profileError =
                "Unable to load profile. Please try again."

            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
    ) {

        // ========================================================
        // TOP BAR
        // ========================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    onBackClick()
                }
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Profile",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "Manage your account",
                    fontSize = 12.sp,
                    color = ShieldTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = ShieldBlue,
                modifier = Modifier.size(28.dp)
            )
        }

        // ========================================================
        // CONTENT
        // ========================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(18.dp)
        ) {

            // ====================================================
            // PROFILE HEADER
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ------------------------------------------------
                    // PROFILE PHOTO
                    // ------------------------------------------------

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                ShieldBlue.copy(alpha = 0.10f)
                            )
                            .border(
                                2.dp,
                                ShieldBlue.copy(alpha = 0.25f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        if (profileBitmap != null) {

                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )

                        } else {

                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = ShieldBlue,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = if (name.isBlank())
                            "User"
                        else
                            name,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = email,
                        fontSize = 13.sp,
                        color = ShieldTextSecondary
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    // ------------------------------------------------
                    // CHANGE PHOTO
                    // ------------------------------------------------

                    Button(
                        onClick = {

                            profileError = null

                            imagePickerLauncher.launch(
                                "image/*"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                ShieldBlue.copy(alpha = 0.08f),
                            contentColor = ShieldBlue
                        )
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(7.dp)
                        )

                        Text(
                            text = "Change Photo",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // ====================================================
            // LOADING
            // ====================================================

            if (isLoading) {

                Text(
                    text = "Loading profile...",
                    color = ShieldTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 8.dp
                    )
                )
            }

            // ====================================================
            // ERROR
            // ====================================================

            if (profileError != null) {

                Text(
                    text = profileError!!,
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 6.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            // ====================================================
            // SAVE MESSAGE
            // ====================================================

            if (saveMessage != null) {

                Text(
                    text = saveMessage!!,
                    color = ShieldGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                        vertical = 6.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            // ====================================================
            // PERSONAL INFORMATION
            // ====================================================

            Text(
                text = "PERSONAL INFORMATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ShieldTextSecondary,
                modifier = Modifier.padding(
                    start = 4.dp,
                    bottom = 8.dp
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor =  Color(0xFF8F9CE3)
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {

                            name = it
                            saveMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Name")
                        },
                        leadingIcon = {

                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Email")
                        },
                        leadingIcon = {

                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        enabled = false,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = {

                            age = it.filter { character ->
                                character.isDigit()
                            }

                            saveMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Age")
                        },
                        leadingIcon = {

                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // ====================================================
            // SECURITY
            // ====================================================

            Text(
                text = "SECURITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ShieldTextSecondary,
                modifier = Modifier.padding(
                    start = 4.dp,
                    bottom = 8.dp
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onChangePasswordClick()
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(
                                RoundedCornerShape(13.dp)
                            )
                            .background(
                                ShieldPurple.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ShieldPurple
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Change Password",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Update your account password",
                            fontSize = 12.sp,
                            color = ShieldTextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = ShieldTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )



            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // ====================================================
            // SAVE CHANGES
            // ====================================================

            Button(
                onClick = {

                    val ageValue =
                        age.toIntOrNull()

                    if (name.isBlank()) {

                        profileError =
                            "Please enter your name"

                        return@Button
                    }

                    if (
                        ageValue == null ||
                        ageValue < 1 ||
                        ageValue > 120
                    ) {

                        profileError =
                            "Please enter a valid age"

                        return@Button
                    }

                    if (gender.isBlank()) {

                        profileError =
                            "Profile gender information is missing"

                        return@Button
                    }

                    val token =
                        tokenManager.getToken()

                    if (token.isNullOrBlank()) {

                        profileError =
                            "Authentication session expired"

                        return@Button
                    }

                    profileError = null
                    saveMessage = null
                    isSaving = true

                    coroutineScope.launch {

                        try {

                            val request =
                                ProfileCreateRequest(
                                    name = name.trim(),
                                    age = ageValue,
                                    gender = gender,
                                    profile_picture =
                                        profilePicture
                                )

                            val response =
                                profileRepository.createProfile(
                                    token = token,
                                    request = request
                                )

                            if (response != null) {

                                name =
                                    response.name
                                        ?: name.trim()

                                age =
                                    response.age
                                        ?.toString()
                                        ?: ageValue.toString()

                                gender =
                                    response.gender
                                        ?: gender

                                profilePicture =
                                    response.profile_picture

                                saveMessage =
                                    "Profile updated successfully"

                                // Keep existing callback compatibility
                                onSaveClick(
                                    name,
                                    ageValue
                                )

                            } else {

                                profileError =
                                    "Unable to update profile. Please try again."
                            }

                        } catch (e: Exception) {

                            println(
                                "VoiceShield Profile Update Error: ${e.message}"
                            )

                            profileError =
                                "Unable to update profile. Please try again."

                        } finally {

                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShieldBlue
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp)
                )

                Spacer(
                    modifier = Modifier.width(9.dp)
                )

                Text(
                    text =
                        if (isSaving)
                            "SAVING..."
                        else
                            "SAVE CHANGES",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Divider(
                color = Color(0xFFE2E8F0)
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // ====================================================
            // PRIVACY FOOTER
            // ====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ShieldGreen,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "Your voice. Your privacy.",
                    color = ShieldTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}