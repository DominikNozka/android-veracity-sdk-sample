package com.veracity.protocol.sample.model

import android.content.Context
import java.io.File

class FilesModel() {
    companion object {

        private const val prefsTag = "veracity_sample_prefs"
        private const val overviewCropFileTag = "overview_crop_file"
        private const val thumbnailFileTag = "thumbnail_file"

        fun getRootDirectory(context: Context):File{
            return File(context.filesDir.path)
        }
        fun deleteAllFiles(context: Context){
            getRootDirectory(context).listFiles()?.forEach {
                it.delete()
            }
        }

        //protection files
        fun getOverviewFile(context: Context):File{
            return File(getRootDirectory(context),"overview.jpg")
        }
        // overview & thumbnail images needs to have unique name
        fun setOverviewCropFile(context: Context):File{
            val filename = System.currentTimeMillis().toString().plus(".jpg")

            context.getSharedPreferences(prefsTag,Context.MODE_PRIVATE)
                .edit().putString(overviewCropFileTag,filename).commit()
            return File(getRootDirectory(context),filename)
        }
        fun getOverviewCropFile(context: Context):File{
            return File(getRootDirectory(context),
                context.getSharedPreferences(prefsTag,Context.MODE_PRIVATE).getString(
                overviewCropFileTag,"overview_crop.jpg"))
        }
        fun setThumbnailFile(context: Context):File{
            val filename = System.currentTimeMillis().toString().plus(".jpg")

            context.getSharedPreferences(prefsTag,Context.MODE_PRIVATE)
                .edit().putString(thumbnailFileTag,filename).commit()
            return File(getRootDirectory(context),filename)
        }
        fun getThumbnailFile(context: Context):File{
            return File(getRootDirectory(context),
                context.getSharedPreferences(prefsTag,Context.MODE_PRIVATE).getString(
                    thumbnailFileTag,"thumbnail.jpg"))
        }

        //verification files
        fun getVerifyOverviewCropFile(context: Context):File{
            return File(getRootDirectory(context),"overview_crop_verify.jpg");
        }
        fun getVerifyThumbnailFile(context: Context):File{
            return File(getRootDirectory(context),"thumbnail_verify.jpg")
        }
    }
}

