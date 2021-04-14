package com.veracity.protocol.sample.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.veracity.protocol.sample.R
import com.veracity.protocol.sample.model.FilesModel
import com.veracity.sdk.VeracitySdk
import com.veracity.sdk.api.add.ProtectAdd
import com.veracity.sdk.api.get.ProtectGet
import com.veracity.sdk.detail.DetailActivity
import com.veracity.sdk.detail.DetailConfig
import com.veracity.sdk.event.ProtectEvent
import kotlinx.android.synthetic.main.fragment_protect_verify.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import java.io.File


class ProtectFragment : Fragment(),ProtectEvent.EventListener {

    private val itemName = "Sample no.22"
    //optional metadata example
    private val itemLocation = "NJ 07013, United States"
    private val itemAge = "32 years"

    private lateinit var protectEvent: ProtectEvent

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_protect_verify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar_info.visibility = View.GONE
        toolbar_back.visibility = View.INVISIBLE
        toolbar_close.setOnClickListener {
            requireActivity().finish()
        }

        protectEvent = ProtectEvent(requireContext(),this)
        protectEvent.registerReceiver()
        protectEvent.waitForAnalyzingFinished()

        item_image_full.setImageBitmap(BitmapFactory.decodeFile(FilesModel.getOverviewCropFile(requireContext()).path))
        //todo add your own item name and optional metadata
        item_name.text = itemName
        param_text_primary.text = resources.getString(R.string.item_detail_location)
        value_text_primary.text = itemLocation
        param_text_secondary.text = resources.getString(R.string.item_detail_age)
        value_text_secondary.text = itemAge

