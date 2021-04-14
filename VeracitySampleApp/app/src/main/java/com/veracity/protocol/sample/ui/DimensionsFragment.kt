package com.veracity.protocol.sample.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.veracity.protocol.sample.R
import com.veracity.protocol.sample.model.FilesModel
import kotlinx.android.synthetic.main.fragment_dimensions.*
import kotlinx.android.synthetic.main.toolbar.*

class DimensionsFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dimensions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar_title.text = resources.getString(R.string.dimensions_title)

        toolbar_info.visibility = View.INVISIBLE
        toolbar_back.setOnClickListener {
            findNavController().navigate(DimensionsFragmentDirections.actionDimensionsFragmentToQualityCheckFragment())
        }
        toolbar_close.setOnClickListener {
            requireActivity().finish()
        }

        overview_image.setImageBitmap(BitmapFactory.decodeFile(FilesModel.getOverviewCropFile(requireContext()).path))

        btn_confirm.setOnClickListener {

            if(dimensWarn()){
                warning_text.visibility=View.VISIBLE
            }
            else {
                val action = DimensionsFragmentDirections
                    .actionDimensionsFragmentToProtectFragment(
                        java.lang.Float.parseFloat(width.text.toString()),
                        java.lang.Float.parseFloat(height.text.toString()))
                findNavController().navigate(action)
            }
        }
    }

    private fun dimensWarn():Boolean{

        if (height.text.isEmpty()) return true
        if (width.text.isEmpty()) return true

        try{
            java.lang.Float.parseFloat(width.text.toString())
            java.lang.Float.parseFloat(height.text.toString())
        }catch (e:Exception){
            return true
        }

        val widthCm= java.lang.Float.parseFloat(width.text.toString())
        val heightCm= java.lang.Float.parseFloat(height.text.toString())

        if(widthCm<4f) return true
        if(heightCm<4f) return true
        if(widthCm>500f) return true
        if(heightCm>500f) return true

        return false
    }

}