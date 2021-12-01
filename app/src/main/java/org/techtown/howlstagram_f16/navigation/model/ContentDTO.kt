package org.techtown.howlstagram_f16.navigation.model

data class ContentDTO(var explain : String? = null,    // 컨텐츠의 설명 관리
                      var imageUrl : String? = null,   // 이미지 주소 관리
                      var uid : String? = null,        // 어느 유저가 올렸는지 관리
                      var userId : String? = null,    // 올린 유저의 이미지를 관리
                      var timestamp : Long? = null,   // 몇시 몇분에 올렸는지
                      var favoriteCount : Int = 0,    // 좋아요 개수
                      var favorites : Map<String,Boolean> = HashMap()) {    // 중복 좋아요 방지, 누가 좋아요를 눌렀는지
    data class Comment(var uid : String? = null,        // 누가 댓글을 남겼는지
                       var userId: String? = null,      // 댓글을 남긴 유저의 아이디
                       var comment : String? = null,    // 뭐라고 남겼는지
                       var timestamp: Long? = null)     // 몇시 몇분에 올렸는지
}