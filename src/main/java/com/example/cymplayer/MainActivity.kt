package com.example.cymplayer

import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cymplayer.DBHelper.Companion.TABLE_NAME
import com.example.cymplayer.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Job

class MainActivity  : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object{
        val DB_NAME = "musicDB"
        val VERSION = 1
    }

    lateinit var binding: ActivityMainBinding

    private lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>

    lateinit var toggle: ActionBarDrawerToggle

    val permission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    val REQUEST_READ = 200

    val dbHelper:DBHelper by lazy { DBHelper(this, MainActivity.DB_NAME, MainActivity.VERSION) }

    lateinit var adapter: MusicRecyclerAdapter

    var playMusicList: MutableList<Music> = mutableListOf()

    var typeOfList = Type.ALL

    var isPlaying = false

    //미디어플레이어
    private var mediaPlayer: MediaPlayer? = null

    //음악정보
    private var currentMusic: Music? = null

    //Coroutine scope
    private var playerJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        //승인이 되어있으면 음악파일을 가져고오고, 승인이 안됏으면 재요청
        if(isPemitted()==true){
            //실행하면됨. 외부파일을 가져와서 , 컬렉션프레임워크에 저장하고, 어뎁터 불러오기

            startProcess()

        }else{
            //승인요청 다시함
            //요청이 승인되면  콜백함수로 승인
            ActivityCompat.requestPermissions(this, permission,REQUEST_READ)
        }
    }
    override fun onBackPressed() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }
        super.onBackPressed()
    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menuAll -> {
                binding.collapssingToolbarLayout.title = "Music"
                getFilteredMusicList(null, Type.ALL)
                binding.drawerLayout.closeDrawers()
            }
            R.id.menuHeart -> {
                binding.collapssingToolbarLayout.title = "Favorite"
                getFilteredMusicList(null, Type.FAVORITE)
                binding.drawerLayout.closeDrawers()
            }
        }

        return true
    }

    //승인요청햇을때 승인결과에 대한 콜백함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==REQUEST_READ){
            if (grantResults[0]== PackageManager.PERMISSION_GRANTED){
                //실행하면됨, 외부파일을 가져와서, 컬렉션프레임워크 저장, 어뎁터 불러오기
                startProcess()
            }else{
                Toast.makeText(this,"권한요청 승인 앱이 실행가능함", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    //외부파일로부터 모든 음악정보를 가져온다
    private fun startProcess() {
        //데이터베이스 기능을 정한다

        //1.음악정보를 가져와야한다
        var musicList:MutableList<Music>? = getMusicList()
        //music 테이블에서 자료를 가져온다(있으면-> 리사클러뷰 보여주고 없으면
        // ->getMusicList 가져오고-> music 테이블에 저장 -> 리사이클러뷰에 보여주기)
        musicList = dbHelper.seletMusicAll()

        if(musicList==null||musicList.size <=0){
            //getMusicList가져오고
            musicList=getMusicList()
            //music 테이블에 모두 저장하고
            for(i in 0..(musicList!!.size-1)){
                val music=musicList.get(i)
                if(dbHelper.insertMusic(music)==false){
                    Log.d("kin","삽입오류")
                }
            }
        }

        val musicRecyclerAdapter = MusicRecyclerAdapter(this,musicList)
        binding.recyclerView.adapter = musicRecyclerAdapter
        binding.recyclerView.layoutManager= LinearLayoutManager(this)

        //2. 데이터 베이스 저장(반드시 중복 저장) id:primary key

        //3. 어뎁터를 만들고, MutableList제공
        binding.recyclerView.adapter=MusicRecyclerAdapter(this,musicList)

        //4.화면에 출력
        binding.recyclerView.layoutManager=LinearLayoutManager(this)

        //5. 꾸밀거
    }

    private fun getMusicList(): MutableList<Music>? {
        //Mp3 외부파일에 음악정보주소
        val listUri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        //2.요청해야될 음원정보 컬럼들
        val proj=arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION

        )

        //3.컨텐트 리졸버 쿼리에URi 요청 음원정보칼럼 요구하고 결과값을  cursor 반환받음
        val cursor = contentResolver.query(listUri,proj,null,null,null)
        //Music:mp3정보를 5가지 기억하고, 파일경로, 이미지경로,내가 원하는 이미지 사이즈(bitmap)
        val musicList: MutableList<Music>?= mutableListOf<Music>()
        while (cursor?.moveToNext()==true){
            val id = cursor.getString(0)
            val title = cursor.getString(1).replace("'","")
            val artist = cursor.getString(2).replace("'","")
            val albumId = cursor.getString(3)
            val duration = cursor.getLong(4)

            val music=Music(id,title,artist,albumId,duration,0)
            musicList?.add(music)
        }
        return musicList
    }

    //외부파일 읽기 승인요청
    fun isPemitted():Boolean{
        if(ContextCompat.checkSelfPermission(this, permission[0])!=PackageManager.PERMISSION_GRANTED){
            return false
        }else{
            return true
        }
    }
    fun getFilteredMusicList(filter:String?, type: Type){
        playMusicList=dbHelper.selectFilter(filter,type)!!
        binding.recyclerView.adapter=MusicRecyclerAdapter(this,playMusicList)
        typeOfList=type
    }
}

