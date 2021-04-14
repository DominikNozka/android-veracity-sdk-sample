package com.veracity.protocol.sample.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.veracity.protocol.sample.R
import com.veracity.protocol.sample.model.FilesModel
import com.veracity.sdk.VeracitySdk
import com.veracity.sdk.api.add.VerifyAdd
import com.veracity.sdk.api.get.ItemInfo
import com.veracity.sdk.api.get.ProtectGet
import com.veracity.sdk.api.get.VerifyGet
import com.veracity.sdk.detail.DetailActivity
import com.veracity.sdk.detail.DetailConfig
import com.veracity.sdk.detail.DetailLocation
import com.veracity.sdk.event.SearchEvent
import com.veracity.sdk.event.VerifyEvent
import com.veracity.sdk.utils.logD
import kotlinx.android.synthetic.main.fragment_protect_verify.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import java.io.File

class VerifyFragment : Fragment(), VerifyEvent.EventListener,ItemInfo.Feedback {

    private lateinit var verifyEvent:VerifyEvent
    private var itemId=""

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

        verifyEvent = VerifyEvent(requireContext(),this)
        verifyEvent.registerReceiver()
        verifyEvent.waitForAnalyzingFinished()

        getVerifyInfo()
    }

    private fun getVerifyInfo(){

        //clean uploader from previous verifications/protections
        VeracitySdk(requireContext()).cleanUpload()

        //default UI setup
        item_layout.visibility=View.INVISIBLE
        prg.visibility=View.VISIBLE
        bar_img.setImageResource(R.drawable.ic_upload)
        bar_lay.setBackgroundColor(ResourcesCompat.getColor(requireActivity().resources, R.color.light_gray, null))
        bar_text_primary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.colorText, null))
        bar_text_secondary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.colorText, null))
        bar_text_primary.text = resources.getString(R.string.item_detail_info_title)
        bar_text_secondary.text = resources.getString(R.string.item_detail_info_desc)
        btn_verify.visibility=View.GONE
        btn_start_over.visibility=View.GONE


        /*
        Documentation:
        https://veracityprotocol.gitbook.io/api/veracity-sdk/verification-guide#1-retrieving-protected-item-info
        https://veracityprotocol.gitbook.io/api/veracity-sdk/verification-guide#2-retrieving-fingerprint-location
         */
        ItemInfo(requireContext(),this,
            thumbnail = FilesModel.getVerifyThumbnailFile(requireContext()),
            overview = FilesModel.getVerifyOverviewCropFile(requireContext()))
            .getVerifyInformation(VerifyFragmentArgs.fromBundle(requireArguments()).ItemPublicId)
    }

    override fun onInfoLoaded(protectGet: ProtectGet, thumbnail: File, overview: File) {

        item_layout.visibility=View.VISIBLE
        prg.visibility=View.INVISIBLE

        //display all the information about verified item
        item_name.text = protectGet.name
        item_image_full.setImageBitmap(BitmapFactory.decodeFile(overview.path))
        param_text_primary.text = resources.getString(R.string.item_detail_location)
        value_text_primary.text = protectGet.meta.get("location").asString
        param_text_secondary.text = resources.getString(R.string.item_detail_age)
        value_text_secondary.text = protectGet.meta.get("age").asString

        //save internal identifier for future verification
        itemId = protectGet.id

        /*
        Documentation:
        https://veracityprotocol.gitbook.io/api/veracity-sdk/verification-guide#3-getting-fingerprint-image
         */
        val detailLocation = DetailLocation(protectGet.fingerprint.location.x,
            protectGet.fingerprint.location.y,protectGet.fingerprint.location.width,
            protectGet.fingerprint.location.height)

        val detailConfigVerify = DetailConfig(captureType = DetailConfig.typeVerify,
            folder = FilesModel.getRootDirectory(requireContext()),
            jpegPath =FilesModel.getVerifyOverviewCropFile(requireContext()).path,
            overviewWidth = protectGet.width.toInt(),
            overviewHeight = protectGet.height.toInt(),
            detailLocation = detailLocation)

        DetailActivity.launch(detailConfigVerify,requireActivity())
    }

    override fun onInfoFail(errorMsg: String) {

        prg.visibility=View.VISIBLE
        bar_text_primary.text = resources.getString(R.string.item_detail_info_title_failed)
        bar_text_secondary.text = resources.getString(R.string.item_detail_info_desc_failed)
        btn_verify.visibility=View.VISIBLE
        btn_verify.text=resources.getString(R.string.item_detail_btn_try_again)
        btn_verify.setOnClickListener {
            getVerifyInfo()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if(resultCode==Activity.RESULT_OK && data!=null){
                val json = JSONObject(data.getStringExtra("result"))
                val fp1 = File(json.optString("image1"))
                startVerification(fp1)
            }
            else {
                bar_text_primary.text = resources.getString(R.string.item_detail_title_pending)
                bar_text_secondary.text = resources.getString(R.string.item_detail_desc_pending)
                btn_verify.visibility=View.VISIBLE
                btn_verify.text=resources.getString(R.string.item_detail_btn_fp_again)
                btn_verify.setOnClickListener {
                    getVerifyInfo()
                }
            }
        }
    }

    private fun startVerification(fp1 : File){

        /*
        Documentation:
        https://veracityprotocol.gitbook.io/api/veracity-sdk/verification-guide#4-upload-item-for-verification
         */

        val verifyAdd = VerifyAdd.Builder()
            .setThumbnailImage(FilesModel.getVerifyThumbnailFile(requireContext()).path)
            .setOverviewImage(FilesModel.getVerifyOverviewCropFile(requireContext()).path)
            .setFingerprint(fp1)
            .setArtworkId(itemId)
            .create()

        VeracitySdk(requireContext()).upload(verifyAdd)

        onVerifyStarted()
    }

    private fun onVerifyStarted(){

        btn_start_over.visibility=View.INVISIBLE
        bar_text_primary.text = resources.getString(R.string.item_detail_title_pending)
        bar_text_secondary.text = resources.getString(R.string.item_detail_desc_pending_connection)
    }

    /*
     Documentation:
     https://veracityprotocol.gitbook.io/api/veracity-sdk/verification-guide#5-receive-verification-result-and-listen-to-item-upload-changes
      */
    override fun onVerifyUploadingProgress(progress: Int, uploadSpeed: String, verifyAdd: VerifyAdd) {

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

    override fun onVerifyUploadingFailed(failReason: String, verifyAdd: VerifyAdd) {
        bar_text_primary.text = resources.getString(R.string.item_detail_title_pending)
        bar_text_secondary.text = resources.getString(R.string.item_detail_desc_pending_connection)
    }

    override fun onVerifyAnalyzingFinished(verifyGet: VerifyGet) {

        if(verifyGet.error==null || verifyGet.error=="") toVerified() else toFailedToVerify(verifyGet.error)
        /*
        //verified
         {"artwork":{"algorithmUsed":"celery","author":{"firstName":"Veracity","lastName":"Authenticator"},"authorized_at":"2021-03-17T08:08:14.523Z","createdBy":"5e43c88c1009160004a999a4","firstCreatedByRole":"trader","height":29.7,"meta":{"torch":"true","vertical":"vertical_documents","upload_time":"20"},"name":"Ucebnice","overviewUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/JPEG_IMG_CROPPED_PROTECT7ae37ef3ecc634971615968364232.jpg","publicId":"446VXMCG9","thumbnailUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/JPEG_IMG_THUMBNAIL_PROTECT7ae37ef3ecc634971615968364232.jpg","width":21.0},"completed":true,"createdAt":"2021-04-12T12:54:42.904Z","dividerText":"","itemType":0,"job":"a4ad5c0f-57a3-419c-a3e9-ee4af41b3471","jobName":"verify","searchArtworkResult":[],"tradingCode":{"code":"ZD9DVU2U","expiration":"1618232693937"},"uploaded":false}
        //failed to verify
        {"artwork":{"algorithmUsed":"celery","author":{"firstName":"Veracity","lastName":"Authenticator"},"authorized_at":"2021-03-17T08:08:14.523Z","createdBy":"5e43c88c1009160004a999a4","firstCreatedByRole":"trader","height":29.7,"meta":{"torch":"true","vertical":"vertical_documents","upload_time":"20"},"name":"Ucebnice","overviewUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/JPEG_IMG_CROPPED_PROTECT7ae37ef3ecc634971615968364232.jpg","publicId":"446VXMCG9","thumbnailUrl":"https://oneprove-dev.s3.eu-central-1.amazonaws.com/JPEG_IMG_THUMBNAIL_PROTECT7ae37ef3ecc634971615968364232.jpg","width":21.0},"completed":true,"createdAt":"2021-04-12T13:10:16.760Z","dividerText":"","error":"The overlap of original and verified detail is not big enough. Please try again.","itemType":0,"job":"527001f0-238d-4f73-9cf5-25dcc57c6840","jobName":"verify","searchArtworkResult":[],"uploaded":false}
        */
    }

    private fun toVerified(){

        bar_lay.setBackgroundColor(ResourcesCompat.getColor(requireActivity().resources, R.color.verify_green_bg, null))
        bar_img.setImageResource(R.drawable.ic_success)
        bar_text_primary.text=resources.getString(R.string.verify_detail_text_verified_primary)
        bar_text_primary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.verify_green, null))
        bar_text_secondary.text=resources.getString(R.string.verify_detail_text_verified_secondary)
        bar_text_secondary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.verify_green, null))

        btn_verify.visibility=View.VISIBLE
        btn_verify.text= resources.getText(R.string.item_detail_verify_item_again)
        btn_verify.setOnClickListener {
            getVerifyInfo()
        }

        btn_start_over.visibility=View.VISIBLE
        btn_start_over.paintFlags = btn_start_over.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        btn_start_over.setOnClickListener {
            findNavController().navigate(VerifyFragmentDirections.actionVerifyFragmentToCameraFragment())
        }
    }

    private fun toFailedToVerify(error: String){

        bar_lay.setBackgroundColor(ResourcesCompat.getColor(requireActivity().resources, R.color.light_red_bg, null))
        bar_img.setImageResource(R.drawable.ic_not_verified)
        bar_text_primary.text=resources.getString(R.string.item_detail_not_verified)
        bar_text_primary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.alert_red, null))
        bar_text_secondary.setTextColor(ResourcesCompat.getColor(requireActivity().resources, R.color.alert_red, null))
        bar_text_secondary.text = error

        btn_verify.visibility=View.VISIBLE
        btn_verify.text= resources.getText(R.string.item_detail_btn_fp_again)
        btn_verify.setOnClickListener {
            getVerifyInfo()
        }
        btn_start_over.visibility=View.VISIBLE
        btn_start_over.paintFlags = btn_start_over.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        btn_start_over.setOnClickListener {
            findNavController().navigate(VerifyFragmentDirections.actionVerifyFragmentToCameraFragment())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        verifyEvent.unregisterReceiver()
    }

}