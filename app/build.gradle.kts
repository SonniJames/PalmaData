plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.palmadata.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.palmadata.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "5.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Tres versiones de la app: avc, pdy y cucu ─────────────────────────────
    // Las tres tienen TODO igual: mismo código, mismas pantallas y la misma
    // configuración inicial (IP, puerto e Id Equipo se escriben al instalar,
    // como siempre). Solo cambia:
    //   applicationIdSuffix  el identificador con que Android reconoce la app.
    //                        Al ser distinto, las tres se instalan a la vez en
    //                        el mismo celular en lugar de actualizarse una encima
    //                        de otra, y cada una guarda aparte sus datos.
    //   app_name             el nombre que sale en la pantalla de apps.
    //   CARPETA_EXPORT       carpeta dentro de Descargas para los Excel de
    //                        DESCARGAR. Debe ser distinta en cada versión para
    //                        que no se mezclen (ni se sobrescriban) los archivos
    //                        de una con los de otra.
    //
    // pdy NO lleva sufijo: conserva el identificador de siempre
    // (com.palmadata.app) y la carpeta PalmaData, así la app que ya está
    // instalada se actualiza encima sin perder pendientes ni configuración.
    flavorDimensions += "plantacion"
    productFlavors {
        create("pdy") {
            dimension = "plantacion"
            // sin applicationIdSuffix → com.palmadata.app (el de siempre)
            resValue("string", "app_name", "PalmaData PDY")
            buildConfigField("String", "CARPETA_EXPORT", "\"PalmaData\"")
        }
        create("avc") {
            dimension = "plantacion"
            applicationIdSuffix = ".avc"     // → com.palmadata.app.avc
            resValue("string", "app_name", "PalmaData AVC")
            buildConfigField("String", "CARPETA_EXPORT", "\"PalmaData_AVC\"")
        }
        create("cucu") {
            dimension = "plantacion"
            applicationIdSuffix = ".cucu"    // → com.palmadata.app.cucu
            resValue("string", "app_name", "PalmaData CUCU")
            buildConfigField("String", "CARPETA_EXPORT", "\"PalmaData_CUCU\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true   // necesario para CARPETA_EXPORT de cada versión
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // RecyclerView y CardView para el grid de módulos
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // GPS - FusedLocationProvider
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}