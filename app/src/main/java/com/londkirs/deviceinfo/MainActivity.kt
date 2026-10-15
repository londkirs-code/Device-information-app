package com.londkirs.deviceinfo

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var search: EditText
    private var allApps: List<ApplicationInfo> = emptyList()
    private var showingDeviceInfo = true
    private val pm: PackageManager get() = packageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderShell()
        showDeviceInfo()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun renderShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4F6FA.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Device Information"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(0xFF183B86.toInt())
        })

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        nav.addView(Button(this).apply {
            text = "Device"
            setOnClickListener {
                showingDeviceInfo = true
                search.visibility = View.GONE
                showDeviceInfo()
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(Button(this).apply {
            text = "Apps"
            setOnClickListener {
                showingDeviceInfo = false
                search.visibility = View.VISIBLE
                loadApps()
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(Button(this).apply {
            text = "Refresh"
            setOnClickListener { if (showingDeviceInfo) showDeviceInfo() else loadApps() }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(nav)

        search = EditText(this).apply {
            hint = "Search installed apps"
            setSingleLine(true)
            visibility = View.GONE
            setPadding(dp(16), dp(4), dp(16), dp(4))
            addTextChangedListener(SimpleTextWatcher { filterApps(it) })
        }
        root.addView(search)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun section(title: String) {
        content.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF183B86.toInt())
            setPadding(0, dp(14), 0, dp(6))
        })
    }

    private fun info(label: String, value: String?) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        card.addView(TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(0xFF606A7B.toInt())
        })
        card.addView(TextView(this).apply {
            text = value?.takeIf { it.isNotBlank() } ?: "Not available"
            textSize = 15f
            setTextColor(0xFF172033.toInt())
            setPadding(0, dp(3), 0, 0)
        })
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(6)
        content.addView(card, lp)
    }

    private fun showDeviceInfo() {
        showingDeviceInfo = true
        content.removeAllViews()
        section("Device")
        info("Manufacturer", Build.MANUFACTURER)
        info("Brand", Build.BRAND)
        info("Model", Build.MODEL)
        info("Device codename", Build.DEVICE)
        info("Product", Build.PRODUCT)
        info("Hardware", Build.HARDWARE)
        section("Android")
        info("Android version", Build.VERSION.RELEASE)
        info("API level", Build.VERSION.SDK_INT.toString())
        info("Security patch", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else null)
        info("Build ID", Build.DISPLAY)
        info("Build type", Build.TYPE)
        info("Build fingerprint", Build.FINGERPRINT)
        info("Supported ABIs", Build.SUPPORTED_ABIS.joinToString())
        section("Application")
        info("App version", BuildConfig.VERSION_NAME)
        info("Package name", packageName)
        info("First installed", runCatching {
            DateFormat.getDateTimeInstance().format(Date(packageManager.getPackageInfo(packageName, 0).firstInstallTime))
        }.getOrNull())
    }

    @Suppress("DEPRECATION")
    private fun installedApplications(): List<ApplicationInfo> {
        return try {
            val apps = if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                pm.getInstalledApplications(0)
            }
            apps.sortedBy { pm.getApplicationLabel(it).toString().lowercase(Locale.getDefault()) }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not read installed apps: ${e.message}", Toast.LENGTH_LONG).show()
            emptyList()
        }
    }

    private fun loadApps() {
        showingDeviceInfo = false
        search.visibility = View.VISIBLE
        allApps = installedApplications()
        filterApps(search.text?.toString().orEmpty())
        if (allApps.isEmpty()) {
            Toast.makeText(this, "No apps returned. Check package visibility settings.", Toast.LENGTH_LONG).show()
        }
    }

    private fun filterApps(query: String) {
        if (!::content.isInitialized || showingDeviceInfo) return
        content.removeAllViews()
        val q = query.trim().lowercase(Locale.getDefault())
        val filtered = allApps.filter {
            pm.getApplicationLabel(it).toString().lowercase(Locale.getDefault()).contains(q) ||
                it.packageName.lowercase(Locale.getDefault()).contains(q)
        }
        section("Installed apps (${filtered.size})")
        filtered.forEach { app ->
            val name = pm.getApplicationLabel(app).toString()
            val system = app.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(0xFFFFFFFF.toInt())
                isClickable = true
                isFocusable = true
                setOnClickListener { showAppDetails(app) }
            }
            card.addView(TextView(this).apply {
                text = name
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF172033.toInt())
            })
            card.addView(TextView(this).apply {
                text = app.packageName
                textSize = 12f
                setTextColor(0xFF606A7B.toInt())
                setPadding(0, dp(3), 0, dp(3))
            })
            card.addView(TextView(this).apply {
                text = if (system) "System app" else "User-installed app"
                textSize = 12f
                setTextColor(0xFF2457C5.toInt())
            })
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(7)
            content.addView(card, lp)
        }
        if (filtered.isEmpty()) info("Results", "No matching apps")
    }

    @Suppress("DEPRECATION")
    private fun showAppDetails(app: ApplicationInfo) {
        val packageInfo: PackageInfo? = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            }
        } catch (_: Exception) { null }

        val permissions = packageInfo?.requestedPermissions?.toList().orEmpty()
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        fun addLine(line: String) {
            details.addView(TextView(this@MainActivity).apply {
                text = line
                textSize = 14f
                setTextColor(0xFF172033.toInt())
                setPadding(0, dp(5), 0, dp(5))
            })
        }
        addLine("Package: ${app.packageName}")
        addLine("Version: ${packageInfo?.versionName ?: "Unknown"}")
        addLine("Category: " + if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) "System app" else "User app")
        addLine("Requested permissions (${permissions.size}):")
        if (permissions.isEmpty()) addLine("No requested permissions reported.")
        permissions.forEach { addLine("• $it") }

        val scroll = ScrollView(this)
        scroll.addView(details)
        android.app.AlertDialog.Builder(this)
            .setTitle(pm.getApplicationLabel(app).toString())
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show()
    }
}

private class SimpleTextWatcher(private val onText: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onText(s?.toString().orEmpty())
    }
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
