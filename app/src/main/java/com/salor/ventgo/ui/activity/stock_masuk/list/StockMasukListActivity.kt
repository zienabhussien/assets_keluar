package com.salor.ventgo.ui.activity.stock_masuk.list

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.transition.TransitionManager
import com.salor.ventgo.R
import com.salor.ventgo.db.DBS
import com.salor.ventgo.helper.Cons
import com.salor.ventgo.helper.See
import com.salor.ventgo.obj.stock_list_barang_masuk.StockListBarangMasuk
import com.salor.ventgo.obj.warehouse_list.WarehouseList
import com.salor.ventgo.service.ApiClient
import com.salor.ventgo.ui.activity.BaseActivity
import com.salor.ventgo.ui.activity.stock_masuk.ScannerActivity
import com.salor.ventgo.ui.adapter.stock_masuk.StockMasukListAdapter
import com.google.gson.Gson
import com.salor.ventgo.databinding.ActivityStockMasukListBinding
import com.salor.ventgo.databinding.DialogFailureCustomBinding
import com.salor.ventgo.databinding.ItemDialogTambahStockOpnameBinding
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.io.IOException

class StockMasukListActivity : BaseActivity() {

    var str_keyword_search: String = ""
    var isAnim: Boolean = false
    var isNotLoad: Boolean = false
    var offset: Int = 0
    var limit: Int = 20
    var listDataBarang: ArrayList<StockListBarangMasuk> = ArrayList()
    var selected_id_warehouse: Int = 0
    var listWarehouse: ArrayList<WarehouseList> = ArrayList()
    var str_selected_status: String = ""
    var listSpinnerStatus: ArrayList<String> = ArrayList()
    lateinit var spinnerAdapter: AdapterSpinnerStatus
    lateinit var stockMasukListAdapter: StockMasukListAdapter
    lateinit var  binding: ActivityStockMasukListBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockMasukListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarGradiantListSearch(this)

        binding.rBack.setOnClickListener(View.OnClickListener { onBackPressed() })

        setData()

        getDataWarehouse()

