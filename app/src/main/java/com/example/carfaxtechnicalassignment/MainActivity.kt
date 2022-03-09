package com.example.carfaxtechnicalassignment


import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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

    private fun getCarList(CarDataList: ArrayList<*>): ArrayList<CarInfoModel> {
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
            val location = hashMap["location"].toString()
            parsedInventory.add(CarInfoModel(year, make, model, trim, price, mileage, location))
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
        val location: String
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
//                        for (i in inventoryList)
//                            ListingCard(i)
                    }

                }
                ScreenState.Error -> ErrorOccured()
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

    @Composable
    private fun ListingCard(cardInfoList: Any) {
        val cardInfo = arrayListOf<CarInfoModel>(cardInfoList as CarInfoModel).elementAt(0)
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { }, elevation = 2.dp
        ) {

            Column {
                Text(
                    cardInfo.year.toString() + " " +
                            cardInfo.make + " " +
                            cardInfo.model
                )
                Text(
                    cardInfo.price + "\t|\t" +
                            cardInfo.mileage
                )
            }
            //TODO: Add support for product images

        }
    }

    @Composable
    private fun ErrorOccured() {
        Text(
            text = "An error has occured when attempting to load the list.",
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.margin_small))
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}



