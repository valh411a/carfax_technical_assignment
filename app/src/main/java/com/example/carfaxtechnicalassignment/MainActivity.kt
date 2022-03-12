package com.example.carfaxtechnicalassignment


import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.elevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow


/**
 * Preface class to run on app launch that sets the Firebase persistence to "true" so that
 * data can be accessed while offline.
 */
class FireBaseStarter : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}


class MainActivity : AppCompatActivity() {

    /**
     * Overridden onCreate method, standard procedure for Activity Lifecycle control
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        FirebaseDatabase.getInstance().reference.get().addOnSuccessListener {
            Log.i(TAG, "Obtained Listing")
            val inventoryList = it.child("listings").value as ArrayList<*>
            val parsedInventoryList = getCarList(inventoryList)
            Log.i(TAG, "list length: ${parsedInventoryList.size}")
            setContent {
                Screen(parsedInventoryList)
            }
        }.addOnFailureListener {
            Log.e(TAG, "Firebase was unable to successfully connect the listener")
        }


    }

    /**
     * pulls the needed info for the compose methods and data inclusion from Firebase
     * @param CarDataList the Firebase RTDB location for the information to be pulled.
     * @return parsedInventory the parsed ArrayList of info that was gathered.
     */
    private fun getCarList(
        CarDataList: ArrayList<*>
    ): ArrayList<CarInfoModel> {
        val size = CarDataList.size
        var index = 0
        val parsedInventory = ArrayList<CarInfoModel>()

        while (index < size) {
            val hashMap: HashMap<*, *> = CarDataList[index] as HashMap<*, *>
            val year = hashMap["year"].toString()
            val make = hashMap["make"].toString()
            val model = hashMap["model"].toString()
            val trim = hashMap["trim"].toString()

            //special formatting instance to convert the price into USD
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.maximumFractionDigits = 0
            format.currency = Currency.getInstance("USD")
            val price = format.format(hashMap["currentPrice"].toString().toDouble())

            val mileageNonF = hashMap["mileage"].toString().toFloat()
            Log.i(TAG, "unformatted mileage: $mileageNonF")
            val mileage = prettyCount(mileageNonF) + " mi"

            //trying to create a special extraction rule from the firebase data took too long so I
            // went for a simple string extraction from the raw JSON data instead to save time,
            // can be fixed later and shouldn't cause any issues at the present.
            val location = hashMap["dealer"].toString().substringAfter("city=")
                .substringBefore(", dealer") + ", " +
                    hashMap["dealer"].toString().substringAfter("state=")
                        .substringBefore(", dealer")

            val exColor = hashMap["exteriorColor"].toString()
            val inColor = hashMap["interiorColor"].toString()
            val driveType = hashMap["drivetype"].toString()
            val trans = hashMap["transmission"].toString()
            val engine = hashMap["engine"].toString() + " " + hashMap["displacement"].toString()
            val bodyStyle = hashMap["bodytype"].toString()
            val fuel = hashMap["fuel"].toString()

            val phoneNum = "tel:" + hashMap["dealer"].toString().substringAfter("phone=")
                .substringBefore(", dealer")

            val imageUrl = hashMap["images"]
                .toString()
                .substringAfter("small=[")
                .substringBefore("],")

            parsedInventory.add(
                CarInfoModel(
                    year,
                    make,
                    model,
                    trim,
                    price,
                    mileage,
                    location,
                    exColor,
                    inColor,
                    driveType,
                    trans,
                    engine,
                    bodyStyle,
                    fuel,
                    phoneNum,
                    imageUrl
                )
            )
            index++
        }
        return parsedInventory
    }

    /**
     * Helper function to format mileage, split based on size of the number (mileage)
     */
    private fun prettyCount(number: Number): String? {
        val suffix = charArrayOf(' ', 'k', 'M', 'B', 'T', 'P', 'E')
        val numValue = number.toLong()
        val value = floor(log10(numValue.toDouble())).toInt()
        val base = value / 3
        return if (value >= 3 && base < suffix.size) {
            DecimalFormat("#0.0").format(
                numValue / 10.0.pow((base * 3).toDouble())
            ) + suffix[base]
        } else {
            DecimalFormat("#,##0").format(numValue)
        }
    }

    /**
     * data class helper to make changes easier to track in the Compose methods
     */
    data class CarInfoModel(
        val year: String,
        val make: String,
        val model: String,
        val trim: String,
        val price: String,
        val mileage: String,
        val location: String,
        val exColor: String,
        val inColor: String,
        val driveType: String,
        val trans: String,
        val engine: String,
        val bodyStyle: String,
        val fuel: String,
        val phoneNum: String,
        val imageUrl: String
    ) : Serializable

