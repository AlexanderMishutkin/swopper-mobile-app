package com.example.swopper.ui.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.swopper.R
import com.example.swopper.database.*
import com.example.swopper.models.AdvertModel
import com.example.swopper.ui.adverts.AdvertFragment
import com.example.swopper.utils.*
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DatabaseReference
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.item_advert.view.*


class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: FirebaseRecyclerAdapter<AdvertModel, AdvertHolder>
    private lateinit var mRefAdverts: DatabaseReference

    override fun onStart() {
        super.onStart()

        APP_ACTIVITY.mBottomNavigationView.visibility = View.VISIBLE
        APP_ACTIVITY.supportActionBar?.show()
        APP_ACTIVITY.title = "Профиль"

        setHasOptionsMenu(true)

        profile_user_photo.setOnClickListener { changeUserPhoto() }

        initRecyclerView()
    }

    override fun onResume() {
        super.onResume()

        initProfile()
    }

    override fun onPause() {
        super.onPause()
        mAdapter.stopListening()
    }

    class AdvertHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.adverts_image
        val name: TextView = view.adverts_name
        val type: TextView = view.adverts_type
        val location: TextView = view.adverts_location
        val posted: TextView = view.adverts_posted
    }

    private fun initRecyclerView() {
        mRecyclerView = profile_adverts_recycler_view
        mRefAdverts = REF_DATABASE_ROOT.child(NODE_ADVERTS)

        val options = FirebaseRecyclerOptions.Builder<AdvertModel>()
            .setQuery(mRefAdverts.orderByChild("owner").equalTo(CURRENT_UID), AdvertModel::class.java)
            .build()

        mAdapter = object : FirebaseRecyclerAdapter<AdvertModel, AdvertHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvertHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_advert, parent, false)
                return AdvertHolder(view)
            }

            override fun onBindViewHolder(
                holder: AdvertHolder,
                position: Int,
                advert: AdvertModel
            ) {
                holder.photo.setImage(advert.photoUrl, R.drawable.im_default_advert)
                holder.name.text = advert.name
                holder.type.text = advert.type
                holder.location.text = advert.location
                holder.posted.text = advert.posted.toString().asDateTime()

                holder.itemView.setOnClickListener {
                    replaceFragment(AdvertFragment(advert), R.id.profileFrameLayout)
                }
            }
        }
        mRecyclerView.adapter = mAdapter
        mAdapter.startListening()
    }

    private fun initProfile() {
        profile_username.text = USER.username
        profile_user_status.text = USER.status
        profile_user_phone.text = USER.phone
        profile_user_location.text = USER.location
        profile_user_registrated.text =
            if (USER.registrated.toString().isEmpty()) "" else USER.registrated.toString().asDate()
        profile_user_photo.setImage(USER.photoUrl, R.drawable.im_default_user)
    }

    private fun changeUserPhoto() {
        CropImage.activity()
            .setAspectRatio(1, 1)
            .setRequestedSize(250, 250)
            .setCropShape(CropImageView.CropShape.OVAL)
            .start(APP_ACTIVITY, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE
            && resultCode == RESULT_OK && data != null
        ) {
            val uri = CropImage.getActivityResult(data).uri
            val path = REF_STORAGE_ROOT.child(FOLDER_PROFILE_IMAGE).child(CURRENT_UID)
            val ref = REF_DATABASE_ROOT.child(NODE_USERS).child(CURRENT_UID).child(CHILD_PHOTO_URL)
            setImageToStorage(uri, path) {
                getUrlFromStorage(path) {
                    setUrlToDatabase(ref, it) {
                        showToast("Фотография профиля обновлена")
                        profile_user_photo?.setImage(it, R.drawable.im_default_user)
                        USER.photoUrl = it
                    }
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.filters)?.isVisible = false
        menu.findItem(R.id.filters)?.isEnabled = false
    }
}