        // TODO set ime click Search
        binding.etSearch.setOnEditorActionListener() { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                str_keyword_search = binding.etSearch.getText().toString()

                isNotLoad = false

                hideKeyboardwithoutPopulate(this@StockMasukListActivity)

                binding.rvList.visibility = View.GONE
                listDataBarang.clear()
                offset = 0

                getDataListBarang(binding.lLoading, true)

            }
            false
        }


    }

    private fun getDataWarehouse() {
        binding.pbLoading.visibility = View.VISIBLE
        val service = ApiClient.getClient()

        val idUser = DBS.with(this).idUser.toInt()

        val call = service.warehouseList(idUser)

        See.logE(Cons.CALLRESPONSE, "" + call.request())

        call.enqueue(object : Callback<ResponseBody> {
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    try {
                        val respon = response.body()!!.string()
                        val json = JSONObject(respon)

                        See.logE("respon_warehouse", respon)

                        val api_status = json.getInt(Cons.API_STATUS)
                        val api_message = json.getString(Cons.API_MESSAGE)

                        if (api_status == Cons.INT_STATUS) {

                            val jsonDataArray = json.getJSONArray(Cons.ITEMS_DATA)

                            val listData = Gson().fromJson(jsonDataArray.toString(), Array<WarehouseList>::class.java).toList()

                            if (listData.isEmpty()) {

                                setVisibleEmptyData()
                                binding.pbLoading.visibility = View.GONE

                            } else {
                                listWarehouse.clear()

                                listWarehouse.addAll(listData)

                                selected_id_warehouse = listWarehouse[0].idWarehouse.toInt()

                                spinnerAdapter = AdapterSpinnerStatus(this@StockMasukListActivity, listWarehouse)
                                spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                                binding.spGudang.setAdapter(spinnerAdapter)

                                binding.spGudang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {

                                        selected_id_warehouse = listWarehouse.get(i).idWarehouse.toInt()

                                        listDataBarang.clear()
                                        offset = 0

                                        getDataListBarang(binding.lLoading, false)

                                    }

                                    override fun onNothingSelected(adapterView: AdapterView<*>) {

                                    }
                                }

                                getDataListBarang(binding.lLoading, false)

                            }

                        } else {
                            See.toast(this@StockMasukListActivity, api_message)
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                } else {
                    binding.pbLoading.visibility = View.GONE
                    dialogFailure("warehouse", resources.getString(R.string.label_failure_content_server_title), resources.getString(R.string.label_failure_content_server_content))
                    //See.toast(this@StockMasukListActivity, resources.getString(R.string.label_something_wrong))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
               binding.pbLoading.visibility = View.GONE
                dialogFailure("warehouse", resources.getString(R.string.label_failure_content1), resources.getString(R.string.label_failure_content2))
            }
        })

    }

    private fun getDataListBarang(pbLoading: LinearLayout, isSearch: Boolean) {
        pbLoading.visibility = View.VISIBLE
        val service = ApiClient.getClient()

        val call = service.stockBarangMasukList(selected_id_warehouse, str_keyword_search)

        See.logE(Cons.CALLRESPONSE, "" + call.request())

        call.enqueue(object : Callback<ResponseBody> {
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                pbLoading.visibility = View.GONE
                binding.rvList.visibility = View.VISIBLE
                if (response.isSuccessful) {
                    try {
                        val respon = response.body()!!.string()
                        val json = JSONObject(respon)

                        See.logE("respon_asset_barang", respon)

                        val api_status = json.getInt(Cons.API_STATUS)
                        val api_message = json.getString(Cons.API_MESSAGE)

                        if (api_status == Cons.INT_STATUS) {

                            val jsonDataArray = json.getJSONArray(Cons.ITEMS_DATA)

                            val listData = Gson().fromJson(jsonDataArray.toString(), Array<StockListBarangMasuk>::class.java).toList()

                            listDataBarang.addAll(listData)

                            stockMasukListAdapter.notifyDataSetChanged()

                            if (!isSearch) {
                                if (listData.isEmpty()) {

                                    isNotLoad = true

                                }
                            }

                            if (!isAnim) {

                                setAnimHeader()

                            }

                            if (offset == 0) {

                                if (listData.isEmpty()) {
                                    setVisibleEmptyData()

                                    return
                                } else {
                                    setVisibleParent()
                                    return
                                }

                            }


                        } else {
                            pbLoading.visibility = View.GONE
                            See.toast(this@StockMasukListActivity, api_message)
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                } else {

                    pbLoading.visibility = View.GONE
                    dialogFailure("list", resources.getString(R.string.label_failure_content_server_title), resources.getString(R.string.label_failure_content_server_content))

                    //           See.toast(this@StockMasukListActivity, resources.getString(R.string.label_something_wrong))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                pbLoading.visibility = View.GONE
                dialogFailure("list", resources.getString(R.string.label_failure_content1), resources.getString(R.string.label_failure_content2))
            }
        })

    }


    fun setData() {
        stockMasukListAdapter = StockMasukListAdapter(this, listDataBarang, this)
        binding.rvList.setAdapter(stockMasukListAdapter)
        binding.rvList.isNestedScrollingEnabled = false
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun setVisibleParent() {
        setAnimViewVisible(binding.lParentContent, binding.rvList, 0)
        setAnimViewGone(binding.lParentContent, binding.itemEmptyData.lParentEmptyData, 0)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun setVisibleEmptyData() {
        binding.rvList.visibility = View.GONE
        setAnimViewVisible(binding.lParentContent, binding.itemEmptyData.lParentEmptyData, 0)
    }


    fun setAnimHeader() {

        isAnim = true

        try {
            Handler().postDelayed({

                TransitionManager.beginDelayedTransition(binding.lParentContent)
                binding.appSpinner.visibility = View.VISIBLE

            }, 700)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.appSpinner.visibility = View.VISIBLE
        }


    }

    fun dialogFailure(type: String, title: String, subTitle: String) {
        try {

            var dialog = Dialog(this, R.style.DialogLight)
            dialog.window?.attributes?.windowAnimations = R.style.PauseDialogAnimation
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val dialogBinding = DialogFailureCustomBinding.inflate(LayoutInflater.from(this))
            dialog.setContentView(dialogBinding.root)
            dialog.setCancelable(false)

            dialogBinding.tvContent.text = title
            dialogBinding.tvContent2.text = subTitle

            dialogBinding.btnBack.setOnClickListener(View.OnClickListener {
                dialog.dismiss()
                onBackPressed()
            })

            dialogBinding.btnRefresh.setOnClickListener(View.OnClickListener {
                dialog.dismiss()
                if (type == "list") {

                    getDataListBarang(binding.lLoading, false)
                } else {
                    getDataWarehouse()
                }

            })

            val size = Point()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            display.getSize(size)
            val mWidth = size.x

            val window = dialog.window
            val wlp = window!!.attributes

            wlp.gravity = Gravity.CENTER
            wlp.x = 0
            wlp.y = 0
            wlp.width = mWidth
            window.attributes = wlp
            dialog.show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class AdapterSpinnerStatus(internal var context: Context, internal var stringList: ArrayList<WarehouseList>) : ArrayAdapter<WarehouseList>(context, R.layout.spinner_item, stringList) {

        internal var inflater: LayoutInflater? = null
        internal var resource: Int = 0
        internal var searchText = ""
        internal var selectedPosisi = 0

        init {
            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }


        fun setFilter(dataSpinnerExhibitors: List<WarehouseList>, search: String) {
            stringList = java.util.ArrayList()
            stringList.addAll(dataSpinnerExhibitors)
            this.searchText = search
            notifyDataSetChanged()
        }


        fun setSelected(posisi: Int) {
            selectedPosisi = posisi
            notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            var vi = convertView
            val holder: ViewHolder

            if (convertView == null)
                vi = inflater!!.inflate(R.layout.spinner_item, null)
            holder = ViewHolder(vi!!)
            vi.tag = holder

            val item = stringList[position]

            holder.text1.text = item.warehouseName


            if (selectedPosisi == position) {
                //                holder.text1.setTextColor(context.getResources().getColor(R.color.color_text_blue));
                holder.text1.setTextColor(context.resources.getColor(R.color.color_tv_blue_btn_login))
            } else {
                holder.text1.setTextColor(context.resources.getColor(R.color.color_tv_blue_btn_login))
            }


            return vi
        }

        private inner class ViewHolder internal constructor(view: View) {
            internal var text1: TextView

            init {
                text1 = view.findViewById(R.id.text1)

            }
        }
    }

    fun dialogAddStock() {

        try {

            val pDialog = Dialog(this, R.style.DialogLight)
            pDialog.window?.attributes?.windowAnimations = R.style.PauseDialogAnimation
            pDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val binding = ItemDialogTambahStockOpnameBinding.inflate(LayoutInflater.from(this))
            pDialog.setContentView(binding.root)
            pDialog.setCancelable(true)

            binding.btnTambah.setOnClickListener(View.OnClickListener {

                val str_stock = binding.etStock.text.toString()
                if (str_stock == "") {
                    binding.etStock.requestFocus()
                    binding.etStock.error = resources.getString(R.string.label_form_wajib_diisi)

                } else {
                    pDialog.dismiss()

                }
            })

            val size = Point()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            display.getSize(size)
            val mWidth = size.x

            val window = pDialog!!.window
            val wlp = window!!.attributes as WindowManager.LayoutParams

            wlp.gravity = Gravity.CENTER
            wlp.x = 0
            wlp.y = 0
            wlp.width = mWidth
            window.attributes = wlp
            pDialog!!.show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        //   super.onBackPressed()
        val intent = Intent(this@StockMasukListActivity, ScannerActivity::class.java)
        startActivity(intent)
        finish()
        setOveridePendingTransisi(this@StockMasukListActivity)
    }
}