    /**
     * Main Jetpack Compose method for generating the screen's resources, and Displaying them with
     * saved states through Architecture component helpers (see below)
     *
     * @param inventoryList the generated list of cars for the cards to be created from
     * @param viewModel the viewModel generated from the ScreenViewModel class below
     */
    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun Screen(inventoryList: ArrayList<*>, viewModel: ScreenViewModel = viewModel()) {
        val clicked = remember { mutableStateOf(false) }
        val elementLocation = remember {
            mutableStateOf(
                CarInfoModel(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
                )
            )
        }
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "cardList") {
            composable("cardList") {
                ListingCard(inventoryList, clicked, elementLocation, navController)
            }
            composable("cardClicked") {
                Details(navController, elementLocation)
            }
        }


    }

    /**
     * Jetpack Compose function that draws the elements that make up the default homepage.
     *
     * @param inventoryList the list of info to use to fill out the cards with
     * @param clicked the variable tracking whether a list item was clicked
     * @param elementLocation the location of the item clicked (if any)
     * @param navController the controller that allows for navigation
     * @optIn uses an experimental Coil API from Compose to grab the image directly from a given link
     */
    @OptIn(ExperimentalCoilApi::class)
    @Composable
    private fun ListingCard(
        inventoryList: ArrayList<*>,
        clicked: MutableState<Boolean>,
        elementLocation: MutableState<CarInfoModel>,
        navController: NavHostController
    ) {
        LazyColumn {
            items(inventoryList) { cardInfoList ->
                val cardInfo = arrayListOf(cardInfoList as CarInfoModel).elementAt(0)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable(onClick = {
                            clicked.value = !clicked.value
                            elementLocation.value = cardInfoList
                            navController.navigate("cardClicked") {
                            }

                        }), elevation = 2.dp
                ) {
                    Column {
                        Image(
                            painter = rememberImagePainter(
                                cardInfo.imageUrl,
                                builder = { size(OriginalSize) }),
                            contentDescription = "carImage",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            cardInfo.year + " " +
                                    cardInfo.make + " " +
                                    cardInfo.model
                        )
                        Text(
                            cardInfo.price + "\t|\t" +
                                    cardInfo.mileage
                        )
                        Text(
                            cardInfo.location
                        )
                        Divider()
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL)
                                intent.data = Uri.parse(cardInfo.phoneNum)
                                startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                            elevation = elevation(0.dp, 0.dp)
                        ) {
                            Text(
                                text = "Call Dealer",
                                color = Color.Blue
                            )
                        }


                    }
                }
            }
        }
    }

    /**
     * The more exhaustive list of details for each car in the cardInfoList
     *
     * This page is displayed by default instead of the ListingCard class until functionality is
     * implemented for button navigation.
     *
     * @see ListingCard for explanation on the @optIn and cardInfoList parameter.
     * @param elementLocation The card from the cardInfoList under the ListingCard function
     */
    @OptIn(ExperimentalCoilApi::class)
    @Composable
    private fun Details(
        navController: NavHostController,
        elementLocation: MutableState<CarInfoModel>
    ) {
        Scaffold(
            bottomBar = {
                BottomAppBar {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL)
                            intent.data = Uri.parse(elementLocation.value.phoneNum)
                            startActivity(intent)
                        },
                        modifier = Modifier.fillMaxSize(),
                        elevation = elevation(0.dp)

                    ) {
                        Text(text = "Call Dealer")
                    }
                }

            }
        ) {
            Column(Modifier.clickable {
                navController.navigate("cardList") {
                    popUpTo("home")
                }
            }) {

                Image(
                    //TODO: Fix image width clipping
                    painter = rememberImagePainter(
                        elementLocation.value.imageUrl,
                        builder = { size(OriginalSize) }),
                    contentDescription = "carImage",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    elementLocation.value.year + " " +
                            elementLocation.value.make + " " +
                            elementLocation.value.model
                )
                Text(
                    elementLocation.value.price + "\t|\t" +
                            elementLocation.value.mileage,
                    fontWeight = FontWeight.Bold
                )
                Divider(modifier = Modifier.padding(16.dp))
                Text(
                    text = "Vehicle Info",
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 16.dp)
                        .width(100.dp),
                    fontWeight = FontWeight.Bold
                )
                Row {
                    Text(
                        text = "Location\t",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(
                        elementLocation.value.location,
                        textAlign = TextAlign.End
                    )
                }
                Row {
                    Text(
                        text = "Exterior Color\t",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(elementLocation.value.exColor)
                }
                Row {
                    Text(
                        text = "Interior Color\t",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(elementLocation.value.inColor)
                }
                Row {
                    Text(
                        text = "Drive Type\t",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(text = elementLocation.value.driveType)
                }
                Row {
                    Text(
                        text = "Transmission\t",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(text = elementLocation.value.trans)
                }
                Row {
                    Text(
                        text = "Body Style\t",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(text = elementLocation.value.bodyStyle)
                }
                Row {
                    Text(
                        text = "Engine\t",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(text = elementLocation.value.engine)
                }
                Row {
                    Text(
                        text = "Fuel\t",
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 16.dp)
                            .width(150.dp),
                        color = Color.Gray
                    )
                    Text(text = elementLocation.value.fuel)
                }
            }
        }

    }

    /**
     * Fallback "error occurred" compose component which displays if the app failed to successfully
     * fetch the necessary data from the database
     *
     * @see Screen for usage
     */
    @Composable
    private fun ErrorOccurred() {
        Text(
            text = "An error has occurred when attempting to load the list.",
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }

    /**
     * Architecture component helper for the Screen function, generates the ViewModel on a
     * successful data gathering attempt
     *
     */
    class ScreenViewModel : ViewModel() {
        private val _uiState = mutableStateOf<ScreenState>(ScreenState.Success)
        val uiState: State<ScreenState> = _uiState
    }

    /**
     * sealed class for lifecycle monitoring using screen states
     */
    sealed class ScreenState {
        object Success : ScreenState()
        object Error : ScreenState()
    }
}



