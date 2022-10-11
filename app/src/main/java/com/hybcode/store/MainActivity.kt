package com.hybcode.store

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.braintreepayments.api.BraintreeClient
import com.braintreepayments.api.DataCollector
import com.braintreepayments.api.PayPalCheckoutRequest
import com.braintreepayments.api.PayPalCheckoutRequest.USER_ACTION_COMMIT
import com.braintreepayments.api.PayPalClient
import com.hybcode.store.ApiConstants.DOMAIN_URL
import com.hybcode.store.ApiConstants.TOKENIZATION_KEY
import com.hybcode.store.ApiConstants.YOUR_API_KEY_HERE
import com.hybcode.store.databinding.ActivityMainBinding
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val storeViewModel: StoreViewModel by viewModels()
    private var deviceData = ""

    // TODO: put the ISO code for your store's base currency as the value of the defCurrency variable
    private val defCurrency = "GBP"
    private var exchangeData: JSONObject? = null
    private var selectedCurrency: Currency? = null
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var paypalClient: PayPalClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as
                NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_products, R.id.navigation_checkout))

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        /* FIXME: Here we manually define a list of products
In reality, you may want to retrieve product information in real-time from your website. */
        val broccoli = Product(R.drawable.broccoli, "Broccoli", 1.40)
        val carrots = Product(R.drawable.carrots, "Carrots", 0.35)
        val strawberries = Product(R.drawable.strawberries, "Strawberries", 2.00)
        val items = listOf(broccoli, carrots, strawberries)
        storeViewModel.products.value = items

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        getCurrencyData()

        braintreeClient = BraintreeClient(this, TOKENIZATION_KEY)
        paypalClient = PayPalClient(braintreeClient)

        getClientToken()

    }

    override fun onResume() {
        super.onResume()
        val browserSwitchResult = braintreeClient.deliverBrowserSwitchResult(this)
        if (browserSwitchResult != null) {
            paypalClient.onBrowserSwitchResult(browserSwitchResult) { payPalAccountNonce, error ->
                if (error != null) {
                    Toast.makeText(this, resources.getString(R.string.payment_error), Toast.LENGTH_SHORT).show()
                } else postNonceToServer(payPalAccountNonce?.string)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.currencies_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (exchangeData == null) {
            Toast.makeText(this, resources.getString(R.string.exchange_data_unavailable),
                Toast.LENGTH_SHORT).show()
            getCurrencyData()
        } else {
            when (item.itemId) {
// TODO: Configure each currency exchange menu item here
                R.id.currency_gbp -> setCurrency("GBP")
                R.id.currency_usd -> setCurrency("USD")
                R.id.currency_eur -> setCurrency("EUR")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getCurrencyData(): JSONObject? {
        val client = AsyncHttpClient()
// TODO: Replace YOUR-API-KEY-HERE with your exchange rate API key
        client.get("https://v6.exchangerate-api.com/v6/${YOUR_API_KEY_HERE}/latest/$defCurrency", object : TextHttpResponseHandler() {

            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseString: String?) {
                if (responseString != null) {
                    exchangeData = JSONObject(responseString)
                    val currencyPreference = sharedPreferences.getString("currency", defCurrency) ?: defCurrency
                    setCurrency(currencyPreference)
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable:
            Throwable?) {
                Toast.makeText(this@MainActivity, resources.getString(R.string.exchange_data_unavailable),
                    Toast.LENGTH_SHORT).show()
                setCurrency(defCurrency)
            }
        })
        return null
    }

    private fun setCurrency(isoCode: String) {
        val exchangeRate = exchangeData?.getJSONObject("conversion_rates")?.getDouble(isoCode)
// TODO: Define the base currency here
        var currency = Currency(defCurrency, "£", null)
        if (exchangeRate != null) {
            when (isoCode) {// TODO: Define each additional currency your store supports here
                "USD" -> currency = Currency(isoCode, "$", exchangeRate)
                "EUR" -> currency = Currency(isoCode, "€", exchangeRate)
            }
        }
        sharedPreferences.edit().apply {
                    putString("currency", isoCode)
                    apply()
                }
        selectedCurrency = currency
        storeViewModel.currency.value = currency
        storeViewModel.calculateOrderTotal()
    }

    //Your website is responsible for generating client tokens, and each token is valid for 24 hours.
    private fun getClientToken() {
        val client = AsyncHttpClient()
// TODO: Replace YOUR-DOMAIN.com with your website domain
        client.get("$DOMAIN_URL/store/client_token.php", object : TextHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseString: String?) {
                braintreeClient = BraintreeClient(this@MainActivity, responseString ?: TOKENIZATION_KEY)
                paypalClient = PayPalClient(braintreeClient)
            }

            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseString: String?, throwable:
            Throwable?) {
                braintreeClient = BraintreeClient(this@MainActivity, TOKENIZATION_KEY)
                paypalClient = PayPalClient(braintreeClient)
            }
        })
    }

    fun initiatePayment() {
        if (storeViewModel.orderTotal.value == 0.00) return
        val orderTotal = storeViewModel.orderTotal.value.toString()
        saveOrderTotal(orderTotal)
        val request = PayPalCheckoutRequest(orderTotal)
        request.currencyCode = selectedCurrency?.code ?: defCurrency
        request.userAction = USER_ACTION_COMMIT
        paypalClient.tokenizePayPalAccount(this, request) { error ->
            error?.let {
                Toast.makeText(this, getString(R.string.paypal_error, it.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveOrderTotal(total: String?) = sharedPreferences.edit().apply {
        putString("orderTotal", total)
        apply()
    }

    private fun collectDeviceData() {
        DataCollector(braintreeClient).collectDeviceData(this) { data, _ ->
            deviceData = data ?: ""
        }
    }

    private fun postNonceToServer(nonce: String?) {
        if (nonce == null) {
        Toast.makeText(this, getString(R.string.payment_error), Toast.LENGTH_LONG).show()
        return
    }
        collectDeviceData()
        val client = AsyncHttpClient()
        val params = RequestParams().apply {
            put("amount", sharedPreferences.getString("orderTotal", null) ?: return)
            put("currency_iso_code", storeViewModel.currency.value?.code ?: defCurrency)
            put("payment_method_nonce", nonce)
            put("client_device_data", deviceData)
        }

        saveOrderTotal(null)
// TODO: Replace YOUR-DOMAIN.com with your website domain
        client.post("https://YOUR-DOMAIN.com/store/process_transaction.php", params,
            object : TextHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, outcome: String?) {
                    if (outcome == "SUCCESSFUL") {
                        Toast.makeText(this@MainActivity, resources.getString(R.string.payment_successful),
                            Toast.LENGTH_LONG).show()
                        clearCart()
                    } else Toast.makeText(this@MainActivity, resources.getString(R.string.payment_error),
                        Toast.LENGTH_LONG).show()
                }

                override fun onFailure(statusCode: Int, headers: Array<out Header>?, outcome: String?, throwable:
                Throwable?) { }
            }
        )
    }

    private fun clearCart() {
        val products = storeViewModel.products.value ?: listOf()
        for (p in products) p.inCart = false
        storeViewModel.products.value = products
        storeViewModel.orderTotal.value = 0.00
        storeViewModel.clearCart.value = true
    }
}