package com.veracity.protocol.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.veracity.protocol.sample.ui.ProtectFragment
import com.veracity.protocol.sample.ui.VerifyFragment
import io.github.inflationx.viewpump.ViewPumpContextWrapper


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // pass ActivityResult to current fragment
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navHost?.let { navFragment ->
            navFragment.childFragmentManager.primaryNavigationFragment?.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navHost?.let { navFragment ->
            //disable back press for ProtectFragment & VerifyFragment
            navFragment.childFragmentManager.primaryNavigationFragment?.let {fragment->
                if(fragment is ProtectFragment || fragment is VerifyFragment) return
            }
        }
        super.onBackPressed()
    }

}