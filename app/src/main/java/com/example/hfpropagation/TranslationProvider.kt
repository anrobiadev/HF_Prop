package com.example.hfpropagation

import android.R

data class AppStrings(
    val appTitle: String = "HF PROP - A field companion",
    val tabs: List<String>,
    val locationSource: String,
    val locationTarget: String,
    val analyzeBtn: String,
    val msgErorLocator: String,
    val pathPrevLabel: String,
    val patGeom: String,
    val beringLabel: String,

    // --- Settings Tab Labels ---
    val stationConfig: String,
    val pwrLabel: String,
    val antLabel: String,
    val modeLabel: String,
    val solarActivity: String,
    val ssnLabel: String,
    val sfiLabel: String,
    val kIndexLabel: String,
    val appearance: String,
    val themeLabel: String,
    val langLabel: String,
    val autoFetchBtn: String,
    val txtKinfo1: String,
    val txtKinfo2: String,
    val solarLabel: String,
    val nooaLabel: String,
    val fof2Label: String,
    val useCorrectionLabel: String,
    val efectiveSSNeLabel: String,
    val infoGenericData: String,
    val activeReferenceStations: String,
    val noInternetTitle: String,
    val noInternetMessage: String,


    //----Onine Data Tab Labels----
    val titlu: String,
    val copyRight: String,
    var onlineTitle: String,
    var onlineMessage: String,
    val lastUpdate: String,


    // --- UI Extras ---
    val copyToast: String,
    val loadingText: String,

    // --- Help Section ---
    val helpIntroduction: String,
    val helpLocation: String,
    val helpResults: String,
    val helpSettings: String,
    val helpGlossary: String,
    val helpCredits: String,
    val errorNoInternet: String,
    val warningDefaultData: String,
    val errorFetchFailed: String,
    val creditTitle: String,
    val introTitle: String,
    val introTitleTab: String,
    val locationTitle: String,
    val resultsTitle: String,
    val settingsTitle: String,
    val glossaryTitle: String,

    val solarDataTitle: String,
    val noDataLabel: String,
    val ionosondeTitle: String,
    val noIonoData: String,
    val lastIonoUpdate: String,
    val engineTitle: String,
    val helpEngine: String,
    val stationUnavailable: String,
    val cachedImagesNote: String,
)

