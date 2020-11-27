package com.llw.translate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.angmarch.views.OnSpinnerItemSelectedListener
import java.io.IOException
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    var fromLanguage = "auto" //目标语言
    var toLanguage = "auto" //翻译语言

    var myClipboard: ClipboardManager? = null // 复制文本

    val appId = "20201125000625305" //APP ID 来源于百度翻译平台 请使用自己的
    val key = "6vjmDnNxypmebgbzKxul" //秘钥 来源于百度翻译平台 请使用自己的

    //配置初始数据
    private val data: List<String> = LinkedList(
        listOf(
            "自动检测语言", "中文 → 英文", "英文 → 中文",
            "中文 → 繁体中文", "中文 → 粤语", "中文 → 日语",
            "中文 → 韩语", "中文 → 法语", "中文 → 俄语",
            "中文 → 阿拉伯语", "中文 → 西班牙语 ", "中文 → 意大利语"
        )
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //设置亮色状态栏模式 systemUiVisibility在Android11中弃用了，可以尝试一下。
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        //页面点击事件
        onClick()

        sp_language.attachDataSource(data)

        //输入框监听
        editTextListener()
        //下拉框监听
        spinnerListener()
        myClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    }

    /**
     * 语言类型选择
     */
    private fun spinnerListener() {
        sp_language.onSpinnerItemSelectedListener =
            OnSpinnerItemSelectedListener { _, _, position, _ ->
                when (position) {
                    0 -> {//自动监测
                        fromLanguage = "auto";toLanguage = fromLanguage
                    }
                    1 -> {//中文 → 英文
                        fromLanguage = "zh";toLanguage = "en"
                    }
                    2 -> {//英文 → 中文
                        fromLanguage = "en";toLanguage = "zh"
                    }
                    3 -> {//中文 → 繁体中文
                        fromLanguage = "zh";toLanguage = "cht"
                    }
                    4 -> {//中文 → 粤语
                        fromLanguage = "zh";toLanguage = "cht"
                    }
                    5 -> {//中文 → 日语
                        fromLanguage = "zh";toLanguage = "jp"
                    }
                    6 -> {//中文 → 韩语
                        fromLanguage = "zh";toLanguage = "kor"
                    }
                    7 -> {//中文 → 法语
                        fromLanguage = "zh";toLanguage = "fra"
                    }
                    8 -> {//中文 → 俄语
                        fromLanguage = "zh";toLanguage = "ru"
                    }
                    9 -> {//中文 → 阿拉伯语
                        fromLanguage = "zh";toLanguage = "ara"
                    }
                    10 -> {//中文 → 西班牙语
                        fromLanguage = "zh";toLanguage = "spa"
                    }
                    11 -> {//中文 → 意大利语
                        fromLanguage = "zh";toLanguage = "it"
                    }
                }
            }
    }

    /**
     * 输入框监听
     */
    private fun editTextListener() {
        ed_content.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                iv_clear_tx.visibility = View.VISIBLE
                val content = ed_content.text.toString().trim()
                if (content.isEmpty()) {
                    result_lay.visibility = View.GONE
                    tv_translation.visibility = View.VISIBLE
                    before_lay.visibility = View.VISIBLE
                    after_lay.visibility = View.GONE
                    iv_clear_tx.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
    }

    /**
     * 点击
     */
    private fun onClick() {
        iv_clear_tx.setOnClickListener {//清空输入框
            ed_content.text.clear()
        }

        iv_copy_tx.setOnClickListener {//复制文本
            val result = tv_result.text.toString()
            myClipboard!!.setPrimaryClip(ClipData.newPlainText("text", result))
            showMsg("已复制")
        }

        tv_translation.setOnClickListener { //翻译
            transition()
        }
    }

    /**
     * 翻译
     */
    private fun transition() {
        //获取输入的内容
        val inputTx = ed_content.text.toString().trim()
        //判断输入内容是否为空
        if (inputTx.isNotEmpty() || "" != inputTx) {//不为空
            tv_translation.text = "翻译中..."
            tv_translation.isEnabled = false //不可更改，无法点击
            val salt = num(1)//随机数
            //拼接一个字符串然后加密
            val spliceStr = appId + inputTx + salt + key
            //将拼接好的字符串进行小写的MD5加密
            val sign = toMD5(spliceStr)
            //异步Get请求访问网络
            asyncGet(inputTx, fromLanguage, toLanguage, salt, sign)
        } else {

        }
    }

    /**
     * 异步Get请求
     * @param content 要翻译的内容
     * @param fromType 翻译源语言
     * @param toType 翻译后语言
     * @param salt 随机数
     * @param sign 标识
     */
    private fun asyncGet(
        content: String,
        fromType: String,
        toType: String,
        salt: String,
        sign: String?
    ) {

        //通用翻译API HTTP地址：
        //http://api.fanyi.baidu.com/api/trans/vip/translate
        //通用翻译API HTTPS地址：
        //https://fanyi-api.baidu.com/api/trans/vip/translate
        val httpStr = "http://api.fanyi.baidu.com/api/trans/vip/translate"
        val httpsStr = "https://fanyi-api.baidu.com/api/trans/vip/translate"
        //拼接请求的地址  Kotlin运行在字符串中直接拼接
        val url = httpsStr +
                "?appid=$appId&q=$content&from=$fromType&to=$toType&salt=$salt&sign=$sign"
        val okHttpClient = OkHttpClient()
        val request = Request.Builder().url(url).get().build()
        val call: Call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //异常返回
                goToUIThread(e.toString(), 0)
            }

            override fun onResponse(call: Call, response: Response) {
                //正常返回
                goToUIThread(response.body()!!.string(), 1)
            }

        })
    }

    /**
     * 接收到返回值后，回到UI线程操作页面变化
     * @param any 接收任意对象
     * @param key 区别正常还是异常
     */
    private fun goToUIThread(any: Any, key: Int) {
        //切换到主线程处理数据
        runOnUiThread {
            tv_translation.text = "翻译"
            tv_translation.isEnabled = true

            if (key == 0) {//异常返回
                showMsg("异常信息：$any")
                Log.e("MainActivity", any.toString())
            } else {//正常返回
                //通过Gson 将 JSON字符串转为实体Bean
                val (from, to, trans_result) = Gson().fromJson<TranslateResult>(
                    any.toString(),
                    TranslateResult::class.java
                )
                tv_translation.visibility = View.GONE
                //显示翻译的结果
                tv_result.text = trans_result!![0].dst
                result_lay.visibility = View.VISIBLE
                before_lay.visibility = View.GONE
                after_lay.visibility = View.VISIBLE
                //翻译成功后的语言判断显示
                initAfter(from, to)
            }
        }
    }

    /**
     * 翻译成功后的语言判断显示
     * @param from 翻译目标语言
     * @param to 翻译后语言
     */
    private fun initAfter(from: String?, to: String?) {
        when (from) {
            "zh" -> tv_from.text = "中文"
            "en" -> tv_from.text = "英文"
            "yue" -> tv_from.text = "粤语"
            "cht" -> tv_from.text = "繁体中文"
            "jp" -> tv_from.text = "日语"
            "kor" -> tv_from.text = "韩语"
            "fra" -> tv_from.text = "法语"
            "ru" -> tv_from.text = "俄语"
            "ara" -> tv_from.text = "阿拉伯语"
            "spa" -> tv_from.text = "西班牙语"
            "it" -> tv_from.text = "意大利语"
        }
        when (to) {
            "zh" -> tv_to.text = "中文"
            "en" -> tv_to.text = "英文"
            "yue" -> tv_to.text = "粤语"
            "cht" -> tv_to.text = "繁体中文"
            "jp" -> tv_to.text = "日语"
            "kor" -> tv_to.text = "韩语"
            "fra" -> tv_to.text = "法语"
            "ru" -> tv_to.text = "俄语"
            "ara" -> tv_to.text = "阿拉伯语"
            "spa" -> tv_to.text = "西班牙语"
            "it" -> tv_to.text = "意大利语"
        }
    }

    /**
     * 随机数（根据百度的要求需要一个随机数）
     */
    private fun num(a: Int): String {
        val random = Random(a)
        var ran1 = 0
        for (i in 0..4) {
            ran1 = random.nextInt(100)
        }
        return ran1.toString()
    }

    /**
     * Toast提示
     * @param msg 提示内容
     */
    private fun showMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
