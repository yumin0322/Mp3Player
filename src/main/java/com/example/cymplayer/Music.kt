package com.example.cymplayer

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.Serializable

data class Music( var id:String="", var title:String?=null, var artist:String?=null, var albumId:String?=null,var duration:Long?=0, var favorite:Int?=0):Serializable{

    //생성자 멤버변수 초기화
    init {
        this.id=id
        this.title=title
        this.artist=artist
        this.albumId=albumId
        this.duration=duration
        this.favorite=favorite
    }

    //엘범의 uri를 가져오는 방법
    //컨텐트리졸버를 이용해서 음악파일 uri정보를 가져오기 위한 방법함수
    fun getAlbumUri(): Uri {
        return  Uri.parse("content://media/external/audio/albumart/"+albumId)
    }

    //음악정보를 가져오기 위한 경로 Uri얻기(음악정보)
    fun getMusicUri():Uri{
        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }

    //해당되는 음악에 비트맵 만들기
    //이미지를 내가 원하는 사이즈로 비트맵 만들어 돌려주기
    //컨텐트리졸버를 이용하여
    fun getAlbumImage(context: Context, albumImageSize:Int): Bitmap? {
        val contentResolver: ContentResolver = context.getContentResolver()
        //앨범경로
        val uri=getAlbumUri()
        //앨범에대한 정보를 저장하는 기능
        val options = BitmapFactory.Options()

        if(uri!=null){
            var parcelFileDescriptor: ParcelFileDescriptor?=null

            try{
                //외부파일에 있는 이미지정보를 가져오기 위한 스트링
                parcelFileDescriptor=contentResolver.openFileDescriptor(uri,"r")
                var bitmap = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor!!.fileDescriptor,null,options)

                //비트맵을 가져와서 사이즈 결정(원본 이미지가 내가 원하는 사이즈와 맞지 않을 경우, 원하는 사이즈로 조정)
                if(bitmap!=null){
                    if(options.outHeight!==albumImageSize || options.outWidth!==albumImageSize){
                        val tempBitmap = Bitmap.createScaledBitmap(bitmap, albumImageSize,albumImageSize,true)
                        bitmap.recycle()
                        bitmap=tempBitmap
                    }
                }
                return bitmap
            }catch (e:Exception){
                e.printStackTrace()
            }finally {
                try{
                    parcelFileDescriptor?.close()
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }//end of if(uri != null)
        return null
    }
}