package com.veracity.protocol.sample.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.veracity.protocol.sample.R
import com.veracity.protocol.sample.model.FilesModel
import com.veracity.sdk.auth.Authenticate
import com.veracity.sdk.utils.AppReq
import kotlinx.android.synthetic.main.fragment_authenticate.*

class AuthenticateFragment : Fragment(), Authenticate.LoginListener {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_authenticate, container, false)
    }

    private lateinit var authenticate: Authenticate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authenticate = Authenticate(requireActivity())

        btn_start.setOnClickListener {

            if(prg.visibility==View.VISIBLE) return@setOnClickListener

            //request permissions
            Dexter.withActivity(requireActivity())
                .withPermissions(android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {

                            logIn()
                            //clean up files at each startup
                            FilesModel.deleteAllFiles(requireContext())
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
        }
    }

    private fun logIn(){
        if(!AppReq.check(requireActivity())) onLogInFailure("This device does not meet camera requirements.")
        else {
            prg.visibility=View.VISIBLE

            authenticate.logIn("domi.nik@veracityprotocol.org","dominik",this)
        }
    }

    override fun onLogInSuccess() {
        findNavController().navigate(R.id.action_AuthenticateFragment_to_CameraFragment)
    }

    override fun onLogInFailure(error: String) {
        prg.visibility=View.INVISIBLE
        showErrorMsg(resources.getString(R.string.connection_offline))
    }

    private fun showErrorMsg(msg:String){
        prg.visibility=View.INVISIBLE
        Toast.makeText(requireContext(),msg,Toast.LENGTH_LONG).show()
    }
}