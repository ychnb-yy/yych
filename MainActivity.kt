package com.example.rootenvinspector

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val bg = Color.rgb(11,13,18)
    private val panel = Color.rgb(21,26,35)
    private val white = Color.rgb(244,246,249)
    private val muted = Color.rgb(160,172,190)
    private val blue = Color.rgb(61,121,232)
    private val green = Color.rgb(119,229,178)
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var selected: TextView
    private lateinit var logs: TextView
    private lateinit var execute: Button
    private var zipFile: File? = null
    private var rootOk = false
    private val pickZip = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        checkRoot()
    }

    private fun buildUi() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(20))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this); scroll.addView(content); setContentView(scroll)
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(label("ych独家的一键root",22,white,true))
        titleBox.addView(label("ROOT 环境检测工具",12,muted))
        header.addView(titleBox, LinearLayout.LayoutParams(0,-2,1f))
        header.addView(Button(this).apply { text="⚙ 设置"; setOnClickListener { settings() } })
        content.addView(header); content.addView(space(14))

        val sc = card()
        status = label("正在检测 ROOT 授权…",16,green,true)
        sc.addView(status)
        sc.addView(space(6))
        sc.addView(label("需要设备已安装 Magisk，并允许本应用使用 su。",13,muted))
        content.addView(sc); content.addView(space(16))

        content.addView(label("设备信息",17,white,true)); content.addView(space(8))
        val ic = card()
        info = label("",14,white)
        ic.addView(info); content.addView(ic)
        info.text = "机型：${Build.MANUFACTURER} ${Build.MODEL}\nAndroid 版本：${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n芯片/ABI：${Build.SUPPORTED_ABIS.firstOrNull() ?: "未知"}\n内核版本：${System.getProperty("os.version") ?: "未知"}\n应用版本：1.0"
        content.addView(space(16))

        content.addView(label("Magisk 模块",17,white,true)); content.addView(space(8))
        val fc = card()
        selected = label("尚未选择 ZIP 文件",13,muted); fc.addView(selected); fc.addView(space(8))
        fc.addView(Button(this).apply { text="选择 Magisk 模块 ZIP"; setOnClickListener { pickModule() } })
        content.addView(fc); content.addView(space(12))

        execute = Button(this).apply {
            text="执行 / 安装模块"; textSize=16f; setTextColor(Color.WHITE); setBackgroundColor(blue)
            isEnabled=false; setOnClickListener { confirmInstall() }
        }
        content.addView(execute, LinearLayout.LayoutParams(-1,dp(52)))
        content.addView(space(16)); content.addView(label("执行日志",17,white,true)); content.addView(space(8))
        val lc=card(); logs=label("等待操作。",12,muted)
        logs.typeface=android.graphics.Typeface.MONOSPACE; lc.addView(logs); content.addView(lc)
    }

    private fun checkRoot() {
        status.text="正在请求 ROOT 授权…"
        thread {
            val (code,out)=runSu("id")
            val ok=code==0 && out.contains("uid=0")
            runOnUiThread {
                rootOk=ok
                if(ok) {
                    status.text="✓ 已获得 ROOT 授权"; status.setTextColor(green)
                    execute.isEnabled=zipFile!=null; log("ROOT 检测成功。")
                } else {
                    status.text="未检测到 ROOT 环境，请获取 ROOT 权限后再试"
                    status.setTextColor(Color.rgb(255,115,115))
                    execute.isEnabled=false
                    log("ROOT 检测失败：请确认 Magisk 已安装并允许 su 授权。")
                    AlertDialog.Builder(this).setTitle("未检测到 ROOT 环境")
                        .setMessage("请先获取 ROOT 权限，并在 Magisk 授权弹窗中允许本应用。")
                        .setPositiveButton("知道了",null).show()
                }
            }
        }
    }

    private fun pickModule() {
        val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type="application/zip"
            putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/zip","application/x-zip-compressed","application/octet-stream"))
        }
        startActivityForResult(i,pickZip)
    }

    @Deprecated("Uses activity result API for broad Android compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=pickZip || resultCode!=RESULT_OK) return
        val uri=data?.data ?: return
        thread {
            try {
                val dest=File(cacheDir,"selected-magisk-module.zip")
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "无法读取所选文件" }
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                ZipFile(dest).use { z ->
                    val entries=z.entries(); var found=false; var n=0
                    while(entries.hasMoreElements()) {
                        val e=entries.nextElement(); n++
                        if(e.name=="module.prop" || e.name.endsWith("/module.prop")) found=true
                        require(n<=20000) { "ZIP 文件条目过多" }
                    }
                    require(found) { "未找到 module.prop，不像是标准 Magisk 模块 ZIP" }
                }
                zipFile=dest
                runOnUiThread {
                    selected.text="已选择：${uri.lastPathSegment ?: "模块 ZIP"}\n大小：${dest.length()/1024} KB\n已找到 module.prop"
                    selected.setTextColor(white); execute.isEnabled=rootOk
                    log("ZIP 基本结构校验通过。请确认文件来源可信。")
                }
            } catch(e:Exception) {
                zipFile=null
                runOnUiThread {
                    selected.text="文件校验失败：${e.message}"; selected.setTextColor(Color.RED)
                    execute.isEnabled=false; log("导入失败：${e.message}")
                }
            }
        }
    }

    private fun confirmInstall() {
        val file=zipFile ?: return
        if(!rootOk) { Toast.makeText(this,"尚未获得 ROOT 授权",Toast.LENGTH_LONG).show(); return }
        AlertDialog.Builder(this).setTitle("确认安装 Magisk 模块？")
            .setMessage("将通过 su 调用 Magisk 安装所选 ZIP。只安装你信任的模块；模块可能修改系统或导致设备异常。安装后通常需要重启。")
            .setNegativeButton("取消",null)
            .setPositiveButton("确认安装") { _,_ -> install(file) }.show()
    }

    private fun install(file:File) {
        execute.isEnabled=false; log("开始调用 Magisk 安装命令…")
        thread {
            val escaped=file.absolutePath.replace("'","'\\''")
            val (code,out)=runSu("magisk --install-module '$escaped'")
            runOnUiThread {
                log("退出码：$code\n${out.ifBlank { "(没有返回文本)" }}")
                if(code==0) {
                    log("安装命令已结束。请在 Magisk 中确认模块状态，并按提示重启。")
                    AlertDialog.Builder(this).setTitle("安装命令已结束")
                        .setMessage("退出码为 0。请确认 Magisk 中的模块状态；需要时重启设备。")
                        .setPositiveButton("确定",null).show()
                } else {
                    AlertDialog.Builder(this).setTitle("安装失败")
                        .setMessage("Magisk 返回非零退出码，请查看执行日志。")
                        .setPositiveButton("确定",null).show()
                }
                execute.isEnabled=rootOk && zipFile!=null
            }
        }
    }

    private fun runSu(command:String):Pair<Int,String> = try {
        val p=ProcessBuilder("su","-c",command).redirectErrorStream(true).start()
        val out=p.inputStream.bufferedReader().use { it.readText() }
        Pair(p.waitFor(),out.take(12000))
    } catch(e:Exception) { Pair(127,"${e.javaClass.simpleName}: ${e.message}") }

    private fun settings() {
        AlertDialog.Builder(this).setTitle("设置")
            .setItems(arrayOf("开发者模式","重新检测 ROOT","关于")) { _,which ->
                when(which) {
                    0 -> passwordDialog()
                    1 -> checkRoot()
                    2 -> AlertDialog.Builder(this).setTitle("关于")
                        .setMessage("ych独家的一键root 1.0\n设备信息查看与 Magisk 模块导入/安装。")
                        .setPositiveButton("确定",null).show()
                }
            }.show()
    }

    private fun passwordDialog() {
        val input=EditText(this).apply {
            hint="输入开发者密码"
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this).setTitle("开发者模式").setMessage("请输入密码").setView(input)
            .setNegativeButton("取消",null).setPositiveButton("进入") { _,_ ->
                if(input.text.toString()=="ychnb666") {
                    AlertDialog.Builder(this).setTitle("开发者模式")
                        .setMessage("已开启。可选择模块 ZIP、检查 module.prop，并在确认后安装。\n\n注意：客户端硬编码密码可被逆向查看，仅用于界面入口。")
                        .setPositiveButton("选择模块 ZIP") { _,_ -> pickModule() }
                        .setNegativeButton("关闭",null).show()
                } else Toast.makeText(this,"密码错误",Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun log(s:String) {
        if(!::logs.isInitialized) return
        logs.text=(logs.text.toString()+"\n\n> "+s).takeLast(14000)
    }
    private fun card()=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(14),dp(14),dp(14)); setBackgroundColor(panel)
    }
    private fun label(s:String,size:Int,color:Int,bold:Boolean=false)=TextView(this).apply {
        text=s; textSize=size.toFloat(); setTextColor(color)
        if(bold) setTypeface(typeface,android.graphics.Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(),1f)
    }
    private fun space(h:Int)=View(this).apply { layoutParams=LinearLayout.LayoutParams(1,dp(h)) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
