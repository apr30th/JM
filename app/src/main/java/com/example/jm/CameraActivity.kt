package com.example.jm

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.jm.databinding.ActivityCameraBinding
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import java.io.File
import java.io.IOException

class CameraActivity : AppCompatActivity() {
    lateinit var binding : ActivityCameraBinding
    private lateinit var classifier: Classifier
    private lateinit var classifier2: Classifier
    private lateinit var searchName: String
    private var imageUri: Uri? = null
    lateinit var recyclerAdapter: RecyclerItemAdapter_camera
    val recipes = mutableListOf<RecipeData>()

    private val cameraResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
            if (isSuccess.not()) {
                finish()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                return@registerForActivityResult
            }
            val selectedImage = imageUri ?: return@registerForActivityResult
            var bitmap: Bitmap? = null
            try {
                bitmap = if (Build.VERSION.SDK_INT >= 28) {
                    val src = ImageDecoder.createSource(contentResolver, selectedImage)
                    ImageDecoder.decodeBitmap(src)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                }
            } catch (exception: IOException) {
                exception.printStackTrace()
                Toast.makeText(this, "Error loading image: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

            bitmap?.let {
                val output = classifier.classify(bitmap)
                val outputString = output.first.split("/")
                val outputName = outputString[0]
                searchName = outputName

                val resultStr =
                    String.format("'%s'이(가) 맞으신가요?", outputName)

                binding.run {
                    imagePhoto.setImageBitmap(bitmap)
                    textResult.text = resultStr

                    val slidePanel = binding.slidingFrame

                    btnYes.setOnClickListener {

                        btnsYorN.visibility = View.GONE
                        slidePanel.panelState = SlidingUpPanelLayout.PanelState.ANCHORED

                        recipes.apply {
                            add(
                                RecipeData(
                                    name = outputName,
                                    micro_recipe1 = outputString[1],
                                    micro_recipe2 = outputString[2],
                                    air_recipe1 = outputString[3],
                                    air_recipe2 = outputString[4]
                                )
                            )
                            recyclerAdapter.datas = recipes
                            recyclerAdapter.notifyDataSetChanged()
                        }

                        textResult.text = "인식된 결과 : "+outputName
                        btnsMs.visibility = View.VISIBLE
                    }

                    btnNo.setOnClickListener {
                        val output2 = classifier2.classifySecond(bitmap)
                        val outputString2 = output2.first.split("/")
                        val outputName2 = outputString2[0]
                        searchName = outputName2

                        btnsYorN.visibility = View.GONE

                        recipes.apply {
                            add(RecipeData(name = outputName2,
                                micro_recipe1 = outputString2[1],
                                micro_recipe2 = outputString2[2],
                                air_recipe1 = outputString2[3],
                                air_recipe2 = outputString2[4]))
                            recyclerAdapter.datas = recipes
                            recyclerAdapter.notifyDataSetChanged()
                        }

                        textResult.text = "수정된 결과 : "+outputName2
                        btnsMs.visibility = View.VISIBLE

                        imagePhoto.setOnClickListener {
                            showConfidence(output2)
                        }
                    }

                    btnMain.setOnClickListener {
                        finish()
                        val intent = Intent(this@CameraActivity, MainActivity::class.java)
                        intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        startActivity(intent)
                    }

                    imagePhoto.setOnClickListener {
                        showConfidence(output)
                    }

                    textResult.setOnClickListener {
                        slidePanel.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initClassifier()
        initRecycler()

        binding.run {
            binding.btnsYorN.visibility = View.VISIBLE
            binding.btnsMs.visibility = View.GONE

            getTmpFileUri().let { uri ->
                    imageUri = uri
                    cameraResult.launch(uri)
            }

            binding.btnMore.setOnClickListener {
                binding.btnsYorN.visibility = View.VISIBLE
                binding.btnsMs.visibility = View.GONE

                recipes.clear()

                getTmpFileUri().let { uri ->
                    imageUri = uri
                    cameraResult.launch(uri)
                }
            }

            val permissions = arrayOf(
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )

            val multiplePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                val resultPermission = it.all { map -> map.value }
                if(!resultPermission)
                    finish()
            }

            fun allPermissionGranted() = permissions.all{
                ActivityCompat.checkSelfPermission(this@CameraActivity, it)== PackageManager.PERMISSION_GRANTED
            }

            fun checkPermissions() {
                multiplePermissionLauncher.launch(permissions)
            }

            binding.btnSearch.setOnClickListener {
                val webpage = Uri.parse("https://www.google.com/search?q="+searchName+" 레시피")

                val webIntent = Intent(Intent.ACTION_VIEW, webpage)
                if (allPermissionGranted())
                    startActivity(webIntent)
                else
                    checkPermissions()
            }

            binding.btnPlace.setOnClickListener {
                val webpage = Uri.parse("https://m.map.naver.com/search2/search.naver?query=주변 "+searchName+"&siteLocation=&queryRank=&type=#/list/")

                val webIntent = Intent(Intent.ACTION_VIEW, webpage)
                if (allPermissionGranted())
                    startActivity(webIntent)
                else
                    checkPermissions()
            }
        }
    }

    override fun onDestroy() {
        classifier.finish()
        classifier2.finish()
        super.onDestroy()
    }

    private fun initClassifier() {
        classifier = Classifier(this, Classifier.CMODEL_NAME)
        classifier2 = Classifier(this, Classifier.CMODEL_NAME)
        try {
            classifier.initForCamera()
            classifier2.initForCamera()
        } catch (exception: IOException) {
            Toast.makeText(this, "Can not init Classifier!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfidence(output: Pair<String, Float>) {
        val confidence = output.second*10
        Toast.makeText(this, confidence.toString(), Toast.LENGTH_SHORT).show()
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initRecycler() {
        recyclerAdapter = RecyclerItemAdapter_camera(this)
        binding.itemRecycler.adapter = recyclerAdapter
        recipes.apply {
            recyclerAdapter.datas = recipes
            recyclerAdapter.notifyDataSetChanged()
        }
    }
}