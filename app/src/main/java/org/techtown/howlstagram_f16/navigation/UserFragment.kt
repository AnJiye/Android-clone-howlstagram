package org.techtown.howlstagram_f16.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.circleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import org.techtown.howlstagram_f16.R
import org.techtown.howlstagram_f16.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.fragment_user.view.*
import org.techtown.howlstagram_f16.LoginActivity
import org.techtown.howlstagram_f16.MainActivity
import org.techtown.howlstagram_f16.navigation.model.FollowDTO

class UserFragment : Fragment() {
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    // 현재 아이디가 내 아이디인지 다른 사람 아이디인지 알아보기 위한 변수
    var currentUserid : String? = null
    // static
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        // uid 값을 이전 화면에서 넘어온 값으로 세팅해주고 나머지 변수들을 초기화
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserid = auth?.currentUser?.uid

        if (uid == currentUserid) {
            // MyPage - 로그아웃
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
            // OtherUserPage
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            mainactivity?.toolbar_btn_back?.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            // 숨기기
            mainactivity?.toolbar_title_image?.visibility = View.GONE
            // 보이기
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.toolbar_btn_back.visibility = View.VISIBLE

            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }

        // adapter를 달아주고 한 줄에 3개씩 뜰 수 있도록 3을 넘겨줌
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(requireActivity(), 3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    // 팔로우 count 변화
    fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            var followDTO = value.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null) {
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null) {
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                if (followDTO?.followers?.containsKey(currentUserid!!)) {
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                } else {
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    if(uid != currentUserid) {
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }

    }

    fun requestFollow() {
        // Save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)) {
                // It remove following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followers?.remove(uid)
            } else {
                // It add following third person when a third person do not follow me
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followers[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        // Save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserid)) {
                // It cancel my follower when I follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserid!!)
            } else {
                // It add my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserid!!] = true
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    // 올린 이미지를 다운로드받는 함수
    fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { value, error ->
            // 사진이 실시간으로 변환된 것을 체크하기 위해서 snapshot으로 받아옴
            if(value == null) return@addSnapshotListener
            if(value.data != null) {
                var url = value?.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions.circleCropTransform()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }

    // detail에서도 사용했던 RecyclerView를 위한 부분
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()

        init {
            // 사용자가 올린 이미지만 가지고 올 수 있도록 쿼리를 만들어 줌
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestore ->
                // Sometimes, This code return null of querySnapshot when it signout
                // querySnapshot이 null이면 종료, 그렇지 않으면 for 문에서 데이터를 받아와서 contentDTO에 넣어줌
                if (querySnapshot == null) return@addSnapshotListener

                // Get data
                for(snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                // POST 개수를 넣어줌
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                // 새로고침
                notifyDataSetChanged()
            }
        }
        // width를 화면의 1/3만 가져와서 정사각형을 만든 후 return
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        }

        // 데이터를 매핑하는 부분
        // ImageView를 불러오고 Glide로 이미지를 다운로드
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}