        takeFingerprints()
    }

    private fun takeFingerprints(){

        //clean uploader from previous verifications/protections
        VeracitySdk(requireContext()).cleanUpload()

        btn_start_over.visibility=View.GONE
        btn_verify.visibility=View.GONE

        bar_img.setImageResource(R.drawable.ic_upload)
        bar_lay.setBackgroundColor(ResourcesCompat.getColor(requireActivity().resources, R.color.light_gray, null))
        bar_text_primary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.colorText, null))
        bar_text_secondary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.colorText, null))
        bar_text_primary.text = resources.getString(R.string.item_detail_title_pending)
        bar_text_secondary.text = resources.getString(R.string.item_detail_desc_pending)

        /*
        Documentation link:
        https://veracityprotocol.gitbook.io/api/veracity-sdk/protection-guide#3-obtaining-fingerprint-images-and-fingerprint-location
        */
        val detailConfigProtect = DetailConfig(captureType = DetailConfig.typeProtect,
            folder = FilesModel.getRootDirectory(requireContext()),
            jpegPath = FilesModel.getOverviewCropFile(requireContext()).path,
            overviewWidth = ProtectFragmentArgs.fromBundle(requireArguments()).ItemWidth.toInt(),
            overviewHeight = ProtectFragmentArgs.fromBundle(requireArguments()).ItemHeight.toInt())

        DetailActivity.launch(detailConfigProtect,requireActivity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if(resultCode==Activity.RESULT_OK && data!=null){

                val json = JSONObject(data.getStringExtra("result"))
                val location: String = json.optString("location")
                val fp1 = File(json.optString("image1"))
                val fp2 = File(json.optString("image2"))

                startProtection(location,fp1,fp2)
            }
            else toFailedToProtect(resources.getString(R.string.item_detail_desc_pending))
        }
    }

    private fun startProtection(location:String, fp1:File, fp2:File){

        bar_text_primary.text = resources.getString(R.string.item_detail_title_pending)
        bar_text_secondary.text = resources.getString(R.string.item_detail_desc_pending_connection)

        /*
        Documentation link:
        https://veracityprotocol.gitbook.io/api/veracity-sdk/protection-guide#4-upload-item-for-protection
        */
        val protectAdd = ProtectAdd.Builder()
            //you can create custom name for protection item
            .setName(itemName)
             //you can attach you own metadata to the protection item
            .setMeta("{ \"location\":\"$itemLocation\", \"age\":\"$itemAge\" }")
             // get dimensions from arguments
            .setWidth(ProtectFragmentArgs.fromBundle(requireArguments()).ItemWidth)
            .setHeight(ProtectFragmentArgs.fromBundle(requireArguments()).ItemHeight)
             //set overview & thumbnail from files
            .setThumbnail(FilesModel.getThumbnailFile(requireContext()))
            .setOverview(FilesModel.getOverviewCropFile(requireContext()))
            // set fingerprints and location from DetailActivity result
            .setFingerprint1(fp1)
            .setFingerprint2(fp2)
            .setFingerprintLocation(location)
            .create()

        VeracitySdk(requireContext()).upload(protectAdd)
    }

    /*
    Documentation link:
    https://veracityprotocol.gitbook.io/api/veracity-sdk/protection-guide#5-receive-protection-result-and-listen-to-item-upload-changes
    */
    override fun onProtectUploadingProgress(progress: Int, uploadSpeed: String, protectAdd: ProtectAdd) {

        bar_text_primary.text = resources.getString(R.string.verify_detail_text_uploading).plus(" ").plus(progress).plus(" %")
        bar_text_secondary.text = resources.getString(R.string.verify_detail_text_uploading_secondary)
        if(progress==100) toAnalyzing()
    }

    private fun toAnalyzing(){

        bar_img.setImageResource(R.drawable.ic_wave_plain)
        bar_lay.setBackgroundColor(ResourcesCompat.getColor(requireActivity().resources, R.color.transparent, null))
        bar_text_primary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.colorTextAnalyzing, null))
        bar_text_primary.text = resources.getString(R.string.verify_detail_text_analyzing)
        bar_text_secondary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.colorTextAnalyzing, null))
        bar_text_secondary.text = resources.getString(R.string.verify_detail_text_analyzing_secondary)

        anim(gradient_upload)
    }

    private var isAnimating=false
    private fun anim(view: View) {

        if(isAnimating) return
        isAnimating=true

        val point = Point()
        requireActivity().windowManager.defaultDisplay.getSize(point)
        val width = point.x.toFloat()

        view.visibility = View.VISIBLE
        val animation = TranslateAnimation(-width, width, 0f, 0f) // new TranslateAnimation(xFrom,xTo, yFrom,yTo)
        animation.duration = 2000 //
        animation.repeatCount = TranslateAnimation.INFINITE
        view.startAnimation(animation)
    }

    override fun onProtectUploadingFailed(failReason:String,protectAdd: ProtectAdd){
        bar_text_primary.text = resources.getString(R.string.item_detail_title_pending)
        bar_text_secondary.text = resources.getString(R.string.item_detail_desc_pending_connection)
    }

    override fun onProtectAnalyzingFinished(protectGet: ProtectGet) {
        /*
        //example of protected item
        {"algorithmUsed":"celery","author":{"firstName":"Veracity","lastName":"Protocol"},"authorized_at":"2021-04-07T13:11:07.189Z","createdAt":"2021-04-07T13:10:46.960Z","error":"","firstCreatedByRole":"trader","height":21.0,"id":"606daf5653bd940004a0917d","meta":{"name":"John","age":30,"upload_time":"18"},"name":"item","overviewUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/overview_crop.jpg","publicId":"3315WBL22","status":"authorized","thumbnailUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/thumbnail.jpg","timestampId":0,"width":15.0,"year":2020}
        //example of failed protection
        {"algorithmUsed":"celery","author":{"firstName":"Veracity","lastName":"Protocol"},"createdAt":"2021-04-07T13:15:09.307Z","error":"You took a picture of a different spot than suggested.","firstCreatedByRole":"trader","height":21.0,"id":"606db05d53bd940004a0924a","meta":{"name":"John","age":30,"upload_time":"17"},"name":"item","overviewUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/overview_crop.jpg","publicId":"C9SZAURXU","status":"authorization_failed","thumbnailUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/thumbnail.jpg","timestampId":0,"width":15.0,"year":2020}
         */
        if(protectGet.status=="authorized") toProtected(protectGet) else toFailedToProtect(protectGet.error)
    }

    private fun toProtected(protectGet: ProtectGet){

        status_layout.visibility=View.GONE
        protected_code_lay.visibility=View.VISIBLE
        btn_verify.visibility=View.VISIBLE
        btn_verify.text= resources.getText(R.string.item_detail_verify_item)
        btn_verify.setOnClickListener {
            //protectGet.publicId is required for future verifications
            val action = ProtectFragmentDirections.actionProtectFragmentToVerifyFragment(protectGet.publicId)
            findNavController().navigate(action)
        }
    }

    private fun toFailedToProtect(error: String){

        bar_lay.setBackgroundColor(ResourcesCompat.getColor(requireActivity().resources, R.color.light_red_bg, null))
        bar_img.setImageResource(R.drawable.ic_not_verified)
        bar_text_primary.text=resources.getString(R.string.item_detail_failed)
        bar_text_primary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.alert_red, null))
        bar_text_secondary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.alert_red, null))
        bar_text_secondary.text = error

        btn_verify.visibility=View.VISIBLE
        btn_verify.text= resources.getText(R.string.item_detail_btn_fp_again)
        btn_verify.setOnClickListener {
            takeFingerprints()
        }
        btn_start_over.visibility=View.VISIBLE
        btn_start_over.paintFlags = btn_start_over.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        btn_start_over.setOnClickListener {
            findNavController().navigate(ProtectFragmentDirections.actionProtectFragmentToCameraFragment())
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        protectEvent.unregisterReceiver()
    }
}