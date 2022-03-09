package com.example.carfaxtechnicalassignment


import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.elevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable
import java.text.NumberFormat
import java.util.*

class FireBaseStarter : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

}


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        /*
        TODO: following block is for parsing the data from the database,
             reposition with architecture viability in mind
        */
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

            val mileage = hashMap["mileage"].toString()

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

            val phoneNum = hashMap["dealer"].toString().substringAfter("phone=")
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

    data class CarInfoModel(
        val year: String? = null,
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
    ) : Serializable {
        //TODO: Add variable to handle image links

    }

    @Composable
    fun Screen(inventoryList: ArrayList<*>, viewModel: ScreenViewModel = viewModel()) {
        if (inventoryList.size > 1) {
            when (val uiState = viewModel.uiState.value) {
                ScreenState.Success -> {
                    LazyColumn {
                        items(inventoryList) { cardInfoList ->
                            ListingCard(cardInfoList = cardInfoList)
                        }
                    }
                }
                ScreenState.Error -> ErrorOccurred()
            }
        } else {
            Log.e(TAG, "parsedInventoryList was not updated correctly")
        }


    }

    class ScreenViewModel : ViewModel() {
        private val _uiState = mutableStateOf<ScreenState>(ScreenState.Success)
        val uiState: State<ScreenState>
            get() = _uiState
    }

    sealed class ScreenState {
        object Success : ScreenState()
        object Error : ScreenState()

    }

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    private fun ListingCard(cardInfoList: Any) {
        val cardInfo = arrayListOf<CarInfoModel>(cardInfoList as CarInfoModel).elementAt(0)
        Log.i(TAG, "Image Url: " + cardInfo.imageUrl)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable(onClick = {
                    Toast
                        .makeText(
                            applicationContext,
                            "TODO: Link Details Page to button press.",
                            Toast.LENGTH_SHORT
                        )
                        .show()
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
                    cardInfo.year.toString() + " " +
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
                        Toast.makeText(
                            applicationContext,
                            "TODO: implement app switch to phone app with number input.",
                            Toast.LENGTH_SHORT
                        ).show()
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

            //TODO: Add support for product images

        }

    }

    @Composable
    private fun ErrorOccurred() {
        Text(
            text = "An error has occured when attempting to load the list.",
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.margin_small))
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }

    @Composable
    private fun Details(cardInfoList: Any) {
        val cardInfo = arrayListOf<CarInfoModel>(cardInfoList as CarInfoModel).elementAt(0)
        Scaffold {
            Text(
                cardInfo.year.toString() + " " +
                        cardInfo.make + " " +
                        cardInfo.model
            )
            Text(
                cardInfo.price + "\t|\t" +
                        cardInfo.mileage
            )
            Divider()
            Text(text = "Vehicle Info")
            Row {
                Text(text = "Location")
                Text(cardInfo.location)
            }
            Row {
                Text(text = "Exterior Color")
                Text(cardInfo.exColor)
            }
            Row {
                Text(text = "Interior Color")
                Text(cardInfo.inColor)
            }
            Row {
                Text(text = "Drive Type")
                Text(text = cardInfo.driveType)
            }
            Row {
                Text(text = "Transmission")
                Text(text = cardInfo.trans)
            }
            Row {
                Text(text = "Body Style")
                Text(text = cardInfo.bodyStyle)
            }
            Row {
                Text(text = "Engine")
                Text(text = cardInfo.engine)
            }
            Row {
                Text(text = "Fuel")
                Text(text = cardInfo.fuel)
            }
            BottomAppBar {
                Button(onClick = {
                    Toast.makeText(
                        applicationContext,
                        "Implement Call Dealer button switch to phone app with number input.",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text(text = "Call Dealer")
                }
            }
        }
    }


}



