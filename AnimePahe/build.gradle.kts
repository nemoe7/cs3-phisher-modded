// use an integer for version numbers
version = 11

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // android.defaults.buildfeatures.buildconfig=true
    }

    buildTypes{
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Animes (SUB/DUB)"
    authors = listOf("Phisher98", "nemoe7")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "AnimeMovie",
        "Anime",
        "OVA",
    )
    iconUrl = "https://www.google.com/s2/favicons?domain=animepahe.ru/&sz=%size%"
    requiresResources = true
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
