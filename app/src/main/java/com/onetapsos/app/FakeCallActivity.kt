package com.onetapsos.app

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FakeCallActivity : BaseActivity() {

    private var ringTimer: CountDownTimer? = null
    private var callTimer: CountDownTimer? = null
    private var delayTimer: CountDownTimer? = null
    private var elapsedSeconds = 0

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var toneGenerator: ToneGenerator? = null

    private var isCallActive = false
    private var isMuted = false
    private var isHolding = false
    private var isSpeakerOn = false
    private var isRecording = false

    private var callerName: String = "father"
    private var selectedDelaySeconds: Int = 0
    private var isSlideAnswerMode: Boolean = false

    // Views
    private lateinit var layoutSetup: View
    private lateinit var layoutRinging: View
    private lateinit var layoutActiveCall: View
    private lateinit var layoutKeypadOverlay: View

    private lateinit var tvIncomingClock: TextView
    private lateinit var tvActiveClock: TextView
    private lateinit var tvIncomingCallerName: TextView
    private lateinit var tvActiveCallerName: TextView
    private lateinit var tvFakeCallTimer: TextView
    private lateinit var tvKeypadDuration: TextView
    private lateinit var tvDialedDigits: TextView
    private lateinit var tvCountdownNotice: TextView
    private lateinit var layoutCountdownBanner: LinearLayout

    private lateinit var etCallerName: EditText
    private lateinit var switchRingtone: SwitchMaterial
    private lateinit var switchVibration: SwitchMaterial

    private lateinit var layoutIncomingButtonsMode: View
    private lateinit var layoutIncomingSlideMode: View
    private lateinit var btnSlideHandle: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeAndUnlockScreen()
        setContentView(R.layout.activity_fake_call)

        initViews()
        initAudioAndHaptics()
        setupEventListeners()
        checkIntentTriggers()
    }

    private fun wakeAndUnlockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun initViews() {
        layoutSetup = findViewById(R.id.layoutSetup)
        layoutRinging = findViewById(R.id.layoutRinging)
        layoutActiveCall = findViewById(R.id.layoutActiveCall)
        layoutKeypadOverlay = findViewById(R.id.layoutKeypadOverlay)

        tvIncomingClock = findViewById(R.id.tvIncomingClock)
        tvActiveClock = findViewById(R.id.tvActiveClock)
        tvIncomingCallerName = findViewById(R.id.tvIncomingCallerName)
        tvActiveCallerName = findViewById(R.id.tvActiveCallerName)
        tvFakeCallTimer = findViewById(R.id.tvFakeCallTimer)
        tvKeypadDuration = findViewById(R.id.tvKeypadDuration)
        tvDialedDigits = findViewById(R.id.tvDialedDigits)
        tvCountdownNotice = findViewById(R.id.tvCountdownNotice)
        layoutCountdownBanner = findViewById(R.id.layoutCountdownBanner)

        etCallerName = findViewById(R.id.etCallerName)
        switchRingtone = findViewById(R.id.switchRingtone)
        switchVibration = findViewById(R.id.switchVibration)

        layoutIncomingButtonsMode = findViewById(R.id.layoutIncomingButtonsMode)
        layoutIncomingSlideMode = findViewById(R.id.layoutIncomingSlideMode)
        btnSlideHandle = findViewById(R.id.btnSlideHandle)

        updateLiveClocks()
    }

    private fun updateLiveClocks() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvIncomingClock.text = currentTime
        tvActiveClock.text = currentTime
    }

    private fun initAudioAndHaptics() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }

            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
        } catch (_: Exception) {}
    }

    private fun setupEventListeners() {
        findViewById<ImageView>(R.id.ivFakeCallBack).setOnClickListener { finish() }

        // Quick Instant Call from top bar
        findViewById<TextView>(R.id.tvQuickTest).setOnClickListener {
            callerName = etCallerName.text.toString().trim().ifEmpty { "father" }
            startIncomingCall()
        }

        // Preset Chips
        setupPresetChips()

        // Style Mode Chips (Buttons vs Slide)
        setupStyleModeChips()

        // Delay Chips
        setupDelayChips()

        // Main Trigger Button
        findViewById<View>(R.id.btnStartFakeCall).setOnClickListener {
            callerName = etCallerName.text.toString().trim().ifEmpty { "father" }
            if (selectedDelaySeconds > 0) {
                startDelayCountdown(selectedDelaySeconds)
            } else {
                startIncomingCall()
            }
        }

        // Screen 1: Incoming Call Buttons
        findViewById<View>(R.id.btnDeclineCall).setOnClickListener { declineCall() }
        findViewById<View>(R.id.btnAnswerCall).setOnClickListener { answerCall() }

        // Screen 2: Slide to Answer Handle
        setupSlideHandle()

        // Screen 3: In-Call Matrix Buttons
        setupInCallButtons()

        // In-Call Dialpad Overlay Keys
        setupDialpadOverlay()
    }

    private fun setupPresetChips() {
        val chipFather = findViewById<TextView>(R.id.chipPresetFather)
        val chipMother = findViewById<TextView>(R.id.chipPresetMother)
        val chipPolice = findViewById<TextView>(R.id.chipPresetPolice)
        val chipBoss = findViewById<TextView>(R.id.chipPresetBoss)
        val chipDoctor = findViewById<TextView>(R.id.chipPresetDoctor)
        val tvPreviewName = findViewById<TextView>(R.id.tvPreviewCallerName)

        val chips = listOf(chipFather, chipMother, chipPolice, chipBoss, chipDoctor)

        fun selectChip(selected: TextView, name: String) {
            chips.forEach {
                it.setBackgroundResource(if (it == selected) R.drawable.bg_pill_accent else R.drawable.bg_pill_neutral)
                it.setTextColor(if (it == selected) getColor(R.color.accent_cyan) else getColor(R.color.white))
            }
            etCallerName.setText(name)
            tvPreviewName.text = name
            callerName = name
        }

        chipFather.setOnClickListener { selectChip(chipFather, "father") }
        chipMother.setOnClickListener { selectChip(chipMother, "mother") }
        chipPolice.setOnClickListener { selectChip(chipPolice, "Police Officer") }
        chipBoss.setOnClickListener { selectChip(chipBoss, "Boss") }
        chipDoctor.setOnClickListener { selectChip(chipDoctor, "Doctor") }
    }

    private fun setupStyleModeChips() {
        val chipButtons = findViewById<TextView>(R.id.chipStyleButtons)
        val chipSlide = findViewById<TextView>(R.id.chipStyleSlide)

        chipButtons.setOnClickListener {
            isSlideAnswerMode = false
            chipButtons.setBackgroundResource(R.drawable.bg_pill_accent)
            chipButtons.setTextColor(getColor(R.color.accent_cyan))
            chipSlide.setBackgroundResource(R.drawable.bg_pill_neutral)
            chipSlide.setTextColor(getColor(R.color.white))
        }

        chipSlide.setOnClickListener {
            isSlideAnswerMode = true
            chipSlide.setBackgroundResource(R.drawable.bg_pill_accent)
            chipSlide.setTextColor(getColor(R.color.accent_cyan))
            chipButtons.setBackgroundResource(R.drawable.bg_pill_neutral)
            chipButtons.setTextColor(getColor(R.color.white))
        }
    }

    private fun setupDelayChips() {
        val delay0 = findViewById<TextView>(R.id.chipDelay0)
        val delay5 = findViewById<TextView>(R.id.chipDelay5)
        val delay15 = findViewById<TextView>(R.id.chipDelay15)
        val delay30 = findViewById<TextView>(R.id.chipDelay30)
        val delay60 = findViewById<TextView>(R.id.chipDelay60)

        val delayChips = listOf(delay0 to 0, delay5 to 5, delay15 to 15, delay30 to 30, delay60 to 60)

        delayChips.forEach { (view, seconds) ->
            view.setOnClickListener {
                selectedDelaySeconds = seconds
                delayChips.forEach { (v, s) ->
                    v.setBackgroundResource(if (s == seconds) R.drawable.bg_pill_accent else R.drawable.bg_pill_neutral)
                    v.setTextColor(if (s == seconds) getColor(R.color.accent_cyan) else getColor(R.color.white))
                }
            }
        }
    }

    private fun setupSlideHandle() {
        btnSlideHandle.setOnTouchListener(object : View.OnTouchListener {
            private var dX = 0f
            private var initialX = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = v.x
                        dX = v.x - event.rawX
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val parentWidth = (v.parent as View).width
                        val maxRight = parentWidth - v.width - 8
                        var newX = event.rawX + dX
                        if (newX < 4) newX = 4f
                        if (newX > maxRight) newX = maxRight.toFloat()
                        v.x = newX

                        if (newX >= maxRight * 0.75f) {
                            answerCall()
                            v.x = initialX
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().x(initialX).setDuration(200).start()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupInCallButtons() {
        // Contacts
        findViewById<View>(R.id.btnCallContacts).setOnClickListener {
            Toast.makeText(this, "Contacts directory opened", Toast.LENGTH_SHORT).show()
        }

        // Add call
        findViewById<View>(R.id.btnCallAdd).setOnClickListener {
            Toast.makeText(this, "Adding caller to conference...", Toast.LENGTH_SHORT).show()
        }

        // Mute Toggle
        val btnMute = findViewById<View>(R.id.btnCallMute)
        val ivMute = findViewById<ImageView>(R.id.ivBtnMute)
        val tvMuteLabel = findViewById<TextView>(R.id.tvBtnMuteLabel)
        btnMute.setOnClickListener {
            isMuted = !isMuted
            ivMute.setColorFilter(if (isMuted) getColor(R.color.accent_cyan) else getColor(R.color.white))
            tvMuteLabel.text = if (isMuted) "Muted" else "Mute"
            tvMuteLabel.setTextColor(if (isMuted) getColor(R.color.accent_cyan) else getColor(R.color.white))
        }

        // Hold Toggle
        val btnHold = findViewById<View>(R.id.btnCallHold)
        val ivHold = findViewById<ImageView>(R.id.ivBtnHold)
        val tvHoldLabel = findViewById<TextView>(R.id.tvBtnHoldLabel)
        btnHold.setOnClickListener {
            isHolding = !isHolding
            ivHold.setColorFilter(if (isHolding) getColor(R.color.warning_yellow) else getColor(R.color.white))
            tvHoldLabel.text = if (isHolding) "On Hold" else "Hold"
            tvHoldLabel.setTextColor(if (isHolding) getColor(R.color.warning_yellow) else getColor(R.color.white))
            if (isHolding) {
                tvFakeCallTimer.text = "ON HOLD"
            } else {
                updateTimerDisplay()
            }
        }

        // Record Toggle
        val btnRecord = findViewById<View>(R.id.btnCallRecord)
        val ivRecord = findViewById<ImageView>(R.id.ivBtnRecord)
        val tvRecordLabel = findViewById<TextView>(R.id.tvBtnRecordLabel)
        btnRecord.setOnClickListener {
            isRecording = !isRecording
            ivRecord.setColorFilter(if (isRecording) getColor(R.color.danger_red) else getColor(R.color.white))
            tvRecordLabel.text = if (isRecording) "Recording" else "Record"
            tvRecordLabel.setTextColor(if (isRecording) getColor(R.color.danger_red) else getColor(R.color.white))
            Toast.makeText(this, if (isRecording) "Recording call audio..." else "Recording stopped", Toast.LENGTH_SHORT).show()
        }

        // Note Dialog
        findViewById<View>(R.id.btnCallNote).setOnClickListener {
            showQuickNoteDialog()
        }

        // Speaker Toggle
        val btnSpeaker = findViewById<View>(R.id.btnCallSpeaker)
        val ivSpeaker = findViewById<ImageView>(R.id.ivBtnSpeaker)
        btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            ivSpeaker.setColorFilter(if (isSpeakerOn) getColor(R.color.accent_cyan) else getColor(R.color.white))
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isSpeakerphoneOn = isSpeakerOn
            Toast.makeText(this, if (isSpeakerOn) "Speaker on" else "Speaker off", Toast.LENGTH_SHORT).show()
        }

        // Keypad button
        findViewById<View>(R.id.btnCallKeypad).setOnClickListener {
            layoutKeypadOverlay.visibility = View.VISIBLE
        }

        // End Call from main active screen
        findViewById<View>(R.id.btnEndActiveCall).setOnClickListener {
            endCall()
        }
    }

    private fun setupDialpadOverlay() {
        findViewById<View>(R.id.tvHideKeypad).setOnClickListener {
            layoutKeypadOverlay.visibility = View.GONE
        }

        findViewById<View>(R.id.btnEndCallFromKeypad).setOnClickListener {
            layoutKeypadOverlay.visibility = View.GONE
            endCall()
        }

        val digitKeys = listOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.keyStar to "*", R.id.key0 to "0", R.id.keyHash to "#"
        )

        digitKeys.forEach { (viewId, char) ->
            findViewById<View>(viewId).setOnClickListener {
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0 + (char[0] - '0').coerceIn(0, 9), 120)
                tvDialedDigits.append(char)
            }
        }
    }

    private fun showQuickNoteDialog() {
        val input = EditText(this).apply {
            hint = "Discreet note / reminder..."
            setPadding(40, 30, 40, 30)
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_muted))
        }
        AlertDialog.Builder(this)
            .setTitle("In-Call Note")
            .setView(input)
            .setPositiveButton("Save") { d, _ ->
                d.dismiss()
                Toast.makeText(this, "Note saved to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkIntentTriggers() {
        val extraName = intent.getStringExtra("CALLER_NAME")
        if (!extraName.isNullOrEmpty()) {
            callerName = extraName
            etCallerName.setText(extraName)
        }

        val isAutoAnswer = intent.getBooleanExtra("AUTO_ANSWER", false)
        val isAutoRing = intent.getBooleanExtra("AUTO_RING", false)

        if (isAutoAnswer) {
            startIncomingCall()
            answerCall()
        } else if (isAutoRing) {
            startIncomingCall()
        }
    }

    private fun startDelayCountdown(seconds: Int) {
        layoutCountdownBanner.visibility = View.VISIBLE
        var remaining = seconds
        delayTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {
                tvCountdownNotice.text = "⏳ Triggering call in ${remaining}s... You can lock phone."
                remaining--
            }
            override fun onFinish() {
                layoutCountdownBanner.visibility = View.GONE
                startIncomingCall()
            }
        }.start()
    }

    private fun startIncomingCall() {
        updateLiveClocks()
        layoutSetup.visibility = View.GONE
        layoutActiveCall.visibility = View.GONE
        layoutKeypadOverlay.visibility = View.GONE
        layoutRinging.visibility = View.VISIBLE

        tvIncomingCallerName.text = callerName

        if (isSlideAnswerMode) {
            layoutIncomingButtonsMode.visibility = View.GONE
            layoutIncomingSlideMode.visibility = View.VISIBLE
        } else {
            layoutIncomingButtonsMode.visibility = View.VISIBLE
            layoutIncomingSlideMode.visibility = View.GONE
        }

        // Start Ringtone & Vibration
        if (switchRingtone.isChecked) {
            try {
                ringtone?.play()
            } catch (_: Exception) {}
        }

        if (switchVibration.isChecked) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
                }
            } catch (_: Exception) {}
        }

        // Auto expire ringtone after 45 seconds if unhandled
        ringTimer = object : CountDownTimer(45_000L, 1000L) {
            override fun onTick(ms: Long) {}
            override fun onFinish() {
                declineCall()
            }
        }.start()
    }

    private fun answerCall() {
        stopRingingFeedback()
        isCallActive = true
        elapsedSeconds = 0

        updateLiveClocks()
        layoutRinging.visibility = View.GONE
        layoutSetup.visibility = View.GONE
        layoutActiveCall.visibility = View.VISIBLE

        // Screen 3 Capitalizes Caller Name as in user's image: "Father"
        tvActiveCallerName.text = callerName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        tvFakeCallTimer.text = "00:00"
        tvKeypadDuration.text = "00:00"

        callTimer = object : CountDownTimer(3600_000L, 1000L) {
            override fun onTick(ms: Long) {
                if (!isHolding) {
                    elapsedSeconds++
                    updateTimerDisplay()
                }
            }
            override fun onFinish() {
                endCall()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        val timeStr = String.format(Locale.ROOT, "%02d:%02d", m, s)
        tvFakeCallTimer.text = timeStr
        tvKeypadDuration.text = timeStr
    }

    private fun declineCall() {
        stopRingingFeedback()
        finish()
    }

    private fun endCall() {
        stopRingingFeedback()
        callTimer?.cancel()
        isCallActive = false

        layoutActiveCall.visibility = View.GONE
        layoutKeypadOverlay.visibility = View.GONE
        layoutRinging.visibility = View.GONE
        layoutSetup.visibility = View.VISIBLE

        Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show()
    }

    private fun stopRingingFeedback() {
        ringTimer?.cancel()
        delayTimer?.cancel()
        try {
            if (ringtone?.isPlaying == true) {
                ringtone?.stop()
            }
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingingFeedback()
        callTimer?.cancel()
        toneGenerator?.release()
    }
}
