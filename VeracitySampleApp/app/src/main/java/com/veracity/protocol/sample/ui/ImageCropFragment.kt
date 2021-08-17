package com.veracity.protocol.sample.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.veracity.protocol.sample.R
import com.veracity.protocol.sample.model.FilesModel
import com.veracity.sdk.crop.CropImageView
import kotlinx.android.synthetic.main.fragment_image_crop.*
import kotlinx.android.synthetic.main.popup_info.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File

/*
    Documentation link:
    https://veracityprotocol.gitbook.io/api/veracity-sdk/protection-guide#2-cropping-the-overview-image
*/

class ImageCropFragment : Fragment(), CropImageView.CropEvent, CropImageView.PositionEvent {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_crop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar_title.text = resources.getString(R.string.crop_title)
        message_popup_text.text = resources.getString(R.string.crop_message)

        toolbar_info.visibility = View.INVISIBLE
        toolbar_back.setOnClickListener {
            findNavController().navigate(ImageCropFragmentDirections.actionImageCropFragmentToCameraFragment())
        }
        toolbar_close.setOnClickListener {
            requireActivity().finish()
        }

        crop_view.findPositions(this,requireActivity(),
            FilesModel.getOverviewFile(requireContext()),
            FilesModel.setOverviewCropFile(requireContext()),
            FilesModel.setThumbnailFile(requireContext()))
    }

    override fun onPositionsFound() {

        crop_btn.setOnClickListener{
            crop_view.cropImage(this)
            crop_btn.isClickable=false
            prg.visibility=View.VISIBLE
        }
        popup_info.visibility=View.VISIBLE
        popup_info.postDelayed({ popup_info?.visibility = View.GONE }, 3000)
        prg.visibility=View.INVISIBLE
        crop_btn.isClickable=true
    }

    override fun onImageCropped(jpegCropped: File, thumbnail: File) {
        findNavController().navigate(ImageCropFragmentDirections.actionImageCropFragmentToQualityCheckFragment())
    }

    override fun onDestroy() {
        super.onDestroy()
        //recycle images in crop_view
        crop_view?.recycle()
    }
}