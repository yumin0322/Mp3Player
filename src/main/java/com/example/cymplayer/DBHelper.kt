package com.example.cymplayer

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DBHelper (context: Context, dbName: String, version: Int):
    SQLiteOpenHelper(context,dbName,null,version) {
    //불안하면 상수화로 진행해서 해도 된다.
    companion object{
        val TABLE_NAME = "musicTBL"
    }
    //테이블설계
    override fun onCreate(db: SQLiteDatabase?) {

        val createQuery = "create table ${TABLE_NAME}(id TEXT primary key, title TEXT, artist TEXT, albumId TEXT, duration INTEGER)"
        db?.execSQL(createQuery)

    }
    //테이블제거
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //테이블제거
        val dropQuery = "drop table ${TABLE_NAME}"
        db?.execSQL(dropQuery)
        //onCreate()
    }

    //삽입 : insert into 테이블명(~) values(~,~,~)
    fun insertMusic(music: Music): Boolean{
        var insertFlag = false
        val insertQuery = "insert into $TABLE_NAME(id, title, artist, albumId, duration) " +
                "values('${music.id}','${music.title}','${music.artist}','${music.albumId}',${music.duration})"
        //db는 SQLiteDatabase 가져오는 방법은 2가지가 있는데 : writableDatabase, readableDatabase
        var db = this.writableDatabase
        try {
            db.execSQL(insertQuery)
            insertFlag = true
        }catch (e: SQLException){
            Log.d("sphia",e.toString())
        }finally {
            db.close()
        }

        return insertFlag
    }

    //선택 : 모든것을 선택
    fun  seletMusicAll(): MutableList<Music>? {
        var musicList: MutableList<Music>? = mutableListOf<Music>()

        val selectQuery = "select * from $TABLE_NAME"

        val db = this.readableDatabase
        var cursor: Cursor? = null

        try{
            cursor = db.rawQuery(selectQuery, null)
            if (cursor.count > 0) {
                while(cursor.moveToNext()){
                    val id = cursor.getString(0) ////cursor.getColumnIndex("id")
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val favorite=cursor.getInt(5)
                    val music = Music(id, title, artist, albumId, duration,favorite)
                    musicList?.add(music)
                }
            } else {
                musicList = null
            }
        }catch (e: Exception){
            Log.d("sphia",e.toString())
            musicList = null
        }finally {
            cursor?.close()
            db.close()
        }
        return musicList
    }

    // 원하는 내용을 추가하면 된다.

    //선택: 조건에 맞는 선택
    fun selectMusic(id: String): Music? {
        var music:Music?=null

        val selectQuery = "select * from $TABLE_NAME where id = '${id}'"
        val db = this.readableDatabase

        var cursor: Cursor?=null

        try{
            cursor=db.rawQuery(selectQuery, null)
            if(cursor.count>0){
                if(cursor.moveToFirst()){
                    val id = cursor.getString(0) ////cursor.getColumnIndex("id")
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val favorite=cursor.getInt(5)
                    val music = Music(id, title, artist, albumId, duration,favorite)
                }
            }else{

            }
        }catch (e:java.lang.Exception){
            e.printStackTrace()
            music=null
        }finally {
            cursor?.close()
            db.close()
        }
        return music
    }
    fun selectFilter(filter: String?, type: com.example.cymplayer.Type): MutableList<Music> {
        var musicList: MutableList<Music> = mutableListOf<Music>()
        val db = this.readableDatabase

        var selectQuery: String? = null

        selectQuery = when(type){
            com.example.cymplayer.Type.ALL -> """
            select * from musicTBL
        """.trimIndent()

            com.example.cymplayer.Type.FAVORITE -> """
                    select * from musicTBL where favorite = 1
                """.trimIndent()
        }

        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ////cursor.getColumnIndex("id")
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val favorite=cursor.getInt(5)
                    musicList?.add(Music(id, title, artist, albumId, duration,favorite))
                }
            }
        } catch (e: Exception){
            Log.d("Log_debug", "${e.printStackTrace()}")
        } finally {
            cursor?.close()
            db.close()
        }

        return musicList
    }

    fun updateFavorite(music: Music): Boolean{
        var flag = false
        val db = this.writableDatabase

        var updateQuery: String = """
            update musicTBL set favorite = '${music.favorite}' where id = '${music.id}'
        """

        try {
            db.execSQL(updateQuery)
            flag = true
        } catch (e: SQLException){
            Log.d("Log_debug", "${e.printStackTrace()}")
        }

        return flag
    }
}

