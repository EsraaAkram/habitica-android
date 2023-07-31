package com.habitrpg.android.habitica.ui.activities

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.lifecycle.lifecycleScope
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.data.InventoryRepository
import com.habitrpg.android.habitica.databinding.ActivityDeathBinding
import com.habitrpg.android.habitica.extensions.observeOnce
import com.habitrpg.android.habitica.helpers.AdHandler
import com.habitrpg.android.habitica.helpers.AdType
import com.habitrpg.android.habitica.helpers.AppConfigManager
import com.habitrpg.android.habitica.ui.viewmodels.MainUserViewModel
import com.habitrpg.android.habitica.ui.views.HabiticaIconsHelper
import com.habitrpg.android.habitica.ui.views.ads.AdButton
import com.habitrpg.common.habitica.extensions.fromHtml
import com.habitrpg.common.habitica.helpers.Animations
import com.habitrpg.common.habitica.helpers.ExceptionHandler
import com.plattysoft.leonids.ParticleSystem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeathActivity : BaseActivity() {
    private lateinit var binding: ActivityDeathBinding

    @Inject
    internal lateinit var inventoryRepository: InventoryRepository

    @Inject
    internal lateinit var appConfigManager: AppConfigManager

    @Inject
    lateinit var userViewModel: MainUserViewModel

    override fun getLayoutResId(): Int = R.layout.activity_death

    override fun getContentView(layoutResId: Int?): View {
        binding = ActivityDeathBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel.user.observeOnce(this) { user ->
            binding.lossDescription.text = getString(R.string.faint_loss_description, (user?.stats?.lvl ?: 2).toInt() - 1, user?.stats?.gp?.toInt()).fromHtml()
        }

        if (appConfigManager.enableFaintAds()) {
            val handler = AdHandler(this, AdType.FAINT) {
                if (!it) {
                    return@AdHandler
                }
                Log.d("AdHandler", "Reviving user")
                lifecycleScope.launch(ExceptionHandler.coroutine()) {
                    userRepository.updateUser("stats.hp", 1)
                    finish()
                }
            }
            handler.prepare {
                if (it && binding.adButton.state == AdButton.State.LOADING) {
                    binding.adButton.state = AdButton.State.READY
                } else if (!it) {
                    binding.adButton.visibility = View.INVISIBLE
                }
            }
            binding.adButton.updateForAdType(AdType.FAINT, lifecycleScope)
            binding.adButton.setOnClickListener {
                binding.adButton.state = AdButton.State.LOADING
                handler.show()
            }
        } else {
            binding.adButton.visibility = View.GONE
        }

        binding.restartButton.setOnClickListener {
            binding.restartButton.isEnabled = false
            lifecycleScope.launch(ExceptionHandler.coroutine()) {
                userRepository.revive()
                finish()
            }
        }
        startAnimating()
    }

    private fun startAnimating() {
        binding.ghostView.startAnimation(Animations.bobbingAnimation())
        binding.heartView.post {
            makeCoins(305)
            makeCoins(160)
        }
    }

    private fun makeCoins(startAngle: Int) {
        val positionArray = intArrayOf(0, 0)
        binding.heartView.getLocationOnScreen(positionArray)
        ParticleSystem(
            binding.confettiContainer,
            14,
            BitmapDrawable(resources, HabiticaIconsHelper.imageOfGold()),
            5000
        )
            .setInitialRotationRange(0, 200)
            .setScaleRange(0.5f, 0.8f)
            .setSpeedRange(0.01f, 0.03f)
            .setFadeOut(4000, AccelerateInterpolator())
            .setSpeedModuleAndAngleRange(0.01f, 0.03f, startAngle, startAngle + 80)
            .emit(binding.root.width / 2, positionArray[1] + (binding.heartView.height / 2), 3, 6000)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
