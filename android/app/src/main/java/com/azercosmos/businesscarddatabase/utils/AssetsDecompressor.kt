package com.azercosmos.businesscarddatabase.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AssetsDecompressor {

    companion object {
        private fun getAssets(): Array<String> {
            return arrayOf()
        }

        private val permissions: Array<String> = arrayOf(
            // Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        fun requestPermissions(activity: AppCompatActivity, requestCode: Int) {
            for (permission in permissions) {
                if (activity.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, permissions, requestCode)
                    Log.d("permission", "$permission is not granted")
                    break
                }
            }
        }

        fun unpack (assetManager: AssetManager, path: String, assetPath: String = "", force: Boolean = false) {
            if (isUnpacked(path) && !force) {
                return
            }
            val assets = assetManager.list(assetPath)
            if (assets != null) {
                Log.d("asset_found", "Found assets. Count - ${assets.size}")
                for (asset in assets.iterator()) {
                    // AssetPath indicates path to the asset folder. E.g. 'folder/', ''
                    //  val file = File("$assetPath$asset")
                    // if (file.isDirectory) {
                        // unpack(assetManager, path, "$assetPath$asset/")
                    // } else {
                    if (!File("$path/$assetPath").exists())
                    {
                        File("$path/$assetPath").mkdirs()
                    }

                    val inStream = assetManager.open("$assetPath/$asset")
                    val outStream = FileOutputStream(File("$path/$assetPath/$asset"))
                    val byte = ByteArray(1024)
                    var length: Int
                    while (true) {
                        length = inStream.read(byte)
                        if (length <= 0) {
                            break
                        }
                        outStream.write(byte, 0, length)
                    }
                    inStream.close()
                    outStream.close()
                    // }
                }
            } else {
                Log.d("asset_found", "Assets not found")
            }
        }

        fun isUnpacked (path: String): Boolean {
            return File(path).exists()
        }

        /*private fun unpackAsset (assetManager: AssetManager, assetPath: String, path: String) {

        }*/
    }
}