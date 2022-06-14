package com.example.cymplayer

import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.example.cymplayer.databinding.ActivityPlaymusicBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class PlaymusicActivity  : AppCompatActivity() {
    lateinit var binding: ActivityPlaymusicBinding
    //1. 뮤직플레이어 변수 선언
    private var mediaPlayer: MediaPlayer?=null
    //음악정보객체 변수
    private  var music:Music?=null
    //음악 앨범이미지 사이즈
    private  val ALBUM_IMAGE_SIZE=150

    //코루틴 스코프 launch
    private var playerJob: Job?=null
    val dbHelper:DBHelper by lazy { DBHelper(this, MainActivity.DB_NAME, MainActivity.VERSION)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlaymusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        music=intent.getSerializableExtra("music")as Music

        if(music!=null){
            //뷰셋팅
            binding.tvTitleP.text=music?.title
            binding.tvSingerP.text=music?.artist
            binding.tvStart.text="00:00"
            binding.tvEnd.text= SimpleDateFormat("mm:ss").format(music?.duration)
            val bitmap: Bitmap?=music?.getAlbumImage(this, ALBUM_IMAGE_SIZE)
            if(bitmap!=null){
                binding.ivArtP.setImageBitmap(bitmap)
            }else{
                binding.ivArtP.setImageResource(R.drawable.music_note_24)
            }

            //음원실행 생성및 재생
            mediaPlayer=MediaPlayer.create(this, music?.getMusicUri())
            binding.seekBar.max=music?.duration!!.toInt()

            //시크바 이벤트 설정해서 노래와 같이 동기화 처리 된다
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                //오버라이딩

                //시크바를 터치하고 이동할때 발생되는 이벤트 fromUser : 유저에 의한 터치유무
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if(fromUser){
                        //미디어 플레이어에 음악위치를 시크바에서 값을 가져와 셋팅
                        mediaPlayer?.seekTo(progress)
                    }
                }
                //시크바를 터치하는 순간 이벤트 발생
                override fun onStartTrackingTouch(p0: SeekBar?) {

                }
                //시크바를 터치하고 떼어놓는 순간
                override fun onStopTrackingTouch(p0: SeekBar?) {

                }

            })
        }

    }

    fun onClickView(view: View?){
        when(view?.id){
            R.id.ivList ->{  //음악정지, 코루틴취소 음악객체 해제, 음악객체에 null
                mediaPlayer?.stop()
                playerJob?.cancel()
                finish()
            }
            R.id.ivPlay ->{
                if(mediaPlayer?.isPlaying==true){
                    mediaPlayer?.pause()
                    binding.ivPlay.setImageResource(R.drawable.play_24)
                    var currentPosition = mediaPlayer?.currentPosition!!
                    binding.seekBar.progress=currentPosition
                }else{
                    mediaPlayer?.start()
                    binding.ivPlay.setImageResource(R.drawable.paused_24)

                    //음악재생시켜야함
                    //시크바하고 시작 시간 진행을 코루틴으로 진행
                    //중요 : 사용자가 만든 스레드에서 화면에 뷰값을 변경하게 되면 문제가 발생한다
                    //해결방법:스레드안에서 뷰에 잇는값을 변경하고 싶으면 runOnUriThread{ }
                    val backgroundScope = CoroutineScope(Dispatchers.Default+ Job())
                    playerJob = backgroundScope.launch {
                        //음악 진행사항을 가져와서 시크바와 시작진행사항값을 변화 시켜줘야한
                        while (mediaPlayer?.isPlaying==true){
                            //노래가 진행하면서 진행위치값을 시크바 위치에 적용한다
                            runOnUiThread{
                                var currentPosition = mediaPlayer?.currentPosition!!
                                binding.seekBar.progress=currentPosition
                                binding.tvStart.text=SimpleDateFormat("mm:ss").format(currentPosition)
                            }
                            try {
                                delay(500)
                            }catch (e:Exception){
                                e.printStackTrace()
                            }
                        }//end of while

                        runOnUiThread {
                            if(mediaPlayer!!.currentPosition>=(binding.seekBar.max-1000)){
                                binding.seekBar.progress=0
                                binding.tvStart.text="00:00"
                            }
                            binding.ivPlay.setImageResource(R.drawable.play_24)
                        }
                        binding.seekBar.progress=0
                    }//end of playJob
                }
            }
            R.id.ivStop ->{
                mediaPlayer?.stop()
                playerJob?.cancel()
                mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())
                binding.seekBar.progress=0
                binding.tvStart.text="00:00"
                binding.ivPlay.setImageResource(R.drawable.play_24)
            }


        }
    }
}