val EnglishStrings = AppStrings(
    tabs = listOf("Location", "Results", "Settings", "Help"),
    locationSource = "Source (TX)",
    locationTarget = "Target (RX)",
    analyzeBtn = "Analyze Propagation",
    msgErorLocator = "Enter valid grid squares to view map",
    pathPrevLabel = "Path preview",
    patGeom = "Path Geometry",
    beringLabel = "Bearing",

    stationConfig = "Station Configuration",
    pwrLabel = "Transmit Power (Watts)",
    antLabel = "Antenna Type",
    modeLabel = "Operating Mode",
    solarActivity = "Solar Activity",
    ssnLabel = "SSN",
    sfiLabel = "SFI",
    kIndexLabel = "K-Index",
    appearance = "Appearance",
    themeLabel = "App Theme",
    langLabel = "Language",
    autoFetchBtn = "Auto-Fetch",
    txtKinfo1 = "Warning: High K-Index (",
    txtKinfo2 = ") indicates geomagnetic storming. HF propagation may be degraded.",
    solarLabel = "Solar Activity",
    nooaLabel = "Live Solar Data:",
    fof2Label = "Avg foF2 Value (MHz)",
    useCorrectionLabel = "Apply ionospheric correction to SSNe",
    efectiveSSNeLabel = "Effective SSN (SSNe): ",
    activeReferenceStations = "Active Reference Stations:",


    titlu = "Live Solar-Terrestrial Data",
    copyRight = "Data provided by N0NBH hamqsl.com",
    noInternetTitle = "No internet connection",
    noInternetMessage = "To access online data, an active internet connection is required.",
    onlineMessage = "This tab displays real-time space weather data via graphical banners provided by N0NBH (hamqsl.com). It includes current solar indices (SFI, SN, A, K), VHF propagation status, and MUF (Maximum Usable Frequency) maps." +
            "These images are essential for a quick visual check of global conditions and require an active internet connection to be loaded.",
    lastUpdate = "Last update:",

    copyToast = "Locator %s copied to clipboard",
    loadingText = "Analyzing Ionosphere...",
    errorNoInternet = "No internet connection. Please check your network.",
    warningDefaultData = "Warning: Using default values. Results may be inaccurate. Fetch live data or enter manually.",
    errorFetchFailed = "Failed to sync data. Please try again later.",
    infoGenericData = "TX location is unset. Using generic ionospheric data (Europe).",
    creditTitle   ="Credits & Info",
    introTitle ="User manual & Reference Guide",
    introTitleTab ="1.Introduction",
    locationTitle ="2.Location Tab",
    onlineTitle = "4.Online Data Tab",
    resultsTitle  ="3.Results Tab",
    settingsTitle ="5.Settings Tab",
    glossaryTitle ="6.Glossary",


    helpIntroduction = "This application uses an advanced mathematical engine to predict High Frequency (HF) radio propagation. It calculates Maximum Usable Frequencies (MUF), signal footprints, and band reliability using real-time space weather data from NRC DRAO Penticton (Solar Flux Index), NOAA (K-Index), and SIDC (Sunspot Number). For maximum precision, ionospheric corrections are derived from real-time global Digisonde measurements via the Global Ionospheric Radio Observatory (GIRO).",
    helpLocation = "• Source (TX): Enter your 4 or 6-character Maidenhead grid square (e.g., KN34). You can also use the GPS button to automatically detect your current grid.\n• Target (RX): Enter the destination grid square.\n• Interactive Map: Use the 'Layers' button on the map to toggle QTH Gridlines or switch between Standard, Topographic, and Satellite views.\n• Map Tools: Long-press anywhere on the map to instantly copy the 6-character Maidenhead locator (e.g., KN34bj) to your clipboard for easy sharing or logging.\n• Analyze Propagation: Press this button to run the prediction engine using the values set in the Settings tab.",
    helpResults = "• MUF / FOT / LUF Chart: Displays the Maximum Usable Frequency (MUF), Frequency of Optimum Transmission (FOT), and Lowest Usable Frequency (LUF) over 24 hours (UTC). Tap the graph to see exact values.\n• 24h Band Reliability: A heatmap showing the probability of a successful contact on various amateur bands.\n• Signal Footprint Map: Shows the geographical coverage area of your signal. Toggle the checkboxes to isolate specific bands.",
    helpSettings = "• Station Configuration: Adjust your Transmit Power (Watts), Antenna Type, and Operating Mode (SSB, CW, FT8).\n• Dynamic Ionosondes: The app automatically selects the nearest global ionosonde stations (within 100km of the closest one) to fetch real-time foF2 data, providing a precise snapshot of the ionosphere above you.\n• foF2 Correction (SSNe): Enable this to calculate the Effective Sunspot Number (SSNe). While standard SSN is a monthly average, SSNe reflects the current ionization levels, offering much higher prediction accuracy for NVIS and short-path contacts.\n• Manual Override: You can manually override SSN, SFI, and K-Index to run theoretical \"What-If\" scenarios (e.g., simulating a solar maximum).",
    helpGlossary = "• MUF: Maximum Usable Frequency - the highest frequency reflected back to Earth by the ionosphere.\n" +
            "• FOT: Frequency of Optimum Transmission - the most reliable frequency for communication (usually ~85% of MUF).\n" +
            "• LUF: Lowest Usable Frequency - the frequency below which the signal is completely absorbed by the ionospheric D-Layer.\n" +
            "• SFI: Solar Flux Index - a measure of solar radio emissions (10.7cm flux). Higher values indicate better ionization.\n" +
            "• SSN: Sunspot Number - a count of sunspots on the solar disk. Indicator of long-term solar activity.\n" +
            "• K-Index: Measures geomagnetic disturbances (0-9). Values ≥ 4 indicate geomagnetic storms.\n" +
            "• foF2: The critical frequency of the F2 layer. It is the highest frequency that a vertically incident wave can have to be reflected.\n" +
            "• SSNe: Effective Sunspot Number - a corrected SSN value that combines theoretical solar data with real-time ionospheric measurements (foF2) for higher accuracy.",
    helpCredits = "Developed by Robert, YO7ZRO, for RVSU (Radioamatori Voluntari in Situatii de Urgenta).\n\n" +
            "Data sources: NRC DRAO Penticton (SFI), NOAA (K-index), SIDC SILSO (Kalman Filtered SSN), and LGDC (real-time foF2 data from global ionosonde stations, dynamically selected based on your location via GIRO).\n\n" +
            "For information, feedback, or bug reports, feel free to contact: yo7zro@gmail.com\n\n" +
            "This application is 100% free and can be used freely by the amateur radio community. 73!",

    solarDataTitle   = "Solar Data",
    noDataLabel      = "No data",
    ionosondeTitle   = "Ionosonde",
    noIonoData       = "No data — tap Auto-Fetch",
    lastIonoUpdate   = "Last ionosonde update:",
    engineTitle      = "7. Propagation Engine",
    stationUnavailable = "Station unavailable",
    cachedImagesNote   = "Showing cached images — no internet connection",
    helpEngine       = "CCIR/ITU engine: foF2 + MUF(D) fetched from GIRO FastChar API (/fastchar/getbest), 2 requests per station with 2s delay. IRI-2016 lookup table for foF2 interpolation (3 SSN x 12 months x 24h x 17lat x 18lon). D-layer absorption: George & Bradley (1974). SNR: ITU-R P.533/P.372. Reliability: joint MUF/LUF/SNR probability. NVIS: reliability boost for f < foF2 at distances < 500km. Sources: SIDC SILSO, DRAO Penticton, NOAA SWPC, GIRO LGDC."
)

