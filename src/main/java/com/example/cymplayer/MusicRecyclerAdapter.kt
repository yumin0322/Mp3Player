package com.example.cymplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.cymplayer.databinding.ItemRecyclerBinding

class MusicRecyclerAdapter(val context: Context, val musicList: MutableList<Music>):
    RecyclerView.Adapter<MusicRecyclerAdapter.ViewHolder>() {
    val mainActivity: MainActivity = (context as MainActivity)
    val dbHelper:DBHelper by lazy { DBHelper(context, MainActivity.DB_NAME, MainActivity.VERSION) }

    val ALBUM_IMAGE_SIZE = 150

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicRecyclerAdapter.ViewHolder {
        val binding = ItemRecyclerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicRecyclerAdapter.ViewHolder, position: Int) {
        val binding = holder.binding
        val music =musicList?.get(position)


        binding.tvSinger.text = musicList[holder.adapterPosition].artist
        binding.tvTitle.text = musicList[holder.adapterPosition].title


        when(musicList[holder.adapterPosition].favorite){
            0 -> {
                holder.binding.ivHeartP.setImageResource(R.drawable.favorite_border_24)
            }

            1 -> {
                holder.binding.ivHeartP.setImageResource(R.drawable.favorite_24)
            }
        }
        val bitmap: Bitmap? = musicList[holder.adapterPosition].getAlbumImage(context, ALBUM_IMAGE_SIZE)
        if (bitmap != null){
            holder.binding.ivMusic.setImageBitmap(bitmap)
        } else {
            holder.binding.ivMusic.setImageResource(R.drawable.music_note_24)
        }


        binding.root.setOnClickListener {
            //액티비티로 음악정보를 넘겨서 음악을 재생해주는 엑티비티 설계
            //intent->Serializable
            val intent= Intent(binding.root.context, PlaymusicActivity::class.java)
            intent.putExtra("music",music)
            binding.root.context.startActivities(arrayOf(intent))

        }

        binding.ivHeartP.setOnClickListener {
            when(musicList[position].favorite){
                0 -> {
                    musicList[position].favorite = 1

                    if (dbHelper.updateFavorite(musicList[position])){
                        Toast.makeText(context, "Successfully added to your favorites", Toast.LENGTH_SHORT).show()
                    }

                    notifyItemChanged(position)
                }

                1 -> {
                    musicList[position].favorite = 0

                    if (dbHelper.updateFavorite(musicList[position])){
                        Toast.makeText(context, "Removed from your favorites", Toast.LENGTH_SHORT).show()
                        if (mainActivity.typeOfList == Type.FAVORITE){
                            Log.d("Log_debug", "list type = ${mainActivity.typeOfList}")
                            musicList.remove(musicList[position])
                        }
                    }
                    notifyDataSetChanged()
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    inner class ViewHolder(val binding: ItemRecyclerBinding): RecyclerView.ViewHolder(binding.root)

}