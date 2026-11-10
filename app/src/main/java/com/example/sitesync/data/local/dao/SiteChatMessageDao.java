package com.example.sitesync.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.sitesync.data.local.entities.SiteChatMessageEntity;

import java.util.List;

@Dao
public interface SiteChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SiteChatMessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SiteChatMessageEntity> messages);

    @Query("SELECT * FROM site_chat_messages WHERE projectCode = :projectCode ORDER BY timestamp ASC")
    LiveData<List<SiteChatMessageEntity>> getMessagesForProjectLive(String projectCode);

    @Query("SELECT * FROM site_chat_messages WHERE projectCode = :projectCode ORDER BY timestamp ASC")
    List<SiteChatMessageEntity> getMessagesForProject(String projectCode);

    @Query("SELECT * FROM site_chat_messages WHERE projectCode = :projectCode AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    List<SiteChatMessageEntity> getMessagesSince(String projectCode, long sinceTimestamp);

    @Query("DELETE FROM site_chat_messages WHERE projectCode = :projectCode")
    void clearProjectMessages(String projectCode);
}
