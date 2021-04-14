package com.veracity.protocol.sample.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.veracity.protocol.sample.R
import com.veracity.protocol.sample.model.FilesModel
import kotlinx.android.synthetic.main.fragment_quality_check.*
import kotlinx.android.synthetic.main.toolbar.*

class QualityCheckFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quality_check, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar_title.text = resources.getString(R.string.quality_check_title)

        toolbar_info.visibility = View.INVISIBLE
        toolbar_back.setOnClickListener {
            findNavController().navigate(QualityCheckFragmentDirections.actionQualityCheckFragmentToImageCropFragment())
        }
        toolbar_close.setOnClickListener {
            requireActivity().finish()
        }

        redo_crop.paintFlags = redo_crop.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        redo_crop.setOnClickListener {
            findNavController().navigate(QualityCheckFragmentDirections.actionQualityCheckFragmentToImageCropFragment())
        }

        image.setImageBitmap(BitmapFactory.decodeFile(FilesModel.getOverviewCropFile(requireContext()).path))

        btn_confirm.setOnClickListener {
            findNavController().navigate(QualityCheckFragmentDirections.actionQualityCheckFragmentToDimensionsFragment())
        }
    }
}