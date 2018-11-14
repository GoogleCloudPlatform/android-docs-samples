/*
 * Copyright 2018 Google LLC.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.speechtranslation

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.chromium.base.ThreadUtils
import org.chromium.net.CronetEngine
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

class TranslateActivity : AppCompatActivity() {

    /**
     * The [androidx.viewpager.widget.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [androidx.fragment.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private lateinit var cronetEngine: CronetEngine

    /**
     * The [ViewPager] that will host the section contents.
     */
    private var mViewPager: ViewPager? = null
    lateinit var speechTranslation: SpeechTranslation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById<ViewPager>(R.id.container)
        mViewPager!!.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tabs)

        mViewPager!!.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(mViewPager))

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            if(!PermissionsHelper.hasRequiredPermissions(this)) {
                PermissionsHelper.requestRequiredPermissions(this)
            } else {
                translateAudioMessage()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_translate_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.sign_out) {
            val gcsScope = Scope(
                    applicationContext.getString(R.string.speechToSpeechOAuth2Scope))
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.gcpClientID))
                    .requestScopes(gcsScope)
                    .requestEmail()
                    .build()

            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut()
            val signInIntent = Intent(this, SignInActivity::class.java)
            startActivity(signInIntent)
            return true
        } else return super.onOptionsItemSelected(item)

    }

    /**
     * Creates an instance of the CronetEngine class.
     * Instances of CronetEngine require a lot of resources. Additionally, their creation is slow
     * and expensive. It's recommended to delay the creation of CronetEngine instances until they
     * are required and reuse them as much as possible.
     * @return An instance of CronetEngine.
     */
    @Synchronized
    fun getCronetEngine(): CronetEngine {
        if (!::cronetEngine.isInitialized) {
            val myBuilder = CronetEngine.Builder(this)
            cronetEngine = myBuilder.build()
        }
        return cronetEngine
    }

    private fun showErrorToast(e: Exception) {
        runOnUiThread {
            Toast.makeText(
                    applicationContext,
                    e.localizedMessage,
                    Toast.LENGTH_LONG).show()
        }
    }

    private fun translateAudioMessage() {
        if (!RecordingHelper.isRecording) {
            RecordingHelper.startRecording(object : RecordingHelper.RecordingListener {
                override fun onRecordingSucceeded(output: File) {
                    val base64EncodedAudioMessage: String
                    try {
                        base64EncodedAudioMessage = Base64EncodingHelper.encode(output)
                        SpeechTranslationHelper.translateAudioMessage(
                                applicationContext,
                                getCronetEngine(),
                                base64EncodedAudioMessage,
                                16000,
                                object : SpeechTranslationHelper.SpeechTranslationListener {
                                    override fun onTranslationSucceeded(responseBody: String) {
                                        Log.i(TAG, responseBody)
                                        try {
                                            val successfulResponse = JSONObject(responseBody)
                                            speechTranslation = SpeechTranslation((successfulResponse))

                                            for(fragment in supportFragmentManager.fragments) {
                                                val placeholder = fragment as PlaceholderFragment
                                                val languageCode = fragment.arguments?.getString(PlaceholderFragment.ARG_LANGUAGE_CODE)
                                                if (languageCode != null) {
                                                    val translation = speechTranslation.getTranslation(languageCode)
                                                    val translatedText = translation.text
                                                    val gcsFile = speechTranslation.gcsBucket + "/" + translation.gcsPath
                                                    // 🔈 Prepend a speaker emoji to text.
                                                    placeholder.setText(resources.getString(R.string.speaker_emoji) + translatedText)
                                                    placeholder.setGcsFile(gcsFile)
                                                }
                                            }
                                        } catch (e: JSONException) {
                                            showErrorToast(e)
                                            Log.e(TAG, e.localizedMessage)
                                        }
                                    }

                                    override fun onTranslationFailed(e: Exception) {
                                        showErrorToast(e)
                                        Log.e(TAG, e.localizedMessage)
                                    }
                                })
                    } catch (e: IOException) {
                        showErrorToast(e)
                        Log.e(TAG, e.localizedMessage)
                    }

                }

                override fun onRecordingFailed(e: Exception) {
                    showErrorToast(e)
                    Log.e(TAG, e.localizedMessage)
                }
            })
        } else {
            RecordingHelper.stopRecording()
        }
    }

    companion object {
        const val TAG = "TranslateActivity"
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {
        private lateinit var mModel: TranslationViewModel
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            mModel = ViewModelProviders.of(this).get(TranslationViewModel::class.java)
            val rootView = inflater.inflate(R.layout.fragment_translate_activity, container, false)
            val textView = rootView.findViewById<TextView>(R.id.section_label)

            textView.setOnClickListener {
                if(!mModel.gcsFile.value.isNullOrBlank()) {
                    val translateActivity = activity as TranslateActivity
                    if(!PermissionsHelper.hasRequiredPermissions(translateActivity)) {
                        PermissionsHelper.requestRequiredPermissions(translateActivity)
                    } else {
                        val gcsFile = mModel.gcsFile.value as String
                        playMessage(gcsFile)
                    }
                }
            }

            // Create the observer which updates the UI.
            val textObserver = Observer<String> { newText ->
                textView.text = newText
            }

            // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
            mModel.text.observe(this, textObserver)

            val parentActivity = activity as TranslateActivity
            if(parentActivity::speechTranslation.isInitialized) {
                val languageCode = arguments?.getString(PlaceholderFragment.ARG_LANGUAGE_CODE)
                if (languageCode != null) {
                    val translation = parentActivity.speechTranslation.getTranslation(languageCode)
                    val translatedText = translation.text
                    val gcsFile = parentActivity.speechTranslation.gcsBucket + "/" + translation.gcsPath
                    // 🔈 Prepend a speaker emoji to text.
                    setText(resources.getString(R.string.speaker_emoji) + translatedText)
                    setGcsFile(gcsFile)
                }
            }

            return rootView
        }

        fun setText(text: String) {
            ThreadUtils.runOnUiThread {
                mModel.text.value = text
            }
        }

        fun setGcsFile(gcsFile: String) {
            ThreadUtils.runOnUiThread {
                mModel.gcsFile.value = gcsFile
            }
        }

        private fun playMessage(gcsFile: String) {
            val localFile = File(activity?.filesDir, gcsFile.substringAfterLast('/'))
            val translateActivity = activity as TranslateActivity

            if (localFile.exists()) {
                val mediaPlayer = MediaPlayer.create(
                        translateActivity.applicationContext, Uri.fromFile(localFile))
                mediaPlayer.start()
            } else {
                GcsDownloadHelper.downloadGcsFile(
                        translateActivity.applicationContext, translateActivity.getCronetEngine(), gcsFile,
                        object: GcsDownloadHelper.GcsDownloadListener {
                            override fun onDownloadSucceeded(file: File) {
                                val mediaPlayer = MediaPlayer.create(
                                        translateActivity.applicationContext, Uri.fromFile(file))
                                mediaPlayer.start()
                            }

                            override fun onDownloadFailed(e: Exception) {
                                translateActivity.showErrorToast(e)
                                Log.e(TAG, e.localizedMessage)
                            }
                        }
                )
            }
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            const val ARG_LANGUAGE_CODE = "language_code"
            const val TAG = "PlaceholderFragment"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(languageCode: String): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putString(ARG_LANGUAGE_CODE, languageCode)
                fragment.arguments = args
                return fragment
            }
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            val languageCode = when(position) {
                0 -> "en"
                1 -> "es"
                2 -> "fr"
                else -> throw SpeechTranslationException("The app only supports three languages.")
            }
            return PlaceholderFragment.newInstance(languageCode)
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }
    }
}
