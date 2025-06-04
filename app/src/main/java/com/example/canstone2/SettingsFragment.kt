package com.example.canstone2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var phoneListContainer: LinearLayout
    private lateinit var addPhoneNumber: TextView
    private lateinit var inputName: EditText
    private lateinit var inputPhone: EditText
    private lateinit var inputCar: EditText
    private lateinit var editProfile: TextView
    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragments_settings, container, false)

        phoneListContainer = view.findViewById(R.id.phoneListContainer)
        addPhoneNumber = view.findViewById(R.id.addPhoneNumber)
        inputName = view.findViewById(R.id.inputName)
        inputPhone = view.findViewById(R.id.inputPhone)
        inputCar = view.findViewById(R.id.inputCar)
        editProfile = view.findViewById(R.id.editProfile)

        editProfile.setOnClickListener {
            isEditing = !isEditing
            setEditMode(inputName, isEditing)
            setEditMode(inputPhone, isEditing)
            setEditMode(inputCar, isEditing)

            if (isEditing) {
                editProfile.text = "저장하기"
                inputName.requestFocus()
            } else {
                editProfile.text = "수정하기"
                Toast.makeText(requireContext(), "정보가 저장되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        addPhoneNumber.setOnClickListener {
            addNewPhoneNumber("추가된 번호")
        }

        return view
    }

    private fun setEditMode(editText: EditText, enable: Boolean) {
        editText.isEnabled = enable
        editText.isFocusable = enable
        editText.isFocusableInTouchMode = enable
    }

    private fun addNewPhoneNumber(number: String) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
            setBackgroundResource(R.drawable.phone_item_background)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val numberInput = EditText(context).apply {
            setText(number)
            textSize = 16f
            isEnabled = false
            isFocusable = false
            isFocusableInTouchMode = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val editIcon = ImageView(context)
        var isEditingNumber = false

        editIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_edit, null))
        editIcon.setPadding(16, 0, 16, 0)

        editIcon.setOnClickListener {
            isEditingNumber = !isEditingNumber
            if (isEditingNumber) {
                setEditMode(numberInput, true)
                numberInput.requestFocus()
                editIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_save, null))
            } else {
                setEditMode(numberInput, false)
                editIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_edit, null))
                Toast.makeText(context, "번호가 저장되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        val deleteIcon = ImageView(context).apply {
            setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, null))
            setPadding(16, 0, 0, 0)
            setOnClickListener {
                phoneListContainer.removeView(container)
            }
        }

        container.addView(numberInput)
        container.addView(editIcon)
        container.addView(deleteIcon)
        phoneListContainer.addView(container)
    }
}