val RomanianStrings = AppStrings(
    tabs = listOf("Locație", "Rezultate", "Setări", "Manual"),
    locationSource = "Sursă (TX)",
    locationTarget = "Țintă (RX)",
    analyzeBtn = "Analizează Propagarea",
    msgErorLocator = "Introduceti un locator valid pentru a activa harta.",
    pathPrevLabel = "Vizualizarea traseului de legătură",
    patGeom = "Geometria traseului",
    beringLabel = "Azimut",

    stationConfig = "Configurație Stație",
    pwrLabel = "Putere Emisie (Wați)",
    antLabel = "Tip Antenă",
    modeLabel = "Mod de Operare",
    solarActivity = "Activitate Solară",
    ssnLabel = "SSN",
    sfiLabel = "Index Flux Solar (SFI)",
    kIndexLabel = "Index K",
    appearance = "Aspect Interfață",
    themeLabel = "Tema Aplicației",
    langLabel = "Limba",
    autoFetchBtn = "Descărcare Date",
    txtKinfo1 = "Atenție: Index K ridicat (",
    txtKinfo2 = ") indică o furtună geomagnetică. Propagarea HF poate fi afectată.",
    solarLabel = "Activitate Solară",
    nooaLabel = "Date Solare Live:",
    fof2Label = "Valoare medie foF2 (MHz)",
    useCorrectionLabel = "Aplică corecția ionosferică pt. SSNe",
    efectiveSSNeLabel = "SSN Efectiv (SSNe): ",
    infoGenericData = "Locație TX nesetată. Se folosesc date ionosferice generice (Europa).",
    activeReferenceStations = "Stații de Referință Active:",

    titlu = "Date Solare Live",
    copyRight = "Date furnizate de N0NBH via hamqsl.com",
    noInternetTitle = "Lipsă conexiune internet",
    noInternetMessage = "Pentru a accesa datele online, o conexiune activă la internet este necesară.",
    onlineMessage = "Acest tab afișează date meteo spațiale în timp real sub formă de bannere grafice, preluate direct de la N0NBH (hamqsl.com). Acestea includ indicii solari actuali (SFI, SN, A, K), starea propagării în benzile VHF și hărți MUF (Frecvența Maximă Utilizabilă)." + "" +
            "Imaginile sunt esențiale pentru o verificare vizuală rapidă a condițiilor globale și necesită o conexiune activă la internet pentru a fi încărcate.",
    lastUpdate = "Ultima actualizare:",

    copyToast = "Locatorul %s a fost copiat",
    loadingText = "Se analizează ionosfera...",
    errorNoInternet = "Lipsă conexiune internet. Te rugăm să verifici rețeaua.",
    warningDefaultData = "Atenție: Se folosesc valori implicite. Rezultatele pot fi eronate. Descarcă date live sau introdu-le manual.",
    errorFetchFailed = "Sincronizarea a eșuat. Te rugăm să încerci mai târziu.",
    creditTitle = "Credite și Info",
    introTitle = "Manual de utilizare și Ghid de referință",
    introTitleTab = "1.Introducere",
    locationTitle = "2.Tab Locație",
    onlineTitle = "4.Tab Date Online",
    resultsTitle = "3.Tab Rezultate",
    settingsTitle = "5.Tab Setări",
    glossaryTitle = "6.Glosar",

    helpIntroduction = "Această aplicație folosește un motor matematic avansat pentru a prezice propagarea radio de înaltă frecvență (HF). Calculează Frecvențele Maxime Utilizabile (MUF), amprentele semnalului și fiabilitatea benzilor folosind date meteo spațiale în timp real de la NRC DRAO Penticton (Indexul de Flux Solar), NOAA (Indexul K) și SIDC (Numărul de Pete Solare). Pentru o precizie maximă, corecțiile ionosferice sunt obținute din măsurătorile globale ale Digisondelor în timp real prin intermediul Global Ionospheric Radio Observatory (GIRO).",
    helpLocation = "• Sursă (TX): Introdu locatorul Maidenhead de 4 sau 6 caractere (ex: KN34). Poți folosi și butonul GPS pentru a detecta automat gridul curent.\n• Țintă (RX): Introdu locatorul de destinație.\n• Hartă Interactivă: Folosește butonul 'Straturi' pentru a activa liniile QTH sau pentru a comuta între vizualizările Standard, Topografic și Satelit.\n• Unelte Hartă: Apasă lung oriunde pe hartă pentru a copia instantaneu locatorul Maidenhead de 6 caractere (ex: KN34bj) în clipboard pentru partajare sau logare.\n• Analizează Propagarea: Apasă acest buton pentru a porni motorul de predicție folosind valorile setate în tab-ul Setări.",
    helpResults = "• Grafic MUF / FOT / LUF: Afișează Frecvența Maximă Utilizabilă (MUF), Frecvența Optimă de Transmisie (FOT) și Frecvența Minimă Utilizabilă (LUF) pe parcursul a 24 de ore (UTC). Atinge graficul pentru valori exacte.\n• Fiabilitate Benzi 24h: O hartă termică ce arată probabilitatea unui contact reușit în diverse benzi de radioamatori.\n• Amprenta Semnalului: Arată zona de acoperire geografică a semnalului tău. Folosește bifele pentru a izola benzi specifice.",
    helpSettings = "• Configurație Stație: Ajustați puterea de emisie (Wați), tipul antenei și modul de lucru (SSB, CW, FT8).\n• Ionosonde Dinamice: Aplicația selectează automat cele mai apropiate stații ionosondă globale (în limita a +100km față de cea mai apropiată) pentru date foF2 în timp real, oferind o imagine precisă a ionosferei deasupra locației dvs.\n• Corecție foF2 (SSNe): Activați această opțiune pentru a calcula numărul de pete solare efectiv (SSNe). Spre deosebire de SSN-ul standard (medie lunară), SSNe reflectă ionizarea actuală, oferind o precizie mult mai mare pentru contactele NVIS sau pe distanțe scurte.\n• Control Manual: Puteți suprascrie manual SSN, SFI și indicele K pentru scenarii teoretice (ex. simularea unui maxim solar).",
    helpGlossary = "• MUF: Frecvența Maximă Utilizabilă - cea mai înaltă frecvență reflectată înapoi spre Pământ de către ionosferă.\n" +
            "• FOT: Frecvența Optimă de Transmisie - cea mai fiabilă frecvență pentru comunicații (aprox. 85% din MUF).\n" +
            "• LUF: Frecvența Minimă Utilizabilă - frecvența sub care semnalul este absorbit complet de stratul D al ionosferei.\n" +
            "• SFI: Indexul de Flux Solar - măsoară emisiile radio solare (fluxul la 10.7cm). Valori mari indică o ionizare mai bună.\n" +
            "• SSN: Numărul de pete solare - numărătoarea petelor de pe discul solar. Indicator al activității solare pe termen lung.\n" +
            "• Index K: Măsoară perturbațiile geomagnetice (0-9). Valorile ≥ 4 indică furtuni geomagnetice.\n" +
            "• foF2: Frecvența critică a stratului F2. Este cea mai înaltă frecvență reflectată la o incidență verticală.\n" +
            "• SSNe: SSN Efectiv - o valoare SSN corectată care combină datele solare teoretice cu măsurătorile ionosferice în timp real (foF2) pentru o precizie sporită.",
    helpCredits = "Dezvoltat de Robert, YO7ZRO, pentru RVSU (Radioamatori Voluntari în Situații de Urgență).\n\n" +
            "Surse de date: NRC DRAO Penticton (SFI), NOAA (Indexul K), SIDC SILSO (SSN filtrat Kalman) și LGDC (date foF2 în timp real de la stații ionosondă globale, selectate dinamic în funcție de locația dvs. prin GIRO).\n\n" +
            "Pentru informații, feedback sau rapoarte de erori, mă poți contacta la: yo7zro@gmail.com\n\n" +
            "Această aplicație este 100% gratuită și poate fi utilizată liber de comunitatea de radioamatori. 73!",

    solarDataTitle   = "Date Solare",
    noDataLabel      = "Lipsa date",
    ionosondeTitle   = "Ionosonda",
    noIonoData       = "Lipsa date — apasa Descarcare Date",
    lastIonoUpdate   = "Ultima actualizare ionosonda:",
    engineTitle      = "7. Motorul de Calcul",
    stationUnavailable = "Stație indisponibilă",
    cachedImagesNote   = "Se afișează imaginile salvate — fără conexiune internet",
    helpEngine       = "Motor CCIR/ITU: foF2 + MUF(D) de la GIRO FastChar API, 2 cereri per statie cu delay 2s. Tabel IRI-2016 pentru interpolarea foF2 (3 SSN x 12 luni x 24h x 17lat x 18lon). Absorbtie D-layer: George & Bradley (1974). SNR: ITU-R P.533/P.372. Fiabilitate: probabilitate combinata MUF/LUF/SNR. NVIS: bonus pentru f < foF2 la distante < 500km. Surse: SIDC SILSO, DRAO Penticton, NOAA SWPC, GIRO LGDC."
)

