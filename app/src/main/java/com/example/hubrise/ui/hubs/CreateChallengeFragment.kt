package com.example.hubrise.ui.hubs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.example.hubrise.R
import com.example.hubrise.data.model.ChallengeTemplate
import com.example.hubrise.data.model.CountConfigInput
import com.example.hubrise.data.model.CreateChallengeRequest
import com.example.hubrise.data.model.ProgressModel
import com.example.hubrise.data.model.StageInput
import com.example.hubrise.data.model.StreakConfigInput
import com.example.hubrise.data.model.TemplateCategory
import com.example.hubrise.data.repository.HubRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CreateChallengeFragment : BottomSheetDialogFragment() {

    private val repository = HubRepository()

    private var selectedModel = ProgressModel.COUNT
    private var selectedTemplateId: Int? = null

    private lateinit var chipStage: TextView
    private lateinit var chipCount: TextView
    private lateinit var chipStreak: TextView
    private lateinit var groupStage: View
    private lateinit var groupCount: View
    private lateinit var groupStreak: View
    private lateinit var containerStages: LinearLayout
    private lateinit var tvSelectedTemplate: TextView

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etTargetCount: TextInputEditText
    private lateinit var etUnitLabel: TextInputEditText
    private lateinit var etTargetDays: TextInputEditText
    private lateinit var spinnerFrequency: Spinner
    private lateinit var etGraceDays: TextInputEditText

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

        etTitle = view.findViewById(R.id.et_title)
        etDescription = view.findViewById(R.id.et_description)
        val rowMainSwitch = view.findViewById<View>(R.id.row_main_switch)
        val swMain = view.findViewById<SwitchCompat>(R.id.sw_main)

        chipStage = view.findViewById(R.id.chip_model_stage)
        chipCount = view.findViewById(R.id.chip_model_count)
        chipStreak = view.findViewById(R.id.chip_model_streak)
        groupStage = view.findViewById(R.id.group_stage)
        groupCount = view.findViewById(R.id.group_count)
        groupStreak = view.findViewById(R.id.group_streak)
        containerStages = view.findViewById(R.id.container_stages)
        tvSelectedTemplate = view.findViewById(R.id.tv_selected_template)

        etTargetCount = view.findViewById(R.id.et_target_count)
        etUnitLabel = view.findViewById(R.id.et_unit_label)
        val swCountRequireProof = view.findViewById<SwitchCompat>(R.id.sw_count_require_proof)
        val swCountCumulative = view.findViewById<SwitchCompat>(R.id.sw_count_cumulative)

        etTargetDays = view.findViewById(R.id.et_target_days)
        spinnerFrequency = view.findViewById(R.id.spinner_frequency)
        etGraceDays = view.findViewById(R.id.et_grace_days)
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

        view.findViewById<View>(R.id.row_use_template).setOnClickListener { showTemplatePicker() }

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
                        isCumulative = swCountCumulative.isChecked,
                    )
                }
            }

            val request = CreateChallengeRequest(
                title = title,
                description = etDescription.text?.toString()?.trim().orEmpty(),
                progressModel = selectedModel,
                isMain = canSetMain && swMain.isChecked,
                templateId = selectedTemplateId,
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

    private fun addStageRow(prefillTitle: String = "", prefillProofType: String = "any") {
        val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_stage_input, containerStages, false)
        val spinner = row.findViewById<Spinner>(R.id.spinner_proof_type)
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, proofTypeLabels)
        spinner.setSelection(proofTypes.indexOf(prefillProofType).coerceAtLeast(0))

        if (prefillTitle.isNotEmpty()) {
            row.findViewById<TextInputEditText>(R.id.et_stage_title).setText(prefillTitle)
        }

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

    private fun showTemplatePicker() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_template_picker, null)
        dialog.setContentView(sheetView)

        val container = sheetView.findViewById<LinearLayout>(R.id.ll_templates_container)
        val pbLoading = sheetView.findViewById<ProgressBar>(R.id.pb_loading)

        sheetView.findViewById<View>(R.id.row_scratch).setOnClickListener {
            selectedTemplateId = null
            tvSelectedTemplate.text = ""
            dialog.dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            when (val r = repository.getTemplates()) {
                is HubRepository.Result.Success -> {
                    pbLoading.visibility = View.GONE
                    r.data.forEach { template ->
                        val row = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_template_picker_row, container, false)
                        row.findViewById<TextView>(R.id.tv_template_icon).text = when (template.progressModel) {
                            ProgressModel.STAGE -> "🏆"
                            ProgressModel.STREAK -> "🔥"
                            else -> "📊"
                        }
                        row.findViewById<TextView>(R.id.tv_template_name).text = template.name
                        row.findViewById<TextView>(R.id.tv_template_meta).text =
                            "${TemplateCategory.label(template.category)} · Used ${template.useCount} times"
                        row.findViewById<TextView>(R.id.tv_template_description).text = template.description
                        row.setOnClickListener {
                            applyTemplate(template)
                            dialog.dismiss()
                        }
                        container.addView(row)
                    }
                }
                is HubRepository.Result.Error -> {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load templates: ${r.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.show()
    }

    private fun applyTemplate(template: ChallengeTemplate) {
        selectedTemplateId = template.id
        tvSelectedTemplate.text = template.name

        etTitle.setText(template.name)
        etDescription.setText(template.description)
        selectModel(template.progressModel)

        when (template.progressModel) {
            ProgressModel.STAGE -> {
                containerStages.removeAllViews()
                template.stages.sortedBy { it.orderIndex }.forEach { stage ->
                    addStageRow(stage.title, stage.proofType)
                }
                if (containerStages.childCount == 0) addStageRow()
            }
            ProgressModel.COUNT -> {
                etTargetCount.setText(template.countTarget?.let { formatNumber(it) } ?: "")
                etUnitLabel.setText(template.countUnitLabel)
            }
            ProgressModel.STREAK -> {
                etTargetDays.setText(template.streakTargetDays?.toString() ?: "")
                spinnerFrequency.setSelection(if (template.streakFrequency == "weekly") 1 else 0)
                etGraceDays.setText(template.streakGraceDays.toString())
            }
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
