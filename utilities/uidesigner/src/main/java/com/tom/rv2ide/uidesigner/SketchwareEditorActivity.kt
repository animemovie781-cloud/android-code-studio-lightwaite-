package com.tom.rv2ide.uidesigner

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.tom.rv2ide.app.BaseIDEActivity
import com.tom.rv2ide.uidesigner.databinding.ActivitySketchwareEditorBinding
import com.tom.rv2ide.uidesigner.fragments.DesignerWorkspaceFragment
import com.tom.rv2ide.uidesigner.fragments.EventLogicFragment
import com.tom.rv2ide.uidesigner.viewmodel.WorkspaceViewModel
import java.io.File
import org.slf4j.LoggerFactory

class SketchwareEditorActivity : BaseIDEActivity() {

    private var binding: ActivitySketchwareEditorBinding? = null
    private val viewModel by viewModels<WorkspaceViewModel>()

    private val viewFragment = DesignerWorkspaceFragment()
    private val eventFragment = EventLogicFragment()

    companion object {
        private val log = LoggerFactory.getLogger(SketchwareEditorActivity::class.java)
        const val EXTRA_FILE = "layout_file"
    }

    override fun bindLayout(): View {
        binding = ActivitySketchwareEditorBinding.inflate(layoutInflater)
        return binding!!.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        intent?.extras?.let {
            val path = it.getString(EXTRA_FILE) ?: return
            val file = File(path)
            if (!file.exists()) {
                throw IllegalArgumentException("File does not exist: $file")
            }
            viewModel.file = file
        }

        setSupportActionBar(binding!!.toolbar)
        supportActionBar?.title = viewModel.file.nameWithoutExtension

        // Setup Bottom Navigation
        binding!!.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_view -> {
                    switchFragment(viewFragment)
                    true
                }
                R.id.nav_event -> {
                    switchFragment(eventFragment)
                    true
                }
                R.id.nav_component -> {
                    // Component logic to be added
                    true
                }
                else -> false
            }
        }

        // Load default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, viewFragment, "VIEW")
                .commit()
        }
    }

    private fun switchFragment(fragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != fragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    fun setupHierarchy(view: com.tom.rv2ide.inflater.IView) {
        // Implement hierarchy setup later, for now just no-op or pass to another fragment
    }
}