val HungarianStrings = AppStrings(
    tabs = listOf("Helyszín", "Eredmények", "Beállítások", "Súgó"),
    locationSource = "Forrás (TX)",
    locationTarget = "Cél (RX)",
    analyzeBtn = "Terjedés elemzése",
    msgErorLocator = "Adjon meg érvényes grid kódokat a térképhez.",
    pathPrevLabel = "Útvonal előnézete",
    patGeom = "Útvonalgeometria",
    beringLabel = "Irányszög (Azimut)",

    stationConfig = "Állomás konfigurációja",
    pwrLabel = "Adóteljesítmény (Watt)",
    antLabel = "Antenna típusa",
    modeLabel = "Üzemmód",
    solarActivity = "Naptevékenység",
    ssnLabel = "SSN",
    sfiLabel = "SFI",
    kIndexLabel = "K-index",
    appearance = "Megjelenés",
    themeLabel = "Alkalmazás témája",
    langLabel = "Nyelv",
    autoFetchBtn = "Adatok letöltése",
    txtKinfo1 = "Figyelem: A magas K-index (",
    txtKinfo2 = ") geomágneses vihart jelez. A rövidhullámú terjedés gyengülhet.",
    solarLabel = "Naptevékenység",
    nooaLabel = "Élő Solar adatok:",
    fof2Label = "Átlagos foF2 érték (MHz)",
    useCorrectionLabel = "SSNe ionoszférikus korrekció alkalmazása",
    infoGenericData = "TX helyszín nincs megadva. Általános ionoszféra adatok használata (Európa).",

    titlu = "Élő Solar adatok",
    copyRight = "Adatok hamqsl.com által",
    noInternetTitle = "Nincs internetkapcsolat",
    noInternetMessage = "Az online adatok eléréséhez aktív internetkapcsolat szükséges.",
    onlineMessage = "Ez a fül valós idejű űridőjárási adatokat jelenít meg grafikus bannerek formájában, közvetlenül az N0NBH (hamqsl.com) oldaláról. Tartalmazza az aktuális naptevékenységi indexeket (SFI, SN, A, K), a VHF terjedési állapotot és a MUF (maximálisan használható frekvencia) térképeket." +
            "Ezek a képek elengedhetetlenek a globális viszonyok gyors ellenőrzéséhez, és a betöltésükhöz aktív internetkapcsolat szükséges.",
    lastUpdate = "Legutóbbi frissítés:",

    copyToast = "%s lokátor a vágólapra másolva",
    loadingText = "Ionoszféra elemzése...",
    errorNoInternet = "Nincs internetkapcsolat. Kérjük, ellenőrizze a hálózatot.",
    warningDefaultData = "Figyelem: Alapértelmezett értékek használata. Az eredmények pontatlanok lehetnek. Töltse le az élő adatokat, vagy adja meg őket kézzel.",
    errorFetchFailed = "A szinkronizálás nem sikerült. Kérjük, próbálja meg később.",
    efectiveSSNeLabel = "Effektív SSN (SSNe): ",
    activeReferenceStations = "Aktív referenciaszórák:",
    creditTitle = "Köszönetnyilvánítás és Infó",
    introTitle = "Használati útmutató és Kézikönyv",
    introTitleTab = "1.Bevezetés",
    locationTitle = "2.Helyszín Tab",
    onlineTitle = "4.Online Adatok Tab",
    resultsTitle = "3.Eredmények Tab",
    settingsTitle = "5.Beállítások Tab",
    glossaryTitle = "6.Szójegyzék",

    helpIntroduction = "Ez az alkalmazás egy fejlett matematikai modellt használ a rövidhullámú (HF) rádióterjedés előrejelzésére. Kiszámítja a maximálisan használható frekvenciákat (MUF), a jellefedettséget és a sávok megbízhatóságát az NRC DRAO Penticton (szoláris fluxus index), a NOAA (K-index) és a SIDC (napfoltszám) valós idejű adatai alapján. A maximális pontosság érdekében az ionoszférikus korrekciókat a Global Ionospheric Radio Observatory (GIRO) hálózatán keresztül, globális Digiszonda állomások valós idejű méréseiből nyeri.",
    helpLocation = "• Forrás (TX): Adja meg a 4 vagy 6 karakteres Maidenhead lokátor kódját (pl. KN34). A GPS gombbal automatikusan is meghatározhatja pozícióját.\n• Cél (RX): Adja meg a célállomás lokátor kódját.\n• Interaktív térkép: A 'Rétegek' gombbal bekapcsolhatja a hálóvonalakat, vagy válthat a nézetek között.\n• Eszközök: Nyomja hosszan a térképet a 6 karakteres lokátor (pl. KN34bj) másolásához.\n• Terjedés elemzése: Indítsa el az elemzést a Beállítások fülön megadott értékek alapján.",
    helpResults = "• MUF / FOT / LUF grafikon: Megmutatja a frekvenciák alakulását 24 órás (UTC) bontásban. Érintse meg a grafikont a pontos értékekhez.\n• 24 órás sávmegbízhatóság: Hőtérkép a sikeres kapcsolat valószínűségéről a különböző amatőrsávokon.\n• Jellefedettségi térkép: Megmutatja a jel földrajzi eloszlását. A jelölőnégyzetekkel szűrhet az egyes sávokra.",
    helpSettings = "• Állomásbeállítások: Állítsa be az adóteljesítményt (Watt), az antenna típusát és az üzemmódot (SSB, CW, FT8).\n• Dinamikus Ionoszondák: Az alkalmazás automatikusan kiválasztja a legközelebbi globális ionoszonda állomásokat (+100 km-es körzeten belül a legközelebbitől) a valós idejű foF2 adatokhoz, pontos képet adva az Ön feletti ionoszféráról.\n• foF2 korrekció (SSNe): Kapcsolja be ezt az effektív napfoltszám (SSNe) kiszámításához. Míg a standard SSN havi átlag, az SSNe a jelenlegi ionizációs szinteket tükrözi, így sokkal pontosabb előrejelzést ad az NVIS és rövid távú összeköttetésekhez.\n• Manuális felülbírálás: Kézzel is megadhatja az SSN, SFI és K-index értékeket elméleti szimulációkhoz (pl. naptevékenységi maximum szimulálása).",
    helpGlossary = "• MUF: Maximális használható frekvencia - az ionoszféra által a Földre visszavert legmagasabb frekvencia.\n" +
            "• FOT: Optimális átviteli frekvencia - a legmegbízhatóbb frekvencia a kommunikációhoz (általában a MUF ~85%-a).\n" +
            "• LUF: Legalacsonyabb használható frekvencia - az a frekvencia, amely alatt a jelet az ionoszféra D-rétege teljesen elnyeli.\n" +
            "• SFI: Szoláris fluxus index - a naprádió-kibocsátás mértéke (10,7 cm-es fluxus). A magasabb értékek jobb ionizációt jeleznek.\n" +
            "• SSN: Napfoltszám - a napkorongon lévő napfoltok száma. A hosszú távú naptevékenység mutatója.\n" +
            "• K-index: A geomágneses zavarokat méri (0-9). A ≥ 4 értékek geomágneses viharokat jeleznek.\n" +
            "• foF2: Az F2 réteg kritikus frekvenciája. Ez az a legmagasabb frekvencia, amelyet a réteg függőleges sugárzás esetén még visszaver.\n" +
            "• SSNe: Effektív napfoltszám - egy korrigált SSN érték, amely az elméleti napadatokat valós idejű ionoszférikus mérésekkel (foF2) ötvözi a nagyobb pontosság érdekében.",
    helpCredits = "Fejlesztette: Robert, YO7ZRO, az RVSU (Radioamatori Voluntari in Situatii de Urgenta - Önkéntes Rádióamatőrök Vészhelyzetekben) részére.\n\n" +
            "Adatforrások: NRC DRAO Penticton (SFI), NOAA (K-index), SIDC SILSO (Kalman-szűrt SSN) és LGDC (valós idejű foF2 adatok globális ionoszonda állomásokról, a tartózkodási helye alapján dinamikusan kiválasztva a GIRO hálózatán keresztül).\n\n" +
            "Információ, visszajelzés vagy hibajelentés: yo7zro@gmail.com\n\n" +
            "Ez az alkalmazás 100% ingyenes és szabadon használható az amatőr rádiós közösség számára. 73!",

    solarDataTitle   = "Naptevékenységi Adatok",
    noDataLabel      = "Nincs adat",
    ionosondeTitle   = "Ionoszonda",
    noIonoData       = "Nincs adat — kattintson az Adatok letöltése gombra",
    lastIonoUpdate   = "Utolsó ionoszonda frissítés:",
    engineTitle      = "7. Terjedési Motor",
    stationUnavailable = "Állomás nem elérhető",
    cachedImagesNote   = "Gyorsítótárazott képek megjelenítése — nincs internetkapcsolat",
    helpEngine       = "CCIR/ITU motor: foF2 + MUF(D) a GIRO FastChar API-bol, 2 keresest/allomas 2s delayel. IRI-2016 tablazat foF2 interpolaciohoz. D-reteg: George & Bradley (1974). SNR: ITU-R P.533/P.372. Megbizhatosag: egyuttes MUF/LUF/SNR valoszinuseg. NVIS: bisztatasa f < foF2-nel 500km alatt. Adatforrasok: SIDC SILSO, DRAO Penticton, NOAA SWPC, GIRO LGDC."
)