package com.veracity.protocol.sample.ui

import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap

import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Executor

import androidx.camera.core.*
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.veracity.protocol.sample.R
import com.veracity.protocol.sample.model.FilesModel
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.fragment_camera.popup_info
import kotlinx.android.synthetic.main.fragment_camera.prg
import kotlinx.android.synthetic.main.fragment_image_crop.*
import kotlinx.android.synthetic.main.popup_info.*
import kotlinx.android.synthetic.main.toolbar.*

/*
    Documentation link:
    https://veracityprotocol.gitbook.io/api/veracity-sdk/protection-guide#1-capturing-an-overview-image

    This sample is based on CameraXBasic code example:
    https://github.com/android/camera-samples/tree/master/CameraXBasic
*/
class CameraFragment : Fragment(), Executor  {

    private val TAG = "CameraXBasic"

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking ic_cam operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        toolbar_title.text = resources.getString(R.string.title_camera)
        message_popup_text.text = resources.getString(R.string.message_camera)

        toolbar_info.visibility = View.INVISIBLE
        toolbar_back.setOnClickListener {
            findNavController().navigate(CameraFragmentDirections.actionCameraFragmentToAuthenticateFragment())
        }
        toolbar_close.setOnClickListener {
            requireActivity().finish()
        }

        view_finder.post {
            displayId = view_finder.display.displayId
            setUpCamera()
        }

        picture.setOnClickListener {
            clickPictureBtn()
            prg.visibility=View.VISIBLE
        }
        popup_info.postDelayed({ popup_info?.visibility = View.GONE }, 3000)
    }

    /** Initialize CameraX, and prepare to bind the ic_cam use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = CameraSelector.LENS_FACING_BACK


            // Build and bind the ic_cam use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {

        val rotation = view_finder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()


        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // ic_cam provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(view_finder.surfaceProvider)

            // camera!!.cameraControl.enableTorch(true)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)

        }
    }

    override fun execute(command: Runnable) {
        command.run()
    }

    private fun clickPictureBtn() {
        //imageCapture!!.takePicture(cameraExecutor,capturedListener)

        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = FilesModel.getOverviewFile(requireContext())

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {

                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(savedUri.toFile().absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(TAG, "Image capture scanned into media store: $uri")

                            findNavController().navigate(CameraFragmentDirections.actionCameraFragmentToImageCropFragment())
                        }
                    }
                })
        }
    }


}