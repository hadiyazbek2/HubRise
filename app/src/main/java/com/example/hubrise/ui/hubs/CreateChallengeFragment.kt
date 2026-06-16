package com.example.hubrise.ui.hubs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.example.hubrise.R
import com.example.hubrise.data.model.CountConfigInput
import com.example.hubrise.data.model.CreateChallengeRequest
import com.example.hubrise.data.model.ProgressModel
import com.example.hubrise.data.model.StageInput
import com.example.hubrise.data.model.StreakConfigInput
import com.example.hubrise.data.repository.HubRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CreateChallengeFragment : BottomSheetDialogFragment() {

    private val repository = HubRepository()

    private var selectedModel = ProgressModel.COUNT

    private lateinit var chipStage: TextView
    private lateinit var chipCount: TextView
    private lateinit var chipStreak: TextView
    private lateinit var groupStage: View
    private lateinit var groupCount: View
    private lateinit var groupStreak: View
    private lateinit var containerStages: LinearLayout

    private val proofTypes = listOf("any", "photo", "video", "text", "number")
    private val proofTypeLabels = listOf("Any", "Photo", "Video", "Text", "Number")

    companion object {
        const val RESULT_KEY = "create_challenge_result"
        private const val ARG_HUB_ID = "hub_id"
        private const val ARG_CAN_SET_MAIN = "can_set_main"

        fun newInstance(hubId: Int, canSetMain: Boolean): CreateChallengeFragment {
            return CreateChallengeFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HUB_ID, hubId)
                    putBoolean(ARG_CAN_SET_MAIN, canSetMain)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.bottom_sheet_create_challenge, container, false)

    override fun onStart() {
        super.onStart()
        // Force full expansion so the Create button is never hidden behind the
        // collapsed peek height or the keyboard.
        val dialog = dialog as? BottomSheetDialog ?: return
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hubId = arguments?.getInt(ARG_HUB_ID) ?: return
        val canSetMain = arguments?.getBoolean(ARG_CAN_SET_MAIN) ?: false

        val etTitle = view.findViewById<TextInputEditText>(R.id.et_title)
        val etDescription = view.findViewById<TextInputEditText>(R.id.et_description)
        val rowMainSwitch = view.findViewById<View>(R.id.row_main_switch)
        val swMain = view.findViewById<SwitchCompat>(R.id.sw_main)

        chipStage = view.findViewById(R.id.chip_model_stage)
        chipCount = view.findViewById(R.id.chip_model_count)
        chipStreak = view.findViewById(R.id.chip_model_streak)
        groupStage = view.findViewById(R.id.group_stage)
        groupCount = view.findViewById(R.id.group_count)
        groupStreak = view.findViewById(R.id.group_streak)
        containerStages = view.findViewById(R.id.container_stages)

        val etTargetCount = view.findViewById<TextInputEditText>(R.id.et_target_count)
        val etUnitLabel = view.findViewById<TextInputEditText>(R.id.et_unit_label)
        val swCountRequireProof = view.findViewById<SwitchCompat>(R.id.sw_count_require_proof)

        val etTargetDays = view.findViewById<TextInputEditText>(R.id.et_target_days)
        val spinnerFrequency = view.findViewById<Spinner>(R.id.spinner_frequency)
        val etGraceDays = view.findViewById<TextInputEditText>(R.id.et_grace_days)
        val swStreakRequireProof = view.findViewById<SwitchCompat>(R.id.sw_streak_require_proof)

        spinnerFrequency.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Daily", "Weekly")
        )

        rowMainSwitch.visibility = if (canSetMain) View.VISIBLE else View.GONE

        chipStage.setOnClickListener { selectModel(ProgressModel.STAGE) }
        chipCount.setOnClickListener { selectModel(ProgressModel.COUNT) }
        chipStreak.setOnClickListener { selectModel(ProgressModel.STREAK) }
        selectModel(ProgressModel.COUNT)

        view.findViewById<View>(R.id.btn_add_stage).setOnClickListener { addStageRow() }
        addStageRow() // start with one stage row for convenience

        view.findViewById<View>(R.id.btn_create).setOnClickListener {
            val title = etTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val stages: List<StageInput>?
            var countConfig: CountConfigInput? = null
            var streakConfig: StreakConfigInput? = null

            when (selectedModel) {
                ProgressModel.STAGE -> {
                    stages = collectStageInputs()
                    if (stages == null) return@setOnClickListener // validation already toasted
                    if (stages.isEmpty()) {
                        Toast.makeText(requireContext(), "Add at least one stage", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                ProgressModel.STREAK -> {
                    stages = null
                    val targetDays = etTargetDays.text?.toString()?.trim()?.toIntOrNull()
                    if (targetDays == null || targetDays <= 0) {
                        Toast.makeText(requireContext(), "Enter a valid target days", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    streakConfig = StreakConfigInput(
                        targetDays = targetDays,
                        frequency = if (spinnerFrequency.selectedItemPosition == 1) "weekly" else "daily",
                        graceDays = etGraceDays.text?.toString()?.trim()?.toIntOrNull() ?: 0,
                        requireProof = swStreakRequireProof.isChecked,
                    )
                }
                else -> {
                    stages = null
                    val target = etTargetCount.text?.toString()?.trim()?.toDoubleOrNull()
                    if (target == null || target <= 0) {
                        Toast.makeText(requireContext(), "Enter a valid target count", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    countConfig = CountConfigInput(
                        targetCount = target,
                        unitLabel = etUnitLabel.text?.toString()?.trim().orEmpty(),
                        requireProofPerEntry = swCountRequireProof.isChecked,
                    )
                }
            }

            val request = CreateChallengeRequest(
                title = title,
                description = etDescription.text?.toString()?.trim().orEmpty(),
                progressModel = selectedModel,
                isMain = canSetMain && swMain.isChecked,
                stages = stages,
                countConfig = countConfig,
                streakConfig = streakConfig,
            )

            viewLifecycleOwner.lifecycleScope.launch {
                when (repository.createChallenge(hubId, request)) {
                    is HubRepository.Result.Success -> {
                        parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
                        dismiss()
                    }
                    is HubRepository.Result.Error -> {
                        Toast.makeText(requireContext(), "Failed to create challenge", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun selectModel(model: String) {
        selectedModel = model
        groupStage.visibility = if (model == ProgressModel.STAGE) View.VISIBLE else View.GONE
        groupCount.visibility = if (model == ProgressModel.COUNT) View.VISIBLE else View.GONE
        groupStreak.visibility = if (model == ProgressModel.STREAK) View.VISIBLE else View.GONE
        styleChip(chipStage, model == ProgressModel.STAGE)
        styleChip(chipCount, model == ProgressModel.COUNT)
        styleChip(chipStreak, model == ProgressModel.STREAK)
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.btn_primary)
            chip.setTextColor(requireContext().getColor(R.color.white))
        } else {
            chip.setBackgroundResource(R.drawable.step_chip_bg)
            chip.setTextColor(requireContext().getColor(R.color.blue_primary))
        }
    }

    private fun addStageRow() {
        val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_stage_input, containerStages, false)
        val spinner = row.findViewById<Spinner>(R.id.spinner_proof_type)
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, proofTypeLabels)

        row.findViewById<View>(R.id.btn_remove_stage).setOnClickListener {
            containerStages.removeView(row)
            renumberStages()
        }

        containerStages.addView(row)
        renumberStages()
    }

    private fun renumberStages() {
        for (i in 0 until containerStages.childCount) {
            val row = containerStages.getChildAt(i)
            row.findViewById<TextView>(R.id.tv_stage_number).text = "${i + 1}."
        }
    }

    /** Returns null if validation failed (and shows a toast), or the list of stage inputs otherwise. */
    private fun collectStageInputs(): List<StageInput>? {
        val stages = mutableListOf<StageInput>()
        for (i in 0 until containerStages.childCount) {
            val row = containerStages.getChildAt(i)
            val title = row.findViewById<TextInputEditText>(R.id.et_stage_title).text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Stage ${i + 1} requires a title", Toast.LENGTH_SHORT).show()
                return null
            }
            val proofType = proofTypes[row.findViewById<Spinner>(R.id.spinner_proof_type).selectedItemPosition]
            stages.add(StageInput(title = title, proofType = proofType))
        }
        return stages
    }
}
